package com.yaniv.gasproject.dm;

public class GPS {
    private double lat;
    private double lng;

    // No-argument constructor required for Firebase
    public GPS() {
        this.lat = 0;
        this.lng = 0;
    }

    public GPS(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public void setCoordinates(GPS coordinates) {
        this.lat = coordinates.getLat();
        this.lng = coordinates.getLng();
    }

    // Getters and setters
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}