package com.example.slagalica.games.MyNumber;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.slagalica.data.StatisticsRepository;
import android.widget.ImageView;

public class MyNumberActivity extends AppCompatActivity implements SensorEventListener {
    private static final int STOP_TIMEOUT_MS   = 5000;
    private static final int ROUND_DURATION_MS = 60000;

    private CountDownTimer targetTimer, numbersTimer, gameTimer;
    private int targetNumber = 0;
    private int[] numbers    = new int[6];
    private boolean targetStopped  = false;
    private boolean numbersStopped = false;

    private TextView tvTimer, tvTargetNumber, tvRoundInfo;
    private TextView tvPlayerName, tvPlayerScore, tvPlayerInfo;
    private ImageView imgYourAvatar, imgOpponentAvatar;

    private TextView tvOpponentName, tvOpponentScore, tvOpponentInfo;
    private TextView[] numberTiles;
    private TextView btnStopTarget, btnStopNumbers, btnConfirm;
    private EditText etExpression;
    private final Random random = new Random();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 800;

    private boolean isMultiplayer = false;
    private boolean isFriendly = false;
    private boolean opponentAlreadyLeft = false;
    private String  gameId;
    private String  currentUid;
    private boolean isPlayer1 = false;
    private boolean isStopper = false;
    private int     currentRound = 1;
    private int     multiplayerGamePoints = 0;

    private FirebaseFirestore    db;
    private ListenerRegistration gameListener;
    private ListenerRegistration scoreListener;

    private int myResult = -1;

    private TextView[] numKeys = new TextView[6];
    private boolean[] numUsed  = new boolean[6];

