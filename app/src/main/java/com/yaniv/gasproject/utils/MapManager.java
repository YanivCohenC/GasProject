package com.yaniv.gasproject.utils;

import android.content.Context;
import android.location.Location;

import com.yaniv.gasproject.dm.GasStation;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapManager {
    private final Context context;
    private final MapView map;
    private final List<Marker> markers = new ArrayList<>();
    private final Map<GeoPoint, Marker> markerMap = new HashMap<>();
    private boolean showingDiesel = false;
    private boolean showingGeneric = true;

    public MapManager(Context context, MapView map) {
        this.context = context;
        this.map = map;
    }

    public void setShowingGeneric(boolean showGeneric) {
        this.showingGeneric = showGeneric;
    }

    private List<GasStation> filterGenericStations(List<GasStation> stations) {
        if (showingGeneric) {
            return new ArrayList<>(stations);
        }
        List<GasStation> filteredStations = new ArrayList<>();
        for (GasStation station : stations) {
            if (station.isFromApi()) {
                filteredStations.add(station);
            }
        }
        return filteredStations;
    }

    public void clearMarkers() {
        for (Marker marker : markers) {
            map.getOverlays().remove(marker);
        }
        markers.clear();
        markerMap.clear();
        map.invalidate();
    }

    public void addMarker(GasStation station, Location userLocation) {
        Marker marker = MarkerUtils.createMarkerForStation(context, map, station, showingDiesel, userLocation);
        markers.add(marker);
        markerMap.put(marker.getPosition(), marker);
        map.getOverlays().add(marker);
    }

    public void updateMarkers(List<GasStation> stations, Location userLocation) {
        clearMarkers();
        List<GasStation> filteredStations = filterGenericStations(stations);
        for (GasStation station : filteredStations) {
            addMarker(station, userLocation);
        }
        map.invalidate();
    }

    public void setShowingDiesel(boolean showingDiesel) {
        this.showingDiesel = showingDiesel;
    }

    public void animateToLocation(GeoPoint point, double zoom) {
        map.getController().animateTo(point, zoom, 1000L);
    }

    public void showStationInfoWindow(GasStation station) {
        GeoPoint stationPoint = new GeoPoint(station.getGps().getLat(), station.getGps().getLng());
        Marker marker = markerMap.get(stationPoint);
        if (marker != null) {
            marker.showInfoWindow();
        }
    }
} 