package com.yaniv.gasproject.dm;

/**
 * Represents geographic coordinates using latitude and longitude.
 * This class is used to store and manage location data for gas stations.
 * All coordinates are stored in decimal degrees format.
 */
public class GPS {
    /** Latitude in decimal degrees (positive for North, negative for South) */
    private final double lat;
    /** Longitude in decimal degrees (positive for East, negative for West) */
    private final double lng;

    /**
     * Default constructor required for Firebase integration.
     * Initializes coordinates to (0,0), which represents the intersection
     * of the equator and prime meridian.
     * @noinspection unused
     */
    public GPS() {
        this.lat = 0;
        this.lng = 0;
    }

    /**
     * Constructs a new GPS coordinate with specified latitude and longitude.
     *
     * @param lat Latitude in decimal degrees
     * @param lng Longitude in decimal degrees
     */
    public GPS(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * @return The latitude coordinate in decimal degrees
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return The longitude coordinate in decimal degrees
     */
    public double getLng() {
        return lng;
    }
}