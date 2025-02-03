package com.yaniv.gasproject.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.FrameLayout;

import com.yaniv.gasproject.R;
import com.yaniv.gasproject.dm.GasStation;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.Locale;

/**
 * Utility class for managing map markers and their associated info windows.
 * Provides functionality for creating, caching, and displaying markers on the map
 * with custom price bubbles and information windows.
 */
public class MarkerUtils {
    /** Cache for storing marker bitmap images to improve performance */
    private static final LruCache<String, Bitmap> bitmapCache = new LruCache<>(100) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return 1; // Count each bitmap as 1 unit
        }
    };

    /**
     * Generates a unique cache key for a price marker bitmap
     * @param price The fuel price to display
     * @param isDiesel Whether the price is for diesel fuel
     * @return A unique string key for the bitmap cache
     */
    private static String getCacheKey(double price, boolean isDiesel) {
        return String.format(Locale.US, "%.2f_%b", price, isDiesel);
    }

    private static Bitmap createMarkerBitmap(Context context, double price) {
        // Create a FrameLayout as the root view with wrap_content dimensions
        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // Inflate the marker layout with the root view
        View markerView = LayoutInflater.from(context).inflate(R.layout.marker_layout, root, false);
        TextView priceText = markerView.findViewById(R.id.price_text);
        priceText.setText(String.format(Locale.US, "%.2f", price));
        
        markerView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap markerBitmap = Bitmap.createBitmap(
            markerView.getMeasuredWidth(),
            markerView.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888);
        
        markerView.draw(new android.graphics.Canvas(markerBitmap));
        return markerBitmap;
    }

    /**
     * Custom info window class for displaying detailed station information
     * when a marker is tapped. Includes station details and a navigation button.
     */
    public static class StationInfoWindow extends InfoWindow {
        private final Context context;
        private final GasStation station;
        private final Location userLocation;

        /**
         * Constructs a new info window for a gas station
         * @param context Application context
         * @param mapView The map view to attach the window to
         * @param station The gas station to display information for
         * @param userLocation Current user location for distance calculation
         */
        public StationInfoWindow(Context context, MapView mapView, GasStation station, Location userLocation) {
            super(R.layout.station_info_window, mapView);
            this.context = context;
            this.station = station;
            this.userLocation = userLocation;
        }

        /**
         * Calculates the distance between the user and the gas station
         * @param userLocation Current user location
         * @param station The gas station to calculate distance to
         * @return Distance in meters
         */
        private float calculateDistance(Location userLocation, GasStation station) {
            if (userLocation == null) return 0;
            
            Location stationLocation = new Location("");
            stationLocation.setLatitude(station.getGps().getLat());
            stationLocation.setLongitude(station.getGps().getLng());
            return userLocation.distanceTo(stationLocation);
        }

        /**
         * Formats a distance value into a human-readable string
         * @param distance Distance in meters
         * @return Formatted string with appropriate units (m or km)
         */
        private String formatDistance(float distance) {
            return distance < 1000 
                ? String.format(Locale.US, "%.0fm", distance)
                : String.format(Locale.US, "%.1fkm", distance / 1000);
        }

        @Override
        public void onOpen(Object item) {
            // Close any previously open windows on the map
            InfoWindow.closeAllInfoWindowsOn(mMapView);

            TextView contentView = mView.findViewById(R.id.info_window_content);
            Button navigateButton = mView.findViewById(R.id.navigate_button);
            
            // Set the station information
            StringBuilder info = new StringBuilder();
            
            // Company name and address sections
            info.append("🏢 ").append(station.getCompany()).append("\n");
            info.append("📍 ").append(station.getAddress()).append("\n");
            
            // Distance section (if available)
            if (userLocation != null) {
                float distance = calculateDistance(userLocation, station);
                info.append("📏 ").append(formatDistance(distance)).append("\n");
            }
            
            // Opening hours section (if available)
            if (station.getOpening_hours() != null && !station.getOpening_hours().isEmpty()) {
                info.append("⏰ ").append(station.getOpening_hours()).append("\n");
            }
            
            // Fuel prices section - only show non-zero prices
            boolean hasAnyPrice = false;
            StringBuilder pricesInfo = new StringBuilder();
            
            if (station.getFuel_prices().getPetrol_95() > 0) {
                pricesInfo.append(String.format(Locale.US, "95: ₪%.2f  ", station.getFuel_prices().getPetrol_95()));
                hasAnyPrice = true;
            }
            if (station.getFuel_prices().getPetrol_98() > 0) {
                pricesInfo.append(String.format(Locale.US, "98: ₪%.2f  ", station.getFuel_prices().getPetrol_98()));
                hasAnyPrice = true;
            }
            if (station.getFuel_prices().getDiesel() > 0) {
                pricesInfo.append(String.format(Locale.US, "סולר: ₪%.2f", station.getFuel_prices().getDiesel()));
                hasAnyPrice = true;
            }
            
            if (hasAnyPrice) {
                info.append("\n⛽ ").append(pricesInfo);
            }

            contentView.setText(info.toString());
            
            // Set up navigation button click listener
            navigateButton.setOnClickListener(v -> {
                String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                    station.getGps().getLat(), station.getGps().getLng(),
                    station.getGps().getLat(), station.getGps().getLng(),
                    station.getCompany() + " - " + station.getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                close(); // Close the info window after starting navigation
            });
        }

        @Override
        public void onClose() {
            // No need to manage static reference anymore
        }
    }

    /**
     * Creates a marker for a gas station on the map
     * @param context Application context
     * @param station The gas station to create a marker for
     * @param showingDiesel Whether to show diesel or 95 fuel prices
     * @param userLocation Current user location for distance calculation
     * @return A configured marker ready to be added to the map
     */
    public static Marker createMarkerForStation(Context context, MapView map, GasStation station,
            boolean showingDiesel, Location userLocation) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(station.getGps().getLat(), station.getGps().getLng()));
        
        // Get price for marker
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        
        // Try to get bitmap from cache
        String cacheKey = getCacheKey(price, showingDiesel);
        Bitmap markerBitmap = bitmapCache.get(cacheKey);
        
        // Create and cache bitmap if not found
        if (markerBitmap == null) {
            markerBitmap = createMarkerBitmap(context, price);
            bitmapCache.put(cacheKey, markerBitmap);
        }
        
        marker.setIcon(new BitmapDrawable(context.getResources(), markerBitmap));
        
        // Set custom info window
        marker.setInfoWindow(new StationInfoWindow(context, map, station, userLocation));

        return marker;
    }
} 