package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.data.StatisticsRepository;
import com.example.slagalica.games.MyNumber.MyNumberActivity;
import com.example.slagalica.games.StepByStep.StepByStepActivity;
import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class GameSessionActivity extends AppCompatActivity {

    private int currentGameIndex = 0;
    private int totalScore = 0;
    private String gameSessionId;
    private boolean isFriendlyMatch = false;
    private boolean isFinishingBattle = false;
    private ListenerRegistration gameSessionListener;
    private boolean remoteSessionClosed = false;

    private final Class<?>[] games = {
            QuizActivity.class,
            MatchingActivity.class,
            AssociationsActivity.class,
            SkockoActivity.class,
            StepByStepActivity.class,
            MyNumberActivity.class
    };

    private ActivityResultLauncher<Intent> gameLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSessionId = getIntent().getStringExtra("gameSessionId");
        isFriendlyMatch = getIntent().getBooleanExtra("isFriendlyMatch", false);

        setCurrentUserInGame(true);
        listenForFriendlySessionEnd();

        gameLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        boolean battleLost = result.getData().getBooleanExtra("battleLost", false);

                        if (battleLost) {
                            finishBattleAsLost();
                            return;
                        }

                        int gameScore = result.getData().getIntExtra("score", 0);
                        totalScore = gameScore;
                    }

                    currentGameIndex++;
                    startNextGame();
                }
        );

        Toast.makeText(this, "Battle started!", Toast.LENGTH_SHORT).show();
        startNextGame();
    }

    private void startNextGame() {
        if (currentGameIndex >= games.length) {
            finishBattle();
            return;
        }

        Intent intent = new Intent(this, games[currentGameIndex]);
        intent.putExtra("isBattleMode", true);
        intent.putExtra("currentGameIndex", currentGameIndex);
        intent.putExtra("totalGames", games.length);
        intent.putExtra("currentTotalScore", totalScore);

        gameLauncher.launch(intent);
    }

    private void finishBattle() {
        isFinishingBattle = true;
        stopGameSessionListener();
        resetFriendlyPlayersInGame();

        StatisticsRepository.saveBattleWin();

        Intent intent = new Intent(this, BattleResultActivity.class);
        intent.putExtra("totalScore", totalScore);

        setCurrentUserInGame(false);

        startActivity(intent);
        finish();
    }

    private void finishBattleAsLost() {
        isFinishingBattle = true;
        stopGameSessionListener();
        resetFriendlyPlayersInGame();

        StatisticsRepository.saveBattleLoss();

        Intent intent = new Intent(this, BattleResultActivity.class);
        intent.putExtra("totalScore", totalScore);
        intent.putExtra("battleLost", true);

        setCurrentUserInGame(false);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        stopGameSessionListener();

        if (!isFinishingBattle && !remoteSessionClosed) {
            markFriendlySessionAbandoned();
        }

        super.onDestroy();
    }

    private void markFriendlySessionAbandoned() {
        if (!isFriendlyMatch || gameSessionId == null) {
            setCurrentUserInGame(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("gameSessions")
                .document(gameSessionId)
                .update("status", "abandoned");

        resetFriendlyPlayersInGame();
    }

    private void setCurrentUserInGame(boolean inGame) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("inGame", inGame);
    }

    private void resetFriendlyPlayersInGame() {
        if (!isFriendlyMatch || gameSessionId == null) {
            setCurrentUserInGame(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("gameSessions")
                .document(gameSessionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        setCurrentUserInGame(false);
                        return;
                    }

                    String player1Id = document.getString("player1Id");
                    String player2Id = document.getString("player2Id");

                    if (player1Id != null) {
                        db.collection("users").document(player1Id).update("inGame", false);
                    }

                    if (player2Id != null) {
                        db.collection("users").document(player2Id).update("inGame", false);
                    }

                    db.collection("gameSessions")
                            .document(gameSessionId)
                            .update("status", "finished");
                });
    }

    private void listenForFriendlySessionEnd() {
        if (!isFriendlyMatch || gameSessionId == null) {
            return;
        }

        gameSessionListener = FirebaseFirestore.getInstance()
                .collection("gameSessions")
                .document(gameSessionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String status = snapshot.getString("status");

                    if ("abandoned".equals(status) || "finished".equals(status)) {
                        if (!isFinishingBattle) {
                            remoteSessionClosed = true;
                            Toast.makeText(this, "Friend left the match.", Toast.LENGTH_SHORT).show();
                            setCurrentUserInGame(false);
                            finish();
                        }
                    }
                });
    }

    private void stopGameSessionListener() {
        if (gameSessionListener != null) {
            gameSessionListener.remove();
            gameSessionListener = null;
        }
    }
}