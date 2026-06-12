package com.example.slagalica.games.StepByStep;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class StepByStepActivity extends AppCompatActivity {

    private static final int MAX_STEPS            = 7;
    private static final int STEP_DURATION_MS     = 10_000;
    private static final int POINTS_FIRST_STEP    = 20;
    private static final int POINTS_LOSS_PER_STEP = 2;
    private static final int OPPONENT_BONUS_MS    = 10_000;
    private static final int OPPONENT_BONUS_PTS   = 5;

    private TextView tvTimer, tvCurrentStep;
    private TextView[] stepTiles;
    private Button btnConfirm;
    private EditText etAnswer;

    private CountDownTimer stepTimer;
    private int currentStep = 0;
    private boolean isBattleMode;
    private boolean isMultiplayer = false;
    private String gameId;
    private String currentUid;
    private boolean isPlayer1 = false;
    private FirebaseFirestore db;
    private ListenerRegistration gameListener;

    private String[] clues;
    private String answer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        gameId = getIntent().getStringExtra("gameId");

        tvTimer       = findViewById(R.id.tvTimer);
        tvCurrentStep = findViewById(R.id.tvCurrentStep);
        btnConfirm    = findViewById(R.id.btnConfirm);
        etAnswer      = findViewById(R.id.etAnswer);

        stepTiles = new TextView[]{
                findViewById(R.id.tvStep1), findViewById(R.id.tvStep2),
                findViewById(R.id.tvStep3), findViewById(R.id.tvStep4),
                findViewById(R.id.tvStep5), findViewById(R.id.tvStep6),
                findViewById(R.id.tvStep7)
        };

        findViewById(R.id.layoutHeader).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bounce_in));

        btnConfirm.setOnClickListener(v -> handleGuess());

        loadQuestionFromFirebase();
    }

    private void loadQuestionFromFirebase() {
        db.collection("stepByStepQuestions")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        var doc = querySnapshot.getDocuments().get(0);
                        clues = new String[]{
                                doc.getString("clue1"), doc.getString("clue2"),
                                doc.getString("clue3"), doc.getString("clue4"),
                                doc.getString("clue5"), doc.getString("clue6"),
                                doc.getString("clue7")
                        };
                        answer = doc.getString("answer");
                    } else {
                        clues = new String[]{
                                "Clue 1", "Clue 2", "Clue 3", "Clue 4",
                                "Clue 5", "Clue 6", "Clue 7"
                        };
                        answer = "ANSWER";
                    }
                    if (isMultiplayer) {
                        determineMyTurn();
                    } else {
                        startStep();
                    }                });
    }

    private void determineMyTurn() {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    String player1 = snapshot.getString("player1");
                    isPlayer1 = currentUid.equals(player1);
                    if (isPlayer1) {
                        startStep();
                    } else {
                        waitForOpponentToFinish();
                    }
                });
    }

    private void waitForOpponentToFinish() {
        tvCurrentStep.setText("Waiting for opponent...");
        btnConfirm.setEnabled(false);
        etAnswer.setEnabled(false);

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;
                    String status = snapshot.getString("stepByStepStatus");
                    if ("player1done".equals(status)) {
                        if (gameListener != null) gameListener.remove();

                        Boolean opponentGuessed = snapshot.getBoolean("stepByStepOpponentGuessed");
                        if (Boolean.TRUE.equals(opponentGuessed)) {
                            waitForBonusChance();
                        } else {
                            btnConfirm.setEnabled(true);
                            etAnswer.setEnabled(true);
                            startStep();
                        }
                    }
                });
    }

    private void waitForBonusChance() {
        tvCurrentStep.setText("Opponent didn't guess! You have 10 seconds for a bonus!");
        btnConfirm.setEnabled(true);
        etAnswer.setEnabled(true);

        stepTimer = new CountDownTimer(OPPONENT_BONUS_MS, 1000) {
            @Override public void onTick(long ms) {
                tvTimer.setText("Bonus: " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                handleResult(0, "Bonus time's up. 0 points.");
            }
        }.start();
    }

    private void startStep() {
        if (currentStep >= MAX_STEPS) {
            endRoundNoGuess();
            return;
        }

        revealClue(currentStep);

        stepTimer = new CountDownTimer(STEP_DURATION_MS, 1000) {
            @Override public void onTick(long ms) {
                tvTimer.setText("⏱ " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                currentStep++;
                startStep();
            }
        }.start();
    }

    private void revealClue(int stepIndex) {
        tvCurrentStep.setText(clues[stepIndex]);
        tvCurrentStep.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        for (int i = 0; i <= stepIndex; i++) {
            stepTiles[i].setBackgroundResource(R.drawable.bg_tile_active);
            stepTiles[i].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void handleGuess() {
        String guess = etAnswer.getText().toString().trim();
        if (guess.isEmpty()) {
            Toast.makeText(this, "Enter your answer!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (guess.equalsIgnoreCase(answer)) {
            if (stepTimer != null) stepTimer.cancel();
            int points = calculatePoints(currentStep);
            handleResult(points, "Correct! You earned " + points + " points.");
        } else {
            Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show();
            etAnswer.setText("");
        }
    }

    private int calculatePoints(int stepIndex) {
        return Math.max(0, POINTS_FIRST_STEP - stepIndex * POINTS_LOSS_PER_STEP);
    }

    private void endRoundNoGuess() {
        tvTimer.setText("⏱ 0s");
        if (!isMultiplayer) {
            handleResult(0, "Time's up! 0 points.");
            return;
        }

        if (isPlayer1) {
            Toast.makeText(this, "You didn't guess! Opponent gets a bonus chance.", Toast.LENGTH_SHORT).show();
            savePointsAndReturn(0, true);
        } else {
            handleResult(0, "Time's up! 0 points.");
        }
    }

    private void handleResult(int points, String message) {

        new AlertDialog.Builder(this)
                .setTitle("Step by Step finished")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {

                    if (isBattleMode) {

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("score", points);

                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } else {

                        finish();
                    }
                })
                .show();

        if (stepTimer != null) stepTimer.cancel();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        btnConfirm.setEnabled(false);
        etAnswer.setEnabled(false);

        if (isMultiplayer) {
            boolean opponentGuessed = !isPlayer1 && points == OPPONENT_BONUS_PTS;
            savePointsAndReturn(points, false);
        } else {
            Intent result = new Intent();
            result.putExtra("points", points);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    private void savePointsAndReturn(int points, boolean opponentGetsBonus) {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    String player1 = snapshot.getString("player1");
                    String scoreField = currentUid.equals(player1) ? "score1" : "score2";
                    long currentScore = snapshot.getLong(scoreField) != null ?
                            snapshot.getLong(scoreField) : 0;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put(scoreField, currentScore + points);

                    if (currentUid.equals(player1)) {
                        updates.put("stepByStepStatus", "player1done");
                        updates.put("stepByStepOpponentGuessed", opponentGetsBonus);
                    } else {
                        updates.put("stepByStepStatus", "done");
                    }

                    db.collection("games").document(gameId).update(updates)
                            .addOnSuccessListener(unused -> {
                                Intent result = new Intent();
                                result.putExtra("points", points);
                                setResult(RESULT_OK, result);
                                finish();
                            });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stepTimer != null) stepTimer.cancel();
        if (gameListener != null) gameListener.remove();
    }
}