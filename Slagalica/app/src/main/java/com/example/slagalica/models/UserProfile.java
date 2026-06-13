package com.example.slagalica.models;

public class UserProfile {

    public String username;
    public String email;
    public String avatar;
    public int tokens;
    public int stars;
    public String league;
    public String region;
    public String qrCodeValue;

    public UserProfile() {
    }

    public UserProfile(String username, String email, String avatar,
                       int tokens, int stars, String league,
                       String region, String qrCodeValue) {
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
        this.region = region;
        this.qrCodeValue = qrCodeValue;
    }
}