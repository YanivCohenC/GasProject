package com.yaniv.gasproject.dm;

/** @noinspection unused*/
public class GPS {
    private final double lat;
    private final double lng;

    // Required empty constructor for Firebase
    public GPS() {
        this.lat = 0;
        this.lng = 0;
    }

    public GPS(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    // Getters and setters
    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}