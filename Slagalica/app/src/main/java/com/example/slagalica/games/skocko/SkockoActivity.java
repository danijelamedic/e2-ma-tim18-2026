package com.example.slagalica.games.skocko;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.data.SkockoRepository;
import com.example.slagalica.models.SkockoGame;

public class SkockoActivity extends AppCompatActivity {

    private static final String[] SYMBOLS = {"🤡", "🟪", "🔵", "❤️", "🔺", "⭐"};
    private final TextView[] guessSlots = new TextView[4];
    private final TextView[][] attemptSlots = new TextView[6][4];
    private final TextView[][] feedbackDots = new TextView[6][4];
    private final TextView[] bonusSlots = new TextView[4];
    private final TextView[] bonusFeedbackDots = new TextView[4];
    private final TextView[] solutionSlots = new TextView[4];
    private final int[] currentGuess = {-1, -1, -1, -1};
    private final int[] secretCode = {-1, -1, -1, -1};

    private TextView tvTimer;
    private TextView tvAttempts;
    private TextView tvMode;
    private TextView tvCurrentGuessHint;
    private TextView tvPlayerScore;
    private TextView tvOpponentScore;
    private TextView tvYourAvatar;
    private TextView tvOpponentAvatar;
    private TextView tvBonusLabel;
    private TextView tvSolutionLabel;
    private Button btnSubmit;
    private CountDownTimer timer;
    private long timeLeftMillis = 30000;
    private int attemptIndex = 0;
    private boolean solved = false;
    private boolean opponentBonusTurn = false;
    private boolean roundFinished = false;
    private boolean gameLoaded = false;
    private int earnedScore = 0;
    private boolean isBattleMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skocko);

        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTimer = findViewById(R.id.tvSkockoTimer);
        tvAttempts = findViewById(R.id.tvSkockoAttempts);
        tvMode = findViewById(R.id.tvSkockoMode);
        tvCurrentGuessHint = findViewById(R.id.tvCurrentGuessHint);
        tvPlayerScore = findViewById(R.id.tvSkockoPlayerScore);
        tvOpponentScore = findViewById(R.id.tvSkockoOpponentScore);
        tvYourAvatar = findViewById(R.id.tvSkockoYourAvatar);
        tvOpponentAvatar = findViewById(R.id.tvSkockoOpponentAvatar);
        tvBonusLabel = findViewById(R.id.tvBonusRowLabel);
        tvSolutionLabel = findViewById(R.id.tvSolutionRowLabel);
        btnSubmit = findViewById(R.id.btnSubmitSkocko);

        setupLeaveButton();
        setupInfoButton();
        setupGuessSlots();
        setupPalette();
        setupAttemptsBoard();
        setupActionButtons();
        updateCurrentGuess();
        hideBonusAndSolutionRows();
        setControlsEnabled(false);
        tvMode.setText("Loading Skocko...");
        loadSkockoGame();
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveSkocko);
        btnLeave.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("YES", (dialog, which) -> finish())
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void setupInfoButton() {
        TextView btnInfo = findViewById(R.id.btnSkockoInfo);
        btnInfo.setOnClickListener(v -> {
            pauseTimer();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.skocko_rules_title)
                    .setMessage(R.string.skocko_rules_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        if (!roundFinished && timeLeftMillis > 0) {
                            startTimer();
                        }
                    })
                    .show();
        });
    }

    private void setupGuessSlots() {
        guessSlots[0] = findViewById(R.id.slotGuess1);
        guessSlots[1] = findViewById(R.id.slotGuess2);
        guessSlots[2] = findViewById(R.id.slotGuess3);
        guessSlots[3] = findViewById(R.id.slotGuess4);
    }

    private void setupPalette() {
        int[] paletteIds = {
                R.id.paletteSymbol1,
                R.id.paletteSymbol2,
                R.id.paletteSymbol3,
                R.id.paletteSymbol4,
                R.id.paletteSymbol5,
                R.id.paletteSymbol6
        };

        for (int i = 0; i < paletteIds.length; i++) {
            final int symbolIndex = i;
            findViewById(paletteIds[i]).setOnClickListener(v -> addSymbolToGuess(symbolIndex));
        }
    }

    private void setupAttemptsBoard() {
        int[][] slotIds = {
                {R.id.attempt1Slot1, R.id.attempt1Slot2, R.id.attempt1Slot3, R.id.attempt1Slot4},
                {R.id.attempt2Slot1, R.id.attempt2Slot2, R.id.attempt2Slot3, R.id.attempt2Slot4},
                {R.id.attempt3Slot1, R.id.attempt3Slot2, R.id.attempt3Slot3, R.id.attempt3Slot4},
                {R.id.attempt4Slot1, R.id.attempt4Slot2, R.id.attempt4Slot3, R.id.attempt4Slot4},
                {R.id.attempt5Slot1, R.id.attempt5Slot2, R.id.attempt5Slot3, R.id.attempt5Slot4},
                {R.id.attempt6Slot1, R.id.attempt6Slot2, R.id.attempt6Slot3, R.id.attempt6Slot4}
        };

        int[] feedbackIds = {
                R.id.attempt1Feedback1, R.id.attempt1Feedback2, R.id.attempt1Feedback3, R.id.attempt1Feedback4,
                R.id.attempt2Feedback1, R.id.attempt2Feedback2, R.id.attempt2Feedback3, R.id.attempt2Feedback4,
                R.id.attempt3Feedback1, R.id.attempt3Feedback2, R.id.attempt3Feedback3, R.id.attempt3Feedback4,
                R.id.attempt4Feedback1, R.id.attempt4Feedback2, R.id.attempt4Feedback3, R.id.attempt4Feedback4,
                R.id.attempt5Feedback1, R.id.attempt5Feedback2, R.id.attempt5Feedback3, R.id.attempt5Feedback4,
                R.id.attempt6Feedback1, R.id.attempt6Feedback2, R.id.attempt6Feedback3, R.id.attempt6Feedback4
        };
        int feedbackIndex = 0;

        for (int row = 0; row < slotIds.length; row++) {
            for (int column = 0; column < slotIds[row].length; column++) {
                attemptSlots[row][column] = findViewById(slotIds[row][column]);
                feedbackDots[row][column] = findViewById(feedbackIds[feedbackIndex++]);
            }
        }

        bonusSlots[0] = findViewById(R.id.bonusSlot1);
        bonusSlots[1] = findViewById(R.id.bonusSlot2);
        bonusSlots[2] = findViewById(R.id.bonusSlot3);
        bonusSlots[3] = findViewById(R.id.bonusSlot4);

        bonusFeedbackDots[0] = findViewById(R.id.bonusFeedback1);
        bonusFeedbackDots[1] = findViewById(R.id.bonusFeedback2);
        bonusFeedbackDots[2] = findViewById(R.id.bonusFeedback3);
        bonusFeedbackDots[3] = findViewById(R.id.bonusFeedback4);

        solutionSlots[0] = findViewById(R.id.solutionSlot1);
        solutionSlots[1] = findViewById(R.id.solutionSlot2);
        solutionSlots[2] = findViewById(R.id.solutionSlot3);
        solutionSlots[3] = findViewById(R.id.solutionSlot4);
    }

    private void setupActionButtons() {
        Button btnClear = findViewById(R.id.btnClearGuess);

        btnClear.setOnClickListener(v -> clearGuess());
        btnSubmit.setOnClickListener(v -> submitGuess());
    }

    private void loadSkockoGame() {
        new SkockoRepository().loadRandomSkockoGame(new SkockoRepository.SkockoCallback() {
            @Override
            public void onSuccess(SkockoGame game) {
                for (int i = 0; i < secretCode.length; i++) {
                    secretCode[i] = game.getSecretCode().get(i).intValue();
                }

                gameLoaded = true;
                setControlsEnabled(true);
                tvMode.setText(R.string.skocko_mode_your_turn);
                startTimer();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(SkockoActivity.this,
                        "Failed to load Skocko game.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        int[] paletteIds = {
                R.id.paletteSymbol1,
                R.id.paletteSymbol2,
                R.id.paletteSymbol3,
                R.id.paletteSymbol4,
                R.id.paletteSymbol5,
                R.id.paletteSymbol6
        };

        for (int id : paletteIds) {
            findViewById(id).setEnabled(enabled);
        }

        findViewById(R.id.btnClearGuess).setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
    }

    private void addSymbolToGuess(int symbolIndex) {
        if (!gameLoaded) {
            return;
        }

        for (int i = 0; i < currentGuess.length; i++) {
            if (currentGuess[i] == -1) {
                currentGuess[i] = symbolIndex;
                updateCurrentGuess();
                return;
            }
        }
    }

    private void clearGuess() {
        for (int i = 0; i < currentGuess.length; i++) {
            currentGuess[i] = -1;
        }
        updateCurrentGuess();
    }

    private void updateCurrentGuess() {
        for (int i = 0; i < guessSlots.length; i++) {
            if (currentGuess[i] == -1) {
                guessSlots[i].setText("?");
                guessSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_empty);
            } else {
                guessSlots[i].setText(SYMBOLS[currentGuess[i]]);
                guessSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
            }
        }
    }

    private void submitGuess() {
        if (!gameLoaded || roundFinished || !isGuessComplete()) {
            return;
        }

        if (opponentBonusTurn) {
            submitOpponentBonusGuess();
            return;
        }

        if (solved || attemptIndex >= 6) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            attemptSlots[attemptIndex][i].setText(SYMBOLS[currentGuess[i]]);
            attemptSlots[attemptIndex][i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
        }

        int[] feedback = evaluateGuess();
        bindFeedback(attemptIndex, feedback);

        attemptIndex++;
        tvAttempts.setText(getString(R.string.skocko_attempts_format, attemptIndex, 6));

        if (isSolved(feedback)) {
            solved = true;
            earnedScore = getScoreForSolvedAttempt(attemptIndex);
            tvPlayerScore.setText(getString(R.string.skocko_player_score_format, earnedScore));
            showEndDialog(getString(R.string.skocko_end_message_solved, attemptIndex, earnedScore));
            return;
        }

        if (attemptIndex == 6) {
            startOpponentBonusTurn();
            return;
        }

        clearGuess();
    }

    private void submitOpponentBonusGuess() {
        int[] feedback = evaluateGuess();
        bindBonusFeedback(feedback);

        for (int i = 0; i < 4; i++) {
            bonusSlots[i].setText(SYMBOLS[currentGuess[i]]);
            bonusSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
        }

        roundFinished = true;
        btnSubmit.setEnabled(false);
        revealSolution();

        if (isSolved(feedback)) {
            tvOpponentScore.setText(R.string.skocko_opponent_score_bonus);
            showEndDialog(getString(R.string.skocko_bonus_success_message));
        } else {
            showEndDialog(getString(R.string.skocko_bonus_fail_message));
        }
    }

    private boolean isGuessComplete() {
        for (int symbol : currentGuess) {
            if (symbol == -1) {
                return false;
            }
        }
        return true;
    }

    private int[] evaluateGuess() {
        int exact = 0;
        int partial = 0;
        boolean[] secretUsed = new boolean[4];
        boolean[] guessUsed = new boolean[4];

        for (int i = 0; i < 4; i++) {
            if (currentGuess[i] == secretCode[i]) {
                exact++;
                secretUsed[i] = true;
                guessUsed[i] = true;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (guessUsed[i]) {
                continue;
            }
            for (int j = 0; j < 4; j++) {
                if (!secretUsed[j] && currentGuess[i] == secretCode[j]) {
                    partial++;
                    secretUsed[j] = true;
                    break;
                }
            }
        }

        int[] feedback = new int[4];
        int index = 0;
        for (int i = 0; i < exact; i++) {
            feedback[index++] = 2;
        }
        for (int i = 0; i < partial; i++) {
            feedback[index++] = 1;
        }
        return feedback;
    }

    private void bindFeedback(int rowIndex, int[] feedback) {
        for (int i = 0; i < 4; i++) {
            if (feedback[i] == 2) {
                feedbackDots[rowIndex][i].setBackgroundResource(R.drawable.bg_skocko_feedback_exact);
            } else if (feedback[i] == 1) {
                feedbackDots[rowIndex][i].setBackgroundResource(R.drawable.bg_skocko_feedback_partial);
            } else {
                feedbackDots[rowIndex][i].setBackgroundResource(R.drawable.bg_skocko_feedback_empty);
            }
        }
    }

    private void bindBonusFeedback(int[] feedback) {
        for (int i = 0; i < 4; i++) {
            if (feedback[i] == 2) {
                bonusFeedbackDots[i].setBackgroundResource(R.drawable.bg_skocko_feedback_exact);
            } else if (feedback[i] == 1) {
                bonusFeedbackDots[i].setBackgroundResource(R.drawable.bg_skocko_feedback_partial);
            } else {
                bonusFeedbackDots[i].setBackgroundResource(R.drawable.bg_skocko_feedback_empty);
            }
        }
    }

    private boolean isSolved(int[] feedback) {
        for (int value : feedback) {
            if (value != 2) {
                return false;
            }
        }
        return true;
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                int seconds = (int) (millisUntilFinished / 1000);
                tvTimer.setText(getString(R.string.skocko_timer_format, seconds));
            }

            @Override
            public void onFinish() {
                timeLeftMillis = 0;
                tvTimer.setText(getString(R.string.skocko_timer_format, 0));
                if (opponentBonusTurn) {
                    roundFinished = true;
                    btnSubmit.setEnabled(false);
                    revealSolution();
                    showEndDialog(getString(R.string.skocko_bonus_timeout_message));
                } else {
                    startOpponentBonusTurn();
                }
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void showEndDialog(String message) {
        if (timer != null) {
            timer.cancel();
        }

        if (isFinishing() || isDestroyed() || getWindow() == null
                || getWindow().getDecorView().getWindowToken() == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.skocko_end_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (isBattleMode) {

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("score", earnedScore);
                        resultIntent.putExtra("points", earnedScore);

                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } else {

                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    }
                })
                .show();
    }

    private void startOpponentBonusTurn() {
        opponentBonusTurn = true;
        solved = false;
        timeLeftMillis = 10000;
        clearGuess();

        tvMode.setText(R.string.skocko_mode_opponent_bonus);
        tvAttempts.setText(R.string.skocko_bonus_attempt_label);
        tvCurrentGuessHint.setText(R.string.skocko_bonus_hint);
        btnSubmit.setText(R.string.skocko_submit_bonus);
        tvBonusLabel.setVisibility(View.GONE);
        findViewById(R.id.rowBonusAttempt).setVisibility(View.GONE);

        tvYourAvatar.setBackgroundResource(R.drawable.bg_inactive_player_avatar);
        tvOpponentAvatar.setBackgroundResource(R.drawable.bg_active_player_avatar);

        startTimer();
    }

    private void revealSolution() {
        tvSolutionLabel.setVisibility(View.VISIBLE);
        findViewById(R.id.rowSolution).setVisibility(View.VISIBLE);

        for (int i = 0; i < 4; i++) {
            solutionSlots[i].setText(SYMBOLS[secretCode[i]]);
            solutionSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
        }
    }

    private void hideBonusAndSolutionRows() {
        tvBonusLabel.setVisibility(View.GONE);
        tvSolutionLabel.setVisibility(View.GONE);
        findViewById(R.id.rowBonusAttempt).setVisibility(View.GONE);
        findViewById(R.id.rowSolution).setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }

    private int getScoreForSolvedAttempt(int solvedAttempt) {
        if (solvedAttempt <= 2) {
            return 20;
        }
        if (solvedAttempt <= 4) {
            return 15;
        }
        return 10;
    }
}
