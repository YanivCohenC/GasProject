package com.yaniv.FullTank.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;

import com.yaniv.FullTank.R;
import com.yaniv.FullTank.dm.GasStation;

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
    /** List of gas stations to display */
    private List<GasStation> stations = new ArrayList<>();
    private Set<Integer> disabledStations = new HashSet<>();  // Store IDs of disabled stations
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

    private static class StationDiffCallback extends DiffUtil.Callback {
        private final List<GasStation> oldList;
        private final List<GasStation> newList;

        StationDiffCallback(List<GasStation> oldList, List<GasStation> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            GasStation oldStation = oldList.get(oldItemPosition);
            GasStation newStation = newList.get(newItemPosition);
            return oldStation.getCompany().equals(newStation.getCompany()) &&
                   oldStation.getAddress().equals(newStation.getAddress()) &&
                   oldStation.getDistance() == newStation.getDistance() &&
                   oldStation.getFuel_prices().getPetrol_95() == newStation.getFuel_prices().getPetrol_95() &&
                   oldStation.getFuel_prices().getDiesel() == newStation.getFuel_prices().getDiesel();
        }
    }

    /**
     * Updates the list of stations and refreshes the view while preserving disabled states
     */
    public void setStations(List<GasStation> newStations) {
        List<GasStation> oldList = new ArrayList<>(stations);
        List<GasStation> newList = new ArrayList<>(newStations != null ? newStations : new ArrayList<>());
        
        // Calculate the difference between old and new lists
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new StationDiffCallback(oldList, newList));

        // Store current disabled states
        Set<Integer> currentDisabled = new HashSet<>(disabledStations);
        
        // Clear and update disabled states for new stations
        Set<Integer> newDisabledStations = new HashSet<>();
        for (GasStation station : newList) {
            if (currentDisabled.contains(station.getId())) {
                newDisabledStations.add(station.getId());
            }
        }

        // Update the data atomically
        stations = newList;
        disabledStations = newDisabledStations;

        // Dispatch updates to the RecyclerView
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Updates the list of disabled stations
     */
    public void updateDisabledStations(Set<Integer> filteredStationIds) {
        List<Integer> changedPositions = new ArrayList<>();
        Set<Integer> newDisabledStations = new HashSet<>();
        
        for (int i = 0; i < stations.size(); i++) {
            GasStation station = stations.get(i);
            boolean wasDisabled = disabledStations.contains(station.getId());
            boolean shouldBeDisabled = !filteredStationIds.contains(station.getId());
            
            if (shouldBeDisabled) {
                newDisabledStations.add(station.getId());
            }
            
            if (wasDisabled != shouldBeDisabled) {
                changedPositions.add(i);
            }
        }
        
        // Update disabled stations atomically
        disabledStations = newDisabledStations;
        
        // Notify only the items that changed state
        for (int position : changedPositions) {
            notifyItemChanged(position, "disabled_state_changed");
        }
    }

    /**
     * Clears all disabled states, making all stations clickable
     */
    public void clearDisabledStations() {
        if (!disabledStations.isEmpty()) {
            List<Integer> previouslyDisabledPositions = new ArrayList<>();
            
            for (int i = 0; i < stations.size(); i++) {
                if (disabledStations.contains(stations.get(i).getId())) {
                    previouslyDisabledPositions.add(i);
                }
            }
            
            // Clear disabled stations atomically
            disabledStations = new HashSet<>();
            
            // Notify only the items that were enabled
            for (int position : previouslyDisabledPositions) {
                notifyItemChanged(position, "disabled_state_changed");
            }
        }
    }

    /**
     * Toggles between showing diesel or 95 fuel prices
     */
    public void setShowingDiesel(boolean showingDiesel) {
        this.showingDiesel = showingDiesel;
        notifyItemRangeChanged(0, stations.size(), "price_type_changed");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_station_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, List<Object> payloads) {
        if (!payloads.isEmpty()) {
            GasStation station = stations.get(position);
            for (Object payload : payloads) {
                if (payload.equals("price_type_changed")) {
                    // Update only the price
                    double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
                    holder.priceText.setText(String.format(Locale.US, "₪%.2f", price));
                    holder.priceText.setTextColor(Color.parseColor("#0077cc"));
                } else if (payload.equals("disabled_state_changed")) {
                    // Update only the disabled state
                    boolean isDisabled = disabledStations.contains(station.getId());
                    float alpha = isDisabled ? 0.5f : 1.0f;
                    holder.companyText.setAlpha(alpha);
                    holder.addressText.setAlpha(alpha);
                    holder.priceText.setAlpha(alpha);
                    holder.distanceText.setAlpha(alpha);
                    holder.itemView.setEnabled(!isDisabled);
                }
            }
        } else {
            onBindViewHolder(holder, position);
        }
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