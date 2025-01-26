package com.yaniv.gasproject.utils;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.yaniv.gasproject.R;
import com.yaniv.gasproject.adapters.NearbyStationsAdapter;
import com.yaniv.gasproject.adapters.SearchResultsAdapter;
import com.yaniv.gasproject.dm.GasStation;

import org.osmdroid.util.GeoPoint;

import java.util.List;

/**
 * Manages all UI components and their interactions in the app.
 * Handles search functionality, nearby stations list, and various control buttons.
 */
public class UIManager {
    private final Activity activity;
    private final LocationHelper locationHelper;
    private final MapManager mapManager;
    private final GasStationDataManager dataManager;
    
    // RecyclerViews and their adapters for displaying station lists
    private RecyclerView searchResultsRecyclerView;
    private SearchResultsAdapter searchResultsAdapter;
    private RecyclerView nearbyStationsRecyclerView;
    private NearbyStationsAdapter nearbyStationsAdapter;
    
    // UI state flags
    private boolean showingDiesel = false;  // Toggle between 95 and diesel fuel prices
    private boolean showingNearbyList = false;  // Controls visibility of nearby stations list
    private boolean sortByPrice = false;  // Toggle between distance and price sorting
    
    private ExtendedFloatingActionButton fuelTypeFab;
    private ExtendedFloatingActionButton sortFab;
    private static final float MAX_NEARBY_DISTANCE = 25000; // 25km in meters

    public UIManager(Activity activity, LocationHelper locationHelper, MapManager mapManager, GasStationDataManager dataManager) {
        this.activity = activity;
        this.locationHelper = locationHelper;
        this.mapManager = mapManager;
        this.dataManager = dataManager;
    }

    /**
     * Initializes all UI components and their event handlers
     */
    public void setupUI() {
        setupLocationFAB();
        setupFuelTypeFAB();
        setupNearbyStationsList();
        setupNearbyFAB();
        setupSortFAB();
        setupSearchFunctionality();
    }

    /**
     * Sets up the location FAB to center the map on user's location
     */
    private void setupLocationFAB() {
        activity.findViewById(R.id.location_fab).setOnClickListener(v -> {
            Location lastFix = locationHelper.getLastLocation();
            if (lastFix != null) {
                mapManager.animateToLocation(
                    new GeoPoint(lastFix.getLatitude(), lastFix.getLongitude()),
                    15.0
                );
                locationHelper.enableFollowLocation();
            } else {
                Toast.makeText(activity, "Waiting for location...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up the fuel type FAB to toggle between 95 and diesel prices
     */
    private void setupFuelTypeFAB() {
        fuelTypeFab = activity.findViewById(R.id.fuel_type_fab);
        fuelTypeFab.setOnClickListener(v -> {
            showingDiesel = !showingDiesel;
            fuelTypeFab.setText(showingDiesel ? "Type: Diesel" : "Type: 95");
            
            // Update all views to show the selected fuel type
            mapManager.setShowingDiesel(showingDiesel);
            searchResultsAdapter.setShowingDiesel(showingDiesel);
            nearbyStationsAdapter.setShowingDiesel(showingDiesel);
            
            mapManager.updateMarkers(dataManager.getAllStations(), locationHelper.getLastLocation());
        });
    }

    /**
     * Sets up the nearby stations FAB to toggle the visibility of nearby stations list
     */
    private void setupNearbyFAB() {
        Button nearbyButton = activity.findViewById(R.id.nearby_fab);
        nearbyButton.setOnClickListener(v -> {
            if (locationHelper.getLastLocation() == null) {
                Toast.makeText(activity, "Waiting for location...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            showingNearbyList = !showingNearbyList;
            if (showingNearbyList) {
                updateNearbyStations();
                nearbyStationsRecyclerView.setVisibility(View.VISIBLE);
                nearbyButton.setText(R.string.nearby_show);
            } else {
                nearbyStationsRecyclerView.setVisibility(View.GONE);
                nearbyButton.setText(R.string.nearby_hide);
            }
        });
    }

    /**
     * Sets up the sort FAB to toggle between distance and price sorting
     */
    private void setupSortFAB() {
        sortFab = activity.findViewById(R.id.sort_fab);
        sortFab.setOnClickListener(v -> {
            sortByPrice = !sortByPrice;
            sortFab.setText(sortByPrice ? "Sort: Price" : "Sort: Distance");
            
            // Update both lists with new sorting
            if (showingNearbyList) {
                updateNearbyStations();
            }
            if (searchResultsRecyclerView.getVisibility() == View.VISIBLE) {
                String currentQuery = ((SearchView) activity.findViewById(R.id.searchView))
                    .getQuery().toString();
                filterStations(currentQuery);
            }
        });
    }

    /**
     * Sets up the search functionality with search view and results list
     */
    private void setupSearchFunctionality() {
        SearchView searchView = activity.findViewById(R.id.searchView);
        setupSearchResultsList();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterStations(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterStations(newText);
                return true;
            }
        });
    }

    private void setupSearchResultsList() {
        searchResultsRecyclerView = activity.findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        searchResultsAdapter = new SearchResultsAdapter(station -> {
            searchResultsRecyclerView.setVisibility(View.GONE);
            locationHelper.disableFollowLocation();
            mapManager.animateToLocation(
                new GeoPoint(station.getGps().getLat(), station.getGps().getLng()),
                17.0
            );
            mapManager.showStationInfoWindow(station);
        });
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
    }

    private void setupNearbyStationsList() {
        nearbyStationsRecyclerView = activity.findViewById(R.id.nearbyStationsRecyclerView);
        nearbyStationsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        nearbyStationsAdapter = new NearbyStationsAdapter(station -> {
            nearbyStationsRecyclerView.setVisibility(View.GONE);
            showingNearbyList = false;
            locationHelper.disableFollowLocation();
            mapManager.animateToLocation(
                new GeoPoint(station.getGps().getLat(), station.getGps().getLng()),
                17.0
            );
            mapManager.showStationInfoWindow(station);
        });
        nearbyStationsRecyclerView.setAdapter(nearbyStationsAdapter);
    }

    private void updateNearbyStations() {
        Location userLocation = locationHelper.getLastLocation();
        if (userLocation == null) return;

        List<GasStation> nearbyStations = dataManager.getNearbyStations(
            userLocation,
            showingDiesel,
            sortByPrice,
            MAX_NEARBY_DISTANCE
        );

        nearbyStationsAdapter.setStations(nearbyStations);
        
        if (nearbyStations.isEmpty()) {
            Toast.makeText(activity, "No stations found within 25km", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterStations(String query) {
        List<GasStation> filteredStations = dataManager.filterStations(
            query,
            locationHelper.getLastLocation(),
            showingDiesel,
            sortByPrice
        );

        mapManager.updateMarkers(filteredStations, locationHelper.getLastLocation());

        if (query == null || query.trim().isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else if (!filteredStations.isEmpty()) {
            searchResultsAdapter.setStations(filteredStations);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
    }
} 