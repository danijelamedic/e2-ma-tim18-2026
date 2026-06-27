package com.example.slagalica;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.example.slagalica.tournament.TournamentResultActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
    private TextView tvGameName;
    private TextView tvMyScore;
    private TextView tvOpponentScore;
    private CountDownTimer interGameTimer;
    private boolean gameAlreadyLaunched = false;
    private boolean isFinishing = false;
    private int pendingLaunchGame = -1;
    private int lastCompletedGame = 0;
    private boolean opponentLeftNotified = false;
    private boolean opponentAlreadyLeft = false;
    private android.widget.Button btnLeaveMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        gameId = getIntent().getStringExtra("gameId");

        tvGameName = findViewById(R.id.tvGameName);
        tvMyScore = findViewById(R.id.tvMyScore);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvGameInfo = findViewById(R.id.tvGameInfo);
        tvGameInfo.setText("Loading game...");

        btnLeaveMatch = findViewById(R.id.btnLeaveMatch);
        btnLeaveMatch.setOnClickListener(v -> confirmAbandonGame());

        listenForGameUpdates();
    }

    private void confirmAbandonGame() {
        new AlertDialog.Builder(this)
                .setTitle("Abandon Game")
                .setMessage("If you leave, you will lose the game. Are you sure?")
                .setPositiveButton("Leave", (dialog, which) -> abandonGame())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void listenForGameUpdates() {
        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    if (isFinishing) return;

                    String status = snapshot.getString("status");
                    String abandonedBy = snapshot.getString("abandonedBy");

                    Boolean isFriendly = snapshot.getBoolean("isFriendly");

                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        opponentAlreadyLeft = true;
                        if (Boolean.TRUE.equals(isFriendly)) {
                            isFinishing = true;

                            if (gameListener != null) {
                                gameListener.remove();
                                gameListener = null;
                            }

                            Toast.makeText(this, "Friend left the match.", Toast.LENGTH_LONG).show();

                            db.collection("users").document(currentUid).update("inGame", false);

                            startActivity(new Intent(this, HomeActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                            return;
                        }

                        if (!opponentLeftNotified) {
                            opponentLeftNotified = true;
                            Toast.makeText(this,
                                    "Opponent left — you finish the match alone.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    if ("declined".equals(status)) {
                        handleInviteDeclined();
                        return;
                    }

                    if ("finished".equals(status)) {
                        showResults(snapshot);
                        return;
                    }

                    Long currentGameLong = snapshot.getLong("currentGame");
                    int currentGame = currentGameLong != null ? currentGameLong.intValue() : 1;
                    String currentTurnUid = snapshot.getString("currentTurnUid");
                    String player1 = snapshot.getString("player1");
                    boolean isPlayer1 = currentUid.equals(player1);
                    String myDoneField = isPlayer1 ? "player1done_game" + currentGame
                            : "player2done_game" + currentGame;
                    Boolean myDone = snapshot.getBoolean(myDoneField);
                    long score1 = snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0;
                    long score2 = snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0;
                    long myScore = isPlayer1 ? score1 : score2;
                    long opponentScore = isPlayer1 ? score2 : score1;

                    tvGameName.setText("Next game: " + getGameName(currentGame));
                    tvMyScore.setText("Your score: " + myScore);
                    tvOpponentScore.setText("Opponent score: " + opponentScore);

                    if (currentGame <= lastCompletedGame) {
                        tvGameInfo.setText("Waiting for next game...");
                        return;
                    }

                    if (Boolean.TRUE.equals(myDone)) {
                        if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                            forceAdvanceSolo(currentGame);
                        } else {
                            tvGameInfo.setText("Waiting for opponent...");
                        }
                        return;
                    }

                    if (!gameAlreadyLaunched && shouldLaunchForCurrentPlayer(currentGame, currentTurnUid)) {
                        startInterGameCountdown(currentGame);
                    }
                });
    }

    private void startInterGameCountdown(int gameNumber) {
        if (gameAlreadyLaunched || pendingLaunchGame == gameNumber) {
            return;
        }

        gameAlreadyLaunched = true;
        pendingLaunchGame = gameNumber;

        if (interGameTimer != null) {
            interGameTimer.cancel();
        }

        tvGameName.setText("Next game: " + getGameName(gameNumber));

        interGameTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvGameInfo.setText("Starting in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                interGameTimer = null;
                tvGameInfo.setText("Starting...");
                launchGame(gameNumber);
            }
        }.start();
    }

    private String getGameName(int gameNumber) {
        switch (gameNumber) {
            case GAME_QUIZ:
                return "Quiz";
            case GAME_MATCHING:
                return "Matchmaking";
            case GAME_ASSOCIATIONS:
                return "Associations";
            case GAME_SKOCKO:
                return "Skočko";
            case GAME_STEP_BY_STEP:
                return "Step by step";
            case GAME_MY_NUMBER:
                return "My number";
            default:
                return "Game";
        }
    }

    private boolean shouldLaunchForCurrentPlayer(int currentGame, String currentTurnUid) {
        return true;
    }

    private void launchGame(int gameNumber) {
        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }
        pendingLaunchGame = -1;

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
        intent.putExtra("opponentAlreadyLeft", opponentAlreadyLeft);
        startActivityForResult(intent, gameNumber);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        gameAlreadyLaunched = false;
        pendingLaunchGame = -1;
        if (interGameTimer != null) {
            interGameTimer.cancel();
            interGameTimer = null;
        }

        boolean leftGame = (resultCode == RESULT_CANCELED)
                || (resultCode == RESULT_OK && data != null
                && data.getBooleanExtra("battleLost", false));
        if (leftGame) {
            abandonGame();
            return;
        }

        int points = 0;
        int completedRound = 1;
        if (resultCode == RESULT_OK && data != null) {
            points = data.getIntExtra("points", 0);
            completedRound = data.getIntExtra("myNumberRound",
                    data.getIntExtra("stepByStepRound", 1));
        }

        if (requestCode == GAME_MY_NUMBER) {
            if (completedRound == 1) {
                saveRound1ScoreAndLaunchRound2(points, GAME_MY_NUMBER);
            } else {
                lastCompletedGame = Math.max(lastCompletedGame, requestCode);
                saveScoreAndAdvance(points, requestCode);
            }
            return;
        }

        if (requestCode == GAME_STEP_BY_STEP) {
            if (completedRound == 1) {
                saveRound1ScoreAndLaunchRound2(points, GAME_STEP_BY_STEP);
            } else {
                lastCompletedGame = Math.max(lastCompletedGame, requestCode);
                saveScoreAndAdvance(points, requestCode);
            }
            return;
        }

        lastCompletedGame = Math.max(lastCompletedGame, requestCode);
        saveScoreAndAdvance(points, requestCode);
    }

    private void saveRound1ScoreAndLaunchRound2(int points, int gameType) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(gameRef);
            if (snapshot == null || !snapshot.exists()) return null;

            String player1 = snapshot.getString("player1");
            boolean isPlayer1 = currentUid.equals(player1);
            String scoreField = isPlayer1 ? "score1" : "score2";
            long currentScore = snapshot.getLong(scoreField) != null ?
                    snapshot.getLong(scoreField) : 0;

            Map<String, Object> updates = new HashMap<>();
            updates.put(scoreField, currentScore + points);

            if (gameType == GAME_MY_NUMBER) {
                updates.put("myNumberRound", 2L);
            } else if (gameType == GAME_STEP_BY_STEP) {
                updates.put("stepByStepRound", 2L);
                updates.put("stepByStepStatus", "");
            }

            transaction.update(gameRef, updates);
            return null;
        }).addOnSuccessListener(unused -> {
            gameAlreadyLaunched = false;
            pendingLaunchGame = -1;

            Intent intent;
            if (gameType == GAME_MY_NUMBER) {
                intent = new Intent(this, MyNumberActivity.class);
            } else {
                intent = new Intent(this, StepByStepActivity.class);
            }
            intent.putExtra("gameId", gameId);
            intent.putExtra("isMultiplayer", true);
            startActivityForResult(intent, gameType);
        });
    }

    private void saveScoreAndAdvance(int points, int gameNumber) {
        DocumentReference gameRef = db.collection("games").document(gameId);

        db.runTransaction(transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(gameRef);
                    if (snapshot == null || !snapshot.exists()) return null;

                    Long currentGameValue = snapshot.getLong("currentGame");
                    long currentGame = currentGameValue != null ? currentGameValue : 1;
                    if (currentGame != gameNumber) {
                        return null;
                    }

                    String player1 = snapshot.getString("player1");
                    String player2 = snapshot.getString("player2");
                    boolean isPlayer1 = currentUid.equals(player1);

                    String scoreField = isPlayer1 ? "score1" : "score2";
                    long currentScore = snapshot.getLong(scoreField) != null ?
                            snapshot.getLong(scoreField) : 0;

                    String currentTurnUid = snapshot.getString("currentTurnUid");

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
                    String abandonedBy = snapshot.getString("abandonedBy");
                    boolean opponentGone = abandonedBy != null && !abandonedBy.equals(currentUid);

                    if (Boolean.TRUE.equals(opponentDone) || opponentGone) {
                        long nextGame = currentGame + 1;
                        if (nextGame > TOTAL_GAMES) {
                            updates.put("status", "finished");
                            updates.put("currentTurnUid", null);
                        } else {
                            updates.put("currentGame", nextGame);
                            updates.put("currentTurnUid", opponentGone ? currentUid : player1);
                            updates.put("player1done_game" + nextGame, false);
                            updates.put("player2done_game" + nextGame, false);
                            if (gameNumber == GAME_MY_NUMBER) {
                                updates.put("myNumberRound", 1L);
                            }
                            if (gameNumber == GAME_STEP_BY_STEP) {
                                updates.put("stepByStepRound", 1L);
                                updates.put("stepByStepStatus", "");
                            }
                        }
                    }

                    transaction.update(gameRef, updates);
                    return null;
                })
                .addOnSuccessListener(unused -> listenForGameUpdates());
    }

    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onBackPressed() {
        confirmAbandonGame();
    }

    private void abandonGame() {
        if (isFinishing) return;
        isFinishing = true;
        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("abandonedBy", currentUid);

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused ->
                        db.collection("games").document(gameId).get()
                                .addOnSuccessListener(snapshot -> {
                                    boolean isFriendly = Boolean.TRUE.equals(snapshot.getBoolean("isFriendly"));

                                    db.collection("users").document(currentUid).update("inGame", false);

                                    if (isFriendly) {
                                        startActivity(new Intent(this, HomeActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                        finish();
                                        return;
                                    }

                                    String player1 = snapshot.getString("player1");
                                    long score1 = snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0;
                                    long score2 = snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0;
                                    long myScore = currentUid.equals(player1) ? score1 : score2;
                                    long opponentScore = currentUid.equals(player1) ? score2 : score1;

                                    boolean isTournament = Boolean.TRUE.equals(snapshot.getBoolean("isTournament"));
                                    Intent intent = new Intent(this, isTournament
                                            ? TournamentResultActivity.class
                                            : GameResultActivity.class);
                                    intent.putExtra("myScore", myScore);
                                    intent.putExtra("opponentScore", opponentScore);
                                    intent.putExtra("isFriendly", false);
                                    intent.putExtra("abandonedMatch", true);
                                    if (isTournament) {
                                        intent.putExtra("gameId", gameId);
                                        intent.putExtra("tournamentId", snapshot.getString("tournamentId"));
                                        intent.putExtra("tournamentRound", snapshot.getString("tournamentRound"));
                                    }
                                    startActivity(intent);
                                    finish();
                                })
                );
    }

    private void forceAdvanceSolo(int gameNumber) {
        DocumentReference gameRef = db.collection("games").document(gameId);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(gameRef);
            if (snapshot == null || !snapshot.exists()) return null;

            Long cg = snapshot.getLong("currentGame");
            long currentGame = cg != null ? cg : 1;
            if (currentGame != gameNumber) return null;

            Map<String, Object> updates = new HashMap<>();
            long nextGame = currentGame + 1;
            if (nextGame > TOTAL_GAMES) {
                updates.put("status", "finished");
                updates.put("currentTurnUid", null);
            } else {
                updates.put("currentGame", nextGame);
                updates.put("currentTurnUid", currentUid);
                updates.put("player1done_game" + nextGame, false);
                updates.put("player2done_game" + nextGame, false);
                if (gameNumber == GAME_MY_NUMBER) updates.put("myNumberRound", 1L);
                if (gameNumber == GAME_STEP_BY_STEP) {
                    updates.put("stepByStepRound", 1L);
                    updates.put("stepByStepStatus", "");
                }
            }
            transaction.update(gameRef, updates);
            return null;
        });
    }

    private void showResults(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (isFinishing) return;
        isFinishing = true;

        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }

        long score1 = snapshot.getLong("score1") != null ? snapshot.getLong("score1") : 0;
        long score2 = snapshot.getLong("score2") != null ? snapshot.getLong("score2") : 0;

        String player1 = snapshot.getString("player1");
        String player2 = snapshot.getString("player2");

        boolean isFriendly = Boolean.TRUE.equals(snapshot.getBoolean("isFriendly"));
        boolean isTournament = Boolean.TRUE.equals(snapshot.getBoolean("isTournament"));

        if (!isTournament) {
            if (player1 != null) {
                db.collection("users").document(player1).update("inGame", false);
            }

            if (player2 != null) {
                db.collection("users").document(player2).update("inGame", false);
            }
        }

        db.collection("games").document(gameId)
                .update("status", "completed");

        long myScore = currentUid.equals(player1) ? score1 : score2;
        long opponentScore = currentUid.equals(player1) ? score2 : score1;

        String abandonedBy = snapshot.getString("abandonedBy");

        Intent intent = new Intent(this, isTournament
                ? TournamentResultActivity.class
                : GameResultActivity.class);
        intent.putExtra("myScore", myScore);
        intent.putExtra("opponentScore", opponentScore);
        intent.putExtra("isFriendly", isFriendly);
        intent.putExtra("gameId", gameId);
        if (isTournament) {
            intent.putExtra("tournamentId", snapshot.getString("tournamentId"));
            intent.putExtra("tournamentRound", snapshot.getString("tournamentRound"));
        }
        if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
            intent.putExtra("opponentAbandoned", true);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }
        if (interGameTimer != null) {
            interGameTimer.cancel();
            interGameTimer = null;
        }
    }

    private void handleInviteDeclined() {
        if (isFinishing) return;
        isFinishing = true;
        if (gameListener != null) { gameListener.remove(); gameListener = null; }
        Toast.makeText(this, "Friend declined the invite.", Toast.LENGTH_LONG).show();
        db.collection("games").document(gameId).delete();
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
