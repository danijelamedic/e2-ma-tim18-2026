package com.example.slagalica.games.StepByStep;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.AnimationUtils;
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
import java.util.List;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;

public class StepByStepActivity extends AppCompatActivity {

    private static final int MAX_STEPS            = 7;
    private static final int STEP_DURATION_MS     = 10_000;
    private static final int POINTS_FIRST_STEP    = 20;
    private static final int POINTS_LOSS_PER_STEP = 2;
    private static final int OPPONENT_BONUS_MS    = 10_000;

    private TextView tvTimer, tvCurrentStep, tvRoundInfo;
    private TextView tvPlayerName, tvPlayerScore, tvPlayerInfo;
    private TextView tvOpponentName, tvOpponentScore, tvOpponentInfo;
    private TextView[] stepTiles;
    private TextView btnConfirm;
    private EditText etAnswer;
    private CountDownTimer stepTimer;
    private int currentStep = 0;

    private boolean isMultiplayer = false;
    private String gameId;
    private String currentUid;
    private boolean isPlayer1 = false;
    private int currentRound = 1;

    private FirebaseFirestore db;
    private ListenerRegistration gameListener;
    private ListenerRegistration scoreListener;

    private String[] clues;
    private String answer;
    private String myQuestionId;

    private boolean activityAlive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

        db            = FirebaseFirestore.getInstance();
        currentUid    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        gameId        = getIntent().getStringExtra("gameId");

        tvTimer       = findViewById(R.id.tvTimer);
        tvCurrentStep = findViewById(R.id.tvCurrentStep);
        tvRoundInfo   = findViewById(R.id.tvRoundInfo);
        btnConfirm    = findViewById(R.id.btnConfirm);
        etAnswer      = findViewById(R.id.etAnswer);

        tvPlayerName    = findViewById(R.id.tvPlayerName);
        tvPlayerScore   = findViewById(R.id.tvPlayerScore);
        tvPlayerInfo    = findViewById(R.id.tvPlayerInfo);
        tvOpponentName  = findViewById(R.id.tvOpponentName);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvOpponentInfo  = findViewById(R.id.tvOpponentInfo);

        tvRoundInfo.setText(currentRound + "/2");

        stepTiles = new TextView[]{
                findViewById(R.id.tvStep1), findViewById(R.id.tvStep2),
                findViewById(R.id.tvStep3), findViewById(R.id.tvStep4),
                findViewById(R.id.tvStep5), findViewById(R.id.tvStep6),
                findViewById(R.id.tvStep7)
        };

