package com.yaniv.gasproject.adapters;

import android.annotation.SuppressLint;
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
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying nearby gas stations in a RecyclerView.
 * Shows gas station details including company name, address, price, and distance.
 * Similar to SearchResultsAdapter but specifically for the nearby stations list.
 * @noinspection ALL
 */
public class NearbyStationsAdapter extends RecyclerView.Adapter<NearbyStationsAdapter.ViewHolder> {
    private List<GasStation> stations = new ArrayList<>();
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
     * Updates the list of stations and refreshes the view
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setStations(List<GasStation> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    /**
     * Toggles between showing diesel or 95 fuel prices
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setShowingDiesel(boolean showingDiesel) {
        this.showingDiesel = showingDiesel;
        notifyDataSetChanged();
    }

    /** @noinspection ClassEscapesDefinedScope*/
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nearby_station_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GasStation station = stations.get(position);

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
        
        // Setup click listener for station selection
        holder.itemView.setOnClickListener(v -> listener.onStationClick(station));
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * ViewHolder class for caching view references
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
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