package io.jawg.osmcontributor.ui.managers.loadPoi;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import io.jawg.osmcontributor.BuildConfig;
import io.jawg.osmcontributor.database.dao.MapAreaDao;
import io.jawg.osmcontributor.database.dao.PoiDao;
import io.jawg.osmcontributor.database.dao.PoiTypeDao;
import io.jawg.osmcontributor.model.entities.MapArea;
import io.jawg.osmcontributor.model.entities.Note;
import io.jawg.osmcontributor.model.entities.Poi;
import io.jawg.osmcontributor.model.entities.PoiType;
import io.jawg.osmcontributor.rest.Backend;
import io.jawg.osmcontributor.rest.NetworkException;
import io.jawg.osmcontributor.rest.dtos.osm.OsmDto;
import io.jawg.osmcontributor.rest.dtos.osm.PoiDto;
import io.jawg.osmcontributor.rest.mappers.PoiMapper;
import io.jawg.osmcontributor.ui.utils.BooleanHolder;
import io.jawg.osmcontributor.utils.Box;
import rx.Subscriber;
import timber.log.Timber;

import static io.jawg.osmcontributor.ui.managers.loadPoi.AreasUtils.computeMapAreaOfBox;
import static io.jawg.osmcontributor.ui.managers.loadPoi.AreasUtils.getIds;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.FINISH;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.LOADING_FROM_SERVER;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.MAPPING_POIS;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.NETWORK_ERROR;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.OUT_DATED_DATA;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.POI_LOADING;
import static io.jawg.osmcontributor.ui.managers.loadPoi.PoiLoadingProgress.LoadingStatus.TOO_MANY_POIS;


/**
 * {@link PoiLoader} for retrieving poi data.
 */
public class PoiLoader {

    public static final int POI_PAGE_MAPPING = 5;
    private final PoiDao poiDao;
    private final MapAreaDao mapAreaDao;
    private final Backend backend;
    private Subscriber<? super PoiLoadingProgress> subscriber;
    private Box box;
    private boolean refreshData;
    private PoiMapper poiMapper;
    private BooleanHolder mustBeKilled;
    List<PoiType> availableTypes;
    PoiTypeDao poiTypeDao;
    List<OsmDto> osmDtos;
    List<PoiDto> nodeDtos = new ArrayList<>();

    //progress
    private PoiLoadingProgress.LoadingStatus loadingStatus;
    private boolean dataNeedRefresh;
    private long totalsElements = 0L;
    private long loadedElements = 0L;
    private long totalAreasToLoad = 0L;
    private long totalAreasLoaded = 0L;
    private List<Poi> pois = new ArrayList<>();
    private List<Note> notes = new ArrayList<>();


    public PoiLoader(PoiDao poiDao, Backend backend, MapAreaDao mapAreaDao, Subscriber<? super PoiLoadingProgress> subscriber, BooleanHolder mustBeKilled, PoiMapper poiMapper, PoiTypeDao poiTypeDao) {
        this.backend = backend;
        this.poiDao = poiDao;
        this.mapAreaDao = mapAreaDao;
        this.subscriber = subscriber;
        this.mustBeKilled = mustBeKilled;
        this.poiMapper = poiMapper;
        this.poiTypeDao = poiTypeDao;
    }

    public void init(final Box box, final boolean refreshData) {
        this.box = box;
        this.refreshData = refreshData;
        availableTypes = poiTypeDao.queryForAll();

    }

    public void getPoiFromBox() {
        List<MapArea> areasNeeded = computeMapAreaOfBox(box);
        if (!areasNeeded.isEmpty()) {
            getPois(areasNeeded);
        }
        loadingStatus = FINISH;
        publishProgress();
        Timber.d(" finished pis ");
        subscriber.onCompleted();

    }

    private void getPois(List<MapArea> areasNeeded) {
        Timber.d("\n--------- Loading Pois already present in BDD ---------\n");
        boolean succeed = loadPoisFromDB();
        if (!succeed) {
            return;
        }

        Timber.d("\n--------- Handling areas ---------\n");
        boolean newAreasLoaded = handleAreas(areasNeeded);
        Timber.d("\n--------- Done Handling areas ---------\n");

        if (newAreasLoaded) {
            Timber.d("\n--------- Loading pois from BDD the just have been refreshed  ---------\n");
            loadPoisFromDB();
        }
    }

