package com.yaniv.gasproject.handlers;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yaniv.gasproject.dm.FuelPrices;
import com.yaniv.gasproject.dm.GPS;
import com.yaniv.gasproject.dm.GasStation;
import com.yaniv.gasproject.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GenericGasStationImpl implements IGasStationHandler{
    Context context;
    List<GasStation> gasStations;

    public GenericGasStationImpl(Context context) {
        this.context = context;
        this.gasStations = fetchGasStations(null, "json");
    }

    @Override
    public List<GasStation> fetchGasStations(String query, String type) {
        if (type.equals("json")) {
            this.gasStations = readFromJsonFile();
        }
        return this.gasStations;
    }

    private GPS convertITMToWGS84(double x, double y) {
        // Constants for ITM to WGS84 conversion
        final double k0 = 1.0000067;
        final double a = 6378137.0;
        final double e = 0.081819191042816;
        final double lon0 = 0.61443473225468920;  // 35.2045169444444 degrees
        final double lat0 = 0.55386965463774187;  // 31.7343936111111 degrees
        final double n = 0.6145667421719;
        final double false_e = 219529.584;
        final double false_n = 626907.390;

        // Compute meridian arc
        double y1 = y - false_n;
        double x1 = x - false_e;

        // Compute lat/lon
        double lat = lat0 + (y1 / (a * k0));
        double lon = lon0 + (x1 / (a * k0 * Math.cos(lat0)));

        // Convert to degrees
        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        return new GPS(lat, lon);
    }

    public List<GasStation> readFromJsonFile() {
        List<GasStation> stations = new ArrayList<>();
        try {
            // Read the JSON file from raw resources
            InputStream is = context.getResources().openRawResource(R.raw.gasstations);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = bfr.readLine()) != null) {
                jsonContent.append(line);
            }
            bfr.close();

            // Parse JSON
            JsonObject jsonObject = new Gson().fromJson(jsonContent.toString(), JsonObject.class);
            JsonArray stationsArray = jsonObject.getAsJsonArray("stations");
            FuelPrices genericPrices = getDefaultPrices();
            int id = 2000;
            for (JsonElement element : stationsArray) {
                JsonObject station = element.getAsJsonObject();

                String address = station.get("כתובת").getAsString();
                String company = station.get("חברה").getAsString();

                // Parse ITM coordinates
                try {
                    double x = Double.parseDouble(station.get("X").getAsString().split("\\.")[0]);
                    double y = Double.parseDouble(station.get("Y").getAsString().split("\\.")[0]);

                    // Convert ITM to WGS84 (GPS)
                    GPS coordinates = convertITMToWGS84(x, y);

                    GasStation gasStation = new GasStation(
                            id++,
                            address,
                            company,
                            coordinates,
                            null, // No opening hours in the data
                            genericPrices // No prices in the data
                    );

                    stations.add(gasStation);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing coordinates for station: " + address);
                }
            }

            Log.d(TAG, "Total stations loaded from JSON: " + stations.size());

        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return stations;
    }

    public FuelPrices getDefaultPrices() {
        FuelPrices defaultPrices = new FuelPrices(0,0,0);
        String response = IGasStationHandler.sendHTTPRequest("https://10ten.co.il/website_api/website/1.0/generalDeclaration");
        JsonObject jsonObject = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
        JsonObject data = jsonObject.getAsJsonObject("data");

        // Get regulated prices from the response
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
        return defaultPrices;
    }

    public List<GasStation> getStations() {
        return gasStations;
    }
}
