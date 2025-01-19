package com.yaniv.gasproject.dm;

public class FuelPrices {
    private double petrol_95;
    private double petrol_98;
    private double diesel;

    // No-argument constructor required for Firebase
    public FuelPrices() {
        this.petrol_95 = 0;
        this.petrol_98 = 0;
        this.diesel = 0;
    }

    public FuelPrices(double petrol_98, double petrol_95, double diesel) {
        this.petrol_98 = petrol_98;
        this.petrol_95 = petrol_95;
        this.diesel = diesel;
    }

    // Getters and setters

    public double getDiesel() {
        return diesel;
    }

    public void setDiesel(double diesel) {
        this.diesel = diesel;
    }

    public double getPetrol_98() {
        return petrol_98;
    }

    public void setPetrol_98(double petrol_98) {
        this.petrol_98 = petrol_98;
    }

    public double getPetrol_95() {
        return petrol_95;
    }

    public void setPetrol_95(double petrol_95) {
        this.petrol_95 = petrol_95;
    }
}