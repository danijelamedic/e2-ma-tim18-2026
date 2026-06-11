package com.example.slagalica.games.MyNumber;

import android.content.Intent;
<<<<<<< Updated upstream
=======
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
>>>>>>> Stashed changes
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class MyNumberActivity extends AppCompatActivity implements SensorEventListener {

    private CountDownTimer targetTimer, numbersTimer, gameTimer, waitTimer;
    private int targetNumber = 0;
    private int[] numbers = new int[6];
    private boolean targetStopped = false;
    private boolean numbersStopped = false;
    private TextView tvTimer, tvTargetNumber;
    private TextView[] numberTiles;
    private Button btnStopTarget, btnStopNumbers, btnConfirm;
    private EditText etExpression;
    private boolean isBattleMode;
    private final Random random = new Random();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 800;

    private boolean isMultiplayer = false;
    private String gameId;
    private String currentUid;
    private boolean isPlayer1 = false;
    private FirebaseFirestore db;
    private ListenerRegistration gameListener;

    private int myResult = -1;
    private static final int[] MEDIUM_NUMBERS = {10, 15, 20};
    private static final int[] LARGE_NUMBERS  = {25, 50, 75, 100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);

<<<<<<< Updated upstream
        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);
=======
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        gameId = getIntent().getStringExtra("gameId");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
>>>>>>> Stashed changes

        tvTimer        = findViewById(R.id.tvTimer);
        tvTargetNumber = findViewById(R.id.tvTargetNumber);
        btnStopTarget  = findViewById(R.id.btnStopTarget);
        btnStopNumbers = findViewById(R.id.btnStopNumbers);
        btnConfirm     = findViewById(R.id.btnConfirm);
        etExpression   = findViewById(R.id.etExpression);

        numberTiles = new TextView[]{
                findViewById(R.id.tvNum1), findViewById(R.id.tvNum2),
                findViewById(R.id.tvNum3), findViewById(R.id.tvNum4),
                findViewById(R.id.tvNum5), findViewById(R.id.tvNum6)
        };

        findViewById(R.id.layoutHeader).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        findViewById(R.id.layoutTargetCard).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        btnStopNumbers.setEnabled(false);
        btnConfirm.setEnabled(false);

        if (isMultiplayer) {
            db.collection("games").document(gameId).get()
                    .addOnSuccessListener(snapshot -> {
                        String player1 = snapshot.getString("player1");
                        isPlayer1 = currentUid.equals(player1);
                        startTargetAnimation();
                    });
        } else {
            startTargetAnimation();
        }

        btnStopTarget.setOnClickListener(v -> stopTarget());
        btnStopNumbers.setOnClickListener(v -> stopNumbers());
        btnConfirm.setOnClickListener(v -> confirmExpression());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        long now = System.currentTimeMillis();
        float acceleration = (x * x + y * y + z * z) /
                (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (acceleration > SHAKE_THRESHOLD / 100f && now - lastShakeTime > 1000) {
            lastShakeTime = now;
            if (!targetStopped) stopTarget();
            else if (!numbersStopped) stopNumbers();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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

    private void startTargetAnimation() {
        targetTimer = new CountDownTimer(5000, 100) {
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
        targetTimer.cancel();
        tvTargetNumber.setText(String.valueOf(targetNumber));
        tvTargetNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        btnStopTarget.setEnabled(false);
        btnStopNumbers.setEnabled(true);
        startNumbersAnimation();
    }

    private void startNumbersAnimation() {
        numbersTimer = new CountDownTimer(5000, 80) {
            @Override public void onTick(long ms) {
                int[] generated = generateNumbers();
                for (int i = 0; i < 6; i++) {
                    numbers[i] = generated[i];
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
        result[5] = LARGE_NUMBERS[random.nextInt(LARGE_NUMBERS.length)];
        return result;
    }

    private void stopNumbers() {
        if (!targetStopped || numbersStopped) return;
        numbersStopped = true;
        numbersTimer.cancel();
        btnStopNumbers.setEnabled(false);
        btnConfirm.setEnabled(true);
        for (int i = 0; i < numberTiles.length; i++) {
            final int index = i;
            numberTiles[i].postDelayed(() ->
                    numberTiles[index].startAnimation(
                            AnimationUtils.loadAnimation(this, R.anim.bounce_in)), i * 100L);
        }
        startGameTimer();
    }

    private void startGameTimer() {
        gameTimer = new CountDownTimer(60000, 1000) {
            @Override public void onTick(long ms) {
                int seconds = (int) (ms / 1000);
                tvTimer.setText("⏱ " + seconds + "s");
                if (seconds <= 10) {
                    tvTimer.startAnimation(AnimationUtils.loadAnimation(MyNumberActivity.this, R.anim.pulse));
                    tvTimer.setTextColor(0xFFE53935);
                }
            }
            @Override public void onFinish() {
                tvTimer.setText("⏱ 0s");
                tvTimer.clearAnimation();
                confirmExpression();
            }
        }.start();
    }

    private void confirmExpression() {
        if (gameTimer != null) gameTimer.cancel();
        btnConfirm.setEnabled(false);

        String expr = etExpression.getText().toString().trim();

        if (expr.isEmpty()) {
            myResult = 0;
            handleMyResult(0);
            return;
        }

        if (!expressionUsesOnlyAllowedNumbers(expr, numbers)) {
            Toast.makeText(this, "Expression contains numbers not in the given set!", Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            startGameTimer();
            return;
        }

        double result;
        try {
            result = evaluate(expr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid expression: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            return;
        }

        myResult = (int) Math.round(result);
        int diff = Math.abs(myResult - targetNumber);

        if (diff == 0) {
            handleMyResult(10);
        } else {
            handleMyResult(myResult);
        }
    }

    private void handleMyResult(int resultValue) {
        int diff = Math.abs(resultValue == 10 ? 0 : Math.abs(resultValue - targetNumber));
        boolean exact = (resultValue == 10 && Math.abs(myResult - targetNumber) == 0);

        if (!isMultiplayer) {
            int points = exact ? 10 : 0;
            Toast.makeText(this, exact ? "Correct! 10 points." : "Result: " + myResult + " (diff: " + Math.abs(myResult - targetNumber) + ")", Toast.LENGTH_LONG).show();
            Intent res = new Intent();
            res.putExtra("points", points);
            setResult(RESULT_OK, res);
            finish();
            return;
        }

        saveMyResultAndCompare(myResult);
    }

    private void saveMyResultAndCompare(int myRes) {
        String myResultField = isPlayer1 ? "myNumberResult1" : "myNumberResult2";
        String opponentResultField = isPlayer1 ? "myNumberResult2" : "myNumberResult1";
        String myStatusField = isPlayer1 ? "myNumberStatus1" : "myNumberStatus2";

        Map<String, Object> updates = new HashMap<>();
        updates.put(myResultField, (long) myRes);
        updates.put(myStatusField, "done");

        db.collection("games").document(gameId).update(updates)
                .addOnSuccessListener(unused -> waitForOpponentResult(opponentResultField));
    }

    private void waitForOpponentResult(String opponentResultField) {
        tvTimer.setText("Waiting for opponent...");
        btnConfirm.setEnabled(false);

        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;

                    Object oppResultObj = snapshot.get(opponentResultField);
                    if (oppResultObj == null) return;

                    int oppResult = ((Number) oppResultObj).intValue();
                    if (gameListener != null) gameListener.remove();

                    calculateFinalPoints(myResult, oppResult);
                });
    }

    private void calculateFinalPoints(int myRes, int oppRes) {
        int myDiff    = Math.abs(myRes  - targetNumber);
        int oppDiff   = Math.abs(oppRes - targetNumber);

        int points;

        if (myDiff == 0 && oppDiff != 0) {
            points = 10;
            Toast.makeText(this, "You got the exact number! 10 points.", Toast.LENGTH_LONG).show();
        } else if (oppDiff == 0 && myDiff != 0) {
            points = 0;
            Toast.makeText(this, "Opponent got the exact number. 0 points.", Toast.LENGTH_LONG).show();
        } else if (myDiff == 0 && oppDiff == 0) {
            points = isPlayer1 ? 5 : 0;
            Toast.makeText(this, isPlayer1 ? "Both exact! You get 5 pts (your round)." : "Both exact! Opponent's round, 0 pts.", Toast.LENGTH_LONG).show();
        } else if (myDiff < oppDiff) {
            points = 5;
            Toast.makeText(this, "You're closer! 5 points.", Toast.LENGTH_LONG).show();
        } else if (oppDiff < myDiff) {
            points = 0;
            Toast.makeText(this, "Opponent is closer. 0 points.", Toast.LENGTH_LONG).show();
        } else {
            points = isPlayer1 ? 5 : 0;
            Toast.makeText(this, isPlayer1 ? "Tie! You get 5 pts (your round)." : "Tie! Opponent's round, 0 pts.", Toast.LENGTH_LONG).show();
        }

        savePointsAndReturn(points);
    }

    private void savePointsAndReturn(int points) {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    String player1 = snapshot.getString("player1");
                    String scoreField = currentUid.equals(player1) ? "score1" : "score2";
                    long currentScore = snapshot.getLong(scoreField) != null ?
                            snapshot.getLong(scoreField) : 0;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put(scoreField, currentScore + points);
                    updates.put("myNumberStatus", currentUid.equals(player1) ? "player1done" : "done");

                    db.collection("games").document(gameId).update(updates)
                            .addOnSuccessListener(unused -> {
                                Intent result = new Intent();
                                result.putExtra("points", points);
                                setResult(RESULT_OK, result);
                                finish();
                            });
                });
    }

    private boolean expressionUsesOnlyAllowedNumbers(String expr, int[] allowed) {
        List<Integer> used = new ArrayList<>();
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
        Stack<Double> values = new Stack<>();
        Stack<Character> ops = new Stack<>();
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
<<<<<<< Updated upstream
    private void handleResult(int playerResult, String message) {

        new AlertDialog.Builder(this)
                .setTitle("My Number finished")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {

                    if (isBattleMode) {

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("score", playerResult);

                        setResult(RESULT_OK, resultIntent);
                        finish();

                    } else {

                        finish();
                    }
                })
                .show();
    }
=======
>>>>>>> Stashed changes

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (targetTimer  != null) targetTimer.cancel();
        if (numbersTimer != null) numbersTimer.cancel();
        if (gameTimer    != null) gameTimer.cancel();
        if (waitTimer    != null) waitTimer.cancel();
        if (gameListener != null) gameListener.remove();
    }
}