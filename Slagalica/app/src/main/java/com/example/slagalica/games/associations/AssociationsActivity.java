package com.example.slagalica.games.associations;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.slagalica.data.AssociationRepository;
import com.example.slagalica.data.PlayerProfileLoader;
import com.example.slagalica.models.AssociationGame;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;

public class AssociationsActivity extends AppCompatActivity {

    private static final String[] COLUMN_KEYS = {"A", "B", "C", "D"};

    private final TextView[][] clueViews = new TextView[4][4];
    private final TextView[] solutionViews = new TextView[4];
    private final boolean[][] openedClues = new boolean[4][4];
    private final boolean[] solvedColumns = new boolean[4];
    private final String[][] columnData = new String[4][5];

    private TextView tvTimer;
    private TextView tvPlayerScore;
    private TextView tvOpponentScore;
    private TextView tvPlayerName;
    private TextView tvOpponentName;
    private ImageView tvPlayerAvatar;
    private ImageView tvOpponentAvatar;
    private TextView tvPlayerInfo;
    private TextView tvOpponentInfo;
    private TextView tvPoints;
    private TextView tvTurn;
    private TextView tvFinalSolution;
    private Button btnPass;
    private CountDownTimer timer;
    private String finalSolution = "";
    private long timeLeftMillis = 120000;
    private int playerScore = 0;
    private int openedFieldsCount = 0;
    private boolean finalSolved = false;
    private boolean gameLoaded = false;
    private boolean isBattleMode;
    private boolean isMultiplayer;
    private boolean multiplayerResultSent = false;
    private boolean multiplayerTimeoutHandled = false;
    private boolean multiplayerPhaseAdvanceStarted = false;
    private FirebaseFirestore db;
    private DocumentReference associationsStateRef;
    private ListenerRegistration associationsStateListener;
    private String gameId;
    private String currentUid;
    private String player1Uid;
    private String player2Uid;
    private String opponentUid;
    private String activeUid;
    private int roundNumber = 1;
    private boolean canOpenClue = true;
    private boolean statisticsSaved = false;
    private Map<String, Long> multiplayerScores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_associations);

        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
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

        bindViews();
        setupLeaveButton();
        setupInfoButton();
        setupPassButton();
        setupBoard();
        setupFinalButton();
        setBoardEnabled(false);
        tvTurn.setText("Loading association...");
        if (isMultiplayer && gameId != null && currentUid != null) {
            loadMultiplayerContext();
        } else {
            loadAssociationGame();
        }
    }

    private void bindViews() {
        tvTimer = findViewById(R.id.tvAssociationsTimer);
        tvPlayerScore = findViewById(R.id.tvAssociationsPlayerScore);
        tvOpponentScore = findViewById(R.id.tvAssociationsOpponentScore);
        tvPlayerName = findViewById(R.id.tvAssociationsPlayerName);
        tvOpponentName = findViewById(R.id.tvAssociationsOpponentName);
        tvPlayerAvatar = findViewById(R.id.tvAssociationsPlayerAvatar);
        tvOpponentAvatar = findViewById(R.id.tvAssociationsOpponentAvatar);
        tvPlayerInfo = findViewById(R.id.tvAssociationsPlayerInfo);
        tvOpponentInfo = findViewById(R.id.tvAssociationsOpponentInfo);
        tvPoints = findViewById(R.id.tvAssociationsPoints);
        tvTurn = findViewById(R.id.tvAssociationsTurn);
        tvFinalSolution = findViewById(R.id.tvFinalSolution);
        btnPass = findViewById(R.id.btnPassAssociations);
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
                    associationsStateRef = db.collection("games")
                            .document(gameId)
                            .collection("gameStates")
                            .document("associations");

                    loadPlayerPanels();
                    listenForAssociationsState();
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
            tvPlayerAvatar.setImageResource(summary.avatarResId);
        });

        PlayerProfileLoader.load(opponentUid, summary -> {
            tvOpponentName.setText(summary.username);
            tvOpponentInfo.setText(summary.info);
            tvOpponentAvatar.setImageResource(summary.avatarResId);
        });
    }

    private void setupPassButton() {
        btnPass.setOnClickListener(v -> {
            if (isMultiplayer && currentUid != null && currentUid.equals(activeUid) && !canOpenClue) {
                passMultiplayerTurn();
            }
        });
    }

    private void listenForAssociationsState() {
        associationsStateListener = associationsStateRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                if (currentUid.equals(player1Uid)) {
                    createAssociationRound(1, player1Uid, null);
                } else {
                    tvTurn.setText("Waiting for game...");
                }
                return;
            }

            applyMultiplayerState(snapshot);
        });
    }

    private void createAssociationRound(int nextRound, String starterUid, Map<String, Long> existingScores) {
        new AssociationRepository().loadRandomAssociation(new AssociationRepository.AssociationCallback() {
            @Override
            public void onSuccess(AssociationGame game) {
                Map<String, Long> scores = existingScores != null ? existingScores : new HashMap<>();
                if (!scores.containsKey(player1Uid)) scores.put(player1Uid, 0L);
                if (!scores.containsKey(player2Uid)) scores.put(player2Uid, 0L);

                Map<String, Object> data = new HashMap<>();
                data.put("roundNumber", (long) nextRound);
                data.put("starterUid", starterUid);
                data.put("activeUid", starterUid);
                data.put("phase", "playing");
                data.put("columnA", game.getColumnA());
                data.put("columnB", game.getColumnB());
                data.put("columnC", game.getColumnC());
                data.put("columnD", game.getColumnD());
                data.put("columnSolutions", game.getColumnSolutions());
                data.put("finalSolution", game.getFinalSolution());
                data.put("openedFields", new ArrayList<>());
                data.put("solvedColumns", new ArrayList<>());
                data.put("scores", scores);
                data.put("finished", false);
                data.put("canOpenClue", true);
                data.put("phaseEndsAt", System.currentTimeMillis() + 120000);
                associationsStateRef.set(data);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(AssociationsActivity.this,
                        "Failed to load association game.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void applyMultiplayerState(DocumentSnapshot snapshot) {
        Long round = snapshot.getLong("roundNumber");
        roundNumber = round != null ? round.intValue() : 1;
        activeUid = snapshot.getString("activeUid");
        String phase = snapshot.getString("phase");
        Boolean finished = snapshot.getBoolean("finished");
        canOpenClue = !Boolean.FALSE.equals(snapshot.getBoolean("canOpenClue"));

        AssociationGame game = new AssociationGame(
                (List<String>) snapshot.get("columnA"),
                (List<String>) snapshot.get("columnB"),
                (List<String>) snapshot.get("columnC"),
                (List<String>) snapshot.get("columnD"),
                (List<String>) snapshot.get("columnSolutions"),
                snapshot.getString("finalSolution")
        );
        bindAssociationGame(game);
        gameLoaded = true;

        Map<String, Object> rawScores = (Map<String, Object>) snapshot.get("scores");
        multiplayerScores = toLongScoreMap(rawScores);
        playerScore = (int) getScoreFor(currentUid);
        tvPlayerScore.setText(getString(R.string.associations_score_format, playerScore));
        tvOpponentScore.setText(getString(R.string.associations_score_format, (int) getScoreFor(opponentUid)));

        List<String> opened = (List<String>) snapshot.get("openedFields");
        List<Long> solved = (List<Long>) snapshot.get("solvedColumns");
        renderMultiplayerBoard(opened != null ? opened : new ArrayList<>(),
                solved != null ? solved : new ArrayList<>());

        if ("round_result".equals(phase) || "game_finished_pending".equals(phase)) {
            revealMultiplayerBoard();
            tvTurn.setText("Round finished");
            updateActiveState();
            setMultiplayerControls(false);
            btnPass.setVisibility(View.GONE);
            Long phaseEndsAt = snapshot.getLong("phaseEndsAt");
            startRoundResultTimer(phaseEndsAt != null ? phaseEndsAt : 0L, phase);
            return;
        }

        multiplayerPhaseAdvanceStarted = false;
        tvTurn.setText(currentUid.equals(activeUid) ? "Your turn" : "Opponent turn");
        updateActiveState();
        setMultiplayerControls("playing".equals(phase)
                && currentUid.equals(activeUid)
                && !Boolean.TRUE.equals(finished));

        Long phaseEndsAt = snapshot.getLong("phaseEndsAt");
        startMultiplayerTimer(phaseEndsAt != null ? phaseEndsAt : 0L);

        if (Boolean.TRUE.equals(finished) && !multiplayerResultSent) {
            multiplayerResultSent = true;

            if (!statisticsSaved) {
                statisticsSaved = true;

                int myScore = (int) getScoreFor(currentUid);
                boolean solvedNumber = myScore > 0;

                StatisticsRepository.saveAssociationsResult(myScore, solvedNumber);
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
                if (entry.getValue() instanceof Number) {
                    scores.put(entry.getKey(), ((Number) entry.getValue()).longValue());
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

    private void renderMultiplayerBoard(List<String> opened, List<Long> solved) {
        clearRenderedBoard();
        openedFieldsCount = 0;
        for (String key : opened) {
            String[] parts = key.split("_");
            if (parts.length != 2) continue;
            int column = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            openedClues[column][row] = true;
            openedFieldsCount++;
            clueViews[column][row].setText(columnData[column][row]);
            clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
        }

        for (Long solvedColumn : solved) {
            int column = solvedColumn.intValue();
            solvedColumns[column] = true;
            for (int row = 0; row < 4; row++) {
                openedClues[column][row] = true;
                clueViews[column][row].setText(columnData[column][row]);
                clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
            }
            solutionViews[column].setText(columnData[column][4]);
            solutionViews[column].setBackgroundResource(R.drawable.bg_associations_solved);
        }
        updateOpenedFieldsStatus();
    }

    private void revealMultiplayerBoard() {
        openedFieldsCount = 0;
        for (int column = 0; column < 4; column++) {
            solvedColumns[column] = true;
            for (int row = 0; row < 4; row++) {
                openedClues[column][row] = true;
                openedFieldsCount++;
                clueViews[column][row].setText(columnData[column][row]);
                clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
            }
            solutionViews[column].setText(columnData[column][4]);
            solutionViews[column].setBackgroundResource(R.drawable.bg_associations_solved);
        }

        finalSolved = true;
        tvFinalSolution.setText(finalSolution);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_solved);
        updateOpenedFieldsStatus();
    }

    private void clearRenderedBoard() {
        openedFieldsCount = 0;
        finalSolved = false;
        for (int column = 0; column < 4; column++) {
            solvedColumns[column] = false;
            for (int row = 0; row < 4; row++) {
                openedClues[column][row] = false;
                clueViews[column][row].setText(COLUMN_KEYS[column] + (row + 1));
                clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_closed);
            }
            solutionViews[column].setText(COLUMN_KEYS[column]);
            solutionViews[column].setBackgroundResource(R.drawable.bg_associations_closed);
        }
        tvFinalSolution.setText(R.string.associations_final_placeholder);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_closed);
    }

    private void updateActiveState() {
        boolean myTurn = currentUid != null && currentUid.equals(activeUid);
        boolean opponentTurn = opponentUid != null && opponentUid.equals(activeUid);

        tvPlayerAvatar.setBackgroundResource(myTurn
                ? R.drawable.bg_active_player_avatar
                : R.drawable.bg_inactive_player_avatar);
        tvOpponentAvatar.setBackgroundResource(opponentTurn
                ? R.drawable.bg_active_player_avatar
                : R.drawable.bg_inactive_player_avatar);
    }

    private void setMultiplayerControls(boolean myTurn) {
        boolean canPass = isMultiplayer && myTurn && !canOpenClue;
        btnPass.setVisibility(canPass ? View.VISIBLE : View.GONE);
        btnPass.setEnabled(canPass);

        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                clueViews[column][row].setEnabled(myTurn
                        && canOpenClue
                        && !openedClues[column][row]
                        && !solvedColumns[column]);
            }

            solutionViews[column].setEnabled(myTurn && !solvedColumns[column]);
        }

        tvFinalSolution.setEnabled(myTurn);
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
                int totalSeconds = (int) (millisUntilFinished / 1000);
                tvTimer.setText(getString(R.string.associations_timer_format, totalSeconds / 60, totalSeconds % 60));
            }

            @Override
            public void onFinish() {
                tvTimer.setText(getString(R.string.associations_timer_format, 0, 0));
                if (currentUid.equals(activeUid) && !multiplayerTimeoutHandled) {
                    multiplayerTimeoutHandled = true;
                    finishMultiplayerRound(new HashMap<>(multiplayerScores), new HashMap<>());
                }
            }
        }.start();
    }

    private void startRoundResultTimer(long phaseEndsAt, String phase) {
        if (timer != null) {
            timer.cancel();
        }

        long remaining = Math.max(0, phaseEndsAt - System.currentTimeMillis());
        timer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int totalSeconds = (int) (millisUntilFinished / 1000);
                tvTimer.setText(getString(R.string.associations_timer_format, 0, totalSeconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText(getString(R.string.associations_timer_format, 0, 0));
                if (currentUid.equals(player1Uid) && !multiplayerPhaseAdvanceStarted) {
                    multiplayerPhaseAdvanceStarted = true;
                    advanceAfterResultPhase(phase);
                }
            }
        }.start();
    }

    private void advanceAfterResultPhase(String phase) {
        associationsStateRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot == null || !snapshot.exists()) {
                return;
            }

            String currentPhase = snapshot.getString("phase");
            if (!phase.equals(currentPhase)) {
                return;
            }

            if ("game_finished_pending".equals(phase)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("finished", true);
                updates.put("phase", "game_finished");
                updates.put("activeUid", null);
                associationsStateRef.update(updates);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rawScores = (Map<String, Object>) snapshot.get("scores");
            Map<String, Long> scores = toLongScoreMap(rawScores);
            createAssociationRound(2, player2Uid, scores);
        });
    }

    private void loadAssociationGame() {
        new AssociationRepository().loadRandomAssociation(new AssociationRepository.AssociationCallback() {
            @Override
            public void onSuccess(AssociationGame game) {
                bindAssociationGame(game);
                gameLoaded = true;
                setBoardEnabled(true);
                tvTurn.setText(R.string.associations_turn_short);
                startTimer();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(AssociationsActivity.this,
                        "Failed to load association game.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void bindAssociationGame(AssociationGame game) {
        String[][] loadedColumns = {
                game.getColumnA().toArray(new String[0]),
                game.getColumnB().toArray(new String[0]),
                game.getColumnC().toArray(new String[0]),
                game.getColumnD().toArray(new String[0])
        };

        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                columnData[column][row] = loadedColumns[column][row];
            }
            columnData[column][4] = game.getColumnSolutions().get(column);
        }

        finalSolution = game.getFinalSolution();
    }

    private void setBoardEnabled(boolean enabled) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                if (clueViews[column][row] != null) {
                    clueViews[column][row].setEnabled(enabled);
                }
            }

            if (solutionViews[column] != null) {
                solutionViews[column].setEnabled(enabled);
            }
        }

        if (tvFinalSolution != null) {
            tvFinalSolution.setEnabled(enabled);
        }
    }

    private void revealClue(int column, int row) {
        if (isMultiplayer) {
            revealMultiplayerClue(column, row);
            return;
        }

        if (!gameLoaded || finalSolved || solvedColumns[column] || openedClues[column][row]) {
            return;
        }

        openedClues[column][row] = true;
        openedFieldsCount++;
        clueViews[column][row].setText(columnData[column][row]);
        clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
        updateOpenedFieldsStatus();
        tvTurn.setText(getString(R.string.associations_turn_opened, COLUMN_KEYS[column], row + 1));
    }

    private void promptColumnGuess(int column) {
        if (isMultiplayer && !currentUid.equals(activeUid)) {
            return;
        }

        if (!gameLoaded || finalSolved || solvedColumns[column]) {
            return;
        }

        showGuessDialog(getString(R.string.associations_column_guess_title, COLUMN_KEYS[column]), guess -> {
            if (normalize(guess).equals(normalize(columnData[column][4]))) {
                if (isMultiplayer) {
                    solveMultiplayerColumn(column);
                } else {
                    solveColumn(column, true);
                }
            } else {
                if (isMultiplayer) {
                    passMultiplayerTurn();
                } else {
                    tvTurn.setText(R.string.associations_turn_wrong);
                }
            }
        });
    }

    private void promptFinalGuess() {
        if (isMultiplayer && !currentUid.equals(activeUid)) {
            return;
        }

        if (!gameLoaded || finalSolved) {
            return;
        }

        showGuessDialog(getString(R.string.associations_final_guess_title), guess -> {
            if (normalize(guess).equals(normalize(finalSolution))) {
                if (isMultiplayer) {
                    solveMultiplayerFinal();
                } else {
                    solveFinal();
                }
            } else {
                if (isMultiplayer) {
                    passMultiplayerTurn();
                } else {
                    tvTurn.setText(R.string.associations_turn_wrong);
                }
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
                    if (!isMultiplayer && !finalSolved && timeLeftMillis > 0) {
                        startTimer();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    if (!isMultiplayer && !finalSolved && timeLeftMillis > 0) {
                        startTimer();
                    }
                })
                .show();
    }

    @SuppressWarnings("unchecked")
    private void revealMultiplayerClue(int column, int row) {
        if (!gameLoaded || !currentUid.equals(activeUid) || !canOpenClue
                || solvedColumns[column] || openedClues[column][row]) {
            return;
        }

        associationsStateRef.get().addOnSuccessListener(snapshot -> {
            Boolean canOpen = snapshot.getBoolean("canOpenClue");
            if (Boolean.FALSE.equals(canOpen)) {
                return;
            }

            List<String> opened = (List<String>) snapshot.get("openedFields");
            if (opened == null) opened = new ArrayList<>();
            String key = column + "_" + row;
            if (!opened.contains(key)) {
                opened.add(key);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("openedFields", opened);
            updates.put("canOpenClue", false);
            associationsStateRef.update(updates)
                    .addOnSuccessListener(unused -> showPostRevealDialog());
        });
    }

    private void showPostRevealDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Field opened")
                .setMessage("Guess a column/final solution or pass the turn.")
                .setPositiveButton("PASS", (dialog, which) -> passMultiplayerTurn())
                .setNegativeButton("KEEP GUESSING", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @SuppressWarnings("unchecked")
    private void solveMultiplayerColumn(int column) {
        associationsStateRef.get().addOnSuccessListener(snapshot -> {
            List<String> opened = (List<String>) snapshot.get("openedFields");
            if (opened == null) opened = new ArrayList<>();
            List<Long> solved = (List<Long>) snapshot.get("solvedColumns");
            if (solved == null) solved = new ArrayList<>();
            if (solved.contains((long) column)) {
                return;
            }

            int unopened = 0;
            for (int row = 0; row < 4; row++) {
                String key = column + "_" + row;
                if (!opened.contains(key)) {
                    unopened++;
                    opened.add(key);
                }
            }

            solved.add((long) column);
            Map<String, Long> scores = toLongScoreMap((Map<String, Object>) snapshot.get("scores"));
            scores.put(currentUid, getScoreFrom(scores, currentUid) + 2 + unopened);

            Map<String, Object> updates = new HashMap<>();
            updates.put("openedFields", opened);
            updates.put("solvedColumns", solved);
            updates.put("scores", scores);
            updates.put("canOpenClue", false);
            associationsStateRef.update(updates);
        });
    }

    @SuppressWarnings("unchecked")
    private void solveMultiplayerFinal() {
        associationsStateRef.get().addOnSuccessListener(snapshot -> {
            List<String> opened = (List<String>) snapshot.get("openedFields");
            if (opened == null) opened = new ArrayList<>();
            List<Long> solved = (List<Long>) snapshot.get("solvedColumns");
            if (solved == null) solved = new ArrayList<>();

            int finalBonus = 7;
            for (int column = 0; column < 4; column++) {
                int openedCount = 0;
                for (int row = 0; row < 4; row++) {
                    if (opened.contains(column + "_" + row)) {
                        openedCount++;
                    }
                }
                if (!solved.contains((long) column)) {
                    if (openedCount == 0) {
                        finalBonus += 6;
                    } else {
                        finalBonus += 2 + (4 - openedCount);
                    }
                }

                if (!solved.contains((long) column)) {
                    solved.add((long) column);
                }
                for (int row = 0; row < 4; row++) {
                    String key = column + "_" + row;
                    if (!opened.contains(key)) {
                        opened.add(key);
                    }
                }
            }

            Map<String, Long> scores = toLongScoreMap((Map<String, Object>) snapshot.get("scores"));
            scores.put(currentUid, getScoreFrom(scores, currentUid) + finalBonus);

            Map<String, Object> updates = new HashMap<>();
            updates.put("openedFields", opened);
            updates.put("solvedColumns", solved);
            updates.put("scores", scores);
            finishMultiplayerRound(scores, updates);
        });
    }

    private long getScoreFrom(Map<String, Long> scores, String uid) {
        Long value = scores.get(uid);
        return value != null ? value : 0L;
    }

    private void passMultiplayerTurn() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("activeUid", opponentUid);
        updates.put("canOpenClue", true);
        associationsStateRef.update(updates);
    }

    private void finishMultiplayerRound(Map<String, Long> scores, Map<String, Object> updates) {
        if (roundNumber == 1) {
            updates.put("scores", scores);
            updates.put("phase", "round_result");
            updates.put("activeUid", null);
            updates.put("canOpenClue", false);
            updates.put("phaseEndsAt", System.currentTimeMillis() + 5000);
            associationsStateRef.update(updates);
            return;
        }

        updates.put("scores", scores);
        updates.put("finished", false);
        updates.put("phase", "game_finished_pending");
        updates.put("activeUid", null);
        updates.put("canOpenClue", false);
        updates.put("phaseEndsAt", System.currentTimeMillis() + 5000);
        associationsStateRef.update(updates);
    }

    private void solveColumn(int column, boolean addPoints) {
        int earned = 2 + countUnopenedFields(column);
        solvedColumns[column] = true;

        for (int row = 0; row < 4; row++) {
            if (!openedClues[column][row]) {
                openedClues[column][row] = true;
                openedFieldsCount++;
            }
            clueViews[column][row].setText(columnData[column][row]);
            clueViews[column][row].setBackgroundResource(R.drawable.bg_associations_opened);
            clueViews[column][row].setEnabled(false);
        }

        solutionViews[column].setText(columnData[column][4]);
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
        int bonus = calculateFinalBonus();
        finalSolved = true;

        for (int column = 0; column < 4; column++) {
            if (!solvedColumns[column]) {
                solveColumn(column, false);
            }
        }

        playerScore += bonus;
        updatePlayerScore();
        tvFinalSolution.setText(finalSolution);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_solved);
        tvFinalSolution.setEnabled(false);
        tvTurn.setText(R.string.associations_final_solved_status);
        showEndDialog(getString(R.string.associations_end_message, playerScore, bonus));
    }

    private int calculateFinalBonus() {
        int bonus = 7;
        for (int column = 0; column < 4; column++) {
            if (solvedColumns[column]) {
                continue;
            }

            int opened = countOpenedClues(column);
            if (opened == 0) {
                bonus += 6;
            } else {
                bonus += 2 + (4 - opened);
            }
        }
        return bonus;
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
        tvFinalSolution.setText(finalSolution);
        tvFinalSolution.setBackgroundResource(R.drawable.bg_associations_opened);
        tvFinalSolution.setEnabled(false);
    }

    private void showEndDialog(String message) {
        pauseTimer();

        StatisticsRepository.saveAssociationsResult(
                playerScore,
                finalSolved
        );

        new AlertDialog.Builder(this)
                .setTitle(R.string.associations_end_title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                    if (isBattleMode) {

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("score", playerScore);
                        resultIntent.putExtra("points", playerScore);

                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } else {

                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    }
                })
                .show();
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private interface GuessHandler {
        void onGuess(String guess);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        if (associationsStateListener != null) {
            associationsStateListener.remove();
        }
        super.onDestroy();
    }
}
