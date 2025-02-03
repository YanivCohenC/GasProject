package com.yaniv.gasproject.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yaniv.gasproject.R;
import com.yaniv.gasproject.dm.GasStation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for displaying nearby gas stations in a RecyclerView.
 * Shows gas station details including company name, address, price, and distance.
 * Similar to SearchResultsAdapter but specifically for the nearby stations list.
 */
public class NearbyStationsAdapter extends RecyclerView.Adapter<NearbyStationsAdapter.ViewHolder> {
    private List<GasStation> stations = new ArrayList<>();
    private final Set<Integer> disabledStations = new HashSet<>();  // Store IDs of disabled stations
    private final OnStationClickListener listener;
    private boolean showingDiesel = false;  // Controls which fuel price to display

    /**
     * Interface for handling station selection events
     */
    public interface OnStationClickListener {
        void onStationClick(GasStation station);
    }

    public NearbyStationsAdapter(OnStationClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of stations and refreshes the view while preserving disabled states
     */
    public void setStations(List<GasStation> newStations) {
        // Store current disabled states
        Set<Integer> currentDisabled = new HashSet<>(disabledStations);
        
        int oldSize = this.stations.size();
        this.stations = new ArrayList<>(newStations);
        
        // Re-apply disabled states
        disabledStations.clear();
        if (!currentDisabled.isEmpty()) {
            for (GasStation station : stations) {
                if (currentDisabled.contains(station.getId())) {
                    disabledStations.add(station.getId());
                }
            }
        }
        
        if (oldSize == 0) {
            notifyItemRangeInserted(0, newStations.size());
        } else if (newStations.isEmpty()) {
            notifyItemRangeRemoved(0, oldSize);
        } else {
            // If both lists have items, notify of the change
            notifyItemRangeChanged(0, Math.max(oldSize, newStations.size()));
        }
    }

    /**
     * Toggles between showing diesel or 95 fuel prices
     */
    public void setShowingDiesel(boolean showingDiesel) {
        this.showingDiesel = showingDiesel;
        // Only price display is changing, so notify items changed
        notifyItemRangeChanged(0, stations.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_station_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        GasStation station = stations.get(position);
        boolean isDisabled = disabledStations.contains(station.getId());

        // Set basic station information
        holder.companyText.setText(station.getCompany());
        holder.addressText.setText(station.getAddress());
        
        // Display appropriate fuel price based on selection
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        holder.priceText.setText(String.format(Locale.US, "₪%.2f", price));
        holder.priceText.setTextColor(Color.parseColor("#0077cc"));
        
        // Format and display distance
        if (station.getDistance() < 1000) {
            holder.distanceText.setText(String.format(Locale.US, "%.0fm", station.getDistance()));
        } else {
            holder.distanceText.setText(String.format(Locale.US, "%.1fkm", station.getDistance() / 1000));
        }
        
        // Apply disabled state to all views
        float alpha = isDisabled ? 0.5f : 1.0f;
        holder.companyText.setAlpha(alpha);
        holder.addressText.setAlpha(alpha);
        holder.priceText.setAlpha(alpha);
        holder.distanceText.setAlpha(alpha);
        holder.itemView.setEnabled(!isDisabled);
        
        // Setup click listener for station selection
        holder.itemView.setOnClickListener(v -> {
            if (!isDisabled && listener != null) {
                listener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * Updates the list of disabled stations
     * @param filteredStationIds IDs of stations that should be enabled (not greyed out)
     */
    public void updateDisabledStations(Set<Integer> filteredStationIds) {
        disabledStations.clear();
        for (GasStation station : stations) {
            if (!filteredStationIds.contains(station.getId())) {
                disabledStations.add(station.getId());
            }
        }
        // Ensure all items are redrawn with their correct disabled state
        notifyItemRangeChanged(0, stations.size(), "disabled_state_changed");
    }

    /**
     * Clears all disabled states, making all stations clickable
     */
    public void clearDisabledStations() {
        if (!disabledStations.isEmpty()) {
            disabledStations.clear();
            // Ensure all items are redrawn with their correct enabled state
            notifyItemRangeChanged(0, stations.size(), "disabled_state_changed");
        }
    }

    /**
     * ViewHolder class for caching view references
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView companyText;
        final TextView addressText;
        final TextView priceText;
        final TextView distanceText;

        ViewHolder(View view) {
            super(view);
            companyText = view.findViewById(R.id.stationCompany);
            addressText = view.findViewById(R.id.stationAddress);
            priceText = view.findViewById(R.id.stationPrice);
            distanceText = view.findViewById(R.id.stationDistance);
        }
    }
} 