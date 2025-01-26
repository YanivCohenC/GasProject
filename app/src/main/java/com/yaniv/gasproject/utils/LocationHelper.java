package com.yaniv.gasproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class LocationHelper {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private final Context context;
    private final LocationManager locationManager;
    private final MapView map;
    private MyLocationNewOverlay locationOverlay;
    private LocationListener locationListener;
    private final Activity activity;

    public LocationHelper(Activity activity, MapView map) {
        this.activity = activity;
        this.context = activity;
        this.map = map;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setupLocationOverlay();
    }

    private void setupLocationOverlay() {
        GpsMyLocationProvider provider = new GpsMyLocationProvider(context);
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
                        double eastLon = lon + angular / Math.cos(lat);
                        
                        GeoPoint edgePoint = new GeoPoint(Math.toDegrees(lat), Math.toDegrees(eastLon));
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
        map.getOverlays().add(locationOverlay);
    }

    public void checkLocationPermission(Runnable onPermissionGranted) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            onPermissionGranted.run();
        }
    }

    public void handlePermissionResult(int requestCode, int[] grantResults, Runnable onGranted, Runnable onDenied) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGranted.run();
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show();
                onDenied.run();
            }
        }
    }

    public void startLocationUpdates(LocationListener listener) {
        this.locationListener = listener;
        if (checkLocationPermission()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, listener);
            locationOverlay.enableMyLocation();
            
            // Get last known location
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                GeoPoint startPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                map.getController().setCenter(startPoint);
            }
        }
    }

    public void stopLocationUpdates() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        locationOverlay.disableMyLocation();
    }

    public Location getLastLocation() {
        return locationOverlay.getLastFix();
    }

    public void enableFollowLocation() {
        locationOverlay.enableFollowLocation();
    }

    public void disableFollowLocation() {
        locationOverlay.disableFollowLocation();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
} 