        findViewById(R.id.layoutHeader).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bounce_in));

        findViewById(R.id.btnLeaveStepByStep).setOnClickListener(v -> {
            if (stepTimer    != null) { stepTimer.cancel();    stepTimer    = null; }
            if (gameListener != null) { gameListener.remove(); gameListener = null; }
            setResult(RESULT_CANCELED);
            finish();
        });

        setConfirmEnabled(false);
        etAnswer.setEnabled(false);
        btnConfirm.setOnClickListener(v -> handleGuess());

        if (isMultiplayer) {
            loadPlayerInfoAndSetup();
        } else {
            loadQuestionRandom(null);
        }
    }

    @Override
    protected void onDestroy() {
        activityAlive = false;
        super.onDestroy();
        if (stepTimer     != null) { stepTimer.cancel();     stepTimer     = null; }
        if (gameListener  != null) { gameListener.remove();  gameListener  = null; }
        if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }
    }

    private boolean isAlive() {
        return activityAlive && !isFinishing() && !isDestroyed();
    }

    private void setConfirmEnabled(boolean enabled) {
        btnConfirm.setClickable(enabled);
        btnConfirm.setAlpha(enabled ? 1.0f : 0.4f);
    }


    private void loadPlayerInfoAndSetup() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(myDoc -> {
                    if (!isAlive()) return;
                    if (myDoc.exists()) {
                        String name  = myDoc.getString("username");
                        Long   coins = myDoc.getLong("coins");
                        Long   stars = myDoc.getLong("stars");
                        Long   level = myDoc.getLong("level");
                        if (name != null) tvPlayerName.setText(name);
                        tvPlayerInfo.setText(
                                "🪙" + (coins != null ? coins : 0) +
                                        " ⭐" + (stars != null ? stars : 0) +
                                        " L"  + (level != null ? level : 0));
                    }
                    setupMultiplayerRound();
                });
    }

    private void loadOpponentInfo(String opponentUid) {
        db.collection("users").document(opponentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAlive()) return;
                    if (doc.exists()) {
                        String name  = doc.getString("username");
                        Long   coins = doc.getLong("coins");
                        Long   stars = doc.getLong("stars");
                        Long   level = doc.getLong("level");
                        if (name != null) tvOpponentName.setText(name);
                        tvOpponentInfo.setText(
                                "🪙" + (coins != null ? coins : 0) +
                                        " ⭐" + (stars != null ? stars : 0) +
                                        " L"  + (level != null ? level : 0));
                    }
                });
    }

    private void loadScores(String player1Uid) {
        // Live listener — scores update in real time as opponent finishes
        if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }
        scoreListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAlive() || snapshot == null) return;
                    boolean iAm1  = currentUid.equals(player1Uid);
                    Long s1 = snapshot.getLong("score1");
                    Long s2 = snapshot.getLong("score2");
                    long myScore  = iAm1 ? (s1 != null ? s1 : 0) : (s2 != null ? s2 : 0);
                    long oppScore = iAm1 ? (s2 != null ? s2 : 0) : (s1 != null ? s1 : 0);
                    tvPlayerScore .setText(myScore  + " pts");
                    tvOpponentScore.setText(oppScore + " pts");
                });
    }


    private void setupMultiplayerRound() {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAlive()) return;

                    String player1Uid = snapshot.getString("player1");
                    String player2Uid = snapshot.getString("player2");
                    isPlayer1 = currentUid.equals(player1Uid);

                    String opponentUid = isPlayer1 ? player2Uid : player1Uid;
                    if (opponentUid != null) loadOpponentInfo(opponentUid);
                    loadScores(player1Uid);

                    Long roundLong = snapshot.getLong("stepByStepRound");
                    currentRound = roundLong != null ? roundLong.intValue() : 1;
                    tvRoundInfo.setText(currentRound + "/2");

                    boolean iAmActive = (currentRound == 1 && isPlayer1)
                            || (currentRound == 2 && !isPlayer1);

                    String savedQuestionId = snapshot.getString(
                            "stepByStepQuestionId_r" + currentRound);

                    if (savedQuestionId != null) {
                        loadQuestionById(savedQuestionId, iAmActive);
                    } else if (iAmActive) {
                        String usedId = snapshot.getString(
                                "stepByStepQuestionId_r" + (currentRound == 1 ? 2 : 1));
                        loadQuestionRandom(usedId);
                    } else {
                        waitForQuestionToBeSet();
                    }
                });
    }


    private void loadQuestionRandom(String excludeId) {
        db.collection("stepByStepQuestions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAlive()) return;

                    if (querySnapshot.isEmpty()) {
                        useFallbackQuestion();
                        if (isMultiplayer) saveQuestionIdAndStart();
                        else startMyTurn();
                        return;
                    }

                    List<com.google.firebase.firestore.DocumentSnapshot> docs =
                            querySnapshot.getDocuments();

                    com.google.firebase.firestore.DocumentSnapshot chosen = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        if (!doc.getId().equals(excludeId)) { chosen = doc; break; }
                    }
                    if (chosen == null) chosen = docs.get(0);

                    myQuestionId = chosen.getId();
                    extractCluesFromDoc(chosen);

                    if (isMultiplayer) saveQuestionIdAndStart();
                    else               startMyTurn();
                });
    }

    private void loadQuestionById(String questionId, boolean iAmActive) {
        db.collection("stepByStepQuestions").document(questionId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAlive()) return;

                    if (doc.exists()) {
                        myQuestionId = doc.getId();
                        extractCluesFromDoc(doc);
                    } else {
                        useFallbackQuestion();
                    }

                    if (iAmActive) startMyTurn();
                    else           waitForOpponentToFinish();
                });
    }

    private void saveQuestionIdAndStart() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("stepByStepQuestionId_r" + currentRound, myQuestionId);
        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> { if (isAlive()) startMyTurn(); });
    }

    private void extractCluesFromDoc(com.google.firebase.firestore.DocumentSnapshot doc) {
        clues = new String[]{
                doc.getString("clue1"), doc.getString("clue2"),
                doc.getString("clue3"), doc.getString("clue4"),
                doc.getString("clue5"), doc.getString("clue6"),
                doc.getString("clue7")
        };
        answer = doc.getString("answer");
    }

    private void useFallbackQuestion() {
        clues  = new String[]{"Clue 1","Clue 2","Clue 3","Clue 4","Clue 5","Clue 6","Clue 7"};
        answer = "ANSWER";
    }


    private void waitForQuestionToBeSet() {
        if (!isAlive()) return;
        tvCurrentStep.setText("Waiting for opponent...");

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAlive() || snapshot == null) return;
                    String savedId = snapshot.getString(
                            "stepByStepQuestionId_r" + currentRound);
                    if (savedId != null) {
                        if (gameListener != null) { gameListener.remove(); gameListener = null; }
                        loadQuestionById(savedId, false);
                    }
                });
    }

    private void waitForOpponentToFinish() {
        if (!isAlive()) return;
        setConfirmEnabled(false);
        etAnswer.setEnabled(false);

        String doneStatus = "r" + currentRound + "done";
        String stepField  = "stepByStepCurrentStep_r" + currentRound;

        if (clues != null) revealClue(0);

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAlive() || snapshot == null) return;

                    Long stepLong = snapshot.getLong(stepField);
                    if (stepLong != null) {
                        int step = stepLong.intValue();
                        if (step > currentStep) {
                            currentStep = step;
                            revealClue(currentStep);
                        }
                    }

                    String status = snapshot.getString("stepByStepStatus");
                    if (doneStatus.equals(status)) {
                        if (gameListener != null) { gameListener.remove(); gameListener = null; }
                        Boolean opponentGuessed =
                                snapshot.getBoolean("stepByStepR" + currentRound + "Guessed");
                        if (Boolean.FALSE.equals(opponentGuessed)) startBonusChance();
                        else                                        finishAndReturn(0);
                    }
                });
    }

    private void startBonusChance() {
        if (!isAlive()) return;
        tvCurrentStep.setText("Opponent didn't guess! You have 10 seconds for a bonus!");
        setConfirmEnabled(true);
        etAnswer.setEnabled(true);
        for (int i = 0; i < MAX_STEPS; i++) revealClue(i);

        stepTimer = new CountDownTimer(OPPONENT_BONUS_MS, 1000) {
            @Override public void onTick(long ms) {
                if (!isAlive()) { cancel(); return; }
                tvTimer.setText("Bonus: " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                if (!isAlive()) return;
                setConfirmEnabled(false);
                etAnswer.setEnabled(false);
                showResultDialog("Bonus time's up!", "You earned 0 points.", 0);
            }
        }.start();
    }


    private void startMyTurn() {
        if (!isAlive()) return;
        setConfirmEnabled(true);
        etAnswer.setEnabled(true);
        currentStep = 0;
        startStep();
    }

    private void startStep() {
        if (!isAlive()) return;
        if (currentStep >= MAX_STEPS) { endRoundNoGuess(); return; }

        revealClue(currentStep);
        broadcastCurrentStep(currentStep);

        stepTimer = new CountDownTimer(STEP_DURATION_MS, 1000) {
            @Override public void onTick(long ms) {
                if (!isAlive()) { cancel(); return; }
                tvTimer.setText("⏱ " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                if (!isAlive()) return;
                currentStep++;
                startStep();
            }
        }.start();
    }

    private void broadcastCurrentStep(int step) {
        if (!isMultiplayer) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("stepByStepCurrentStep_r" + currentRound, step);
        db.collection("games").document(gameId).update(updates);
    }

    private void revealClue(int stepIndex) {
        if (!isAlive()) return;
        if (stepIndex >= MAX_STEPS || clues == null) return;

        tvCurrentStep.setText(clues[stepIndex]);
        tvCurrentStep.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        for (int i = 0; i <= stepIndex; i++) {
            stepTiles[i].setBackgroundResource(R.drawable.bg_tile_active);
            stepTiles[i].setTextColor(
                    ContextCompat.getColor(this, android.R.color.white));
        }
    }


    private void handleGuess() {
        if (!isAlive()) return;
        String guess = etAnswer.getText().toString().trim();
        if (guess.isEmpty()) {
            Toast.makeText(this, "Enter your answer!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (guess.equalsIgnoreCase(answer)) {
            if (stepTimer != null) { stepTimer.cancel(); stepTimer = null; }
            int points = calculatePoints(currentStep);
            setConfirmEnabled(false);
            etAnswer.setEnabled(false);
            showResultDialog("Correct! 🎉", "You earned " + points + " points.", points);
        } else {
            Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show();
            etAnswer.setText("");
        }
    }

    private int calculatePoints(int stepIndex) {
        return Math.max(0, POINTS_FIRST_STEP - stepIndex * POINTS_LOSS_PER_STEP);
    }

    private void endRoundNoGuess() {
        if (!isAlive()) return;
        if (stepTimer != null) { stepTimer.cancel(); stepTimer = null; }
        setConfirmEnabled(false);
        etAnswer.setEnabled(false);
        showResultDialog("Time's up!", "You earned 0 points.", 0);
    }


    private void showResultDialog(String title, String message, int points) {
        if (!isAlive()) {
            onActivePlayerFinished(points, points > 0);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    onActivePlayerFinished(points, points > 0);
                })
                .show();
    }


    private void onActivePlayerFinished(int points, boolean guessed) {
        if (!isMultiplayer) { finishAndReturn(points); return; }

        String doneStatus   = "r" + currentRound + "done";
        String guessedField = "stepByStepR" + currentRound + "Guessed";

        Map<String, Object> updates = new HashMap<>();
        updates.put("stepByStepStatus", doneStatus);
        updates.put(guessedField, guessed);

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> finishAndReturn(points));
    }

    private void finishAndReturn(int points) {
        if (stepTimer    != null) { stepTimer.cancel();    stepTimer    = null; }
        if (gameListener != null) { gameListener.remove(); gameListener = null; }

        int guessedStep = points > 0 ? currentStep + 1 : 0;

        StatisticsRepository.saveStepByStepResult(
                points,
                guessedStep
        );

        Intent result = new Intent();
        result.putExtra("points", points);
        result.putExtra("stepByStepRound", currentRound);
        setResult(RESULT_OK, result);
        finish();
    }
}