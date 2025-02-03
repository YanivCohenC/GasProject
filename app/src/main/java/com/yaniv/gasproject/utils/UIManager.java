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
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final float MAX_NEARBY_DISTANCE = 15000; // in meters
    private boolean isProcessingFuelTypeChange = false; // Flag to prevent rapid clicks

    private String currentSearchQuery = "";  // Store current search query

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
            // If already processing a change, ignore the click
            if (isProcessingFuelTypeChange) {
                return;
            }
            
            isProcessingFuelTypeChange = true;
            showingDiesel = !showingDiesel;
            fuelTypeFab.setText(showingDiesel ? R.string.fuel_type_diesel : R.string.fuel_type_95);
            
            // Update all views to show the selected fuel type
            mapManager.setShowingDiesel(showingDiesel);
            searchResultsAdapter.setShowingDiesel(showingDiesel);
            nearbyStationsAdapter.setShowingDiesel(showingDiesel);
            
            // Reapply current filter with new fuel type
            filterStations(currentSearchQuery);
            
            // Update nearby stations with new fuel type
            if (showingNearbyList) {
                updateNearbyStations();
            }
            
            // Reset the processing flag after a short delay to prevent rapid clicks
            fuelTypeFab.postDelayed(() -> isProcessingFuelTypeChange = false, 500);
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
            sortFab.setText(sortByPrice ? R.string.sort_by_price : R.string.sort_by_distance);
            
            // Update both lists with new sorting
            if (showingNearbyList) {
                updateNearbyStations();  // This will now maintain filtered state
            }
            if (searchResultsRecyclerView.getVisibility() == View.VISIBLE) {
                filterStations(currentSearchQuery);  // Re-apply current filter with new sort
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
        Button nearbyButton = activity.findViewById(R.id.nearby_fab);
        nearbyStationsAdapter = new NearbyStationsAdapter(station -> {
            nearbyStationsRecyclerView.setVisibility(View.GONE);
            showingNearbyList = false;
            nearbyButton.setText(R.string.nearby_hide);
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
        
        // Re-apply current filter if exists
        if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
            List<GasStation> filteredStations = dataManager.filterStations(
                currentSearchQuery,
                userLocation,
                showingDiesel,
                sortByPrice
            );
            Set<Integer> filteredStationIds = filteredStations.stream()
                .map(GasStation::getId)
                .collect(Collectors.toSet());
            nearbyStationsAdapter.updateDisabledStations(filteredStationIds);
        }
        
        if (nearbyStations.isEmpty()) {
            Toast.makeText(activity, "No stations found within " + MAX_NEARBY_DISTANCE / 1000 + "km", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterStations(String query) {
        currentSearchQuery = query;  // Store the query
        List<GasStation> filteredStations = dataManager.filterStations(
            query,
            locationHelper.getLastLocation(),
            showingDiesel,
            sortByPrice
        );

        mapManager.updateMarkers(filteredStations, locationHelper.getLastLocation());

        // Update nearby stations list to grey out non-matching stations
        if (query != null && !query.trim().isEmpty()) {
            Set<Integer> filteredStationIds = filteredStations.stream()
                .map(GasStation::getId)
                .collect(Collectors.toSet());
            nearbyStationsAdapter.updateDisabledStations(filteredStationIds);
        } else {
            nearbyStationsAdapter.clearDisabledStations();
        }

        // Update search results visibility
        if (query == null || query.trim().isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else if (!filteredStations.isEmpty()) {
            searchResultsAdapter.setStations(filteredStations);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
    }

    /**
     * Closes the nearby stations list and updates UI state
     */
    public void closeNearbyList() {
        if (showingNearbyList) {
            showingNearbyList = false;
            nearbyStationsRecyclerView.setVisibility(View.GONE);
            Button nearbyButton = activity.findViewById(R.id.nearby_fab);
            nearbyButton.setText(R.string.nearby_hide);
        }
    }
} 