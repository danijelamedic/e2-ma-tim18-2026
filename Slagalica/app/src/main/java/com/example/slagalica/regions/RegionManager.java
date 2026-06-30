package com.example.slagalica.regions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RegionManager {

    public static final String BEOGRAD = "BEOGRAD";
    public static final String VOJVODINA = "VOJVODINA";
    public static final String SUMADIJA = "SUMADIJA";
    public static final String ZAPADNA_SRBIJA = "ZAPADNA_SRBIJA";
    public static final String ISTOCNA_SRBIJA = "ISTOCNA_SRBIJA";
    public static final String JUZNA_SRBIJA = "JUZNA_SRBIJA";
    public static final String KOSOVO_METOHIJA = "KOSOVO_METOHIJA";
    public static final String OSTALO = "OSTALO";

    public static List<String> getAllRegionIds() {
        return Arrays.asList(
                BEOGRAD,
                VOJVODINA,
                SUMADIJA,
                ZAPADNA_SRBIJA,
                ISTOCNA_SRBIJA,
                JUZNA_SRBIJA,
                KOSOVO_METOHIJA,
                OSTALO
        );
    }

    public static String normalizeRegion(String input) {
        if (input == null || input.trim().isEmpty()) {
            return OSTALO;
        }

        String value = input.trim()
                .toLowerCase(Locale.ROOT)
                .replace("š", "s")
                .replace("đ", "dj")
                .replace("č", "c")
                .replace("ć", "c")
                .replace("ž", "z");

        if (value.contains("beograd") || value.equals("bg")) return BEOGRAD;
        if (value.contains("vojvodina") || value.contains("novi sad")) return VOJVODINA;
        if (value.contains("sumadija") || value.contains("kragujevac")) return SUMADIJA;
        if (value.contains("zapad")) return ZAPADNA_SRBIJA;
        if (value.contains("istok") || value.contains("istocna")) return ISTOCNA_SRBIJA;
        if (value.contains("jug") || value.contains("juzna") || value.contains("nis")) return JUZNA_SRBIJA;
        if (value.contains("kosovo") || value.contains("metohija")) return KOSOVO_METOHIJA;

        return OSTALO;
    }

    public static String getDisplayName(String regionId) {
        if (regionId == null) return "Ostalo";

        switch (regionId) {
            case BEOGRAD:
                return "Beograd";
            case VOJVODINA:
                return "Vojvodina";
            case SUMADIJA:
                return "Šumadija";
            case ZAPADNA_SRBIJA:
                return "Zapadna Srbija";
            case ISTOCNA_SRBIJA:
                return "Istočna Srbija";
            case JUZNA_SRBIJA:
                return "Južna Srbija";
            case KOSOVO_METOHIJA:
                return "Kosovo i Metohija";
            default:
                return "Ostalo";
        }
    }

    public static String getRegionIcon(String regionId) {
        if (regionId == null) return "📍";

        switch (regionId) {
            case BEOGRAD:
                return "🏙️";
            case VOJVODINA:
                return "🌾";
            case SUMADIJA:
                return "🌳";
            case ZAPADNA_SRBIJA:
                return "⛰️";
            case ISTOCNA_SRBIJA:
                return "🌄";
            case JUZNA_SRBIJA:
                return "☀️";
            case KOSOVO_METOHIJA:
                return "🏛️";
            default:
                return "📍";
        }
    }

    public static double[] getRandomPointForRegion(String regionId) {
        Random random = new Random();

        switch (regionId) {
            case BEOGRAD:
                return randomPoint(44.70, 44.90, 20.30, 20.60, random);
            case VOJVODINA:
                return randomPoint(45.00, 46.10, 19.00, 21.30, random);
            case SUMADIJA:
                return randomPoint(43.80, 44.40, 20.30, 21.20, random);
            case ZAPADNA_SRBIJA:
                return randomPoint(43.40, 44.40, 19.20, 20.30, random);
            case ISTOCNA_SRBIJA:
                return randomPoint(43.40, 44.80, 21.20, 22.90, random);
            case JUZNA_SRBIJA:
                return randomPoint(42.30, 43.70, 20.80, 22.30, random);
            case KOSOVO_METOHIJA:
                return randomPoint(42.00, 43.30, 20.00, 21.80, random);
            default:
                return randomPoint(43.50, 45.50, 19.50, 22.50, random);
        }
    }

    private static double[] randomPoint(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            Random random
    ) {
        double lat = minLat + (maxLat - minLat) * random.nextDouble();
        double lon = minLon + (maxLon - minLon) * random.nextDouble();
        return new double[]{lat, lon};
    }
}