package com.example.slagalica.ranking;

import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueManager;
import com.example.slagalica.notifications.AppNotification;
import com.example.slagalica.notifications.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RankingRepository {

    public static final String TYPE_WEEKLY = "weekly";
    public static final String TYPE_MONTHLY = "monthly";

    private static final String LEADERBOARDS = "leaderboards";
    private static final String ENTRIES = "entries";
    private static final String CLAIMS = "rankingRewardClaims";
    private static final int REFRESH_LIMIT = 50;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository notificationRepository = new NotificationRepository();

    public interface EntriesCallback {
        void onLoaded(List<RankingEntry> entries);
        void onError(Exception exception);
    }

    public interface RewardCallback {
        void onChecked(String message);
    }

    private interface RewardResultCallback {
        void onResult(String message);
    }

    public static void recordRankedMatch(long starsDelta) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return;
        }

        recordRankedMatchForUser(user.getUid(), starsDelta);
    }

    public static void recordRankedMatchForUser(String uid, long starsDelta) {
        if (uid == null) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String username = snapshot.getString("username");
                    String avatar = snapshot.getString("avatar");
                    long stars = valueOrZero(snapshot.getLong("stars"));
                    long leagueLevel = valueOrZero(snapshot.getLong("league"));
                    League league = LeagueManager.getLeague(leagueLevel);

                    writeEntry(db, uid, username, avatar, league, currentCycle(TYPE_WEEKLY), starsDelta, stars);
                    writeEntry(db, uid, username, avatar, league, currentCycle(TYPE_MONTHLY), starsDelta, stars);
                });
    }

    private static void writeEntry(FirebaseFirestore db, String uid, String username, String avatar, League league,
                                   Cycle cycle, long starsDelta, long totalStars) {
        DocumentReference entryRef = db.collection(LEADERBOARDS)
                .document(cycle.id)
                .collection(ENTRIES)
                .document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(entryRef);
            long currentCycleStars = snapshot.exists()
                    ? valueOrZero(snapshot.getLong("stars"))
                    : 0L;
            long newCycleStars = Math.max(0L, currentCycleStars + starsDelta);

            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("username", username != null ? username : "Player");
            data.put("avatar", avatar != null ? avatar : "owl");
            data.put("league", league.getLevel());
            data.put("leagueName", league.getName());
            data.put("leagueIcon", league.getIcon());
            data.put("stars", newCycleStars);
            data.put("totalStars", totalStars);
            data.put("matchesPlayed", FieldValue.increment(1));
            data.put("cycleType", cycle.type);
            data.put("cycleId", cycle.id);
            data.put("cycleStart", cycle.startMillis);
            data.put("cycleEnd", cycle.endMillis);
            data.put("cycleLabel", cycle.label);
            data.put("updatedAt", System.currentTimeMillis());

            transaction.set(entryRef, data, SetOptions.merge());
            return null;
        });
    }

    public ListenerRegistration listenToCurrentEntries(String type, EntriesCallback callback) {
        Cycle cycle = currentCycle(type);
        return db.collection(LEADERBOARDS)
                .document(cycle.id)
                .collection(ENTRIES)
                .orderBy("stars", Query.Direction.DESCENDING)
                .limit(REFRESH_LIMIT)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }

                    List<RankingEntry> entries = new ArrayList<>();
                    if (snapshot != null) {
                        int rank = 1;
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            entries.add(RankingEntry.fromDocument(document, rank));
                            rank++;
                        }
                    }
                    callback.onLoaded(entries);
                });
    }

    public Cycle getCurrentCycle(String type) {
        return currentCycle(type);
    }

    public void checkPreviousCycleRewards(String uid, RewardCallback callback) {
        checkRewardForCycle(uid, previousCycle(TYPE_WEEKLY), weeklyMessage ->
                checkRewardForCycle(uid, previousCycle(TYPE_MONTHLY), monthlyMessage -> {
                    String combined = combineMessages(weeklyMessage, monthlyMessage);
                    callback.onChecked(combined);
                }));
    }

    private void checkRewardForCycle(String uid, Cycle cycle, RewardResultCallback finished) {
        DocumentReference claimRef = db.collection("users")
                .document(uid)
                .collection(CLAIMS)
                .document(cycle.id);

        claimRef.get().addOnSuccessListener(claim -> {
            if (claim.exists()) {
                finished.onResult("");
                return;
            }

            db.collection(LEADERBOARDS)
                    .document(cycle.id)
                    .collection(ENTRIES)
                    .orderBy("stars", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        int rank = 1;
                        int reward = 0;
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            if (uid.equals(document.getId())) {
                                reward = rewardFor(cycle.type, rank);
                                break;
                            }
                            rank++;
                        }

                        Map<String, Object> claimData = new HashMap<>();
                        claimData.put("cycleId", cycle.id);
                        claimData.put("cycleType", cycle.type);
                        claimData.put("rank", reward > 0 ? rank : 0);
                        claimData.put("tokens", reward);
                        claimData.put("claimedAt", System.currentTimeMillis());
                        claimRef.set(claimData, SetOptions.merge());

                        String message = "";
                        if (reward > 0) {
                            message = grantReward(uid, cycle, rank, reward);
                        }
                        finished.onResult(message);
                    })
                    .addOnFailureListener(e -> finished.onResult(""));
        }).addOnFailureListener(e -> finished.onResult(""));
    }

    private String grantReward(String uid, Cycle cycle, int rank, int tokens) {
        db.collection("users").document(uid)
                .update("tokens", FieldValue.increment(tokens));

        String message = "You finished #" + rank + " in the " + cycle.displayName()
                + " ranking and earned " + tokens + " token(s).";

        notificationRepository.create(uid, new AppNotification(
                uid,
                AppNotification.TYPE_REWARD,
                "Ranking reward",
                message,
                AppNotification.ACTION_OPEN_REWARDS,
                new HashMap<>()
        ));

        notificationRepository.create(uid, new AppNotification(
                uid,
                AppNotification.TYPE_RANKING,
                "Ranking cycle finished",
                "Final placement for " + cycle.label + ": #" + rank + ".",
                AppNotification.ACTION_OPEN_RANKING,
                new HashMap<>()
        ));

        return message;
    }

    private static String combineMessages(String first, String second) {
        boolean hasFirst = first != null && !first.isEmpty();
        boolean hasSecond = second != null && !second.isEmpty();
        if (hasFirst && hasSecond) {
            return first + "\n\n" + second;
        }
        if (hasFirst) return first;
        if (hasSecond) return second;
        return "";
    }

    private static int rewardFor(String type, int rank) {
        if (rank == 1) return TYPE_WEEKLY.equals(type) ? 5 : 10;
        if (rank == 2) return TYPE_WEEKLY.equals(type) ? 3 : 6;
        if (rank == 3) return TYPE_WEEKLY.equals(type) ? 2 : 4;
        if (rank >= 4 && rank <= 10) return TYPE_WEEKLY.equals(type) ? 1 : 2;
        return 0;
    }

    private static long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private static Cycle currentCycle(String type) {
        return cycleForDate(type, new Date());
    }

    private static Cycle previousCycle(String type) {
        Calendar calendar = Calendar.getInstance();
        if (TYPE_MONTHLY.equals(type)) {
            calendar.add(Calendar.MONTH, -1);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -7);
        }
        return cycleForDate(type, calendar.getTime());
    }

    private static Cycle cycleForDate(String type, Date date) {
        Calendar original = Calendar.getInstance();
        original.setFirstDayOfWeek(Calendar.MONDAY);
        original.setMinimalDaysInFirstWeek(4);
        original.setTime(date);

        Calendar start = Calendar.getInstance();
        start.setFirstDayOfWeek(Calendar.MONDAY);
        start.setMinimalDaysInFirstWeek(4);
        start.setTime(date);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end;
        if (TYPE_MONTHLY.equals(type)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
            end.add(Calendar.MILLISECOND, -1);
            String id = String.format(Locale.ROOT, "monthly_%04d_%02d",
                    start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1);
            return new Cycle(id, TYPE_MONTHLY, start.getTimeInMillis(), end.getTimeInMillis());
        }

        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 7);
        end.add(Calendar.MILLISECOND, -1);
        String id = String.format(Locale.ROOT, "weekly_%04d_%02d",
                original.getWeekYear(), original.get(Calendar.WEEK_OF_YEAR));
        return new Cycle(id, TYPE_WEEKLY, start.getTimeInMillis(), end.getTimeInMillis());
    }

    public static class Cycle {
        public final String id;
        public final String type;
        public final long startMillis;
        public final long endMillis;
        public final String label;

        Cycle(String id, String type, long startMillis, long endMillis) {
            this.id = id;
            this.type = type;
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.label = formatDate(startMillis) + " - " + formatDate(endMillis);
        }

        public String displayName() {
            return TYPE_MONTHLY.equals(type) ? "monthly" : "weekly";
        }
    }

    public static class RankingEntry {
        public final int rank;
        public final String uid;
        public final String username;
        public final String avatar;
        public final String leagueName;
        public final String leagueIcon;
        public final long stars;
        public final long matchesPlayed;

        RankingEntry(int rank, String uid, String username, String avatar, String leagueName,
                     String leagueIcon, long stars, long matchesPlayed) {
            this.rank = rank;
            this.uid = uid;
            this.username = username;
            this.avatar = avatar;
            this.leagueName = leagueName;
            this.leagueIcon = leagueIcon;
            this.stars = stars;
            this.matchesPlayed = matchesPlayed;
        }

        static RankingEntry fromDocument(DocumentSnapshot document, int rank) {
            return new RankingEntry(
                    rank,
                    document.getId(),
                    stringOrDefault(document.getString("username"), "Player"),
                    stringOrDefault(document.getString("avatar"), "owl"),
                    stringOrDefault(document.getString("leagueName"), "Beginner League"),
                    stringOrDefault(document.getString("leagueIcon"), ""),
                    valueOrZero(document.getLong("stars")),
                    valueOrZero(document.getLong("matchesPlayed"))
            );
        }

        private static String stringOrDefault(String value, String fallback) {
            return value != null ? value : fallback;
        }
    }

    private static String formatDate(long millis) {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(millis));
    }
}
