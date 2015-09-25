/**
 * Copyright (C) 2015 eBusiness Information
 *
 * This file is part of OSM Contributor.
 *
 * OSM Contributor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OSM Contributor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OSM Contributor.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mapsquare.osmcontributor.sync;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.mapsquare.osmcontributor.core.PoiManager;
import io.mapsquare.osmcontributor.core.model.Poi;
import io.mapsquare.osmcontributor.core.model.PoiType;
import io.mapsquare.osmcontributor.core.model.PoiTypeTag;
import io.mapsquare.osmcontributor.sync.assets.PoiAssetLoader;
import io.mapsquare.osmcontributor.sync.converter.PoiConverter;
import io.mapsquare.osmcontributor.sync.dto.osm.ChangeSetDto;
import io.mapsquare.osmcontributor.sync.dto.osm.NodeDto;
import io.mapsquare.osmcontributor.sync.dto.osm.OsmDto;
import io.mapsquare.osmcontributor.sync.dto.osm.TagDto;
import io.mapsquare.osmcontributor.sync.dto.osm.WayDto;
import io.mapsquare.osmcontributor.sync.events.error.SyncDownloadRetrofitErrorEvent;
import io.mapsquare.osmcontributor.sync.events.error.SyncUploadRetrofitErrorEvent;
import io.mapsquare.osmcontributor.sync.rest.OsmRestClient;
import io.mapsquare.osmcontributor.sync.rest.OverpassRestClient;
import io.mapsquare.osmcontributor.utils.Box;
import io.mapsquare.osmcontributor.utils.DateTimeUtils;
import retrofit.RetrofitError;
import retrofit.mime.TypedString;
import timber.log.Timber;

import static java.util.Collections.singletonList;

/**
 * Implementation of a {@link io.mapsquare.osmcontributor.sync.Backend} for an OpenStreetMap backend.
 */
public class OsmBackend implements Backend {

    PoiManager poiManager;

    PoiAssetLoader poiAssetLoader;

    OSMProxy osmProxy;

    OverpassRestClient overpassRestClient;

    OsmRestClient osmRestClient;

    PoiConverter poiConverter;

    EventBus bus;

