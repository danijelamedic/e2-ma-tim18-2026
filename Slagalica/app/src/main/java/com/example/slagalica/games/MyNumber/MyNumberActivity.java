package com.example.slagalica.games.MyNumber;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyNumberActivity extends AppCompatActivity {

    private CountDownTimer targetTimer, numbersTimer, gameTimer;
    private int targetNumber = 0;
    private int[] numbers = new int[6];
    private boolean targetStopped = false;
    private boolean numbersStopped = false;
    private TextView tvTimer, tvTargetNumber;
    private TextView[] numberTiles;
    private Button btnStopTarget, btnStopNumbers, btnConfirm;
    private EditText etExpression;
    private final Random random = new Random();

    private static final int[] MEDIUM_NUMBERS = {10, 15, 20};
    private static final int[] LARGE_NUMBERS  = {25, 50, 75, 100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);

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

        startTargetAnimation();

        btnStopTarget.setOnClickListener(v -> stopTarget());
        btnStopNumbers.setOnClickListener(v -> stopNumbers());
        btnConfirm.setOnClickListener(v -> confirmExpression());
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
                                    AnimationUtils.loadAnimation(this, R.anim.bounce_in)),
                    i * 100L);
        }

        startGameTimer();
    }

    private void startGameTimer() {
        gameTimer = new CountDownTimer(60000, 1000) {
            @Override public void onTick(long ms) {
                int seconds = (int) (ms / 1000);
                tvTimer.setText("⏱ " + seconds + "s");
                if (seconds <= 10) {
                    tvTimer.startAnimation(
                            AnimationUtils.loadAnimation(MyNumberActivity.this, R.anim.pulse));
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
            handleResult(0, "No expression entered. 0 points.");
            return;
        }

        if (!expressionUsesOnlyAllowedNumbers(expr, numbers)) {
            Toast.makeText(this, "Expression contains numbers not in the given set!",
                    Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            startGameTimer();
            return;
        }

        double result;
        try {
            result = evaluate(expr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid expression: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            return;
        }

        int roundedResult = (int) Math.round(result);
        int diff = Math.abs(roundedResult - targetNumber);

        if (diff == 0) {
            handleResult(10, "Correct! You earned 10 points.");
        } else {
            handleResult(roundedResult, "Your result: " + roundedResult + " (difference: " + diff + ")");
        }
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
    private void handleResult(int playerResult, String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (targetTimer  != null) targetTimer.cancel();
        if (numbersTimer != null) numbersTimer.cancel();
        if (gameTimer    != null) gameTimer.cancel();
    }
}