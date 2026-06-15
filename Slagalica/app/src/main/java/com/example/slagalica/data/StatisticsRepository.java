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

    public static void saveQuizResult(int correctAnswers, int totalQuestions, int score, boolean won) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("quizGamesPlayed", FieldValue.increment(1));
        updates.put("quizCorrectAnswers", FieldValue.increment(correctAnswers));
        updates.put("quizTotalQuestions", FieldValue.increment(totalQuestions));
        updates.put("quizTotalScore", FieldValue.increment(score));
        updates.put(won ? "quizGamesWon" : "quizGamesLost", FieldValue.increment(1));

        FirebaseFirestore.getInstance()
                .collection(COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveMatchingResult(int correctMatches, int totalMatches, int score, boolean won) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("matchingGamesPlayed", FieldValue.increment(1));
        updates.put("matchingCorrectMatches", FieldValue.increment(correctMatches));
        updates.put("matchingTotalMatches", FieldValue.increment(totalMatches));
        updates.put("matchingTotalScore", FieldValue.increment(score));
        updates.put(won ? "matchingGamesWon" : "matchingGamesLost", FieldValue.increment(1));

        FirebaseFirestore.getInstance()
                .collection(COLLECTION)
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

    public static void saveAssociationsResult(int score, boolean solved) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("associationsGamesPlayed", FieldValue.increment(1));
        updates.put("associationsTotalScore", FieldValue.increment(score));
        updates.put(solved ? "associationsSolved" : "associationsUnsolved", FieldValue.increment(1));

        FirebaseFirestore.getInstance().collection(COLLECTION).document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveMyNumberResult(int score, boolean exactHit) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("myNumberGamesPlayed", FieldValue.increment(1));
        updates.put("myNumberTotalScore", FieldValue.increment(score));
        if (exactHit) updates.put("myNumberExactHits", FieldValue.increment(1));

        FirebaseFirestore.getInstance().collection(COLLECTION).document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveStepByStepResult(int score, int guessedStep) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("stepByStepGamesPlayed", FieldValue.increment(1));
        updates.put("stepByStepTotalScore", FieldValue.increment(score));

        if (guessedStep >= 1 && guessedStep <= 7) {
            updates.put("step" + guessedStep + "Hits", FieldValue.increment(1));
        }

        FirebaseFirestore.getInstance().collection(COLLECTION).document(userId)
                .set(updates, SetOptions.merge());
    }

    public static void saveSkockoResult(int score, int guessedAttempt) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("skockoGamesPlayed", FieldValue.increment(1));
        updates.put("skockoTotalScore", FieldValue.increment(score));

        if (guessedAttempt >= 1 && guessedAttempt <= 6) {
            updates.put("attempt" + guessedAttempt + "Hits", FieldValue.increment(1));
        }

        FirebaseFirestore.getInstance().collection(COLLECTION).document(userId)
                .set(updates, SetOptions.merge());
    }
}