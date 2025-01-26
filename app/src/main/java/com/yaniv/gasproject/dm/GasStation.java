package com.yaniv.gasproject.dm;

public class GasStation {
    private int id;
    private final String company;
    private final String address;
    private final GPS gps;
    private final String opening_hours;
    private float distance;
    private final FuelPrices fuel_prices;

    // No-argument constructor required for Firebase
    public GasStation() {
        // Initialize with default values
        this.id = 0;
        this.company = "";
        this.address = "";
        this.gps = new GPS(0, 0);
        this.opening_hours = "";
        this.fuel_prices = new FuelPrices(0, 0, 0);
    }

    public GasStation(int id, String address, String company, GPS gps, String opening_hours, FuelPrices prices) {
        this.id = id;
        this.address = address;
        this.company = company; // company, eg: ten, mika, etc..
        this.gps = gps; // coordinates
        this.opening_hours = opening_hours;
        this.fuel_prices = prices;
    }

    // Getters and setters
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompany() {
        return this.company;
    }

    public String getAddress() {
        return address;
    }

    public GPS getGps() {
        return gps;
    }

    public String getOpening_hours() {
        return opening_hours;
    }

    public FuelPrices getFuel_prices() {
        return fuel_prices;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return this.distance;
    }
}