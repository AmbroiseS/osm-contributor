/**
 * Copyright (C) 2016 eBusiness Information
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
package io.mapsquare.osmcontributor.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.mapsquare.osmcontributor.OsmTemplateApplication;
import io.mapsquare.osmcontributor.core.ConfigManager;
import io.mapsquare.osmcontributor.core.events.NotesLoadedEvent;
import io.mapsquare.osmcontributor.core.events.PleaseLoadNotesEvent;
import io.mapsquare.osmcontributor.core.events.PleaseLoadPoiTypes;
import io.mapsquare.osmcontributor.core.events.PleaseLoadPoisEvent;
import io.mapsquare.osmcontributor.core.events.PoiTypesLoaded;
import io.mapsquare.osmcontributor.core.events.PoisAndNotesDownloadedEvent;
import io.mapsquare.osmcontributor.core.events.PoisLoadedEvent;
import io.mapsquare.osmcontributor.core.events.RevertFinishedEvent;
import io.mapsquare.osmcontributor.core.model.Note;
import io.mapsquare.osmcontributor.core.model.Poi;
import io.mapsquare.osmcontributor.core.model.PoiType;
import io.mapsquare.osmcontributor.map.events.PleaseChangeValuesDetailNoteFragmentEvent;
import io.mapsquare.osmcontributor.map.events.PleaseChangeValuesDetailPoiFragmentEvent;
import io.mapsquare.osmcontributor.map.events.PleaseInitializeDrawer;
import io.mapsquare.osmcontributor.map.marker.LocationMarker;
import io.mapsquare.osmcontributor.map.marker.LocationMarkerOptions;
import io.mapsquare.osmcontributor.sync.events.SyncDownloadPoisAndNotesEvent;
import io.mapsquare.osmcontributor.utils.Box;
import timber.log.Timber;

public class MapFragmentPresenter {

    MapFragment mapFragment;

    @Inject
    EventBus eventBus;

    @Inject
    ConfigManager configManager;

    private boolean forceRefreshPoi = false;
    private boolean forceRefreshNotes = false;

    public MapFragmentPresenter(MapFragment mapFragment) {
        this.mapFragment = mapFragment;
        ((OsmTemplateApplication) mapFragment.getActivity().getApplication()).getOsmTemplateComponent().inject(this);
    }

    public void register() {
        eventBus.register(this);
    }

    public void unregister() {
        eventBus.unregister(this);
    }

    private List<PoiType> poiTypes = null;

    public int getNumberOfPoiTypes() {
        return poiTypes != null ? poiTypes.size() : 0;
    }

    public List<PoiType> getPoiTypes() {
        return poiTypes == null ? new ArrayList<PoiType>() : poiTypes;
    }

    public PoiType getPoiType(int id) {
        return poiTypes.get(id);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPoiTypesLoaded(PoiTypesLoaded event) {
        Timber.d("Received event PoiTypesLoaded");
        poiTypes = event.getPoiTypes();
        setForceRefreshPoi();
        setForceRefreshNotes();
        loadPoisIfNeeded();
        eventBus.post(new PleaseInitializeDrawer(poiTypes, mapFragment.getPoiTypeHidden()));
        mapFragment.loadPoiTypeSpinner();
        mapFragment.loadPoiTypeFloatingBtn();
        eventBus.removeStickyEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRevertFinishedEvent(RevertFinishedEvent event) {
        Timber.d("Received event RevertFinishedEvent");
        setForceRefreshPoi();
        loadPoisIfNeeded();
    }

    private Float previousZoom;
    private LatLngBounds triggerReloadPoiLatLngBounds;

    public void loadPoisIfNeeded() {
        if (poiTypes == null) {
            Timber.v("PleaseLoadPoiTypes");
            eventBus.post(new PleaseLoadPoiTypes());
        }

        LatLngBounds viewLatLngBounds = mapFragment.getViewLatLngBounds();
        if (viewLatLngBounds != null) {
            if (mapFragment.getZoomLevel() > 15) {
                if (shouldReload(viewLatLngBounds)) {
                    Timber.d("Reloading pois");
                    previousZoom = mapFragment.getZoomLevel();
                    triggerReloadPoiLatLngBounds = enlarge(viewLatLngBounds, 1.5);
                    eventBus.post(new PleaseLoadPoisEvent(enlarge(viewLatLngBounds, 1.75)));
                    eventBus.post(new PleaseLoadNotesEvent(enlarge(viewLatLngBounds, 1.75)));
                }
            } else {
                if (mapFragment.hasMarkers()) {
                    Timber.d("area displayed is too big, hiding pois");
                    previousZoom = mapFragment.getZoomLevel();
                    mapFragment.removeAllMarkers();
                }
            }
        }
    }

    public void setForceRefreshPoi() {
        this.forceRefreshPoi = true;
    }

    public void setForceRefreshNotes() {
        this.forceRefreshNotes = true;
    }

    private boolean shouldReload(LatLngBounds viewLatLngBounds) {
        if (forceRefreshPoi || forceRefreshNotes) {
            forceRefreshPoi = false;
            forceRefreshNotes = false;
            return true;
        }
        if (previousZoom != null && previousZoom < 15) {
            return true;
        }
        return triggerReloadPoiLatLngBounds == null
                || !triggerReloadPoiLatLngBounds.union(viewLatLngBounds).equals(triggerReloadPoiLatLngBounds);
    }

    LatLngBounds enlarge(LatLngBounds viewLatLngBounds, double factor) {
        double n = viewLatLngBounds.getLatNorth();
        double e = viewLatLngBounds.getLonEast();
        double s = viewLatLngBounds.getLatSouth();
        double w = viewLatLngBounds.getLonWest();
        double f = (factor - 1) / 2;
        return new LatLngBounds.Builder()
        .include(new LatLng(n + f * (n - s), e + f * (e - w)))
                .include(new LatLng(s - f * (n - s), w - f * (e - w)))
                .build();
    }

    public void downloadAreaPoisAndNotes() {
        mapFragment.showProgressBar(true);
        eventBus.post(new SyncDownloadPoisAndNotesEvent(Box.convertFromLatLngBounds(enlarge(mapFragment.getViewLatLngBounds(), 1.75))));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPoisAndNotesDownloadedEvent(PoisAndNotesDownloadedEvent event) {
        mapFragment.showProgressBar(false);
        forceRefreshPoi = true;
        loadPoisIfNeeded();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPoisLoadedEvent(PoisLoadedEvent event) {
        List<Poi> pois = event.getPois();
        Timber.d("Received event PoisLoaded  : " + pois.size());
        forceRefreshPoi = false;

        List<Long> poiIds = new ArrayList<>(pois.size());
        LocationMarker markerSelected = mapFragment.getMarkerSelected();
        for (Poi poi : pois) {
            poiIds.add(poi.getId());
            LocationMarkerOptions<Poi> locationMarker = mapFragment.getMarkersPoi().get(poi.getId());
            boolean selected = false;
            if (locationMarker == null) {
                locationMarker = new LocationMarkerOptions<Poi>().relatedObject(poi).position(poi.getPosition());

                //is it the marker selected
                if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.POI) && poi.getId().equals(mapFragment.getMarkerSelectedId())) {
                    mapFragment.setMarkerSelected(locationMarker.getMarker());
                    selected = true;
                } else if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.POI) && markerSelected != null &&
                        poi.getId().equals(((Poi) markerSelected.getRelatedObject()).getId())) {
                    selected = true;
                }

                //the poi in edition should be hidden
                if (!(markerSelected != null && mapFragment.getMapMode() == MapMode.POI_POSITION_EDITION && markerSelected.equals(locationMarker)) && !poi.getToDelete()) {
                    mapFragment.addMarker(locationMarker);
                }

            } else {
                locationMarker.relatedObject(poi);

                if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.POI) && (poi.getId().equals(mapFragment.getMarkerSelectedId()) ||
                        markerSelected != null && poi.getId().equals(((Poi) markerSelected.getRelatedObject()).getId()))) {
                    selected = true;
                }

                //update the detail banner data
                if (selected && mapFragment.getMapMode() == MapMode.DETAIL_POI) {
                    eventBus.post(new PleaseChangeValuesDetailPoiFragmentEvent(poi.getType().getName(), poi.getName(), poi.getWay()));
                }
            }

            // Draw the marker in the right color
            Bitmap bitmap = mapFragment.getBitmapHandler().getMarkerBitmap(poi.getType(), Poi.computeState(selected, false, poi.getUpdated()));

            if (bitmap != null) {
                locationMarker.icon(IconFactory.getInstance(mapFragment.getActivity()).fromBitmap(bitmap));
            }
        }

        mapFragment.removePoiMarkersNotIn(poiIds);

        //use to click on the selected marker when the activity resume
        if (mapFragment.getMapMode() == MapMode.DEFAULT) {
            mapFragment.reselectMarker();
        }

        if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.POI) && markerSelected == null) {
            mapFragment.setMarkerSelectedId(-1L);
        }

        mapFragment.invalidateMap();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotesLoadedEvent(NotesLoadedEvent event) {
        Timber.d("Showing notes : " + event.getNotes().size());
        List<Note> notes = event.getNotes();
        forceRefreshNotes = false;
        LocationMarkerOptions<Note> marker;

        List<Long> noteIds = new ArrayList<>(notes.size());
        for (Note note : notes) {
            noteIds.add(note.getId());
            if (mapFragment.getNote(note.getId()) == null) {

                marker = new LocationMarkerOptions<Note>().relatedObject(note).position(note.getPosition());
                Bitmap bitmap;

                if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.NOTE) && note.getId().equals(mapFragment.getMarkerSelectedId())) {
                    mapFragment.setMarkerSelected(marker.getMarker());
                    bitmap = mapFragment.getBitmapHandler().getNoteBitmap(Note.computeState(note, true, false));
                } else {
                    bitmap = mapFragment.getBitmapHandler().getNoteBitmap(Note.computeState(note, false, false));
                }

                if (bitmap != null) {
                    marker.icon(IconFactory.getInstance(mapFragment.getActivity()).fromBitmap(bitmap));
                }
                mapFragment.addNote(marker);
            } else {
                boolean selected = false;
                LocationMarkerOptions<Note> currentMarker = mapFragment.getNote(note.getId());
                // refresh the note inside the marker the data may have changed
                currentMarker.relatedObject(note);
                //if it's the selected marker refresh the banner view

                if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.NOTE) && mapFragment.getMarkerSelected() != null
                        && note.getId().equals(((Note) mapFragment.getMarkerSelected().getRelatedObject()).getId())) {
                    //detailNoteFragment.setNote(mapFragment.getMarkerSelected().getNote());
                    selected = true;
                }

                //update the detail banner data
                if (selected && mapFragment.getMapMode() == MapMode.DETAIL_NOTE) {
                    eventBus.post(new PleaseChangeValuesDetailNoteFragmentEvent(note));
                }

                // refresh the icon
                Bitmap bitmap = mapFragment.getBitmapHandler().getNoteBitmap(Note.computeState(note, selected, false));
                if (bitmap != null) {
                    currentMarker.icon(IconFactory.getInstance(mapFragment.getActivity()).fromBitmap(bitmap));
                }
            }
        }

        mapFragment.removeNoteMarkersNotIn(noteIds);

        if ((mapFragment.getMapMode() == MapMode.DEFAULT || mapFragment.getMapMode() == MapMode.POI_CREATION)) {
            mapFragment.reselectMarker();

        }

        if (mapFragment.getSelectedMarkerType().equals(LocationMarker.MarkerType.NOTE) && mapFragment.getMarkerSelected() == null) {
            mapFragment.setMarkerSelectedId(-1L);
        }
        mapFragment.invalidateMap();
    }
}
