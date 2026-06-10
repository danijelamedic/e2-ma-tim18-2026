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

public class StepByStepActivity extends AppCompatActivity {

    private static final int MAX_STEPS            = 7;
    private static final int STEP_DURATION_MS     = 10_000;
    private static final int POINTS_FIRST_STEP    = 20;
    private static final int POINTS_LOSS_PER_STEP = 2;
    private static final int OPPONENT_TIME_MS     = 10_000;

    private TextView tvTimer, tvCurrentStep;
    private TextView[] stepTiles;
    private Button btnConfirm;
    private EditText etAnswer;

    private CountDownTimer stepTimer;
    private int currentStep = 0;
    private boolean isBattleMode;

    private final String[] clues = {
            "Clue 1 (hardest)", "Clue 2", "Clue 3", "Clue 4",
            "Clue 5", "Clue 6", "Clue 7 (easiest)"
    };
    private final String answer = "ANSWER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

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

        startStep();
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
            stepTimer.cancel();
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
        Toast.makeText(this, "Time's up! Opponent gets a chance.", Toast.LENGTH_SHORT).show();
        startOpponentChance();
    }

    private void startOpponentChance() {
        btnConfirm.setEnabled(false);
        etAnswer.setEnabled(false);

        stepTimer = new CountDownTimer(OPPONENT_TIME_MS, 1000) {
            @Override public void onTick(long ms) {
                tvTimer.setText("Opponent: " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                handleResult(0, "Opponent missed too. 0 points.");
            }
        }.start();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stepTimer != null) stepTimer.cancel();
    }
}