    private static final int[] MEDIUM_NUMBERS = {10, 15, 20};
    private static final int[] LARGE_NUMBERS  = {25, 50, 75, 100};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);

        if (getIntent().getBooleanExtra("isBattleMode", false)) {
            android.view.View opponentPanel = findViewById(R.id.layoutOpponentPanel);
            if (opponentPanel != null) opponentPanel.setVisibility(android.view.View.GONE);
            android.view.View vsLabel = findViewById(R.id.tvVsLabel);
            if (vsLabel != null) vsLabel.setVisibility(android.view.View.GONE);
            android.view.View playerScoreView = findViewById(R.id.tvPlayerScore);
            if (playerScoreView != null) playerScoreView.setVisibility(android.view.View.GONE);
            android.view.View playerPanel = findViewById(R.id.layoutPlayerPanel);
            if (playerPanel != null && playerPanel.getLayoutParams() instanceof android.widget.LinearLayout.LayoutParams) {
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) playerPanel.getLayoutParams();
                lp.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
                lp.weight = 0f;
                playerPanel.setLayoutParams(lp);
            }
        }

        db            = FirebaseFirestore.getInstance();
        currentUid    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        isFriendly = getIntent().getBooleanExtra("isFriendly", false);
        opponentAlreadyLeft = getIntent().getBooleanExtra("opponentAlreadyLeft", false);
        gameId        = getIntent().getStringExtra("gameId");


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        tvTimer        = findViewById(R.id.tvTimer);
        tvTargetNumber = findViewById(R.id.tvTargetNumber);
        tvRoundInfo    = findViewById(R.id.tvRoundInfo);
        btnStopTarget  = findViewById(R.id.btnStopTarget);
        btnStopNumbers = findViewById(R.id.btnStopNumbers);
        btnConfirm     = findViewById(R.id.btnConfirm);
        etExpression   = findViewById(R.id.etExpression);

        etExpression.setShowSoftInputOnFocus(false);
        etExpression.setCursorVisible(true);
        etExpression.setFocusableInTouchMode(false);
        etExpression.setKeyListener(null);

        tvPlayerName    = findViewById(R.id.tvPlayerName);
        tvPlayerScore   = findViewById(R.id.tvPlayerScore);
        tvPlayerInfo    = findViewById(R.id.tvPlayerInfo);
        tvOpponentName  = findViewById(R.id.tvOpponentName);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvOpponentInfo  = findViewById(R.id.tvOpponentInfo);
        imgYourAvatar     = findViewById(R.id.tvYourAvatar);
        imgOpponentAvatar = findViewById(R.id.tvOpponentAvatar);


        numberTiles = new TextView[]{
                findViewById(R.id.tvNum1), findViewById(R.id.tvNum2),
                findViewById(R.id.tvNum3), findViewById(R.id.tvNum4),
                findViewById(R.id.tvNum5), findViewById(R.id.tvNum6)
        };

        stylePremiumTiles();
        buildKeypad();

        findViewById(R.id.layoutHeader).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        findViewById(R.id.layoutTargetCard).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        setButtonEnabled(btnStopTarget,  false);
        setButtonEnabled(btnStopNumbers, false);
        setButtonEnabled(btnConfirm,     false);

        findViewById(R.id.btnLeaveMyNumber).setOnClickListener(v -> {
            cancelAllTimers();
            if (gameListener  != null) { gameListener.remove();  gameListener  = null; }
            if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }
            Intent result = new Intent();
            result.putExtra("battleLost", true);
            setResult(RESULT_OK, result);
            finish();
        });

        btnStopTarget .setOnClickListener(v -> stopTarget());
        btnStopNumbers.setOnClickListener(v -> stopNumbers());
        btnConfirm    .setOnClickListener(v -> submitExpression());

        if (isMultiplayer) {
            loadPlayerInfoAndSetup();
        } else {
            isStopper = true;
            setButtonEnabled(btnStopTarget, true);
            startTargetAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllTimers();
        if (gameListener  != null) { gameListener.remove();  gameListener  = null; }
        if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }



    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable grad(int radius, int top, int bottom) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        g.setCornerRadius(radius);
        return g;
    }

    private void styleKeyByIndex(TextView v, int index) {
        if (index == 5) {
            v.setBackground(grad(dp(14), 0xFFF7D667, 0xFFD9A33A));
            v.setTextColor(0xFF3A2A00);
        } else if (index == 4) {
            v.setBackground(grad(dp(14), 0xFF4F46E5, 0xFF3730A3));
            v.setTextColor(0xFFFFFFFF);
        } else {
            v.setBackground(grad(dp(14), 0xFF7C3AED, 0xFF5B21B6));
            v.setTextColor(0xFFFFFFFF);
        }
        v.setElevation(dp(3));
    }

    private void stylePremiumTiles() {
        for (int i = 0; i < numberTiles.length; i++) {
            styleKeyByIndex(numberTiles[i], i);
            numberTiles[i].setTextSize(18);
        }
    }

    private void buildKeypad() {
        LinearLayout card = findViewById(R.id.layoutExpressionCard);
        if (card == null) return;

        LinearLayout pad = new LinearLayout(this);
        LinearLayout.LayoutParams padLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        padLp.setMargins(0, dp(10), 0, dp(6));
        pad.setLayoutParams(padLp);
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("⌨  Tap to build your expression");
        title.setTextColor(0xFF5B2FC4);
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        pad.addView(title);

        LinearLayout numRow = new LinearLayout(this);
        LinearLayout.LayoutParams nrp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nrp.setMargins(0, dp(6), 0, 0);
        numRow.setLayoutParams(nrp);
        numRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 6; i++) {
            numKeys[i] = makeNumKey(i);
            numRow.addView(numKeys[i]);
        }
        pad.addView(numRow);

        pad.addView(opRow(new String[]{"(", ")", "+", "-"}));
        pad.addView(opRow(new String[]{"*", "/", "⌫", "C"}));

        int idx = card.indexOfChild(btnConfirm);
        if (idx < 0) idx = card.getChildCount();
        card.addView(pad, idx);
    }

    private TextView makeNumKey(int index) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        chip.setLayoutParams(lp);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextSize(18);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setText("?");
        styleKeyByIndex(chip, index);
        chip.setOnClickListener(v -> onNumKey(index));
        return chip;
    }

    private LinearLayout opRow(String[] keys) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(6), 0, 0);
        row.setLayoutParams(rp);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String k : keys) row.addView(makeOpChip(k));
        return row;
    }

    private TextView makeOpChip(String label) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        chip.setLayoutParams(lp);
        chip.setGravity(Gravity.CENTER);
        chip.setText(label);
        chip.setTextColor(0xFFFFFFFF);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextSize(20);
        chip.setElevation(dp(3));
        if ("C".equals(label)) {
            chip.setBackground(grad(dp(14), 0xFFEF4444, 0xFFB91C1C));   // red clear
        } else if ("⌫".equals(label)) {
            chip.setBackground(grad(dp(14), 0xFF6B5E99, 0xFF4A3D78));   // muted backspace
        } else {
            chip.setBackground(grad(dp(14), 0xFF7C3AED, 0xFF5B21B6));   // violet
        }
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(v -> onOpKey(label));
        return chip;
    }

    private void refreshNumKeys() {
        if (numKeys[0] == null) return;
        for (int i = 0; i < 6; i++) {
            numKeys[i].setText(String.valueOf(numbers[i]));
            numKeys[i].setAlpha(numUsed[i] ? 0.3f : 1.0f);
            numKeys[i].setClickable(!numUsed[i]);
        }
    }

    private void resetKeypadUsage() {
        for (int i = 0; i < 6; i++) numUsed[i] = false;
        refreshNumKeys();
    }

    private void onNumKey(int index) {
        if (numUsed[index]) return;
        etExpression.append(String.valueOf(numbers[index]));
        etExpression.setSelection(etExpression.getText().length());
        numUsed[index] = true;
        refreshNumKeys();
    }

    private void onOpKey(String k) {
        if ("C".equals(k)) {
            etExpression.setText("");
            resetKeypadUsage();
            return;
        }
        if ("⌫".equals(k)) {
            String s = etExpression.getText().toString();
            if (s.isEmpty()) return;
            // if the deleted tail is one of the numbers, free that key again
            for (int i = 0; i < 6; i++) {
                String num = String.valueOf(numbers[i]);
                if (numUsed[i] && s.endsWith(num)) {
                    s = s.substring(0, s.length() - num.length());
                    numUsed[i] = false;
                    etExpression.setText(s);
                    etExpression.setSelection(s.length());
                    refreshNumKeys();
                    return;
                }
            }
            etExpression.setText(s.substring(0, s.length() - 1));
            etExpression.setSelection(etExpression.getText().length());
            return;
        }
        etExpression.append(k);
        etExpression.setSelection(etExpression.getText().length());
    }


    private void loadPlayerInfoAndSetup() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(myDoc -> {
                    if (myDoc.exists()) {
                        String name  = myDoc.getString("username");
                        Long   coins = myDoc.getLong("tokens");
                        Long   stars = myDoc.getLong("stars");
                        Long   level = myDoc.getLong("league");
                        if (name != null) tvPlayerName.setText(name);
                        imgYourAvatar.setImageResource(
                                com.example.slagalica.data.PlayerProfileLoader.getAvatarResource(myDoc.getString("avatar")));
                        tvPlayerInfo.setText(
                                "🪙" + (coins != null ? coins : 0) +
                                        " ⭐" + (stars != null ? stars : 0) +
                                        " L"  + (level != null ? level : 0));
                    }
                    setupRound();
                });
    }

    private void loadOpponentInfo(String opponentUid) {
        db.collection("users").document(opponentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("username");
                        Long   coins = doc.getLong("tokens");
                        Long   stars = doc.getLong("stars");
                        Long   level = doc.getLong("league");
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

    private void startScoreListener(String player1Uid) {
        if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }
        scoreListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;
                    boolean iAm1  = currentUid.equals(player1Uid);
                    Long s1 = snapshot.getLong("score1");
                    Long s2 = snapshot.getLong("score2");
                    long myScore  = iAm1 ? (s1 != null ? s1 : 0) : (s2 != null ? s2 : 0);
                    long oppScore = iAm1 ? (s2 != null ? s2 : 0) : (s1 != null ? s1 : 0);
                    tvPlayerScore .setText(myScore  + " pts");
                    tvOpponentScore.setText(oppScore + " pts");
                });
    }


    private void setupRound() {
        myResult = -1;
        targetStopped  = false;
        numbersStopped = false;
        targetNumber   = 0;
        etExpression.setText("");
        resetKeypadUsage();

        setButtonEnabled(btnStopTarget,  false);
        setButtonEnabled(btnStopNumbers, false);
        setButtonEnabled(btnConfirm,     false);

        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    String player1Uid = snapshot.getString("player1");
                    String player2Uid = snapshot.getString("player2");
                    isPlayer1 = currentUid.equals(player1Uid);

                    loadOpponentInfo(isPlayer1 ? player2Uid : player1Uid);
                    startScoreListener(player1Uid);

                    Long roundLong = snapshot.getLong("myNumberRound");
                    currentRound = roundLong != null ? roundLong.intValue() : 1;
                    tvRoundInfo.setText(currentRound + "/2");

                    isStopper = (currentRound == 1 && isPlayer1)
                            || (currentRound == 2 && !isPlayer1);
                    if (opponentAlreadyLeft) {
                        isStopper = true;
                    }

                    Long       sharedTarget  = snapshot.getLong("myNumberTarget_r" + currentRound);
                    List<Long> sharedNumbers = (List<Long>) snapshot.get("myNumberNumbers_r" + currentRound);

                    boolean numbersReady = sharedTarget != null
                            && sharedNumbers != null
                            && sharedNumbers.size() == 6;

                    if (numbersReady) {
                        applySharedNumbers(sharedTarget, sharedNumbers);
                        String myResField = isPlayer1
                                ? "myNumberResult1_r" + currentRound
                                : "myNumberResult2_r" + currentRound;
                        Object myResObj = snapshot.get(myResField);
                        if (myResObj != null) {
                            myResult = ((Number) myResObj).intValue();
                            waitForOpponentResult();
                        } else {
                            startGameTimer();
                        }
                    } else if (isStopper) {
                        setButtonEnabled(btnStopTarget, true);
                        startTargetAnimation();
                    } else {
                        tvTargetNumber.setText("...");
                        tvTimer.setText("Waiting...");
                        waitForStopperToSetNumbers();
                    }
                });
    }

    private void applySharedNumbers(Long sharedTarget, List<Long> sharedNumbers) {
        targetNumber   = sharedTarget.intValue();
        targetStopped  = true;
        numbersStopped = true;
        tvTargetNumber.setText(String.valueOf(targetNumber));
        for (int i = 0; i < 6; i++) {
            numbers[i] = sharedNumbers.get(i).intValue();
            numberTiles[i].setText(String.valueOf(numbers[i]));
        }
        setButtonEnabled(btnConfirm, true);
        resetKeypadUsage();
    }


    private void waitForStopperToSetNumbers() {
        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;
                    Long       sharedTarget  = snapshot.getLong("myNumberTarget_r" + currentRound);
                    List<Long> sharedNumbers = (List<Long>) snapshot.get("myNumberNumbers_r" + currentRound);
                    if (sharedTarget != null && sharedNumbers != null && sharedNumbers.size() == 6) {
                        if (gameListener != null) { gameListener.remove(); gameListener = null; }
                        applySharedNumbers(sharedTarget, sharedNumbers);
                        startGameTimer();
                    }
                });
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        if (!isStopper) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        long  now = System.currentTimeMillis();
        float acceleration = (x * x + y * y + z * z) /
                (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (acceleration > SHAKE_THRESHOLD / 100f && now - lastShakeTime > 1000) {
            lastShakeTime = now;
            if      (!targetStopped)  stopTarget();
            else if (!numbersStopped) stopNumbers();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    private void startTargetAnimation() {
        targetTimer = new CountDownTimer(STOP_TIMEOUT_MS, 100) {
            @Override public void onTick(long ms) {
                targetNumber = 100 + random.nextInt(900);
                tvTargetNumber.setText(String.valueOf(targetNumber));
            }
            @Override public void onFinish() { stopTarget(); }
        }.start();
    }

    private void stopTarget() {
        if (targetStopped) return;
        targetStopped = true;
        if (targetTimer != null) { targetTimer.cancel(); targetTimer = null; }
        tvTargetNumber.setText(String.valueOf(targetNumber));
        tvTargetNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        setButtonEnabled(btnStopTarget,  false);
        setButtonEnabled(btnStopNumbers, true);
        startNumbersAnimation();
    }

    private void startNumbersAnimation() {
        numbersTimer = new CountDownTimer(STOP_TIMEOUT_MS, 80) {
            @Override public void onTick(long ms) {
                int[] g = generateNumbers();
                for (int i = 0; i < 6; i++) {
                    numbers[i] = g[i];
                    numberTiles[i].setText(String.valueOf(numbers[i]));
                }
            }
            @Override public void onFinish() { stopNumbers(); }
        }.start();
    }

    private int[] generateNumbers() {
        int[] result = new int[6];
        for (int i = 0; i < 4; i++) result[i] = 1 + random.nextInt(9);
        result[4] = MEDIUM_NUMBERS[random.nextInt(MEDIUM_NUMBERS.length)];
        result[5] = LARGE_NUMBERS [random.nextInt(LARGE_NUMBERS.length)];
        return result;
    }

    private void stopNumbers() {
        if (!targetStopped || numbersStopped) return;
        numbersStopped = true;
        if (numbersTimer != null) { numbersTimer.cancel(); numbersTimer = null; }
        setButtonEnabled(btnStopNumbers, false);

        for (int i = 0; i < numberTiles.length; i++) {
            final int idx = i;
            numberTiles[i].postDelayed(() ->
                    numberTiles[idx].startAnimation(
                            AnimationUtils.loadAnimation(this, R.anim.bounce_in)), i * 100L);
        }

        if (isMultiplayer) {
            saveStoppedNumbers();
        } else {
            resetKeypadUsage();
            setButtonEnabled(btnConfirm, true);
            startGameTimer();
        }
    }

    private void saveStoppedNumbers() {
        List<Long> toSave = new ArrayList<>();
        for (int n : numbers) toSave.add((long) n);

        Map<String, Object> updates = new HashMap<>();
        updates.put("myNumberTarget_r"  + currentRound, (long) targetNumber);
        updates.put("myNumberNumbers_r" + currentRound, toSave);

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> {
                    resetKeypadUsage();
                    setButtonEnabled(btnConfirm, true);
                    startGameTimer();
                });
    }


    private void startGameTimer() {
        setButtonEnabled(btnConfirm, true);
        gameTimer = new CountDownTimer(ROUND_DURATION_MS, 1000) {
            @Override public void onTick(long ms) {
                int s = (int) (ms / 1000);
                tvTimer.setText("⏱ " + s + "s");
                if (s <= 10) {
                    tvTimer.startAnimation(
                            AnimationUtils.loadAnimation(MyNumberActivity.this, R.anim.pulse));
                    tvTimer.setTextColor(0xFFE53935);
                }
            }
            @Override
            public void onFinish() {
                tvTimer.setText("⏱ 0s");
                tvTimer.clearAnimation();
                if (myResult == -1) {
                    myResult = 0;
                    saveMyResult();
                }
            }
        }.start();
    }


    private void submitExpression() {
        if (myResult != -1) return;

        String expr = etExpression.getText().toString().trim();

        if (expr.isEmpty()) {
            cancelAllTimers();
            setButtonEnabled(btnConfirm, false);
            etExpression.setEnabled(false);
            myResult = 0;
            saveMyResult();
            return;
        }

        if (!expressionUsesOnlyAllowedNumbers(expr, numbers)) {
            Toast.makeText(this,
                    "Expression contains numbers not in the given set!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double result;
        try {
            result = evaluate(expr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid expression", Toast.LENGTH_LONG).show();
            return;
        }

        cancelAllTimers();
        setButtonEnabled(btnConfirm, false);
        etExpression.setEnabled(false);
        myResult = (int) Math.round(result);
        saveMyResult();
    }


    private void saveMyResult() {
        if (!isMultiplayer) {
            int points = (myResult == targetNumber) ? 10 : 0;
            Toast.makeText(this,
                    points == 10 ? "Correct! 10 points." :
                            "Result: " + myResult + " (diff: " + Math.abs(myResult - targetNumber) + ")",
                    Toast.LENGTH_LONG).show();
            Intent res = new Intent();
            res.putExtra("points", points);
            setResult(RESULT_OK, res);
            finish();
            return;
        }

        String myResField = isPlayer1
                ? "myNumberResult1_r" + currentRound
                : "myNumberResult2_r" + currentRound;

        Map<String, Object> updates = new HashMap<>();
        updates.put(myResField, (long) myResult);

        tvTimer.setText("Waiting for opponent...");

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> waitForOpponentResult());
    }

    private void waitForOpponentResult() {
        if (opponentAlreadyLeft) {
            calculateAndSavePoints(myResult, -1);
            return;
        }

        String oppResField = isPlayer1
                ? "myNumberResult2_r" + currentRound
                : "myNumberResult1_r" + currentRound;

        if (gameListener != null) { gameListener.remove(); gameListener = null; }

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;

                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        if (gameListener != null) {
                            gameListener.remove();
                            gameListener = null;
                        }
                        calculateAndSavePoints(myResult, -1);
                        return;
                    }

                    Object oppResObj = snapshot.get(oppResField);
                    if (oppResObj == null) return;
                    int oppResult = ((Number) oppResObj).intValue();
                    if (gameListener != null) { gameListener.remove(); gameListener = null; }

                    calculateAndSavePoints(myResult, oppResult);
                });
    }


    private void calculateAndSavePoints(int myRes, int oppRes) {
        boolean myExact  = (myRes  == targetNumber);
        boolean oppExact = (oppRes == targetNumber);
        int myDiff  = Math.abs(myRes  - targetNumber);
        int oppDiff = Math.abs(oppRes - targetNumber);

        int myPoints;
        String msg;
        if (oppRes == -1) {
            myPoints = (myRes == targetNumber) ? 10 : 0;
            msg = myPoints == 10
                    ? "Correct! 10 points."
                    : "Opponent left the match.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            savePointsAndAdvance(myPoints);
            return;
        }

        if (myExact && oppExact) {
            myPoints = 10;
            msg = "Both exact! You get 10 points.";
        } else if (myExact) {
            myPoints = 10;
            msg = "Correct! 10 points.";
        } else if (oppExact) {
            myPoints = 0;
            msg = "Opponent found it exactly. 0 points.";
        } else if (myRes == 0 && oppRes == 0) {
            myPoints = 0;
            msg = "Nobody entered an expression. 0 points.";
        } else if (myRes == oppRes && myRes != 0) {
            myPoints = isStopper ? 5 : 0;
            msg = isStopper ? "Tie! Your round — 5 points." : "Tie! Opponent's round — 0 points.";
        } else if (myRes == 0) {
            myPoints = 0;
            msg = "You didn't enter anything. 0 points.";
        } else if (oppRes == 0) {
            myPoints = 5;
            msg = "Opponent didn't enter anything. 5 points.";
        } else if (myDiff < oppDiff) {
            myPoints = 5;
            msg = "You're closer! 5 points.";
        } else if (oppDiff < myDiff) {
            myPoints = 0;
            msg = "Opponent is closer. 0 points.";
        } else {
            myPoints = isStopper ? 5 : 0;
            msg = isStopper ? "Equal distance! Your round — 5 points." : "Equal distance! Opponent's round — 0 points.";
        }

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        savePointsAndAdvance(myPoints);
    }

    private void savePointsAndAdvance(int points) {
        multiplayerGamePoints += points;
        if (currentRound == 1) {
            advanceToRound2();
        } else {
            finishGame(multiplayerGamePoints);
        }
    }


    private void advanceToRound2() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("myNumberRound", 2);

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> {
                    currentRound = 2;
                    runOnUiThread(() -> {
                        tvTimer.setText("⏱ 60s");
                        tvTimer.setTextColor(0xFF5B2FC4);
                        tvTargetNumber.setText("0");
                        for (TextView t : numberTiles) t.setText("0");
                        etExpression.setText("");
                        etExpression.setEnabled(true);
                        resetKeypadUsage();
                        setupRound();
                    });
                });
    }

    private void finishGame(int totalGamePoints) {
        if (scoreListener != null) { scoreListener.remove(); scoreListener = null; }

        // Friendly matches do not count towards statistics.
        if (!isFriendly) {
            StatisticsRepository.saveMyNumberResult(
                    totalGamePoints,
                    myResult == targetNumber
            );
        }

        Intent result = new Intent();
        result.putExtra("points", totalGamePoints);
        result.putExtra("myNumberRound", currentRound);
        setResult(RESULT_OK, result);
        finish();
    }


    private void setButtonEnabled(TextView btn, boolean enabled) {
        btn.setClickable(enabled);
        btn.setAlpha(enabled ? 1.0f : 0.4f);
    }

    private void cancelAllTimers() {
        if (targetTimer  != null) { targetTimer.cancel();  targetTimer  = null; }
        if (numbersTimer != null) { numbersTimer.cancel(); numbersTimer = null; }
        if (gameTimer    != null) { gameTimer.cancel();    gameTimer    = null; }
    }

    private boolean expressionUsesOnlyAllowedNumbers(String expr, int[] allowed) {
        List<Integer> used      = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(expr);
        while (m.find()) used.add(Integer.parseInt(m.group()));
        List<Integer> available = new ArrayList<>();
        for (int n : allowed) available.add(n);
        for (int n : used) {
            if (!available.remove(Integer.valueOf(n))) return false;
        }
        return true;
    }

    private double evaluate(String expression) {
        expression = expression.replaceAll("\\s+", "");
        char[] tokens = expression.toCharArray();
        Stack<Double>    values = new Stack<>();
        Stack<Character> ops    = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            char c = tokens[i];
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < tokens.length && Character.isDigit(tokens[i])) sb.append(tokens[i++]);
                i--;
                values.push(Double.parseDouble(sb.toString()));
            } else if (c == '(') {
                ops.push(c);
            } else if (c == ')') {
                while (ops.peek() != '(') values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                ops.pop();
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!ops.isEmpty() && hasPrecedence(c, ops.peek()))
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                ops.push(c);
            } else {
                throw new IllegalArgumentException("Illegal character: " + c);
            }
        }
        while (!ops.isEmpty()) values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        return values.pop();
    }

    private boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') return false;
        return (op1 != '*' && op1 != '/') || (op2 != '+' && op2 != '-');
    }

    private double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero!");
                return a / b;
        }
        return 0;
    }
}
