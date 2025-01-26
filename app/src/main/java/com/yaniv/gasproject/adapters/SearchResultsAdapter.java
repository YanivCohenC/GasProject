package com.yaniv.gasproject.adapters;

import static java.lang.String.*;

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
 * Adapter for displaying search results in a RecyclerView.
 * Shows gas station details including company name, address, price, and distance.
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private List<GasStation> stations = new ArrayList<>();
    private final OnStationClickListener listener;
    private boolean showingDiesel = false;  // Controls which fuel price to display

    /**
     * Interface for handling station selection events
     */
    public interface OnStationClickListener {
        void onStationClick(GasStation station);
    }

    public SearchResultsAdapter(OnStationClickListener listener) {
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
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    /** @noinspection ClassEscapesDefinedScope*/
    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GasStation station = stations.get(position);
        
        // Set basic station information
        holder.companyTextView.setText(station.getCompany());
        holder.addressTextView.setText(station.getAddress());
        
        // Display appropriate fuel price based on selection
        double price = showingDiesel ? station.getFuel_prices().getDiesel() : station.getFuel_prices().getPetrol_95();
        holder.priceTextView.setText(format("₪%.2f", price));
        holder.priceTextView.setTextColor(Color.parseColor("#0077cc"));
        
        // Format and display distance
        if (station.getDistance() < 1000) {
            holder.distanceTextView.setText(format(Locale.US, "%.0fm", station.getDistance()));
        } else {
            holder.distanceTextView.setText(format(Locale.US, "%.1fkm", station.getDistance() / 1000));
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
     * ViewHolder class for caching view references
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView companyTextView;
        final TextView addressTextView;
        final TextView priceTextView;
        final TextView distanceTextView;

        ViewHolder(View view) {
            super(view);
            companyTextView = view.findViewById(R.id.stationCompany);
            addressTextView = view.findViewById(R.id.stationAddress);
            priceTextView = view.findViewById(R.id.stationPrice);
            distanceTextView = view.findViewById(R.id.stationDistance);
        }
    }
} 