    public OsmBackend(EventBus bus, OSMProxy osmProxy, OverpassRestClient overpassRestClient, OsmRestClient osmRestClient, PoiConverter poiConverter, PoiManager poiManager, PoiAssetLoader poiAssetLoader) {
        this.bus = bus;
        this.osmProxy = osmProxy;
        this.overpassRestClient = overpassRestClient;
        this.osmRestClient = osmRestClient;
        this.poiConverter = poiConverter;
        this.poiManager = poiManager;
        this.poiAssetLoader = poiAssetLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String initializeTransaction(String comment) {
        final OsmDto osmDto = new OsmDto();
        ChangeSetDto changeSetDto = new ChangeSetDto();
        List<TagDto> tagDtos = new ArrayList<>();

        osmDto.setChangeSetDto(changeSetDto);
        changeSetDto.setTagDtoList(tagDtos);
        tagDtos.add(new TagDto(comment, "comment"));

        OSMProxy.Result<String> result = osmProxy.proceed(new OSMProxy.NetworkAction<String>() {
            @Override
            public String proceed() {
                String changeSetId = osmRestClient.addChangeSet(osmDto);
                Timber.d("Retrieved changeSet Id: %s", changeSetId);
                return changeSetId;
            }
        });

        if (result.isSuccess()) {
            return result.getResult();
        } else if (result.getRetrofitError() != null) {
            RetrofitError retrofitError = result.getRetrofitError();
            Timber.e(retrofitError, "Retrofit error, couldn't create Changeset!");
            bus.post(new SyncUploadRetrofitErrorEvent(-1L));
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<Poi> getPoisInBox(final Box box, final boolean withDate) {
        Timber.d("Requesting overpass for download");

        OSMProxy.Result<OsmDto> result = osmProxy.proceed(new OSMProxy.NetworkAction<OsmDto>() {
            @Override
            public OsmDto proceed() {
                String request = generateOverpassRequest(box, withDate);
                return overpassRestClient.sendRequest(new TypedString(request));
            }
        });
        if (!result.isSuccess() && result.getRetrofitError() != null) {
            RetrofitError retrofitError = result.getRetrofitError();
            Timber.e(retrofitError, "Retrofit error, couldn't download from overpass");
            bus.post(new SyncDownloadRetrofitErrorEvent());
            return new ArrayList<>();
        }

        OsmDto osmDto = result.getResult();
        return convertPois(osmDto);
    }

    @NonNull
    private List<Poi> convertPois(OsmDto osmDto) {
        List<Poi> pois = poiConverter.convertDtosToPois(osmDto.getNodeDtoList());
        pois.addAll(poiConverter.convertDtosToPois(osmDto.getWayDtoList()));
        return pois;
    }

    private String generateOverpassRequest(Box box, boolean withDate) {

        StringBuilder cmplReq = new StringBuilder("(");

        for (String type : Arrays.asList("node", "way")) {
            for (PoiType poiTypeDto : poiManager.loadPoiTypes().values()) {
                StringBuilder req = new StringBuilder(type);

                boolean valid = false;

                //values
                for (PoiTypeTag poiTypeTag : poiTypeDto.getTags()) {

                    if (poiTypeTag.getValue() != null) {
                        String keyValue = "[\"" + poiTypeTag.getKey() + "\"~\"" + poiTypeTag.getValue() + "\"]";
                        req.append(keyValue);
                        valid = true;
                    }

                }
                if (valid) {
                    cmplReq.append(req);

                    // newer than
                    if (withDate) {
                        cmplReq.append("(")
                                .append("newer:")
                                .append("\"")
                                .append(DateTimeUtils.UTC_DATE_TIME_FORMATTER.print(poiManager.getLastUpdateDate()))
                                .append("\"")
                                .append(")");
                    }

                    // bounding box
                    cmplReq.append("(")
                            .append(box.getSouth()).append(",")
                            .append(box.getWest()).append(",")
                            .append(box.getNorth()).append(",")
                            .append(box.getEast())
                            .append(");");
                }
            }
        }

        cmplReq.append(");out meta center;");
        return cmplReq.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreationResult addPoi(final Poi poi, String transactionId) {
        final OsmDto osmDto = new OsmDto();

        if (poi.getWay()) {
            WayDto nodeDto = poiConverter.convertPoiToWayDto(poi, transactionId);
            osmDto.setWayDtoList(singletonList(nodeDto));
        } else {
            NodeDto nodeDto = poiConverter.convertPoiToNodeDto(poi, transactionId);
            osmDto.setNodeDtoList(singletonList(nodeDto));
        }

        OSMProxy.Result<String> result = osmProxy.proceed(new OSMProxy.NetworkAction<String>() {
            @Override
            public String proceed() {
                String nodeId;
                if (poi.getWay()) {
                    nodeId = osmRestClient.addWay(osmDto);
                    Timber.d("Created way with id: %s", nodeId);
                } else {
                    nodeId = osmRestClient.addNode(osmDto);
                    Timber.d("Created node with id: %s", nodeId);
                }
                return nodeId;
            }
        });

        if (result.isSuccess()) {
            return new CreationResult(ModificationStatus.SUCCESS, result.getResult());
        }
        Timber.e(result.getRetrofitError(), "Couldn't add node %s", poi);
        return new CreationResult(ModificationStatus.FAILURE_UNKNOWN, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UpdateResult updatePoi(final Poi poi, String transactionId) {
        final OsmDto osmDto = new OsmDto();

        if (poi.getWay()) {
            WayDto nodeDto = poiConverter.convertPoiToWayDto(poi, transactionId);
            osmDto.setWayDtoList(singletonList(nodeDto));
        } else {
            NodeDto nodeDto = poiConverter.convertPoiToNodeDto(poi, transactionId);
            osmDto.setNodeDtoList(singletonList(nodeDto));
        }

        OSMProxy.Result<String> result = osmProxy.proceed(new OSMProxy.NetworkAction<String>() {
            @Override
            public String proceed() {
                String version;
                if (poi.getWay()) {
                    version = osmRestClient.updateWay(poi.getBackendId(), osmDto);
                    Timber.d("Updated way with new version: %s", version);
                } else {
                    version = osmRestClient.updateNode(poi.getBackendId(), osmDto);
                    Timber.d("Updated node with new version: %s", version);
                }
                return version;
            }
        });

        if (result.isSuccess()) {
            return new UpdateResult(ModificationStatus.SUCCESS, result.getResult());
        }

        if (result.getRetrofitError() != null) {
            RetrofitError e = result.getRetrofitError();
            if (e.getResponse() != null && e.getResponse().getStatus() == 400) {
                Timber.e(e, "Couldn't update node, conflicting version");
                return new UpdateResult(ModificationStatus.FAILURE_CONFLICT, null);
            } else if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                Timber.e(e, "Couldn't update node, no existing node found with the id");
                return new UpdateResult(ModificationStatus.FAILURE_NOT_EXISTING, null);
            } else {
                Timber.e(e, "Couldn't update node");
                return new UpdateResult(ModificationStatus.FAILURE_UNKNOWN, null);
            }
        }
        return new UpdateResult(ModificationStatus.FAILURE_UNKNOWN, null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModificationStatus deletePoi(final Poi poi, String transactionId) {
        final OsmDto osmDto = new OsmDto();

        if (poi.getWay()) {
            WayDto nodeDto = poiConverter.convertPoiToWayDto(poi, transactionId);
            osmDto.setWayDtoList(singletonList(nodeDto));
        } else {
            NodeDto nodeDto = poiConverter.convertPoiToNodeDto(poi, transactionId);
            osmDto.setNodeDtoList(singletonList(nodeDto));
        }

        OSMProxy.Result<Void> result = osmProxy.proceed(new OSMProxy.NetworkAction<Void>() {
            @Override
            public Void proceed() {
                if (poi.getWay()) {
                    osmRestClient.deleteWay(poi.getBackendId(), osmDto);
                    Timber.d("Deleted way %s", poi.getBackendId());
                } else {
                    osmRestClient.deleteNode(poi.getBackendId(), osmDto);
                    Timber.d("Deleted node %s", poi.getBackendId());
                }
                poiManager.deletePoi(poi);
                return null;
            }
        });

        if (result.isSuccess()) {
            return ModificationStatus.SUCCESS;
        }

        RetrofitError retrofitError = result.getRetrofitError();
        if (retrofitError != null) {
            if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 400) {
                return ModificationStatus.FAILURE_CONFLICT;
            } else if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 404) {
                //the point doesn't exist
                return ModificationStatus.FAILURE_NOT_EXISTING;
            } else if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 410) {
                //the poi was already deleted
                return ModificationStatus.FAILURE_NOT_EXISTING;
            }
        }
        return ModificationStatus.FAILURE_UNKNOWN;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Poi getPoiById(final String backendId) {
        OSMProxy.Result<Poi> result = osmProxy.proceed(new OSMProxy.NetworkAction<Poi>() {
            @Override
            public Poi proceed() {
                OsmDto osmDtoRetrievedNode = osmRestClient.getNode(backendId);
                List<NodeDto> nodeDtoList = osmDtoRetrievedNode.getNodeDtoList();
                if (nodeDtoList != null && nodeDtoList.size() > 0) {
                    List<Poi> pois = poiConverter.convertDtosToPois(nodeDtoList);
                    if (pois.size() > 0) {
                        return pois.get(0);
                    }
                }
                return null;
            }
        });

        return result.getResult();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PoiType> getPoiTypes() {
        return poiAssetLoader.loadPoiTypesFromAssets();
    }
}