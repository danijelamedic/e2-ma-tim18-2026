package com.example.slagalica.leagues;

public class League {
    private final int level;
    private final String name;
    private final String icon;
    private final int minStars;
    private final int dailyBonusTokens;

    public League(int level, String name, String icon, int minStars, int dailyBonusTokens) {
        this.level = level;
        this.name = name;
        this.icon = icon;
        this.minStars = minStars;
        this.dailyBonusTokens = dailyBonusTokens;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getMinStars() {
        return minStars;
    }

    public int getDailyBonusTokens() {
        return dailyBonusTokens;
    }
}