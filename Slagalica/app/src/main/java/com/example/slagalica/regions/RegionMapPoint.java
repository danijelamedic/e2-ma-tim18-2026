package com.example.slagalica.regions;

public class RegionMapPoint {
    public String uid;
    public String username;
    public String regionId;
    public double latitude;
    public double longitude;

    public RegionMapPoint(String uid, String username, String regionId, double latitude, double longitude) {
        this.uid = uid;
        this.username = username;
        this.regionId = regionId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}