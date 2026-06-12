package com.example.slagalica;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.games.StepByStep.StepByStepActivity;
import com.example.slagalica.games.MyNumber.MyNumberActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private static final int GAME_QUIZ         = 1;
    private static final int GAME_MATCHING     = 2;
    private static final int GAME_ASSOCIATIONS = 3;
    private static final int GAME_SKOCKO       = 4;
    private static final int GAME_STEP_BY_STEP = 5;
    private static final int GAME_MY_NUMBER    = 6;
    private static final int TOTAL_GAMES       = 6;

    private FirebaseFirestore db;
    private String gameId;
    private String currentUid;
    private ListenerRegistration gameListener;
    private TextView tvGameInfo;
    private boolean gameAlreadyLaunched = false;
    private boolean isFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        gameId = getIntent().getStringExtra("gameId");

        tvGameInfo = findViewById(R.id.tvGameInfo);
        tvGameInfo.setText("Loading game...");

        listenForGameUpdates();
    }

    private void listenForGameUpdates() {
        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    if (isFinishing) return;

                    String status = snapshot.getString("status");

                    if ("abandoned".equals(status)) {
                        handleOpponentAbandoned();
                        return;
                    }

                    if ("finished".equals(status)) {
                        showResults(snapshot);
                        return;
                    }

                    Long currentGameLong = snapshot.getLong("currentGame");
                    int currentGame = currentGameLong != null ? currentGameLong.intValue() : 1;
                    String currentTurnUid = snapshot.getString("currentTurnUid");

                    tvGameInfo.setText("Game " + currentGame + " of " + TOTAL_GAMES);

                    if (!gameAlreadyLaunched && shouldLaunchForCurrentPlayer(currentGame, currentTurnUid)) {
                        gameAlreadyLaunched = true;
                        launchGame(currentGame);
                    }
                });
    }

    private boolean shouldLaunchForCurrentPlayer(int currentGame, String currentTurnUid) {
        if (currentGame == GAME_MY_NUMBER || currentGame == GAME_STEP_BY_STEP) {
            return true;
        }

        return currentTurnUid != null && currentTurnUid.equals(currentUid);
    }

    private void launchGame(int gameNumber) {
        if (gameListener != null) gameListener.remove();

        Intent intent;
        switch (gameNumber) {
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
                return;
        }
        intent.putExtra("gameId", gameId);
        intent.putExtra("isMultiplayer", true);
        startActivityForResult(intent, gameNumber);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        gameAlreadyLaunched = false;

        int points = 0;
        if (resultCode == RESULT_OK && data != null) {
            points = data.getIntExtra("points", 0);
        }

        saveScoreAndAdvance(points, requestCode);
    }

    private void saveScoreAndAdvance(int points, int gameNumber) {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    String player1 = snapshot.getString("player1");
                    String player2 = snapshot.getString("player2");
                    boolean isPlayer1 = currentUid.equals(player1);

                    String scoreField = isPlayer1 ? "score1" : "score2";
                    long currentScore = snapshot.getLong(scoreField) != null ?
                            snapshot.getLong(scoreField) : 0;

                    String currentTurnUid = snapshot.getString("currentTurnUid");
                    long currentGame = snapshot.getLong("currentGame") != null ?
                            snapshot.getLong("currentGame") : 1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put(scoreField, currentScore + points);

                    String opponentUid = isPlayer1 ? player2 : player1;

                    String myDoneField = isPlayer1 ? "player1done_game" + gameNumber
                            : "player2done_game" + gameNumber;
                    String opponentDoneField = isPlayer1 ? "player2done_game" + gameNumber
                            : "player1done_game" + gameNumber;

                    updates.put(myDoneField, true);
                    updates.put("currentTurnUid", opponentUid);

                    Boolean opponentDone = snapshot.getBoolean(opponentDoneField);

                    if (Boolean.TRUE.equals(opponentDone)) {
                        long nextGame = currentGame + 1;
                        if (nextGame > TOTAL_GAMES) {
                            updates.put("status", "finished");
                            updates.put("currentTurnUid", null);
                        } else {
                            updates.put("currentGame", nextGame);
                            updates.put("currentTurnUid", player1);
                            updates.put("player1done_game" + nextGame, false);
                            updates.put("player2done_game" + nextGame, false);
                        }
                    }

                    db.collection("games").document(gameId).update(updates)
                            .addOnSuccessListener(unused -> {
                                listenForGameUpdates();
                            });
                });
    }

    private void handleOpponentAbandoned() {
        if (isFinishing) return;
        isFinishing = true;
        if (gameListener != null) gameListener.remove();

        Toast.makeText(this, "Opponent left the game. You win!", Toast.LENGTH_LONG).show();

        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    String player1 = snapshot.getString("player1");
                    long myScore = currentUid.equals(player1) ?
                            (snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0) :
                            (snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0);
                    long opponentScore = currentUid.equals(player1) ?
                            (snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0) :
                            (snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0);

                    boolean isFriendly = Boolean.TRUE.equals(snapshot.getBoolean("isFriendly"));

                    Intent intent = new Intent(this, GameResultActivity.class);
                    intent.putExtra("myScore", myScore + 1); // +1 da bi pobedio
                    intent.putExtra("opponentScore", opponentScore);
                    intent.putExtra("isFriendly", isFriendly);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Abandon Game")
                .setMessage("If you leave, you will lose the game. Are you sure?")
                .setPositiveButton("Leave", (dialog, which) -> abandonGame())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void abandonGame() {
        isFinishing = true;
        if (gameListener != null) gameListener.remove();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "abandoned");
        updates.put("abandonedBy", currentUid);

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, GameResultActivity.class);
                    intent.putExtra("myScore", (long) 0);
                    intent.putExtra("opponentScore", (long) 999);
                    intent.putExtra("isFriendly", false);
                    startActivity(intent);
                    finish();
                });
    }

    private void showResults(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (isFinishing) return;
        isFinishing = true;
        if (gameListener != null) gameListener.remove();

        long score1 = snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0;
        long score2 = snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0;
        String player1 = snapshot.getString("player1");
        boolean isFriendly = Boolean.TRUE.equals(snapshot.getBoolean("isFriendly"));

        long myScore = currentUid.equals(player1) ? score1 : score2;
        long opponentScore = currentUid.equals(player1) ? score2 : score1;

        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra("myScore", myScore);
        intent.putExtra("opponentScore", opponentScore);
        intent.putExtra("isFriendly", isFriendly);
        intent.putExtra("gameId", gameId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListener != null) gameListener.remove();
    }
}
