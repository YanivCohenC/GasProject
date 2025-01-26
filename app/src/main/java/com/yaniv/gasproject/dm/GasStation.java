package com.yaniv.gasproject.dm;

public class GasStation {
    private int id;
    private String company;
    private String address;
    private GPS gps;
    private String opening_hours;
    private FuelPrices fuel_prices;
    private float distance;

    // No-argument constructor required for Firebase
    public GasStation() {
        // Initialize with default values
        this.id = 0;
        this.company = "";
        this.address = "";
        this.gps = new GPS(0, 0);
        this.opening_hours = "";
        this.fuel_prices = new FuelPrices(0, 0, 0);
        this.distance = 0;
    }

    public GasStation(int id, String address, String company, GPS gps, String opening_hours, FuelPrices prices) {
        this.id = id;
        this.address = address;
        this.company = company; // company, eg: ten, mika, etc..
        this.gps = gps; // coordinates
        this.opening_hours = opening_hours;
        this.fuel_prices = prices;
        this.distance = 0;
    }

    // Getters and setters
    public float getDistance() {
        return this.distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GPS getGps() {
        return gps;
    }

    public void setGps(GPS gps) {
        this.gps = gps;
    }

    public String getOpening_hours() {
        return opening_hours;
    }

    public void setOpening_hours(String opening_hours) {
        this.opening_hours = opening_hours;
    }

    public FuelPrices getFuel_prices() {
        return fuel_prices;
    }

    public void setFuel_prices(FuelPrices fuel_prices) {
        this.fuel_prices = fuel_prices;
    }

    // Keeping these for backward compatibility
    public GPS getCoordinates() {
        return gps;
    }

    public void setCoordinates(double lng, double lat) {
        this.gps = new GPS(lng, lat);
    }
}