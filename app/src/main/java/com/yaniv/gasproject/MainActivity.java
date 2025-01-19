package com.yaniv.gasproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yaniv.gasproject.DatabaseHandler.GenericFirebaseImpl;
import com.yaniv.gasproject.DatabaseHandler.IFirebaseDao;
import com.yaniv.gasproject.GasStationsHandler.APIGasStationImpl;
import com.yaniv.gasproject.GasStationsHandler.CrawlingGasStationImpl;
import com.yaniv.gasproject.GasStationsHandler.GenericGasStationImpl;
import com.yaniv.gasproject.GasStationsHandler.IGasStationHandler;
import com.yaniv.gasproject.dm.GasStation;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapView map;
    private LocationManager locationManager;
    private MyLocationNewOverlay locationOverlay;
    private List<GasStation> allGasStations = new ArrayList<>();
    private List<Marker> currentMarkers = new ArrayList<>();
    private RecyclerView searchResultsRecyclerView;
    private SearchResultsAdapter searchResultsAdapter;
    private boolean showingDiesel = false;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fuelTypeFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize map
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(15.0);
        map.setMultiTouchControls(true);

        // Setup location FAB
        findViewById(R.id.location_fab).setOnClickListener(v -> {
            Location lastFix = locationOverlay.getLastFix();
            if (lastFix != null) {
                GeoPoint currentLocation = new GeoPoint(lastFix.getLatitude(), lastFix.getLongitude());
                map.getController().animateTo(currentLocation);
                map.getController().setZoom(15.0);
                locationOverlay.enableFollowLocation();
            } else {
                Toast.makeText(this, "Waiting for location...", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup fuel type FAB
        fuelTypeFab = findViewById(R.id.fuel_type_fab);
        fuelTypeFab.setOnClickListener(v -> {
            showingDiesel = !showingDiesel;
            fuelTypeFab.setText(showingDiesel ? "DSL" : "95");
            
            // Update search results adapter
            searchResultsAdapter.setShowingDiesel(showingDiesel);
            
            // Update all markers with new fuel type
            for (Marker marker : currentMarkers) {
                map.getOverlays().remove(marker);
            }
            currentMarkers.clear();
            
            for (GasStation station : allGasStations) {
                Marker marker = createMarkerForStation(station);
                currentMarkers.add(marker);
                map.getOverlays().add(marker);
            }
            map.invalidate();
        });

        // Create custom location overlay
        GpsMyLocationProvider provider = new GpsMyLocationProvider(ctx);
        locationOverlay = new MyLocationNewOverlay(provider, map) {
            @Override
            public void draw(Canvas canvas, MapView mapView, boolean shadow) {
                if (shadow) return;
                
                Location lastFix = getLastFix();
                if (lastFix != null && mapView != null) {
                    GeoPoint currentLocation = new GeoPoint(lastFix.getLatitude(), lastFix.getLongitude());
                    android.graphics.Point screenCoords = new android.graphics.Point();
                    mapView.getProjection().toPixels(currentLocation, screenCoords);

                    // Draw accuracy circle
                    if (lastFix.hasAccuracy()) {
                        float accuracy = lastFix.getAccuracy();
                        // Calculate a point at the accuracy distance (east)
                        double earthRadius = 6371000; // Earth's radius in meters
                        double lat = Math.toRadians(lastFix.getLatitude());
                        double lon = Math.toRadians(lastFix.getLongitude());
                        double angular = accuracy / earthRadius;
                        
                        // Calculate point at accuracy distance to the east
                        double eastLat = lat;
                        double eastLon = lon + angular / Math.cos(lat);
                        
                        GeoPoint edgePoint = new GeoPoint(Math.toDegrees(eastLat), Math.toDegrees(eastLon));
                        android.graphics.Point edgeScreenCoords = new android.graphics.Point();
                        mapView.getProjection().toPixels(edgePoint, edgeScreenCoords);
                        
                        // Calculate radius in pixels
                        float accuracyInPixels = Math.abs(edgeScreenCoords.x - screenCoords.x);

                        Paint accuracyPaint = new Paint();
                        accuracyPaint.setColor(Color.argb(40, 0, 0, 255));
                        accuracyPaint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(screenCoords.x, screenCoords.y, accuracyInPixels * 1.5f, accuracyPaint);
                    }

                    // Draw location dot
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    
                    // Outer circle (white border)
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(screenCoords.x, screenCoords.y, 25, paint);
                    
                    // Inner circle (blue fill)
                    paint.setColor(Color.rgb(0, 122, 255));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(screenCoords.x, screenCoords.y, 20, paint);
                }
            }
        };

        locationOverlay.enableMyLocation();
        map.getOverlays().add(locationOverlay);

        // Setup search functionality
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchView);
        setupSearchResultsList();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
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

        // Check for location permissions
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                // Set default location to Israel center if permission denied
                GeoPoint startPoint = new GeoPoint(31.7683, 35.2137);
                map.getController().setCenter(startPoint);
                loadGasStations();
            }
        }
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            
            // Get last known location and center map
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                GeoPoint startPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                map.getController().setCenter(startPoint);
            }
            
            // Load gas stations after setting location
            loadGasStations();
        }
    }

    private void loadGasStations() {
        // Initialize Firebase DAO
        IFirebaseDao firebaseDao = new GenericFirebaseImpl();

        // Show loading indicator
        runOnUiThread(() -> {
            Toast.makeText(this, "Loading gas stations...", Toast.LENGTH_SHORT).show();
        });

        // Check last update date in Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference lastUpdatedRef = database.getReference("lastUpdated");
        
        lastUpdatedRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String lastUpdated = task.getResult().getValue(String.class);
                Calendar now = Calendar.getInstance();
                int currentMonth = now.get(Calendar.MONTH);
                int currentYear = now.get(Calendar.YEAR);
                String currentDate = currentMonth + "" + currentYear;

                if (lastUpdated == null || !lastUpdated.equals(currentDate)) {
                    // Data is outdated, update from handlers
                    Log.d(TAG, "Data is outdated, updating from handlers...");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Updating gas station data...", Toast.LENGTH_LONG).show();
                    });
                    new Thread(() -> updateFromHandlers(firebaseDao)).start();
                } else {
                    // Data is current, just load from Firebase
                    loadFromFirebase(firebaseDao);
                }
            } else {
                // Error getting last update date, load from Firebase anyway
                Log.e(TAG, "Error checking last update date", task.getException());
                loadFromFirebase(firebaseDao);
            }
        });
    }

    private void updateFromHandlers(IFirebaseDao firebaseDao) {
        try {
            // Initialize gas station handlers
            String tenAPI = "https://10ten.co.il/website_api/website/1.0/generalDeclaration";
            String mikaCrawling = "https://mika.org.il/%D7%9B%D7%9C-%D7%94%D7%9E%D7%AA%D7%97%D7%9E%D7%99%D7%9D/";
            
            // Create handlers in parallel
            CompletableFuture<List<GasStation>> tenFuture = CompletableFuture.supplyAsync(() -> {
                IGasStationHandler worker = new APIGasStationImpl(tenAPI, "ten");
                return ((APIGasStationImpl) worker).getStations();
            });

            CompletableFuture<List<GasStation>> mikaFuture = CompletableFuture.supplyAsync(() -> {
                IGasStationHandler worker = new CrawlingGasStationImpl(mikaCrawling, "mika", getBaseContext());
                return ((CrawlingGasStationImpl) worker).getStations();
            });

            CompletableFuture<List<GasStation>> genericFuture = CompletableFuture.supplyAsync(() -> {
                IGasStationHandler worker = new GenericGasStationImpl(getBaseContext());
                return ((GenericGasStationImpl) worker).getStations();
            });

            // Wait for all futures to complete
            CompletableFuture.allOf(tenFuture, mikaFuture, genericFuture).thenAccept(v -> {
                // Combine all stations
                List<GasStation> allStations = new ArrayList<>();
                try {
                    allStations.addAll(tenFuture.get());
                    allStations.addAll(mikaFuture.get());
                    allStations.addAll(genericFuture.get());

                    // Save to Firebase
                    Log.d(TAG, "Saving " + allStations.size() + " stations to Firebase");
                    firebaseDao.saveToDatabase(allStations);

                    // Add markers on UI thread
                    runOnUiThread(() -> {
                        for (GasStation station : allStations) {
                            addCustomMarker(station);
                        }
                        map.invalidate(); // refresh map
                        Toast.makeText(MainActivity.this, "Updated " + allStations.size() + " stations", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error combining station data", e);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error updating stations", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error updating from handlers", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error updating stations", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadFromFirebase(IFirebaseDao firebaseDao) {
        new Thread(() -> {
            try {
                allGasStations = firebaseDao.readFromDatabase();
                Log.d(TAG, "Loaded " + allGasStations.size() + " stations from Firebase");
                
                // Add markers on UI thread
                runOnUiThread(() -> {
                    for (GasStation station : allGasStations) {
                        Marker marker = createMarkerForStation(station);
                        currentMarkers.add(marker);
                        map.getOverlays().add(marker);
                    }
                    map.invalidate(); // refresh map
                    Toast.makeText(this, "Loaded " + allGasStations.size() + " stations", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading from Firebase", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading stations", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.getController().animateTo(currentLocation);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Required for older Android versions
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Called when the provider is enabled by the user
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Called when the provider is disabled by the user
        Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
    }

    private void addCustomMarker(GasStation station) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(station.getGps().getLat(), station.getGps().getLng()));
        
        // Create custom marker icon with price
        View markerView = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        TextView priceText = markerView.findViewById(R.id.price_text);
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        priceText.setText(String.format(Locale.US, "%.2f", price));
        
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap markerBitmap = Bitmap.createBitmap(
            markerView.getMeasuredWidth(),
            markerView.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888);
        
        markerView.draw(new android.graphics.Canvas(markerBitmap));
        marker.setIcon(new BitmapDrawable(getResources(), markerBitmap));

        // Set detailed information in the marker popup
        StringBuilder info = new StringBuilder();
        info.append(station.getCompany()).append("\n");
        info.append("Address: ").append(station.getAddress()).append("\n");
        
        if (station.getOpening_hours() != null && !station.getOpening_hours().isEmpty()) {
            info.append("Opening Hours: ").append(station.getOpening_hours()).append("\n");
        }
        
        info.append("\nFuel Prices:\n");
        info.append("95: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getPetrol_95())).append("\n");
        info.append("98: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getPetrol_98())).append("\n");
        info.append("Diesel: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getDiesel()));

        marker.setTitle(station.getCompany());
        marker.setSnippet(info.toString());
        map.getOverlays().add(marker);
    }

    private void setupSearchResultsList() {
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsAdapter = new SearchResultsAdapter(station -> {
            // Handle station selection
            searchResultsRecyclerView.setVisibility(View.GONE);
            locationOverlay.disableFollowLocation();
            GeoPoint stationPoint = new GeoPoint(station.getGps().getLat(), station.getGps().getLng());
            map.getController().animateTo(stationPoint);
            map.getController().setZoom(17.0);
        });
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
    }

    private void filterStations(String query) {
        // Clear current markers
        for (Marker marker : currentMarkers) {
            map.getOverlays().remove(marker);
        }
        currentMarkers.clear();

        if (query == null || query.trim().isEmpty()) {
            // Hide search results and show all stations
            searchResultsRecyclerView.setVisibility(View.GONE);
            for (GasStation station : allGasStations) {
                Marker marker = createMarkerForStation(station);
                currentMarkers.add(marker);
                map.getOverlays().add(marker);
            }
        } else {
            // Filter stations and show in dropdown
            String lowercaseQuery = query.toLowerCase().trim();
            List<GasStation> matchingStations = new ArrayList<>();
            
            for (GasStation station : allGasStations) {
                if (station.getAddress().toLowerCase().contains(lowercaseQuery)) {
                    matchingStations.add(station);
                    Marker marker = createMarkerForStation(station);
                    currentMarkers.add(marker);
                    map.getOverlays().add(marker);
                }
            }

            // Update search results list
            if (!matchingStations.isEmpty()) {
                searchResultsAdapter.setStations(matchingStations);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
            } else {
                searchResultsRecyclerView.setVisibility(View.GONE);
            }
        }
        
        map.invalidate();
    }

    private Marker createMarkerForStation(GasStation station) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(station.getGps().getLat(), station.getGps().getLng()));
        
        // Create custom marker icon with price
        View markerView = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        TextView priceText = markerView.findViewById(R.id.price_text);
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        priceText.setText(String.format(Locale.US, "%.2f", price));
        
        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap markerBitmap = Bitmap.createBitmap(
            markerView.getMeasuredWidth(),
            markerView.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888);
        
        markerView.draw(new android.graphics.Canvas(markerBitmap));
        marker.setIcon(new BitmapDrawable(getResources(), markerBitmap));

        // Set detailed information in the marker popup
        StringBuilder info = new StringBuilder();
        info.append(station.getCompany()).append("\n");
        info.append("Address: ").append(station.getAddress()).append("\n");
        
        if (station.getOpening_hours() != null && !station.getOpening_hours().isEmpty()) {
            info.append("Opening Hours: ").append(station.getOpening_hours()).append("\n");
        }
        
        info.append("\nFuel Prices:\n");
        info.append("95: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getPetrol_95())).append("\n");
        info.append("98: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getPetrol_98())).append("\n");
        info.append("Diesel: ₪").append(String.format(Locale.US, "%.2f", station.getFuel_prices().getDiesel()));

        marker.setTitle(station.getCompany());
        marker.setSnippet(info.toString());
        return marker;
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        locationOverlay.disableMyLocation();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private static class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
        private List<GasStation> stations = new ArrayList<>();
        private final OnStationClickListener listener;
        private boolean showingDiesel = false;

        interface OnStationClickListener {
            void onStationClick(GasStation station);
        }

        SearchResultsAdapter(OnStationClickListener listener) {
            this.listener = listener;
        }

        void setStations(List<GasStation> stations) {
            this.stations = stations;
            notifyDataSetChanged();
        }

        void setShowingDiesel(boolean showingDiesel) {
            this.showingDiesel = showingDiesel;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GasStation station = stations.get(position);
            holder.companyText.setText(station.getCompany());
            holder.addressText.setText(station.getAddress());
            
            // Show either diesel or 95 price based on current selection
            double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
            String fuelType = showingDiesel ? "DSL" : "95";
            holder.priceText.setText(String.format(Locale.US, "%s: ₪%.2f", fuelType, price));
            
            holder.itemView.setOnClickListener(v -> listener.onStationClick(station));
        }

        @Override
        public int getItemCount() {
            return stations.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView companyText;
            final TextView addressText;
            final TextView priceText;

            ViewHolder(View view) {
                super(view);
                companyText = view.findViewById(R.id.stationCompany);
                addressText = view.findViewById(R.id.stationAddress);
                priceText = view.findViewById(R.id.stationPrice);
            }
        }
    }
}