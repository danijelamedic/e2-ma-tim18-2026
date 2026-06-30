package com.example.slagalica.games.skocko;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.example.slagalica.data.PlayerProfileLoader;
import com.example.slagalica.data.SkockoRepository;
import com.example.slagalica.models.SkockoGame;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;

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
    private TextView tvCurrentGuessTitle;
    private TextView tvCurrentGuessHint;
    private TextView tvPlayerScore;
    private TextView tvOpponentScore;
    private TextView tvPlayerName;
    private TextView tvOpponentName;
    private TextView tvPlayerInfo;
    private TextView tvOpponentInfo;
    private ImageView tvYourAvatar;
    private ImageView tvOpponentAvatar;
    private TextView tvBonusLabel;
    private TextView tvSolutionLabel;
    private TextView btnSubmit;
    private CountDownTimer timer;
    private long timeLeftMillis = 30000;
    private int attemptIndex = 0;
    private boolean solved = false;
    private boolean opponentBonusTurn = false;
    private boolean roundFinished = false;
    private boolean gameLoaded = false;
    private boolean statisticsSaved = false;
    private int earnedScore = 0;
    private boolean isBattleMode;
    private boolean isMultiplayer;
    private boolean opponentAlreadyLeft;
    private boolean isFriendly;
    private boolean multiplayerResultSent = false;
    private boolean multiplayerTimeoutHandled = false;
    private FirebaseFirestore db;
    private DocumentReference skockoStateRef;
    private ListenerRegistration skockoStateListener;
    private ListenerRegistration abandonListener;
    private String gameId;
    private String currentUid;
    private String player1Uid;
    private String player2Uid;
    private String opponentUid;
    private String activeUid;
    private String phase = "";
    private int roundNumber = 1;
    private Map<String, Long> multiplayerScores = new HashMap<>();
    private List<Map<String, Object>> multiplayerAttempts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skocko);

        if (getIntent().getBooleanExtra("isBattleMode", false)) {
            android.view.View opponentPanel = findViewById(R.id.layoutOpponentPanel);
            if (opponentPanel != null) opponentPanel.setVisibility(android.view.View.GONE);
            android.view.View vsLabel = findViewById(R.id.tvVsLabel);
            if (vsLabel != null) vsLabel.setVisibility(android.view.View.GONE);
            android.view.View playerScoreView = findViewById(R.id.tvSkockoPlayerScore);
            if (playerScoreView != null) playerScoreView.setVisibility(android.view.View.GONE);
            android.view.View playerPanel = findViewById(R.id.layoutPlayerPanel);
            if (playerPanel != null && playerPanel.getLayoutParams() instanceof android.widget.LinearLayout.LayoutParams) {
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) playerPanel.getLayoutParams();
                lp.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
                lp.weight = 0f;
                playerPanel.setLayoutParams(lp);
            }
        }

        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        opponentAlreadyLeft = getIntent().getBooleanExtra("opponentAlreadyLeft", false);
        isFriendly = getIntent().getBooleanExtra("isFriendly", false);
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        gameId = getIntent().getStringExtra("gameId");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTimer = findViewById(R.id.tvSkockoTimer);
        tvAttempts = findViewById(R.id.tvSkockoAttempts);
        tvMode = findViewById(R.id.tvSkockoMode);
        tvCurrentGuessTitle = findViewById(R.id.tvCurrentGuessTitle);
        tvCurrentGuessHint = findViewById(R.id.tvCurrentGuessHint);
        tvPlayerScore = findViewById(R.id.tvSkockoPlayerScore);
        tvOpponentScore = findViewById(R.id.tvSkockoOpponentScore);
        tvPlayerName = findViewById(R.id.tvSkockoPlayerName);
        tvOpponentName = findViewById(R.id.tvSkockoOpponentName);
        tvPlayerInfo = findViewById(R.id.tvSkockoPlayerInfo);
        tvOpponentInfo = findViewById(R.id.tvSkockoOpponentInfo);
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
        loadCurrentPlayerPanel();
        tvMode.setText("Loading Skocko...");
        if (isMultiplayer && gameId != null && currentUid != null) {
            loadMultiplayerContext();
        } else {
            loadSkockoGame();
        }
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveSkocko);
        btnLeave.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Leave Game")
                .setMessage("Are you sure you want to leave the game?")
                .setPositiveButton("YES", (dialog, which) -> leaveGame())
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void leaveGame() {
        pauseTimer();
        if (skockoStateListener != null) {
            skockoStateListener.remove();
            skockoStateListener = null;
        }
        if (abandonListener != null) {
            abandonListener.remove();
            abandonListener = null;
        }

        if (isBattleMode || isMultiplayer) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("battleLost", true);
            setResult(RESULT_OK, resultIntent);
        }
        finish();
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

    private void loadMultiplayerContext() {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Game session not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    player1Uid = snapshot.getString("player1");
                    player2Uid = snapshot.getString("player2");
                    opponentUid = currentUid.equals(player1Uid) ? player2Uid : player1Uid;
                    skockoStateRef = db.collection("games")
                            .document(gameId)
                            .collection("gameStates")
                            .document("skocko");

                    loadPlayerPanels();
                    listenForSkockoState();
                    listenForAbandon();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load players.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadPlayerPanels() {
        PlayerProfileLoader.load(currentUid, summary -> {
            tvPlayerName.setText(summary.username);
            tvPlayerInfo.setText(summary.info);
            tvYourAvatar.setImageResource(summary.avatarResId);
        });

        PlayerProfileLoader.load(opponentUid, summary -> {
            tvOpponentName.setText(summary.username);
            tvOpponentInfo.setText(summary.info);
            tvOpponentAvatar.setImageResource(summary.avatarResId);
        });
    }

    private void loadCurrentPlayerPanel() {
        if (currentUid == null) {
            return;
        }

        PlayerProfileLoader.load(currentUid, summary -> {
            tvPlayerName.setText(summary.username);
            tvPlayerInfo.setText(summary.info);
            tvYourAvatar.setImageResource(summary.avatarResId);
        });
    }

    private void listenForSkockoState() {
        skockoStateListener = skockoStateRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                if (currentUid.equals(player1Uid) || opponentAlreadyLeft) {
                    createSkockoRound(1, opponentAlreadyLeft ? currentUid : player1Uid, null);
                } else {
                    tvMode.setText("Waiting for game...");
                }
                return;
            }

            applyMultiplayerState(snapshot);
        });
    }

    private void createSkockoRound(int nextRound, String starterUid, Map<String, Long> existingScores) {
        new SkockoRepository().loadRandomSkockoGame(new SkockoRepository.SkockoCallback() {
            @Override
            public void onSuccess(SkockoGame game) {
                Map<String, Long> scores = existingScores != null ? existingScores : new HashMap<>();
                if (!scores.containsKey(player1Uid)) scores.put(player1Uid, 0L);
                if (!scores.containsKey(player2Uid)) scores.put(player2Uid, 0L);

                Map<String, Object> data = new HashMap<>();
                data.put("roundNumber", (long) nextRound);
                data.put("starterUid", starterUid);
                data.put("activeUid", starterUid);
                data.put("phase", "starter_turn");
                data.put("secretCode", game.getSecretCode());
                data.put("attempts", new ArrayList<>());
                data.put("bonusAttempt", null);
                data.put("draftGuess", emptyGuessList());
                data.put("scores", scores);
                data.put("finished", false);
                data.put("showSolution", false);
                data.put("phaseEndsAt", System.currentTimeMillis() + 30000);
                skockoStateRef.set(data);
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

    @SuppressWarnings("unchecked")
    private void applyMultiplayerState(DocumentSnapshot snapshot) {
        Long round = snapshot.getLong("roundNumber");
        roundNumber = round != null ? round.intValue() : 1;
        phase = snapshot.getString("phase") != null ? snapshot.getString("phase") : "";
        activeUid = snapshot.getString("activeUid");
        Boolean finished = snapshot.getBoolean("finished");

        List<Long> code = (List<Long>) snapshot.get("secretCode");
        if (code != null && code.size() == 4) {
            for (int i = 0; i < 4; i++) {
                secretCode[i] = code.get(i).intValue();
            }
            gameLoaded = true;
        }

        Map<String, Object> rawScores = (Map<String, Object>) snapshot.get("scores");
        multiplayerScores = toLongScoreMap(rawScores);
        tvPlayerScore.setText(getString(R.string.skocko_player_score_format, getScoreFor(currentUid)));
        tvOpponentScore.setText(getString(R.string.skocko_player_score_format, getScoreFor(opponentUid)));

        Object attemptsObject = snapshot.get("attempts");
        multiplayerAttempts = attemptsObject instanceof List
                ? (List<Map<String, Object>>) attemptsObject
                : new ArrayList<>();
        renderMultiplayerAttempts();

        Map<String, Object> bonusAttempt = (Map<String, Object>) snapshot.get("bonusAttempt");
        renderBonusAttempt(bonusAttempt);
        renderSolution(Boolean.TRUE.equals(snapshot.getBoolean("showSolution")));

        int[] draftGuess = listToIntArray(snapshot.get("draftGuess"));
        boolean playablePhase = "starter_turn".equals(phase) || "bonus_turn".equals(phase);
        if (isMultiplayer && playablePhase) {
            copyGuess(draftGuess, currentGuess);
            updateCurrentGuess();
        }

        boolean myTurnNow = opponentAlreadyLeft || currentUid.equals(activeUid);
        tvCurrentGuessTitle.setText(myTurnNow ? "Your guess" : "Opponent guess");
        tvAttempts.setText("bonus_turn".equals(phase)
                ? getString(R.string.skocko_bonus_attempt_label)
                : getString(R.string.skocko_attempts_format, multiplayerAttempts.size(), 6));
        tvMode.setText(myTurnNow ? "Your turn" : "Opponent turn");
        updateActiveAvatar();

        boolean myTurn = myTurnNow
                && gameLoaded
                && !Boolean.TRUE.equals(finished)
                && ("starter_turn".equals(phase) || "bonus_turn".equals(phase));
        setControlsEnabled(myTurn);

        Long phaseEndsAt = snapshot.getLong("phaseEndsAt");
        startMultiplayerTimer(phaseEndsAt != null ? phaseEndsAt : 0L);

        if (Boolean.TRUE.equals(finished) && !multiplayerResultSent) {
            multiplayerResultSent = true;

            if (!statisticsSaved) {
                statisticsSaved = true;

                int myScore = (int) getScoreFor(currentUid);
                int guessedAttempt = getMySolvedAttempt();

                // Friendly matches do not count towards statistics.
                if (!isFriendly) {
                    StatisticsRepository.saveSkockoResult(myScore, guessedAttempt);
                }
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("score", (int) getScoreFor(currentUid));
            resultIntent.putExtra("points", (int) getScoreFor(currentUid));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private Map<String, Long> toLongScoreMap(Map<String, Object> rawScores) {
        Map<String, Long> scores = new HashMap<>();
        if (rawScores != null) {
            for (Map.Entry<String, Object> entry : rawScores.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    scores.put(entry.getKey(), ((Number) value).longValue());
                }
            }
        }
        if (player1Uid != null && !scores.containsKey(player1Uid)) scores.put(player1Uid, 0L);
        if (player2Uid != null && !scores.containsKey(player2Uid)) scores.put(player2Uid, 0L);
        return scores;
    }

    private long getScoreFor(String uid) {
        Long score = multiplayerScores.get(uid);
        return score != null ? score : 0L;
    }

    private void renderMultiplayerAttempts() {
        resetAttemptsBoard();
        for (int row = 0; row < multiplayerAttempts.size() && row < 6; row++) {
            Map<String, Object> attempt = multiplayerAttempts.get(row);
            int[] guess = listToIntArray(attempt.get("guess"));
            int[] feedback = listToIntArray(attempt.get("feedback"));
            for (int column = 0; column < 4; column++) {
                if (guess[column] >= 0) {
                    attemptSlots[row][column].setText(SYMBOLS[guess[column]]);
                    attemptSlots[row][column].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
                }
            }
            bindFeedback(row, feedback);
        }
    }

    private void resetAttemptsBoard() {
        for (int row = 0; row < attemptSlots.length; row++) {
            for (int column = 0; column < 4; column++) {
                attemptSlots[row][column].setText("?");
                attemptSlots[row][column].setBackgroundResource(R.drawable.bg_skocko_slot_empty);
                feedbackDots[row][column].setBackgroundResource(R.drawable.bg_skocko_feedback_empty);
            }
        }
    }

    private void renderBonusAttempt(Map<String, Object> bonusAttempt) {
        if (bonusAttempt == null) {
            tvBonusLabel.setVisibility(View.GONE);
            findViewById(R.id.rowBonusAttempt).setVisibility(View.GONE);
            return;
        }

        int[] guess = listToIntArray(bonusAttempt.get("guess"));
        int[] feedback = listToIntArray(bonusAttempt.get("feedback"));
        for (int i = 0; i < 4; i++) {
            if (guess[i] >= 0) {
                bonusSlots[i].setText(SYMBOLS[guess[i]]);
                bonusSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
            }
        }
        bindBonusFeedback(feedback);
        tvBonusLabel.setVisibility(View.GONE);
        findViewById(R.id.rowBonusAttempt).setVisibility(View.VISIBLE);
    }

    private void renderSolution(boolean visible) {
        if (!visible) {
            tvSolutionLabel.setVisibility(View.GONE);
            findViewById(R.id.rowSolution).setVisibility(View.GONE);
            return;
        }

        tvSolutionLabel.setVisibility(View.GONE);
        findViewById(R.id.rowSolution).setVisibility(View.VISIBLE);
        for (int i = 0; i < 4; i++) {
            if (secretCode[i] >= 0) {
                solutionSlots[i].setText(SYMBOLS[secretCode[i]]);
                solutionSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
            }
        }
    }

    private int[] listToIntArray(Object object) {
        int[] values = {-1, -1, -1, -1};
        if (!(object instanceof List)) {
            return values;
        }
        List<?> list = (List<?>) object;
        for (int i = 0; i < list.size() && i < 4; i++) {
            Object value = list.get(i);
            if (value instanceof Number) {
                values[i] = ((Number) value).intValue();
            }
        }
        return values;
    }

    private void updateActiveAvatar() {
        boolean myTurn = currentUid.equals(activeUid);
        boolean opponentTurn = opponentUid != null && opponentUid.equals(activeUid);
        tvYourAvatar.setBackgroundResource(myTurn
                ? R.drawable.bg_active_player_avatar
                : R.drawable.bg_inactive_player_avatar);
        tvOpponentAvatar.setBackgroundResource(opponentTurn
                ? R.drawable.bg_active_player_avatar
                : R.drawable.bg_inactive_player_avatar);
    }

    private void startMultiplayerTimer(long phaseEndsAt) {
        if (timer != null) {
            timer.cancel();
        }
        long remaining = Math.max(0, phaseEndsAt - System.currentTimeMillis());
        multiplayerTimeoutHandled = false;
        timer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(getString(R.string.skocko_timer_format, (int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                tvTimer.setText(getString(R.string.skocko_timer_format, 0));
                boolean resultPhase = "round_result".equals(phase)
                        || "game_finished_pending".equals(phase);
                boolean shouldHandlePhase = resultPhase
                        ? (currentUid.equals(player1Uid) || opponentAlreadyLeft)
                        : currentUid.equals(activeUid);
                if (shouldHandlePhase && !multiplayerTimeoutHandled) {
                    multiplayerTimeoutHandled = true;
                    handleMultiplayerTimeout();
                }
            }
        }.start();
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
        if (isMultiplayer && !opponentAlreadyLeft && !currentUid.equals(activeUid)) {
            return;
        }

        for (int i = 0; i < currentGuess.length; i++) {
            if (currentGuess[i] == -1) {
                currentGuess[i] = symbolIndex;
                updateCurrentGuess();
                syncDraftGuess();
                return;
            }
        }
    }

    private void clearGuess() {
        for (int i = 0; i < currentGuess.length; i++) {
            currentGuess[i] = -1;
        }
        updateCurrentGuess();
        syncDraftGuess();
    }

    private void syncDraftGuess() {
        if (!isMultiplayer || skockoStateRef == null || (!opponentAlreadyLeft && !currentUid.equals(activeUid))) {
            return;
        }
        skockoStateRef.update("draftGuess", intArrayToList(currentGuess));
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
        if (isMultiplayer) {
            submitMultiplayerGuess();
            return;
        }

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
            if (isBattleMode) {
                revealSolution();
                showEndDialog(getString(R.string.skocko_challenge_no_solve_message));
                return;
            }
            startOpponentBonusTurn();
            return;
        }

        clearGuess();
    }

    private void submitMultiplayerGuess() {
        if (!gameLoaded || !isGuessComplete() || (!opponentAlreadyLeft && !currentUid.equals(activeUid))) {
            return;
        }

        int[] feedback = evaluateGuess();
        Map<String, Object> updates = new HashMap<>();
        Map<String, Long> scores = new HashMap<>(multiplayerScores);

        if ("starter_turn".equals(phase)) {
            Map<String, Object> attempt = createAttemptMap(currentUid, currentGuess, feedback);
            List<Map<String, Object>> attempts = new ArrayList<>(multiplayerAttempts);
            attempts.add(attempt);
            updates.put("attempts", attempts);

            if (isSolved(feedback)) {
                long earned = getScoreForSolvedAttempt(attempts.size());
                scores.put(currentUid, getScoreFor(currentUid) + earned);
                finishMultiplayerRound(scores, updates);
                return;
            }

            if (attempts.size() >= 6) {
                if (opponentAlreadyLeft) {
                    updates.put("scores", scores);
                    updates.put("draftGuess", emptyGuessList());
                    finishMultiplayerRound(scores, updates);
                    return;
                }
                updates.put("phase", "bonus_turn");
                updates.put("activeUid", opponentUid);
                updates.put("phaseEndsAt", System.currentTimeMillis() + 10000);
                updates.put("scores", scores);
                updates.put("draftGuess", emptyGuessList());
                skockoStateRef.update(updates);
                clearGuess();
                return;
            }

            updates.put("scores", scores);
            updates.put("draftGuess", emptyGuessList());
            skockoStateRef.update(updates);
            clearGuess();
            return;
        }

        if ("bonus_turn".equals(phase)) {
            Map<String, Object> bonusAttempt = createAttemptMap(currentUid, currentGuess, feedback);
            updates.put("bonusAttempt", bonusAttempt);
            updates.put("draftGuess", emptyGuessList());
            if (isSolved(feedback)) {
                scores.put(currentUid, getScoreFor(currentUid) + 10);
            }
            finishMultiplayerRound(scores, updates);
        }
    }

    private Map<String, Object> createAttemptMap(String uid, int[] guess, int[] feedback) {
        Map<String, Object> attempt = new HashMap<>();
        attempt.put("uid", uid);
        attempt.put("guess", intArrayToList(guess));
        attempt.put("feedback", intArrayToList(feedback));
        return attempt;
    }

    private List<Long> intArrayToList(int[] values) {
        List<Long> list = new ArrayList<>();
        for (int value : values) {
            list.add((long) value);
        }
        return list;
    }

    private List<Long> emptyGuessList() {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            list.add(-1L);
        }
        return list;
    }

    private void copyGuess(int[] source, int[] target) {
        for (int i = 0; i < target.length; i++) {
            target[i] = source[i];
        }
    }

    private void handleMultiplayerTimeout() {
        if ("starter_turn".equals(phase)) {
            Map<String, Object> updates = new HashMap<>();
            if (opponentAlreadyLeft) {
                finishMultiplayerRound(new HashMap<>(multiplayerScores), updates);
                return;
            }
            updates.put("phase", "bonus_turn");
            updates.put("activeUid", opponentUid);
            updates.put("phaseEndsAt", System.currentTimeMillis() + 10000);
            updates.put("draftGuess", emptyGuessList());
            skockoStateRef.update(updates);
            return;
        }

        if ("bonus_turn".equals(phase)) {
            finishMultiplayerRound(new HashMap<>(multiplayerScores), new HashMap<>());
            return;
        }

        if ("round_result".equals(phase)) {
            createSkockoRound(2, opponentAlreadyLeft ? currentUid : player2Uid, new HashMap<>(multiplayerScores));
            return;
        }

        if ("game_finished_pending".equals(phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("finished", true);
            updates.put("phase", "game_finished");
            updates.put("activeUid", null);
            skockoStateRef.update(updates);
        }
    }

    private void finishMultiplayerRound(Map<String, Long> scores, Map<String, Object> updates) {
        if (roundNumber == 1) {
            updates.put("scores", scores);
            updates.put("showSolution", true);
            updates.put("phase", "round_result");
            updates.put("activeUid", null);
            updates.put("phaseEndsAt", System.currentTimeMillis() + 4000);
            updates.put("draftGuess", emptyGuessList());
            skockoStateRef.update(updates);
            clearGuess();
            return;
        }

        updates.put("scores", scores);
        updates.put("showSolution", true);
        updates.put("finished", false);
        updates.put("phase", "game_finished_pending");
        updates.put("activeUid", null);
        updates.put("phaseEndsAt", System.currentTimeMillis() + 4000);
        updates.put("draftGuess", emptyGuessList());
        skockoStateRef.update(updates);
    }

    private void submitOpponentBonusGuess() {
        int[] feedback = evaluateGuess();
        bindBonusFeedback(feedback);

        for (int i = 0; i < 4; i++) {
            bonusSlots[i].setText(SYMBOLS[currentGuess[i]]);
            bonusSlots[i].setBackgroundResource(R.drawable.bg_skocko_slot_filled);
        }
        tvBonusLabel.setVisibility(View.VISIBLE);
        findViewById(R.id.rowBonusAttempt).setVisibility(View.VISIBLE);

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
                } else if (isBattleMode) {
                    revealSolution();
                    showEndDialog(getString(R.string.skocko_challenge_no_solve_message));
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

        int guessedAttempt = solved ? attemptIndex : 0;

        StatisticsRepository.saveSkockoResult(
                earnedScore,
                guessedAttempt
        );

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
        tvCurrentGuessTitle.setText("Opponent guess");
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
        if (skockoStateListener != null) {
            skockoStateListener.remove();
        }
        if (abandonListener != null) { abandonListener.remove(); abandonListener = null; }
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

    private int getMySolvedAttempt() {
        for (int i = 0; i < multiplayerAttempts.size(); i++) {
            Map<String, Object> attempt = multiplayerAttempts.get(i);

            String uid = (String) attempt.get("uid");
            if (!currentUid.equals(uid)) {
                continue;
            }

            int[] feedback = listToIntArray(attempt.get("feedback"));

            if (isSolved(feedback)) {
                return i + 1;
            }
        }

        return 0;
    }
    private void listenForAbandon() {
        abandonListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        opponentAlreadyLeft = true;
                    }
                });
    }

}