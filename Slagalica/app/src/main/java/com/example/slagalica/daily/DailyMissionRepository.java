package com.example.slagalica.daily;

import android.content.Context;

import com.example.slagalica.leagues.LeagueManager;
import com.example.slagalica.notifications.NotificationFactory;
import com.example.slagalica.ranking.RankingRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DailyMissionRepository {
    private static final int SINGLE_MISSION_STARS = 3;
    private static final int ALL_COMPLETED_STARS = 3;
    private static final int ALL_COMPLETED_TOKENS = 2;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface MissionCallback {
        void onLoaded(DailyMission mission);
        void onError(Exception exception);
    }

    private static class CompletionResult {
        boolean missionCompletedNow;
        boolean allBonusCompletedNow;
        long oldLeague;
        long newLeague;
        int starsDelta;
        int tokensDelta;
    }

    public ListenerRegistration listenToday(String uid, MissionCallback callback) {
        ensureTodayDocument(uid);
        return todayRef(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onLoaded(DailyMission.fromDocument(snapshot));
        });
    }

    public void completeMission(Context context, String uid, String missionField) {
        if (uid == null || missionField == null) {
            return;
        }

        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference missionRef = todayRef(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot user = transaction.get(userRef);
            DocumentSnapshot mission = transaction.get(missionRef);
            CompletionResult result = new CompletionResult();

            if (user == null || !user.exists()) {
                return result;
            }
            if (mission.exists() && Boolean.TRUE.equals(mission.getBoolean(missionField))) {
                return result;
            }

            boolean winMatch = valueAfterMission(mission, DailyMission.WIN_MATCH, missionField);
            boolean sendChat = valueAfterMission(mission, DailyMission.SEND_CHAT_MESSAGE, missionField);
            boolean friendly = valueAfterMission(mission, DailyMission.PLAY_FRIENDLY_MATCH, missionField);
            boolean tournament = valueAfterMission(mission, DailyMission.WIN_TOURNAMENT_MATCH, missionField);
            boolean allCompleted = winMatch && sendChat && friendly && tournament;
            boolean bonusAlreadyClaimed = mission.exists()
                    && Boolean.TRUE.equals(mission.getBoolean("allCompletedBonusClaimed"));

            result.missionCompletedNow = true;
            result.allBonusCompletedNow = allCompleted && !bonusAlreadyClaimed;
            result.starsDelta = SINGLE_MISSION_STARS
                    + (result.allBonusCompletedNow ? ALL_COMPLETED_STARS : 0);
            result.tokensDelta = result.allBonusCompletedNow ? ALL_COMPLETED_TOKENS : 0;

            long currentStars = valueOrZero(user.getLong("stars"));
            result.oldLeague = valueOrZero(user.getLong("league"));
            long newStars = currentStars + result.starsDelta;
            result.newLeague = LeagueManager.getLeagueForStars((int) newStars).getLevel();

            Map<String, Object> missionUpdates = new HashMap<>();
            missionUpdates.put(missionField, true);
            missionUpdates.put("completedCount", completedCount(winMatch, sendChat, friendly, tournament));
            missionUpdates.put("allCompletedBonusClaimed",
                    result.allBonusCompletedNow || bonusAlreadyClaimed);
            missionUpdates.put("dateId", todayId());
            missionUpdates.put("updatedAt", System.currentTimeMillis());
            if (!mission.exists()) {
                missionUpdates.put("createdAt", System.currentTimeMillis());
            }
            transaction.set(missionRef, missionUpdates, SetOptions.merge());

            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("stars", FieldValue.increment(result.starsDelta));
            userUpdates.put("league", result.newLeague);
            if (result.tokensDelta > 0) {
                userUpdates.put("tokens", FieldValue.increment(result.tokensDelta));
            }
            transaction.update(userRef, userUpdates);
            return result;
        }).addOnSuccessListener(result -> {
            if (result == null || !result.missionCompletedNow) {
                return;
            }

            RankingRepository.recordRankedMatchForUser(uid, result.starsDelta);
            NotificationFactory factory = new NotificationFactory();
            factory.sendDailyMissionReward(context, uid, SINGLE_MISSION_STARS, 0, false);
            if (result.allBonusCompletedNow) {
                factory.sendDailyMissionReward(context, uid, ALL_COMPLETED_STARS,
                        ALL_COMPLETED_TOKENS, true);
            }
            if (result.oldLeague != result.newLeague) {
                factory.sendLeagueChange(context, uid, result.oldLeague, result.newLeague);
            }
        });
    }

    private void ensureTodayDocument(String uid) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("dateId", todayId());
        defaults.put("createdAt", System.currentTimeMillis());
        todayRef(uid).set(defaults, SetOptions.merge());
    }

    private DocumentReference todayRef(String uid) {
        return db.collection("users")
                .document(uid)
                .collection("dailyMissions")
                .document(todayId());
    }

    private boolean valueAfterMission(DocumentSnapshot document, String field, String completedField) {
        return field.equals(completedField)
                || (document != null && document.exists() && Boolean.TRUE.equals(document.getBoolean(field)));
    }

    private int completedCount(boolean winMatch, boolean sendChat, boolean friendly, boolean tournament) {
        int count = 0;
        if (winMatch) count++;
        if (sendChat) count++;
        if (friendly) count++;
        if (tournament) count++;
        return count;
    }

    private static long valueOrZero(Long value) {
        return value != null ? value : 0;
    }

    private String todayId() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }
}
