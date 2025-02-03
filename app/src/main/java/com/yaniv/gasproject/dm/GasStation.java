package com.yaniv.gasproject.dm;

/**
 * Represents a gas station entity in the application.
 * This class holds all relevant information about a gas station including its
 * identification, location, pricing, and operational details.
 */
public class GasStation {
    /** Unique identifier for the gas station */
    private int id;
    /** Name of the gas station company (e.g., Ten, Mika, etc.) */
    private final String company;
    /** Physical address of the gas station */
    private final String address;
    /** Geographic coordinates of the station */
    private final GPS gps;
    /** Operating hours of the station */
    private final String opening_hours;
    /** Distance from user's current location (in meters) */
    private float distance;
    /** Current fuel prices at the station */
    private final FuelPrices fuel_prices;

    /**
     * Default constructor required for Firebase integration.
     * Initializes all fields with default values.
     */
    public GasStation() {
        this.id = 0;
        this.company = "";
        this.address = "";
        this.gps = new GPS(0, 0);
        this.opening_hours = "";
        this.fuel_prices = new FuelPrices(0, 0, 0);
    }

    /**
     * Constructs a new GasStation with all required information.
     *
     * @param id Unique identifier for the station
     * @param address Physical location of the station
     * @param company Name of the gas station company
     * @param gps Geographic coordinates
     * @param opening_hours Operating hours
     * @param prices Current fuel prices
     */
    public GasStation(int id, String address, String company, GPS gps, String opening_hours, FuelPrices prices) {
        this.id = id;
        this.address = address;
        this.company = company;
        this.gps = gps;
        this.opening_hours = opening_hours;
        this.fuel_prices = prices;
    }

    // Getters and setters with descriptive comments
    /**
     * @return The unique identifier of the station
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets a new ID for the station
     * @param id The new unique identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return The name of the gas station company
     */
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