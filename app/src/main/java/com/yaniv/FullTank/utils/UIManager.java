package com.yaniv.FullTank.utils;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.yaniv.FullTank.R;
import com.yaniv.FullTank.adapters.NearbyStationsAdapter;
import com.yaniv.FullTank.adapters.SearchResultsAdapter;
import com.yaniv.FullTank.dm.GasStation;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
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
    private boolean showingGeneric = true;  // Toggle visibility of generic stations
    
    private ExtendedFloatingActionButton fuelTypeFab;
    private ExtendedFloatingActionButton sortFab;
    private ExtendedFloatingActionButton genericFab;
    private static final float MAX_NEARBY_DISTANCE = 15000; // in meters
    private boolean isProcessingFuelTypeChange = false; // Flag to prevent rapid clicks
    private boolean isGenericUpdateInProgress = false;

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
        setupGenericFAB();
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
            
            // Toggle between text labels using string resources
            fuelTypeFab.setText(showingDiesel ? R.string.fuel_diesel : R.string.fuel_95);
            
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
     * Sets up the generic FAB to toggle visibility of generic stations
     */
    private void setupGenericFAB() {
        genericFab = activity.findViewById(R.id.generic_fab);
        // Set initial icon state and sync with MapManager
        updateGenericFabIcon();
        mapManager.setShowingGeneric(showingGeneric);
        
        genericFab.setOnClickListener(v -> {
            // If an update is already in progress, ignore the click
            if (isGenericUpdateInProgress) {
                return;
            }
            
            // Disable the button and start the update
            isGenericUpdateInProgress = true;
            genericFab.setEnabled(false);
            
            showingGeneric = !showingGeneric;
            updateGenericFabIcon();
            mapManager.setShowingGeneric(showingGeneric);
            
            // Update all views to reflect the new filter
            updateAllViews();
            
            // Re-enable the button after a short delay
            genericFab.postDelayed(() -> {
                isGenericUpdateInProgress = false;
                genericFab.setEnabled(true);
            }, 300); // 300ms delay to prevent rapid clicks
        });
    }

    /**
     * Updates the generic FAB icon based on current state
     */
    private void updateGenericFabIcon() {
        genericFab.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(
            activity,
            showingGeneric ? R.drawable.ic_generic_stations_pressed : R.drawable.ic_generic_stations
        ));
    }

    /**
     * Updates all views to reflect current filter state
     */
    private void updateAllViews() {
        Location userLocation = locationHelper.getLastLocation();
        
        // Update map markers - no need to filter here as MapManager will handle it
        List<GasStation> stations = currentSearchQuery.isEmpty() ? 
            dataManager.getAllStations() : 
            dataManager.filterStations(currentSearchQuery, userLocation, showingDiesel, sortByPrice);
        mapManager.updateMarkers(stations, userLocation);
        
        // Update nearby list if visible
        if (showingNearbyList) {
            updateNearbyStations();
        }
        
        // Update search results if visible
        if (searchResultsRecyclerView.getVisibility() == View.VISIBLE) {
            filterStations(currentSearchQuery);
        }
    }

    private List<GasStation> filterGenericStations(List<GasStation> stations) {
        if (showingGeneric) {
            return new ArrayList<>(stations); // Return a copy to prevent modification
        }
        List<GasStation> filteredStations = new ArrayList<>();
        for (GasStation station : stations) {
            if (station.isFromApi()) {
                filteredStations.add(station);
            }
        }
        return filteredStations;
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

    private void filterStations(String query) {
        currentSearchQuery = query;
        Location userLocation = locationHelper.getLastLocation();
        
        // First get stations based on search query
        List<GasStation> searchFilteredStations = dataManager.filterStations(
            query,
            userLocation,
            showingDiesel,
            sortByPrice
        );
        
        // Then filter out generic stations if needed
        List<GasStation> finalFilteredStations = filterGenericStations(searchFilteredStations);

        // Update map markers
        mapManager.updateMarkers(finalFilteredStations, userLocation);

        // Update nearby stations list to grey out non-matching stations
        if (query != null && !query.trim().isEmpty()) {
            Set<Integer> filteredStationIds = finalFilteredStations.stream()
                .map(GasStation::getId)
                .collect(Collectors.toSet());
            nearbyStationsAdapter.updateDisabledStations(filteredStationIds);
        } else {
            nearbyStationsAdapter.clearDisabledStations();
        }

        // Update search results visibility
        if (query == null || query.trim().isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else if (!finalFilteredStations.isEmpty()) {
            searchResultsAdapter.setStations(finalFilteredStations);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void updateNearbyStations() {
        Location userLocation = locationHelper.getLastLocation();
        if (userLocation == null) return;

        // Get nearby stations
        List<GasStation> nearbyStations = dataManager.getNearbyStations(
            userLocation,
            showingDiesel,
            sortByPrice,
            MAX_NEARBY_DISTANCE
        );

        // Filter out generic stations if needed
        List<GasStation> filteredNearbyStations = filterGenericStations(nearbyStations);

        nearbyStationsAdapter.setStations(filteredNearbyStations);
        
        // Re-apply current filter if exists
        if (!currentSearchQuery.trim().isEmpty()) {
            List<GasStation> searchFilteredStations = filterGenericStations(
                dataManager.filterStations(
                    currentSearchQuery,
                    userLocation,
                    showingDiesel,
                    sortByPrice
                )
            );
            Set<Integer> filteredStationIds = searchFilteredStations.stream()
                .map(GasStation::getId)
                .collect(Collectors.toSet());
            nearbyStationsAdapter.updateDisabledStations(filteredStationIds);
        }
        
        if (filteredNearbyStations.isEmpty()) {
            Toast.makeText(activity, "No stations found within " + MAX_NEARBY_DISTANCE / 1000 + "km", Toast.LENGTH_SHORT).show();
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