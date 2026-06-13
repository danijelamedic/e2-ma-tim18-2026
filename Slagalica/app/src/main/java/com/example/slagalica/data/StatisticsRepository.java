package com.example.slagalica.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class StatisticsRepository {

    private static final String COLLECTION = "statistics";

    private static String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static void saveQuizResult(int correctAnswers, int totalQuestions, boolean won) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("quizGamesPlayed", FieldValue.increment(1));
        updates.put("quizCorrectAnswers", FieldValue.increment(correctAnswers));
        updates.put("quizTotalQuestions", FieldValue.increment(totalQuestions));
        updates.put(won ? "quizGamesWon" : "quizGamesLost", FieldValue.increment(1));

        db.collection(COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveMatchingResult(int correctMatches, int totalMatches, boolean won) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("matchingGamesPlayed", FieldValue.increment(1));
        updates.put("matchingCorrectMatches", FieldValue.increment(correctMatches));
        updates.put("matchingTotalMatches", FieldValue.increment(totalMatches));
        updates.put(won ? "matchingGamesWon" : "matchingGamesLost", FieldValue.increment(1));

        db.collection(COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveBattleWin() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalBattlesPlayed", FieldValue.increment(1));
        updates.put("battlesWon", FieldValue.increment(1));

        db.collection(COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveBattleLoss() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalBattlesPlayed", FieldValue.increment(1));
        updates.put("battlesLost", FieldValue.increment(1));

        db.collection(COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge());
    }
}