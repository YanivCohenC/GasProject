package com.yaniv.gasproject.handlers;

import static android.content.ContentValues.TAG;
import static java.lang.Double.max;

import android.util.Log;

import com.yaniv.gasproject.dm.FuelPrices;
import com.yaniv.gasproject.dm.GPS;
import com.yaniv.gasproject.dm.GasStation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class APIGasStationImpl implements IGasStationHandler {
    String query;
    String source;
    List<GasStation> stations;

    static FuelPrices defaultPrices;

    public List<GasStation> getStations() {
        return stations;
    }

    public APIGasStationImpl(String query, String source) {
        this.query = query;
        this.source = source;
        defaultPrices = new FuelPrices(0,0,0);
        this.stations = fetchGasStations(query, source);
    }

    public List<GasStation> fetchGasStations(String query, String source) {
        if (source.equals("ten"))
            return handleTenAPI(query);
        // Add more APIs in future...
        return Collections.emptyList();
    }

    public List<GasStation> handleTenAPI(String source) {
        String response = IGasStationHandler.sendHTTPRequest(source);
        List<GasStation> stations = new ArrayList<>();
        try {
            JsonObject jsonObject = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = jsonObject.getAsJsonObject("data");
            JsonArray stationsArr = data.getAsJsonArray("stationsArr");

            // Get regulated prices from the response
            double regulatedPricePetrol95 = 0.0;
            double regulatedPriceDiesel = 0.0;
            if (data.has("fuel_typesArr")) {
                JsonArray fuelTypesArr = data.getAsJsonArray("fuel_typesArr");
                for (JsonElement fuelElement : fuelTypesArr) {
                    JsonObject fuelType = fuelElement.getAsJsonObject();
                    String code = fuelType.get("code").getAsString();
                    if (code.equals("5") && fuelType.has("regulated_price_self_service")) { // Petrol 95
                        defaultPrices.setPetrol_95(fuelType.get("regulated_price_self_service").getAsDouble());
                    } else if (code.equals("0") && fuelType.has("regulated_price_self_service")) { // Diesel
                        defaultPrices.setDiesel(fuelType.get("regulated_price_self_service").getAsDouble());
                    }
                }
            }

            int id;
            for (JsonElement element : stationsArr) {
                JsonObject station = element.getAsJsonObject();

                String address = station.get("full_address").getAsString();

                JsonObject gpsObj = station.getAsJsonObject("gps");
                double lat = gpsObj.get("lat").getAsDouble();
                double lng = gpsObj.get("lng").getAsDouble();

                StringBuilder openingHours = new StringBuilder();
                JsonObject openingHoursObj = station.getAsJsonObject("opening_hours");

                for (Map.Entry<String, JsonElement> entry : openingHoursObj.entrySet()) {
                    String day = entry.getKey();
                    JsonObject dayObj = entry.getValue().getAsJsonObject();
                    JsonArray hoursArr = dayObj.getAsJsonArray("hoursArr");

                    if (hoursArr != null && !hoursArr.isEmpty()) {
                        JsonObject hours = hoursArr.get(0).getAsJsonObject();
                        String fromHour = hours.get("from_hour").getAsString();
                        String toHour = hours.get("to_hour").getAsString();

                        if (!fromHour.equals("0") || !toHour.equals("0")) {
                            openingHours.append("Day ").append(day)
                                    .append(": ").append(fromHour)
                                    .append("-").append(toHour)
                                    .append(", ");
                        }
                    }
                }

                JsonObject pricesObj = station.getAsJsonObject("fuel_prices");
                JsonObject byFuelType = pricesObj.getAsJsonObject("by_fuel_type");
                
                // Initialize default values
                double petrol95 = 0.0;
                double petrol98 = 0.0;
                double diesel = 0.0;
                
                // Get prices based on fuel_type_code
                if (byFuelType.has("5")) { // 95 octane
                    JsonObject fuel95 = byFuelType.getAsJsonObject("5");
                    petrol95 = max(fuel95.get("self_service").getAsDouble(), fuel95.get("cash").getAsDouble());

                    // If self_service price is 0.0, use regulated price
                    if (petrol95 == 0.0) {
                        petrol95 = regulatedPricePetrol95;
                    }
                }
                if (byFuelType.has("6")) { // 98 octane
                    JsonObject fuel98 = byFuelType.getAsJsonObject("6");
                    if (fuel98.has("self_service") && !fuel98.get("self_service").isJsonNull()) {
                        petrol98 = fuel98.get("self_service").getAsDouble();
                    }
                }
                if (byFuelType.has("0")) { // diesel
                    JsonObject fuelDiesel = byFuelType.getAsJsonObject("0");
                    diesel = max(fuelDiesel.get("self_service").getAsDouble(), fuelDiesel.get("cash").getAsDouble());
                    // If self_service price is 0.0, use regulated price
                    if (diesel == 0.0) {
                        diesel = regulatedPriceDiesel;
                    }
                }

                // Get unique id from API
                id = Integer.parseInt(station.get("id").getAsString());

                GasStation gasStation = new GasStation(id, address, "טן", new GPS(lat, lng), openingHours.toString(), new FuelPrices(petrol98, petrol95, diesel), true);
                stations.add(gasStation);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing station data", e);
        }
        return stations;
    }
}
