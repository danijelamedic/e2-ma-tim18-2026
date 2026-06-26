package com.example.slagalica.games.StepByStep;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;
import android.widget.ImageView;

public class StepByStepActivity extends AppCompatActivity {

    private static final int MAX_STEPS            = 7;
    private static final int STEP_DURATION_MS     = 10_000;
    private static final int POINTS_FIRST_STEP    = 20;
    private static final int POINTS_LOSS_PER_STEP = 2;
    private static final int OPPONENT_BONUS_MS    = 10_000;

    // Premium palette
    private static final int GOLD_TOP   = 0xFFF7D667;
    private static final int GOLD_BOT   = 0xFFD9A33A;
    private static final int GOLD_TEXT  = 0xFF3A2A00;
    private static final int ACT_TOP    = 0xFF7C3AED;
    private static final int ACT_BOT    = 0xFF5B21B6;
    private static final int REV_TOP    = 0xFF4B2E8F;
    private static final int REV_BOT    = 0xFF38226E;
    private static final int LOCK_TOP   = 0xFF2C2150;
    private static final int LOCK_BOT   = 0xFF241B43;
    private static final int REV_BADGE_TOP = 0xFF8B5CF6;
    private static final int REV_BADGE_BOT = 0xFF6D28D9;
    private static final int LOCK_BADGE_TOP = 0xFF4A3D78;
    private static final int LOCK_BADGE_BOT = 0xFF3A2F62;

    private TextView tvTimer, tvRoundInfo;
    private TextView tvPlayerName, tvPlayerScore, tvPlayerInfo;
    private ImageView imgYourAvatar, imgOpponentAvatar;

    private TextView tvOpponentName, tvOpponentScore, tvOpponentInfo;
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
    private LinearLayout llCluesList;
    private ScrollView scrollClues;

    private LinearLayout[] slotCard;
    private TextView[] slotBadge;
    private TextView[] slotText;
    private int cluesShown = 0;
    private int activeIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

        db            = FirebaseFirestore.getInstance();
        currentUid    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        gameId        = getIntent().getStringExtra("gameId");

        tvTimer       = findViewById(R.id.tvTimer);
        llCluesList   = findViewById(R.id.llCluesList);
        scrollClues   = findViewById(R.id.scrollClues);
        tvRoundInfo   = findViewById(R.id.tvRoundInfo);
        btnConfirm    = findViewById(R.id.btnConfirm);
        etAnswer      = findViewById(R.id.etAnswer);

        tvPlayerName    = findViewById(R.id.tvPlayerName);
        tvPlayerScore   = findViewById(R.id.tvPlayerScore);
        tvPlayerInfo    = findViewById(R.id.tvPlayerInfo);
        tvOpponentName  = findViewById(R.id.tvOpponentName);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvOpponentInfo  = findViewById(R.id.tvOpponentInfo);
        imgYourAvatar     = findViewById(R.id.tvYourAvatar);
        imgOpponentAvatar = findViewById(R.id.tvOpponentAvatar);

