package com.yaniv.FullTank.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.yaniv.FullTank.dao.GenericFirebaseImpl;
import com.yaniv.FullTank.dao.IFirebaseDao;
import com.yaniv.FullTank.dm.GasStation;
import com.yaniv.FullTank.handlers.APIGasStationImpl;
import com.yaniv.FullTank.handlers.CrawlingGasStationImpl;
import com.yaniv.FullTank.handlers.GenericGasStationImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GasStationDataManager {
    private static final String TAG = "GasStationDataManager";
    private final Context context;
    private final IFirebaseDao firebaseDao;
    private List<GasStation> allGasStations = new ArrayList<>();
    private final DataUpdateCallback callback;

    public interface DataUpdateCallback {
        void onDataLoaded(List<GasStation> stations);
        void onDataLoadFailed(String message);
        void onLoadingStarted(String message);
    }

    public GasStationDataManager(Context context, DataUpdateCallback callback) {
        this.context = context;
        this.callback = callback;
        this.firebaseDao = new GenericFirebaseImpl();
    }

    public void loadGasStations() {
        callback.onLoadingStarted("Loading gas stations...");

        // Check last update date in Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://fulltank-a5b8b-default-rtdb.europe-west1.firebasedatabase.app/");
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
                    callback.onLoadingStarted("Updating gas station data...");
                    new Thread(this::updateFromHandlers).start();
                } else {
                    // Data is current, just load from Firebase
                    loadFromFirebase();
                }
            } else {
                // Error getting last update date, load from Firebase anyway
                Log.e(TAG, "Error checking last update date", task.getException());
                loadFromFirebase();
            }
        });
    }

    private void updateFromHandlers() {
        try {
            // Initialize gas station handlers
            String tenAPI = "https://10ten.co.il/website_api/website/1.0/generalDeclaration";
            String mikaCrawling = "https://mika.org.il/%D7%9B%D7%9C-%D7%94%D7%9E%D7%AA%D7%97%D7%9E%D7%99%D7%9D/";
            
            // Create handlers in parallel
            CompletableFuture<List<GasStation>> tenFuture = CompletableFuture.supplyAsync(() -> {
                APIGasStationImpl worker = new APIGasStationImpl(tenAPI, "ten");
                return worker.getStations();
            });

            CompletableFuture<List<GasStation>> mikaFuture = CompletableFuture.supplyAsync(() -> {
                CrawlingGasStationImpl worker = new CrawlingGasStationImpl(mikaCrawling, "mika", context);
                return worker.getStations();
            });

            CompletableFuture<List<GasStation>> genericFuture = CompletableFuture.supplyAsync(() -> {
                GenericGasStationImpl worker = new GenericGasStationImpl(context);
                return worker.getStations();
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

                    allGasStations = allStations;
                    callback.onDataLoaded(allGasStations);
                } catch (Exception e) {
                    Log.e(TAG, "Error combining station data", e);
                    callback.onDataLoadFailed("Error updating stations");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error updating from handlers", e);
            callback.onDataLoadFailed("Error updating stations");
        }
    }

    private void loadFromFirebase() {
        new Thread(() -> {
            try {
                allGasStations = firebaseDao.readFromDatabase();
                Log.d(TAG, "Loaded " + allGasStations.size() + " stations from Firebase");
                callback.onDataLoaded(allGasStations);
            } catch (Exception e) {
                Log.e(TAG, "Error loading from Firebase", e);
                callback.onDataLoadFailed("Error loading stations");
            }
        }).start();
    }

    public List<GasStation> filterStations(String query, Location userLocation, boolean showingDiesel, boolean sortByPrice) {
        if (query == null || query.trim().isEmpty()) {
            return allGasStations;
        }

        String lowercaseQuery = query.toLowerCase().trim();
        List<GasStation> matchingStations = new ArrayList<>();
        
        for (GasStation station : allGasStations) {
            if (station.getAddress().toLowerCase().contains(lowercaseQuery)) {
                float distance = 0;
                if (userLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        station.getGps().getLat(), station.getGps().getLng(),
                        results
                    );
                    distance = results[0];
                }
                station.setDistance(distance);
                matchingStations.add(station);
            }
        }

        sortStations(matchingStations, userLocation, showingDiesel, sortByPrice);
        return matchingStations;
    }

    public List<GasStation> getNearbyStations(Location userLocation, boolean showingDiesel, boolean sortByPrice, float maxDistance) {
        List<GasStation> nearbyStations = new ArrayList<>();
        
        for (GasStation station : allGasStations) {
            float[] results = new float[1];
            Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                station.getGps().getLat(), station.getGps().getLng(),
                results
            );
            if (results[0] <= maxDistance) {
                station.setDistance(results[0]);
                nearbyStations.add(station);
            }
        }

        sortStations(nearbyStations, userLocation, showingDiesel, sortByPrice);
        return nearbyStations.subList(0, Math.min(20, nearbyStations.size()));
    }

    private void sortStations(List<GasStation> stations, Location userLocation, boolean showingDiesel, boolean sortByPrice) {
        if (sortByPrice) {
            stations.sort((a, b) -> {
                double priceA = showingDiesel ? a.getFuel_prices().getDiesel() : a.getFuel_prices().getPetrol_95();
                double priceB = showingDiesel ? b.getFuel_prices().getDiesel() : b.getFuel_prices().getPetrol_95();
                return Double.compare(priceA, priceB);
            });
        } else if (userLocation != null) {
            stations.sort((a, b) -> Float.compare(a.getDistance(), b.getDistance()));
        }
    }

    public List<GasStation> getAllStations() {
        return allGasStations;
    }
} 