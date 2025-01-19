package com.yaniv.gasproject.DatabaseHandler;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yaniv.gasproject.dm.GasStation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenericFirebaseImpl implements IFirebaseDao {
    private static final String TAG = "GenericFirebaseImpl";
    private final FirebaseDatabase database;
    private final DatabaseReference stationsRef;

    public GenericFirebaseImpl() {
        database = FirebaseDatabase.getInstance();
        stationsRef = database.getReference("stations");
    }

    @Override
    public void saveToDatabase(List<GasStation> stations) {
        try {
            // Clear existing data
            stationsRef.removeValue();
            
            // Save each station
            for (GasStation station : stations) {
                DatabaseReference stationRef = stationsRef.child(String.valueOf(station.getId()));
                stationRef.setValue(station)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Station saved successfully: " + station.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving station: " + station.getId(), e));
            }

            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            String date = (month + "" + year);
            // Save last update timestamp
            database.getReference("lastUpdated").setValue(date);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving stations to database", e);
        }
    }

    @Override
    public List<GasStation> readFromDatabase() {
        CompletableFuture<List<GasStation>> future = new CompletableFuture<>();
        List<GasStation> stations = new ArrayList<>();

        stationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot stationSnapshot : dataSnapshot.getChildren()) {
                    try {
                        GasStation station = stationSnapshot.getValue(GasStation.class);
                        if (station != null) {
                            stations.add(station);
                            Log.d(TAG, "Read station: " + station.getId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing station data", e);
                    }
                }
                future.complete(stations);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error reading from database", databaseError.toException());
                future.completeExceptionally(databaseError.toException());
            }
        });

        try {
            return future.get(); // Wait for the data
        } catch (Exception e) {
            Log.e(TAG, "Error waiting for database read", e);
            return new ArrayList<>();
        }
    }
}