    private boolean handleAreas(List<MapArea> areasNeeded) {
        boolean newAreasLoaded = false;
        List<MapArea> localAreas = mapAreaDao.queryForIds(getIds(areasNeeded));

        if (refreshData || areasNeeded.size() != localAreas.size()) {
            // some areas are not loaded in ou BD
            // we call the backend to received the data
            //we notify the subscriber that we have some loading to do

            loadingStatus = LOADING_FROM_SERVER;
            totalAreasToLoad = areasNeeded.size() - localAreas.size();
            totalAreasLoaded = 0L;
            totalsElements = 0L;
            loadedElements = 0L;
            publishProgress();

            loadMissingMapAreas(areasNeeded, localAreas);
            newAreasLoaded = true;
        }

        if (!refreshData) {
            // find outdated areas
            for (MapArea mapArea : localAreas) {
                LocalDateTime lastUpate = new LocalDateTime(mapArea.getUpdateDate());
                //we check if the data is outDated and ask for Update
                boolean outDatedData = LocalDateTime.now().isAfter(lastUpate.plusMonths(1));
                if (outDatedData) {
                    loadingStatus = OUT_DATED_DATA;
                    dataNeedRefresh = true;
                    publishProgress();
                }
            }
        }

        return newAreasLoaded;
    }

    private boolean loadPoisFromDB() {

        Long count = poiDao.countForAllInRect(box);

        if (count > BuildConfig.MAX_POIS_ON_MAP) {
            loadingStatus = TOO_MANY_POIS;
            totalsElements = count;
            publishProgress();
            Timber.d(" too many Pois to display %d", count);
            return false;
        }

        pois.clear();
        pois = poiDao.queryForAllInRect(box);
        loadingStatus = POI_LOADING;
        totalsElements = count;
        publishProgress();
        return true;
    }


    private void loadMissingMapAreas(final List<MapArea> toLoadAreas, List<MapArea> loadedAreas) {
        totalAreasToLoad = toLoadAreas.size() - loadedAreas.size();
        int loadedArea = 0;
        for (MapArea toLoadArea : toLoadAreas) {
            killIfNeeded();
            if (refreshData || !loadedAreas.contains(toLoadArea)) {
                try {
                    Timber.d("----- Downloading Area : " + toLoadArea.getId());
                    totalAreasLoaded = loadedArea++;
                    loadedElements = 0L;
                    totalsElements = 0L;
                    loadAndSavePoisFromBackend(toLoadArea);

                    //publish poi loaded
                    toLoadArea.setUpdateDate(new DateTime(System.currentTimeMillis()));

                    Timber.d("----- Saving Area in BDD : " + toLoadArea.getId());
                    mapAreaDao.createOrUpdate(toLoadArea);
                    loadedAreas.add(toLoadArea);

                } catch (NetworkException e) {
                    Timber.w("Network error wile saving areas ");
                    loadingStatus = NETWORK_ERROR;
                    publishProgress();
                    return;
                }
            }
        }
    }

    private void loadAndSavePoisFromBackend(MapArea toLoadArea) {
        loadingStatus = LOADING_FROM_SERVER;
        publishProgress();
        loadedElements = 0L;
        nodeDtos.clear();
        osmDtos = backend.getPoisDtosInBox(toLoadArea.getBox());

        for (OsmDto osmDto : osmDtos) {
            if (osmDto != null) {
                nodeDtos.addAll(osmDto.getNodeDtoList());
                nodeDtos.addAll(osmDto.getWayDtoList());
            }
        }

        osmDtos.clear();

        loadingStatus = MAPPING_POIS;
        totalsElements = nodeDtos.size();

        int i = 0;
        for (PoiDto dto : nodeDtos) {
            poiDao.createOrUpdate(poiMapper.convertDtoToPoi(false, availableTypes, dto));

            if (i >= POI_PAGE_MAPPING) {
                killIfNeeded();
                Timber.d("----- Mapping and saving  POI : " + loadedElements);
                loadedElements += i;
                loadedElements = loadedElements;
                publishProgress();
                i = 0;
            }
            i++;
        }
    }

    private void killIfNeeded() {
        if (mustBeKilled.getValue()) {
            throw new KillByRequestException();
        }
    }

    private void publishProgress() {
        PoiLoadingProgress progress = new PoiLoadingProgress();
        progress.setTotalsElements(totalsElements);
        progress.setLoadedElements(loadedElements);
        progress.setTotalAreasLoaded(totalAreasLoaded);
        progress.setTotalAreasToLoad(totalAreasToLoad);
        progress.setLoadingStatus(loadingStatus);
        progress.setPois(pois);
        progress.setNotes(notes);
        progress.setLoadingStatus(loadingStatus);
        progress.setDataNeedRefresh(dataNeedRefresh);
        subscriber.onNext(progress);
    }
}
