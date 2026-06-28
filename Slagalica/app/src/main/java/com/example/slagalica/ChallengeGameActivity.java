package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.games.StepByStep.StepByStepActivity;
import com.example.slagalica.games.MyNumber.MyNumberActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeGameActivity extends AppCompatActivity {

    private static final int GAME_QUIZ         = 1;
    private static final int GAME_MATCHING     = 2;
    private static final int GAME_ASSOCIATIONS = 3;
    private static final int GAME_SKOCKO       = 4;
    private static final int GAME_STEP_BY_STEP = 5;
    private static final int GAME_MY_NUMBER    = 6;
    private static final int TOTAL_GAMES       = 6;

    private FirebaseFirestore db;
    private String challengeId;
    private String currentUid;
    private ListenerRegistration challengeListener;
    private TextView tvStatus;
    private int myTotalPoints = 0;
    private int currentGameNumber = 1;
    private boolean gameAlreadyLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeId = getIntent().getStringExtra("challengeId");

        tvStatus = findViewById(R.id.tvGameInfo);
        tvStatus.setText("Challenge starting...");

        playNextGame();
    }

    private void playNextGame() {
        if (currentGameNumber > TOTAL_GAMES) {
            submitFinalScore();
            return;
        }

        tvStatus.setText("Game " + currentGameNumber + " of " + TOTAL_GAMES);

        Intent intent;
        switch (currentGameNumber) {
            case GAME_QUIZ:
                intent = new Intent(this, QuizActivity.class);
                break;
            case GAME_MATCHING:
                intent = new Intent(this, MatchingActivity.class);
                break;
            case GAME_ASSOCIATIONS:
                intent = new Intent(this, AssociationsActivity.class);
                break;
            case GAME_SKOCKO:
                intent = new Intent(this, SkockoActivity.class);
                break;
            case GAME_STEP_BY_STEP:
                intent = new Intent(this, StepByStepActivity.class);
                break;
            case GAME_MY_NUMBER:
                intent = new Intent(this, MyNumberActivity.class);
                break;
            default:
                submitFinalScore();
                return;
        }

        intent.putExtra("isMultiplayer", false);
        intent.putExtra("isBattleMode", true);
        startActivityForResult(intent, currentGameNumber);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            myTotalPoints += data.getIntExtra("points", 0);
        }

        currentGameNumber++;
        playNextGame();
    }

    private void submitFinalScore() {
        tvStatus.setText("Submitting score...");

        db.collection("challenges").document(challengeId)
                .update("scores." + currentUid, (long) myTotalPoints)
                .addOnSuccessListener(unused -> listenForAllScores());
    }

    private void listenForAllScores() {
        tvStatus.setText("Waiting for other players...");

        challengeListener = db.collection("challenges").document(challengeId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    List<String> participants = (List<String>) snapshot.get("participants");
                    Map<String, Object> scores = (Map<String, Object>) snapshot.get("scores");

                    if (participants == null || scores == null) return;

                    boolean allDone = true;
                    for (String uid : participants) {
                        if (!scores.containsKey(uid)) {
                            allDone = false;
                            break;
                        }
                    }

                    if (allDone) {
                        if (challengeListener != null) challengeListener.remove();
                        distributeRewards(participants, scores, snapshot);
                    }
                });
    }

    private void distributeRewards(List<String> participants,
                                   Map<String, Object> scores,
                                   com.google.firebase.firestore.DocumentSnapshot snapshot) {

        long starsStake = snapshot.getLong("starsStake") != null ? snapshot.getLong("starsStake") : 0;
        long tokensStake = snapshot.getLong("tokensStake") != null ? snapshot.getLong("tokensStake") : 0;
        int numPlayers = participants.size();

        List<Map.Entry<String, Long>> sorted = new ArrayList<>();
        for (String uid : participants) {
            Object scoreObj = scores.get(uid);
            long score = scoreObj instanceof Long ? (Long) scoreObj :
                    scoreObj instanceof Number ? ((Number) scoreObj).longValue() : 0L;
            sorted.add(new java.util.AbstractMap.SimpleEntry<>(uid, score));
        }
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        long totalStars = starsStake * numPlayers;
        long totalTokens = tokensStake * numPlayers;

        long winnerStars  = Math.round(totalStars  * 0.75);
        long winnerTokens = Math.round(totalTokens * 0.75);

        long secondStars  = starsStake;
        long secondTokens = tokensStake;

        String winnerId = sorted.get(0).getKey();
        String secondId = sorted.size() > 1 ? sorted.get(1).getKey() : null;

        db.collection("users").document(winnerId)
                .update("stars",  FieldValue.increment(winnerStars),
                        "tokens", FieldValue.increment(winnerTokens));

        if (secondId != null) {
            db.collection("users").document(secondId)
                    .update("stars",  FieldValue.increment(secondStars),
                            "tokens", FieldValue.increment(secondTokens));
        }

        db.collection("challenges").document(challengeId)
                .update("status", "finished",
                        "winnerId", winnerId,
                        "finalScores", scores);

        long myScore = 0;
        Object myScoreObj = scores.get(currentUid);
        if (myScoreObj instanceof Number) myScore = ((Number) myScoreObj).longValue();

        boolean iWon = currentUid.equals(winnerId);
        boolean iSecond = currentUid.equals(secondId);

        String resultMsg;
        if (iWon) {
            resultMsg = "🏆 You won the challenge!\n+" + winnerStars + " stars, +" + winnerTokens + " tokens";
        } else if (iSecond) {
            resultMsg = "2nd place - you get your stake back!\n+" + secondStars + " stars, +" + secondTokens + " tokens";
        } else {
            resultMsg = "You didn't place. Better luck next time!";
        }

        Intent intent = new Intent(this, ChallengeResultActivity.class);
        intent.putExtra("myScore", myScore);
        intent.putExtra("resultMsg", resultMsg);
        intent.putExtra("challengeId", challengeId);

        ArrayList<String> playerIds = new ArrayList<>(participants);
        intent.putStringArrayListExtra("playerIds", playerIds);
        ArrayList<Long> playerScores = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sorted) {
            playerScores.add(entry.getValue());
        }
        intent.putExtra("playerScores", playerScores.stream()
                .mapToLong(Long::longValue).toArray());

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (challengeListener != null) challengeListener.remove();
    }
}