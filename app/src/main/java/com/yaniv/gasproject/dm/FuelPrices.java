package com.yaniv.gasproject.dm;

/**
 * Represents the fuel prices at a gas station.
 * This class stores and manages prices for different types of fuel:
 * - 95 octane petrol
 * - 98 octane petrol
 * - Diesel
 * All prices are stored in Israeli Shekels (₪).
 */
public class FuelPrices {
    /** Price of 95 octane petrol in ₪ */
    private double petrol_95;
    /** Price of 98 octane petrol in ₪ */
    private final double petrol_98;
    /** Price of diesel fuel in ₪ */
    private double diesel;

    /**
     * Default constructor required for Firebase integration.
     * Initializes all fuel prices to 0.
     * @noinspection unused
     */
    public FuelPrices() {
        this.petrol_95 = 0;
        this.petrol_98 = 0;
        this.diesel = 0;
    }

    /**
     * Constructs a new FuelPrices object with specified prices for all fuel types.
     *
     * @param petrol_98 Price of 98 octane petrol in ₪
     * @param petrol_95 Price of 95 octane petrol in ₪
     * @param diesel Price of diesel fuel in ₪
     */
    public FuelPrices(double petrol_98, double petrol_95, double diesel) {
        this.petrol_98 = petrol_98;
        this.petrol_95 = petrol_95;
        this.diesel = diesel;
    }

    /**
     * @return The current price of diesel fuel in ₪
     */
    public double getDiesel() {
        return diesel;
    }

    /**
     * Updates the price of diesel fuel
     * @param diesel New price in ₪
     */
    public void setDiesel(double diesel) {
        this.diesel = diesel;
    }

    /**
     * @return The current price of 98 octane petrol in ₪
     */
    public double getPetrol_98() {
        return petrol_98;
    }

    /**
     * @return The current price of 95 octane petrol in ₪
     */
    public double getPetrol_95() {
        return petrol_95;
    }

    /**
     * Updates the price of 95 octane petrol
     * @param petrol_95 New price in ₪
     */
    public void setPetrol_95(double petrol_95) {
        this.petrol_95 = petrol_95;
    }
}