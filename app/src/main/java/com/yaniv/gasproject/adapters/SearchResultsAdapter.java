package com.yaniv.gasproject.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;

import com.yaniv.gasproject.R;
import com.yaniv.gasproject.dm.GasStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying gas station search results in a RecyclerView.
 * This class manages the display and updates of gas station information in the search results list.
 * It uses DiffUtil for efficient updates and supports toggling between diesel and petrol prices.
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    /** List of gas stations to display */
    private List<GasStation> stations = new ArrayList<>();
    /** Listener for handling station selection events */
    private final OnStationClickListener listener;
    /** Flag to control which fuel price type to display (diesel or 95) */
    private boolean showingDiesel = false;

    /**
     * Interface for handling station selection events.
     * Implementing classes can receive callbacks when a station is selected from the list.
     */
    public interface OnStationClickListener {
        /**
         * Called when a station is clicked in the list
         * @param station The selected gas station
         */
        void onStationClick(GasStation station);
    }

    /**
     * Constructs a new adapter with the specified click listener
     * @param listener Callback for station selection events
     */
    public SearchResultsAdapter(OnStationClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of stations using DiffUtil for efficient updates.
     * This method calculates the minimum number of changes needed to update the list
     * and applies them with proper animations.
     *
     * @param newStations New list of stations to display
     */
    public void setStations(List<GasStation> newStations) {
        List<GasStation> newList = new ArrayList<>(newStations != null ? newStations : new ArrayList<>());
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return stations.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return stations.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                GasStation oldStation = stations.get(oldItemPosition);
                GasStation newStation = newList.get(newItemPosition);
                
                // Compare all relevant fields to determine if an update is needed
                return oldStation.getCompany().equals(newStation.getCompany()) &&
                       oldStation.getAddress().equals(newStation.getAddress()) &&
                       oldStation.getDistance() == newStation.getDistance() &&
                       oldStation.getFuel_prices().getPetrol_95() == newStation.getFuel_prices().getPetrol_95() &&
                       oldStation.getFuel_prices().getDiesel() == newStation.getFuel_prices().getDiesel();
            }
        });
        
        stations = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Toggles between showing diesel or 95 fuel prices in the list.
     * Only updates the price display without changing other station information.
     *
     * @param showingDiesel true to show diesel prices, false to show 95 prices
     */
    public void setShowingDiesel(boolean showingDiesel) {
        this.showingDiesel = showingDiesel;
        // Only price display is changing, so notify items changed with payload
        notifyItemRangeChanged(0, stations.size(), "price_type_changed");
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
        if (position >= stations.size()) return;  // Safety check
        
        GasStation station = stations.get(position);
        
        // Set basic station information
        holder.companyTextView.setText(station.getCompany());
        holder.addressTextView.setText(station.getAddress());
        
        // Display appropriate fuel price based on selection
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        holder.priceTextView.setText(String.format(Locale.US, "₪%.2f", price));
        holder.priceTextView.setTextColor(Color.parseColor("#0077cc"));
        
        // Format and display distance
        if (station.getDistance() < 1000) {
            holder.distanceTextView.setText(String.format(Locale.US, "%.0fm", station.getDistance()));
        } else {
            holder.distanceTextView.setText(String.format(Locale.US, "%.1fkm", station.getDistance() / 1000));
        }
        
        // Setup click listener for station selection
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * ViewHolder class for caching view references.
     * Holds references to all the TextViews that display station information.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the station company name */
        final TextView companyTextView;
        /** TextView for displaying the station address */
        final TextView addressTextView;
        /** TextView for displaying the fuel price */
        final TextView priceTextView;
        /** TextView for displaying the distance to the station */
        final TextView distanceTextView;

        /**
         * Constructs a new ViewHolder and finds all required views
         * @param view The root view for this item
         */
        ViewHolder(View view) {
            super(view);
            companyTextView = view.findViewById(R.id.stationCompany);
            addressTextView = view.findViewById(R.id.stationAddress);
            priceTextView = view.findViewById(R.id.stationPrice);
            distanceTextView = view.findViewById(R.id.stationDistance);
        }
    }
} 