        tvRoundInfo.setText(currentRound + "/2");

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
                        imgYourAvatar.setImageResource(
                                com.example.slagalica.data.PlayerProfileLoader.getAvatarResource(myDoc.getString("avatar")));
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
                        imgOpponentAvatar.setImageResource(
                                com.example.slagalica.data.PlayerProfileLoader.getAvatarResource(doc.getString("avatar")));
                        tvOpponentInfo.setText(
                                "🪙" + (coins != null ? coins : 0) +
                                        " ⭐" + (stars != null ? stars : 0) +
                                        " L"  + (level != null ? level : 0));
                    }
                });
    }

    private void loadScores(String player1Uid) {
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
        showStatusCard("⏳  Waiting for opponent to set the question…");
        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAlive() || snapshot == null) return;

                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        if (gameListener != null) { gameListener.remove(); gameListener = null; }
                        finishAndReturn(0);
                        return;
                    }

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

        buildBoard();
        revealClue(0);

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAlive() || snapshot == null) return;

                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        if (gameListener != null) { gameListener.remove(); gameListener = null; }
                        finishAndReturn(0);
                        return;
                    }

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
        Toast.makeText(this,
                "Opponent didn't guess! You have 10 seconds for a bonus!",
                Toast.LENGTH_LONG).show();
        setConfirmEnabled(true);
        etAnswer.setEnabled(true);
        revealClue(MAX_STEPS - 1);

        stepTimer = new CountDownTimer(OPPONENT_BONUS_MS, 1000) {
            @Override public void onTick(long ms) {
                if (!isAlive()) { cancel(); return; }
                tvTimer.setText("🏆 " + (ms / 1000) + "s");
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
        buildBoard();
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


    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bg(int radius, int top, int bottom) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        g.setCornerRadius(radius);
        return g;
    }

    private GradientDrawable circle(int top, int bottom) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        g.setShape(GradientDrawable.OVAL);
        return g;
    }

    private void buildBoard() {
        if (llCluesList == null) return;
        llCluesList.removeAllViews();
        cluesShown = 0;
        activeIndex = -1;
        slotCard  = new LinearLayout[MAX_STEPS];
        slotBadge = new TextView[MAX_STEPS];
        slotText  = new TextView[MAX_STEPS];

        for (int i = 0; i < MAX_STEPS; i++) {
            LinearLayout card = new LinearLayout(this);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            cp.setMargins(0, 0, 0, dp(7));
            card.setLayoutParams(cp);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(8), dp(16), dp(8));
            card.setMinimumHeight(dp(50));

            TextView badge = new TextView(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(38), dp(38));
            bp.setMargins(0, 0, dp(14), 0);
            badge.setLayoutParams(bp);
            badge.setGravity(Gravity.CENTER);
            badge.setText(String.valueOf(i + 1));
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextSize(16);

            TextView text = new TextView(this);
            text.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            text.setTypeface(null, Typeface.BOLD);
            text.setLetterSpacing(0.02f);
            text.setMaxLines(2);

            card.addView(badge);
            card.addView(text);
            llCluesList.addView(card);

            slotCard[i] = card; slotBadge[i] = badge; slotText[i] = text;
            styleLocked(i);
        }
    }

    private void styleLocked(int i) {
        slotCard[i].setBackground(bg(dp(16), LOCK_TOP, LOCK_BOT));
        slotCard[i].setElevation(dp(2));
        slotBadge[i].setBackground(circle(LOCK_BADGE_TOP, LOCK_BADGE_BOT));
        slotBadge[i].setTextColor(0xFF9D8FC9);
        slotText[i].setText("• • • • •");
        slotText[i].setTextColor(0xFF6B5E99);
        slotText[i].setTextSize(15);
    }

    private void styleRevealed(int i) {
        slotCard[i].setBackground(bg(dp(16), REV_TOP, REV_BOT));
        slotCard[i].setElevation(dp(4));
        slotBadge[i].setBackground(circle(REV_BADGE_TOP, REV_BADGE_BOT));
        slotBadge[i].setTextColor(0xFFFFFFFF);
        slotText[i].setText(clues != null && clues[i] != null ? clues[i] : "");
        slotText[i].setTextColor(0xFFE9E2FF);
        slotText[i].setTextSize(16);
    }

    private void styleActive(int i) {
        GradientDrawable g = bg(dp(16), ACT_TOP, ACT_BOT);
        g.setStroke(dp(2), GOLD_TOP);
        slotCard[i].setBackground(g);
        slotCard[i].setElevation(dp(12));
        slotBadge[i].setBackground(circle(GOLD_TOP, GOLD_BOT));
        slotBadge[i].setTextColor(GOLD_TEXT);
        slotText[i].setText(clues != null && clues[i] != null ? clues[i] : "");
        slotText[i].setTextColor(0xFFFFFFFF);
        slotText[i].setTextSize(19);
        slotCard[i].startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_in));
    }

    private void revealClue(int target) {
        if (!isAlive() || slotCard == null) return;
        for (int i = cluesShown; i <= target && i < MAX_STEPS; i++) {
            if (activeIndex >= 0) styleRevealed(activeIndex);
            styleActive(i);
            activeIndex = i;
            cluesShown = i + 1;
        }
        final int focus = activeIndex;
        scrollClues.post(() -> {
            if (focus >= 0 && slotCard != null && slotCard[focus] != null)
                scrollClues.smoothScrollTo(0, slotCard[focus].getTop());
        });
    }

    private void showStatusCard(String msg) {
        if (llCluesList == null) return;
        llCluesList.removeAllViews();
        TextView t = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(20), 0, 0);
        t.setLayoutParams(lp);
        t.setText(msg);
        t.setTextColor(0xFF8B7FB8);
        t.setTextSize(15);
        t.setPadding(dp(24), dp(28), dp(24), dp(28));
        t.setGravity(Gravity.CENTER);
        llCluesList.addView(t);
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