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
    private boolean gameScreenOpen = false;
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

                    // Check terminal states FIRST so a finished match always shows
                    // results, even when the opponent had abandoned earlier.
                    if ("declined".equals(status)) {
                        handleInviteDeclined();
                        return;
                    }
                    if ("finished".equals(status)) {
                        showResults(snapshot);
                        return;
                    }

                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        boolean wasAlreadyLeft = opponentAlreadyLeft;
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

                        // Opponent left. Per REQ3f the remaining player continues SOLO
                        // with no waiting. Re-evaluate every snapshot (not just once) so
                        // we never get stuck, regardless of which phase we are in.
                        if (interGameTimer != null) {
                            interGameTimer.cancel();
                            interGameTimer = null;
                        }
                        // Clear BOTH guards so the solo (re)launch below can proceed.
                        gameAlreadyLaunched = false;
                        pendingLaunchGame = -1;

                        Long soloGameLong = snapshot.getLong("currentGame");
                        int soloGame = soloGameLong != null ? soloGameLong.intValue() : 1;
                        String soloPlayer1 = snapshot.getString("player1");
                        boolean soloIsPlayer1 = currentUid.equals(soloPlayer1);
                        String soloMyDoneField = soloIsPlayer1
                                ? "player1done_game" + soloGame
                                : "player2done_game" + soloGame;
                        Boolean soloMyDone = snapshot.getBoolean(soloMyDoneField);

                        if (Boolean.TRUE.equals(soloMyDone)) {
                            // I already finished this game and was waiting -> advance now.
                            if (soloGame > lastCompletedGame) {
                                saveScoreAndAdvance(0, soloGame);
                            }
                            return;
                        } else {
                            // I have not finished this game yet -> start/continue it solo.
                            startInterGameCountdown(soloGame);
                            return;
                        }
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

                    // Always safe to refresh the score.
                    tvMyScore.setText("Your score: " + myScore);
                    tvOpponentScore.setText("Opponent score: " + opponentScore);

                    // Stale cached snapshot guard FIRST (before showing any game name),
                    // so we never flash "Next game: <already-played game>".
                    // If currentGame is one I've already played, wait for the advance.
                    if (currentGame <= lastCompletedGame && !Boolean.TRUE.equals(myDone)) {
                        tvGameInfo.setText("Loading next game...");
                        return;
                    }
                    if (currentGame < lastCompletedGame) {
                        tvGameInfo.setText("Waiting for next game...");
                        return;
                    }

                    if (Boolean.TRUE.equals(myDone)) {
                        if (!opponentAlreadyLeft) {
                            tvGameInfo.setText("Waiting for opponent...");
                            return;
                        }
                        tvGameInfo.setText("Opponent left — you continue alone!");
                        if (!gameAlreadyLaunched) {
                            gameAlreadyLaunched = true;
                            saveScoreAndAdvance(0, currentGame);
                        }
                        return;
                    }

                    // Validated: show the upcoming game name only now.
                    tvGameName.setText("Next game: " + getGameName(currentGame));

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

        long countdownMs = opponentAlreadyLeft ? 1000 : 10000;
        interGameTimer = new CountDownTimer(countdownMs, 1000) {
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
        // Guard: a game screen is already open -> never stack another on top.
        if (gameScreenOpen) {
            return;
        }
        gameScreenOpen = true;
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

        gameScreenOpen = false;
        gameAlreadyLaunched = false;
        pendingLaunchGame = -1;
        // Show the upcoming game name immediately (we know it = requestCode + 1)
        // instead of a generic loading text, so there is no visible delay while
        // the next Firestore snapshot arrives. The listener will confirm/correct it.
        int nextGuess = requestCode + 1;
        if (tvGameName != null) {
            if (nextGuess <= TOTAL_GAMES) {
                tvGameName.setText("Next game: " + getGameName(nextGuess));
            } else {
                tvGameName.setText("");
            }
        }
        if (tvGameInfo != null) tvGameInfo.setText("Starting...");
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
            intent.putExtra("opponentAlreadyLeft", opponentAlreadyLeft);
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
                .addOnSuccessListener(unused -> {
                    gameAlreadyLaunched = false;
                    pendingLaunchGame = -1;
                    listenForGameUpdates();
                });
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