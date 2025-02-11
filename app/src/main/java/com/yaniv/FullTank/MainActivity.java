package com.yaniv.FullTank;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yaniv.FullTank.dm.GasStation;
import com.yaniv.FullTank.utils.GasStationDataManager;
import com.yaniv.FullTank.utils.LocationHelper;
import com.yaniv.FullTank.utils.MapManager;
import com.yaniv.FullTank.utils.UIManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.List;

/**
 * Main activity of the Gas Station Finder app.
 * Handles map display, location services, and coordinates between different managers.
 */
public class MainActivity extends AppCompatActivity implements LocationListener, GasStationDataManager.DataUpdateCallback {
    private MapView map;
    private MapManager mapManager;
    private LocationHelper locationHelper;
    private GasStationDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        // Initialize osmdroid configuration for map display
        Configuration.getInstance().load(context, context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize and configure the map view
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(15.0);
        map.setMultiTouchControls(true);

        // Initialize all manager classes
        mapManager = new MapManager(this, map);
        locationHelper = new LocationHelper(this, map);
        dataManager = new GasStationDataManager(this, this);
        UIManager uiManager = new UIManager(this, locationHelper, mapManager, dataManager);

        // Close info windows and lists when touching the map
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                InfoWindow.closeAllInfoWindowsOn(map);
                // Hide search results and nearby list
                findViewById(R.id.searchResultsRecyclerView).setVisibility(View.GONE);
                uiManager.closeNearbyList();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        map.getOverlays().add(mapEventsOverlay);

        // Setup custom zoom controls
        findViewById(R.id.zoom_in_fab).setOnClickListener(v -> map.getController().zoomIn());

        findViewById(R.id.zoom_out_fab).setOnClickListener(v -> map.getController().zoomOut());

        // Setup UI components and check location permissions
        uiManager.setupUI();
        checkLocationPermission();
    }

    /**
     * Initiates location permission check using LocationHelper
     */
    private void checkLocationPermission() {
        locationHelper.checkLocationPermission(this::startLocationUpdates);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationHelper.handlePermissionResult(requestCode, grantResults,
            // If permission granted, start location updates
                this::startLocationUpdates,
            // If permission denied, set default location to Israel center
            () -> {
                mapManager.animateToLocation(new GeoPoint(31.7683, 35.2137), 15.0);
                dataManager.loadGasStations();
            }
        );
    }

    /**
     * Starts location updates and loads gas station data
     */
    private void startLocationUpdates() {
        locationHelper.startLocationUpdates(this);
        dataManager.loadGasStations();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Update map view to center on new location
        mapManager.animateToLocation(
            new GeoPoint(location.getLatitude(), location.getLongitude()),
            map.getZoomLevelDouble()
        );
        // Update markers with new distances from current location
        // The MapManager will handle filtering of generic stations
        mapManager.updateMarkers(dataManager.getAllStations(), location);
    }

    // Callbacks for gas station data loading
    @Override
    public void onDataLoaded(List<GasStation> stations) {
        runOnUiThread(() -> {
            Location currentLocation = locationHelper.getLastLocation();
            // The MapManager will handle filtering of generic stations
            mapManager.updateMarkers(stations, currentLocation);
            Toast.makeText(this, "Loaded " + stations.size() + " stations", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDataLoadFailed(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onLoadingStarted(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    // Lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        locationHelper.stopLocationUpdates();
    }
}