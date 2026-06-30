package com.example.slagalica.regions;

public class RegionRankingItem {
    public String regionId;
    public String displayName;
    public String icon;
    public int monthlyStars;
    public int totalPlayers;
    public int activePlayers;
    public int firstPlaces;
    public int secondPlaces;
    public int thirdPlaces;
    public boolean isMyRegion;

    public RegionRankingItem(String regionId) {
        this.regionId = regionId;
        this.displayName = RegionManager.getDisplayName(regionId);
        this.icon = RegionManager.getRegionIcon(regionId);
        this.monthlyStars = 0;
        this.totalPlayers = 0;
        this.activePlayers = 0;
        this.firstPlaces = 0;
        this.secondPlaces = 0;
        this.thirdPlaces = 0;
        this.isMyRegion = false;
    }
}