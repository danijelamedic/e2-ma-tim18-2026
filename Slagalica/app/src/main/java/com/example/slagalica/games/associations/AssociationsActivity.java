package com.example.slagalica.games.associations;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;

import java.util.Locale;

public class AssociationsActivity extends AppCompatActivity {

    private static final String[][] COLUMN_DATA = {
            {"JAVA", "PYTHON", "C++", "KOTLIN", "LANGUAGE"},
            {"ARRAY", "STACK", "QUEUE", "LIST", "STRUCTURE"},
            {"REACT", "ANGULAR", "VUE", "SPRING", "FRAMEWORK"},
            {"GIT", "COMMIT", "BRANCH", "MERGE", "VERSION"}
    };
    private static final String FINAL_SOLUTION = "PROGRAMMING";
    private static final String[] COLUMN_KEYS = {"A", "B", "C", "D"};

    private final TextView[][] clueViews = new TextView[4][4];
    private final TextView[] solutionViews = new TextView[4];
    private final boolean[][] openedClues = new boolean[4][4];
    private final boolean[] solvedColumns = new boolean[4];

    private TextView tvTimer;
    private TextView tvPlayerScore;
    private TextView tvPoints;
    private TextView tvTurn;
    private TextView tvFinalSolution;
    private CountDownTimer timer;
    private long timeLeftMillis = 120000;
    private int playerScore = 0;
    private int openedFieldsCount = 0;
    private boolean finalSolved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_associations);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupLeaveButton();
        setupInfoButton();
        setupBoard();
        setupFinalButton();
        startTimer();
    }

    private void bindViews() {
        tvTimer = findViewById(R.id.tvAssociationsTimer);
        tvPlayerScore = findViewById(R.id.tvAssociationsPlayerScore);
        tvPoints = findViewById(R.id.tvAssociationsPoints);
        tvTurn = findViewById(R.id.tvAssociationsTurn);
        tvFinalSolution = findViewById(R.id.tvFinalSolution);
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveAssociations);
        btnLeave.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("YES", (dialog, which) -> finish())
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void setupInfoButton() {
        TextView btnInfo = findViewById(R.id.btnAssociationsInfo);
        btnInfo.setOnClickListener(v -> {
            pauseTimer();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.associations_rules_title)
                    .setMessage(R.string.associations_rules_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        if (!finalSolved && timeLeftMillis > 0) {
                            startTimer();
                        }
                    })
                    .show();
        });
    }

    private void setupBoard() {
        int[][] clueIds = {
                {R.id.tvA1, R.id.tvA2, R.id.tvA3, R.id.tvA4},
                {R.id.tvB1, R.id.tvB2, R.id.tvB3, R.id.tvB4},
                {R.id.tvC1, R.id.tvC2, R.id.tvC3, R.id.tvC4},
                {R.id.tvD1, R.id.tvD2, R.id.tvD3, R.id.tvD4}
        };
        int[] solutionIds = {R.id.tvASolution, R.id.tvBSolution, R.id.tvCSolution, R.id.tvDSolution};

        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                clueViews[column][row] = findViewById(clueIds[column][row]);
                final int currentColumn = column;
                final int currentRow = row;
                clueViews[column][row].setOnClickListener(v -> revealClue(currentColumn, currentRow));
            }

            solutionViews[column] = findViewById(solutionIds[column]);
            final int currentColumn = column;
            solutionViews[column].setOnClickListener(v -> promptColumnGuess(currentColumn));
        }
    }

    private void setupFinalButton() {
        tvFinalSolution.setOnClickListener(v -> promptFinalGuess());
    }

    private void revealClue(int column, int row) {
        if (finalSolved || solvedColumns[column] || openedClues[column][row]) {
            return;
        }

        openedClues[column][row] = true;
        openedFieldsCount++;
        clueViews[column][row].setText(COLUMN_DATA[column][row]);
        clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
        updateOpenedFieldsStatus();
        tvTurn.setText(getString(R.string.associations_turn_opened, COLUMN_KEYS[column], row + 1));
    }

    private void promptColumnGuess(int column) {
        if (finalSolved || solvedColumns[column]) {
            return;
        }

        showGuessDialog(getString(R.string.associations_column_guess_title, COLUMN_KEYS[column]), guess -> {
            if (normalize(guess).equals(normalize(COLUMN_DATA[column][4]))) {
                solveColumn(column, true);
            } else {
                tvTurn.setText(R.string.associations_turn_wrong);
            }
        });
    }

    private void promptFinalGuess() {
        if (finalSolved) {
            return;
        }

        showGuessDialog(getString(R.string.associations_final_guess_title), guess -> {
            if (normalize(guess).equals(normalize(FINAL_SOLUTION))) {
                solveFinal();
            } else {
                tvTurn.setText(R.string.associations_turn_wrong);
            }
        });
    }

    private void showGuessDialog(String title, GuessHandler handler) {
        pauseTimer();

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.associations_guess_hint);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(R.string.associations_guess_action, (dialog, which) -> {
                    handler.onGuess(input.getText().toString().trim());
                    if (!finalSolved && timeLeftMillis > 0) {
                        startTimer();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    if (!finalSolved && timeLeftMillis > 0) {
                        startTimer();
                    }
                })
                .show();
    }

    private void solveColumn(int column, boolean addPoints) {
        int earned = 2 + countUnopenedFields(column);
        solvedColumns[column] = true;

        for (int row = 0; row < 4; row++) {
            if (!openedClues[column][row]) {
                openedClues[column][row] = true;
                openedFieldsCount++;
            }
            clueViews[column][row].setText(COLUMN_DATA[column][row]);
            clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
            clueViews[column][row].setEnabled(false);
        }

        solutionViews[column].setText(COLUMN_DATA[column][4]);
        solutionViews[column].setBackgroundResource(R.drawable.bg_associations_solved);
        solutionViews[column].setEnabled(false);
        updateOpenedFieldsStatus();

        if (addPoints) {
            playerScore += earned;
            updatePlayerScore();
            tvTurn.setText(getString(R.string.associations_column_solved_message, COLUMN_KEYS[column], earned));
        }
    }

    private void solveFinal() {
        int bonus = 7 + countUnopenedColumns() * 6;
        finalSolved = true;

        for (int column = 0; column < 4; column++) {
            if (!solvedColumns[column]) {
                solveColumn(column, false);
            }
        }

        playerScore += bonus;
        updatePlayerScore();
        tvFinalSolution.setText(FINAL_SOLUTION);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_solved);
        tvFinalSolution.setEnabled(false);
        tvTurn.setText(R.string.associations_final_solved_status);
        showEndDialog(getString(R.string.associations_end_message, playerScore, bonus));
    }

    private int countUnopenedFields(int column) {
        int count = 0;
        for (int row = 0; row < 4; row++) {
            if (!openedClues[column][row]) {
                count++;
            }
        }
        return count;
    }

    private int countUnopenedColumns() {
        int count = 0;
        for (int column = 0; column < 4; column++) {
            if (!solvedColumns[column] && countOpenedClues(column) == 0) {
                count++;
            }
        }
        return count;
    }

    private int countOpenedClues(int column) {
        int count = 0;
        for (int row = 0; row < 4; row++) {
            if (openedClues[column][row]) {
                count++;
            }
        }
        return count;
    }

    private void updatePlayerScore() {
        tvPlayerScore.setText(getString(R.string.associations_score_format, playerScore));
    }

    private void updateOpenedFieldsStatus() {
        tvPoints.setText(getString(R.string.associations_progress_format, openedFieldsCount));
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                int totalSeconds = (int) (millisUntilFinished / 1000);
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                tvTimer.setText(getString(R.string.associations_timer_format, minutes, seconds));
            }

            @Override
            public void onFinish() {
                timeLeftMillis = 0;
                tvTimer.setText(getString(R.string.associations_timer_format, 0, 0));
                revealBoard();
                showEndDialog(getString(R.string.associations_timeout_message, playerScore));
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void revealBoard() {
        for (int column = 0; column < 4; column++) {
            if (!solvedColumns[column]) {
                solveColumn(column, false);
            }
        }
        tvFinalSolution.setText(FINAL_SOLUTION);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_opened);
        tvFinalSolution.setEnabled(false);
    }

    private void showEndDialog(String message) {
        pauseTimer();

        new AlertDialog.Builder(this)
                .setTitle(R.string.associations_end_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .show();
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private interface GuessHandler {
        void onGuess(String guess);
    }
}
