package com.example.slagalica.regions;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionRepository {

    public interface RegionRankingCallback {
        void onSuccess(List<RegionRankingItem> regions);
        void onError(Exception e);
    }

    public interface RegionMapCallback {
        void onSuccess(List<RegionMapPoint> points);
        void onError(Exception e);
    }

    public interface RegionCycleCallback {
        void onSuccess(String message);
        void onError(Exception e);
    }

    private final FirebaseFirestore db;

    public RegionRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void loadRegionalRanking(String currentUid, RegionRankingCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, RegionRankingItem> map = new HashMap<>();

                    for (String regionId : RegionManager.getAllRegionIds()) {
                        map.put(regionId, new RegionRankingItem(regionId));
                    }

                    String[] myRegionHolder = new String[]{RegionManager.OSTALO};

                    for (var doc : querySnapshot.getDocuments()) {
                        if (!isRegisteredPlayer(doc)) continue;

                        String uid = doc.getId();

                        String rawRegion = doc.getString("region");
                        String regionId = RegionManager.normalizeRegion(rawRegion);

                        RegionRankingItem item = map.get(regionId);
                        if (item == null) {
                            item = new RegionRankingItem(regionId);
                            map.put(regionId, item);
                        }

                        item.totalPlayers++;

                        Boolean online = doc.getBoolean("online");
                        if (online != null && online) {
                            item.activePlayers++;
                        }

                        long monthlyStars = 0;
                        Long ms = doc.getLong("monthlyStars");
                        if (ms != null) {
                            monthlyStars = ms;
                        } else {
                            Long stars = doc.getLong("stars");
                            if (stars != null) monthlyStars = stars;
                        }

                        item.monthlyStars += (int) monthlyStars;

                        if (uid.equals(currentUid)) {
                            item.isMyRegion = true;
                            myRegionHolder[0] = regionId;
                        }
                    }

                    db.collection("regions")
                            .get()
                            .addOnSuccessListener(regionStatsSnapshot -> {

                                for (var regionDoc : regionStatsSnapshot.getDocuments()) {
                                    String regionId = regionDoc.getId();

                                    RegionRankingItem item = map.get(regionId);
                                    if (item == null) continue;

                                    Long firstPlaces = regionDoc.getLong("firstPlaces");
                                    Long secondPlaces = regionDoc.getLong("secondPlaces");
                                    Long thirdPlaces = regionDoc.getLong("thirdPlaces");

                                    item.firstPlaces = firstPlaces != null ? firstPlaces.intValue() : 0;
                                    item.secondPlaces = secondPlaces != null ? secondPlaces.intValue() : 0;
                                    item.thirdPlaces = thirdPlaces != null ? thirdPlaces.intValue() : 0;
                                }

                                List<RegionRankingItem> list = new ArrayList<>(map.values());

                                list.sort((a, b) -> Integer.compare(b.monthlyStars, a.monthlyStars));

                                for (RegionRankingItem item : list) {
                                    item.isMyRegion = item.regionId.equals(myRegionHolder[0]);
                                }

                                callback.onSuccess(list);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void loadMapPoints(RegionMapCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RegionMapPoint> points = new ArrayList<>();

                    for (var doc : querySnapshot.getDocuments()) {
                        if (!isRegisteredPlayer(doc)) continue;

                        String uid = doc.getId();
                        String username = doc.getString("username");
                        String regionId = RegionManager.normalizeRegion(doc.getString("region"));

                        Double lat = doc.getDouble("mapLat");
                        Double lng = doc.getDouble("mapLng");

                        if (lat == null || lng == null) {
                            double[] randomPoint = RegionManager.getRandomPointForRegion(regionId);
                            lat = randomPoint[0];
                            lng = randomPoint[1];

                            doc.getReference().update(
                                    "mapLat", lat,
                                    "mapLng", lng,
                                    "normalizedRegion", regionId
                            );
                        }

                        points.add(new RegionMapPoint(
                                uid,
                                username != null ? username : "Player",
                                regionId,
                                lat,
                                lng
                        ));
                    }

                    callback.onSuccess(points);
                })
                .addOnFailureListener(callback::onError);
    }

    public void simulateMonthlyRegionCycle(RegionCycleCallback callback) {
        loadRegionalRanking("", new RegionRankingCallback() {
            @Override
            public void onSuccess(List<RegionRankingItem> regions) {
                if (regions == null || regions.isEmpty()) {
                    callback.onSuccess("No regions to rank.");
                    return;
                }

                String first = regions.size() > 0 ? regions.get(0).regionId : null;
                String second = regions.size() > 1 ? regions.get(1).regionId : null;
                String third = regions.size() > 2 ? regions.get(2).regionId : null;

                if (first != null) {
                    db.collection("regions").document(first)
                            .set(new HashMap<String, Object>() {{
                                put("firstPlaces", FieldValue.increment(1));
                            }}, SetOptions.merge());
                }

                if (second != null) {
                    db.collection("regions").document(second)
                            .set(new HashMap<String, Object>() {{
                                put("secondPlaces", FieldValue.increment(1));
                            }}, SetOptions.merge());
                }

                if (third != null) {
                    db.collection("regions").document(third)
                            .set(new HashMap<String, Object>() {{
                                put("thirdPlaces", FieldValue.increment(1));
                            }}, SetOptions.merge());
                }

                db.collection("users")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (var doc : querySnapshot.getDocuments()) {
                                String regionId = RegionManager.normalizeRegion(doc.getString("region"));

                                String border = "none";
                                if (regionId.equals(first)) border = "gold";
                                else if (regionId.equals(second)) border = "silver";
                                else if (regionId.equals(third)) border = "bronze";

                                doc.getReference().update(
                                        "avatarBorder", border,
                                        "monthlyStars", 0
                                );
                            }

                            callback.onSuccess(
                                    "Monthly regional cycle finished.\n\n" +
                                            "🥇 " + RegionManager.getDisplayName(first) + "\n" +
                                            "🥈 " + RegionManager.getDisplayName(second) + "\n" +
                                            "🥉 " + RegionManager.getDisplayName(third)
                            );
                        })
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private boolean isRegisteredPlayer(com.google.firebase.firestore.DocumentSnapshot doc) {
        String email = doc.getString("email");
        String avatar = doc.getString("avatar");
        String region = doc.getString("region");

        if (email == null || email.trim().isEmpty()) return false;
        if ("guest".equals(avatar)) return false;
        if (region == null || region.trim().isEmpty()) return false;

        return true;
    }

    public void checkAndProcessMonthlyRegionCycle(RegionCycleCallback callback) {
        int currentMonthKey = getCurrentMonthKey();

        db.collection("system")
                .document("regionalCycle")
                .get()
                .addOnSuccessListener(document -> {
                    Long lastProcessedMonth = document.getLong("lastProcessedMonth");

                    if (lastProcessedMonth != null && lastProcessedMonth == currentMonthKey) {
                        callback.onSuccess("");
                        return;
                    }

                    simulateMonthlyRegionCycle(new RegionCycleCallback() {
                        @Override
                        public void onSuccess(String message) {
                            db.collection("system")
                                    .document("regionalCycle")
                                    .set(new HashMap<String, Object>() {{
                                        put("lastProcessedMonth", currentMonthKey);
                                    }}, SetOptions.merge())
                                    .addOnSuccessListener(unused -> callback.onSuccess(message))
                                    .addOnFailureListener(callback::onError);
                        }

                        @Override
                        public void onError(Exception e) {
                            callback.onError(e);
                        }
                    });
                })
                .addOnFailureListener(callback::onError);
    }

    private int getCurrentMonthKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        return year * 100 + month;
    }
}