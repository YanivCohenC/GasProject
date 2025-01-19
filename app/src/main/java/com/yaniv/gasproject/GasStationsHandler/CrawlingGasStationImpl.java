package com.yaniv.gasproject.GasStationsHandler;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import com.yaniv.gasproject.dm.FuelPrices;
import com.yaniv.gasproject.dm.GPS;
import com.yaniv.gasproject.dm.GasStation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CrawlingGasStationImpl implements IGasStationHandler {
    private static final String TAG = "CrawlingGasStationImpl";
    String url;
    String source;
    List<GasStation> stations;
    Context context;

    public CrawlingGasStationImpl(String url, String source, Context context) {
        this.url = url;
        this.source = source;
        this.context = context;
        this.stations = fetchGasStations(url, source);
    }

    @Override
    public List<GasStation> fetchGasStations(String query, String type) {
        if (!type.equals("mika")) {
            return new ArrayList<>();
        }

        List<GasStation> stations = new ArrayList<>();
        Set<String> processedStations = new HashSet<>(); // To avoid duplicates
        
        try {
            String html = IGasStationHandler.sendHTTPRequest(query);
            Document doc = Jsoup.parse(html);
            
            // Find all h2 elements
            Elements allH2 = doc.select("h2");
            Log.d(TAG, "All H2 elements: " + allH2.size());
            int id = 1000;
            for (Element h2 : allH2) {
                try {
                    String fullText = h2.text().trim();
                    
                    // Skip if already processed
                    if (processedStations.contains(fullText)) {
                        continue;
                    }
                    
                    processedStations.add(fullText);
                    String innerURL = h2.select("a[href]").attr("href");
                    if (!innerURL.startsWith("http"))
                        continue;
                    String innerHTML = IGasStationHandler.sendHTTPRequest(innerURL);
                    Document innerDoc = Jsoup.parse(innerHTML);
                    String openingHours = innerDoc.select(".info.activity-hours .sub-title").text();
                    Elements priceElements = innerDoc.select(".info.cash .list li");
                    String price95 = "";
                    String price98 = "";
                    String dieselPrice = "";
                    for (Element priceElement : priceElements) {
                        String fuelType = priceElement.select(".sub-title").text();
                        String price = priceElement.select("span.value").text().replace("₪", "").trim();
                        switch (fuelType) {
                            case "בנזין 95":
                                price95 = price;
                                break;
                            case "בנזין 98":
                                price98 = price;
                                break;
                            case "סולר":
                                dieselPrice = price;
                                break;
                        }
                    }
                    // Remove "חדש!" prefix if present, but keep the rest exactly as is
                    String stationText = fullText.startsWith("חדש!") ? 
                        fullText.substring("חדש!".length()).trim() : fullText;
                    
                    // Find the company name from the image alt attribute
                    String company = "unknown";
                    Element parent = h2.parent();
                    if (parent != null) {
                        Element img = parent.select("img[class^=alignnone]").first();
                        if (img != null) {
                            String alt = img.attr("alt");
                            if (!alt.isEmpty()) {
                                company = alt;
                            }
                            Log.d(TAG, "Found image with alt: " + alt + " and class: " + img.attr("class"));
                        } else {
                            Log.d(TAG, "No image found with class starting with alignnone");
                        }
                        String stationURL = parent.select("[href]").first().absUrl("href");
                    }
                    Geocoder geocode = new Geocoder(context, Locale.getDefault());
                    Address address = geocode.getFromLocationName(stationText, 1).get(0);
                    Double double95 = 0.0;
                    Double double98 = 0.0;
                    Double doubleDiesel = 0.0;
                    if (!price95.isEmpty())
                        double95 = Double.parseDouble(price95);
                    if (!price98.isEmpty())
                        double98 = Double.parseDouble(price98);
                    if (!dieselPrice.isEmpty())
                        doubleDiesel = Double.parseDouble(dieselPrice);
                    GasStation station = new GasStation(id++, stationText, company, new GPS(address.getLatitude(), address.getLongitude()), openingHours, new FuelPrices(double98,double95,doubleDiesel));
                    stations.add(station);
                    Log.d(TAG, "Added station: " + stationText + " (Company: " + company + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Error processing station element", e);
                    continue;
                }
            }
            
            Log.d(TAG, "Total stations found: " + stations.size());
        } catch (Exception e) {
            Log.e(TAG, "Error fetching stations", e);
            e.printStackTrace();
        }
        return stations;
    }
    
    private double parsePrice(String priceStr) {
        try {
            // Remove any non-numeric characters except decimal point
            priceStr = priceStr.replaceAll("[^0-9.]", "");
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public List<GasStation> getStations() {
        return stations;
    }
}
