package com.example.slagalica.games.matching;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import android.os.CountDownTimer;
import java.util.HashMap;
import java.util.Map;

public class MatchingActivity extends AppCompatActivity {

    private TextView selectedLeftItem = null;
    private int connectedPairs = 0;
    private final Map<Integer, Integer> correctMatches = new HashMap<>();
    private int playerScore = 0;
    private CountDownTimer timer;
    private long timeLeftMillis = 30000;
    private TextView tvTimer;
    private TextView[] leftItems;
    private TextView[] rightItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matching);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupLeaveButton();
        setupSubmitButton();
        setupCorrectMatches();
        setupMatchingClicks();
        setupInfoButton();

        tvTimer = findViewById(R.id.tvTimer);
        startTimer();
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveGame);

        btnLeave.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle("Leave Game")
                    .setMessage("Are you sure you want to leave the game?")
                    .setPositiveButton("YES", (dialog, which) -> finish())
                    .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void setupSubmitButton() {
        Button btnSubmit = findViewById(R.id.btnSubmitMatching);

        btnSubmit.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle("Submit Answers")
                    .setMessage("Are you sure you want to submit your answers?")
                    .setPositiveButton("YES", (dialog, which) -> showEndDialog())
                    .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void setupCorrectMatches() {
        correctMatches.put(R.id.leftItem1, R.id.rightItem3); // Serbia - Belgrade
        correctMatches.put(R.id.leftItem2, R.id.rightItem4); // Italy - Rome
        correctMatches.put(R.id.leftItem3, R.id.rightItem5); // France - Paris
        correctMatches.put(R.id.leftItem4, R.id.rightItem1); // Germany - Berlin
        correctMatches.put(R.id.leftItem5, R.id.rightItem2); // Spain - Madrid
    }

    private void setupMatchingClicks() {
        leftItems = new TextView[]{
                findViewById(R.id.leftItem1),
                findViewById(R.id.leftItem2),
                findViewById(R.id.leftItem3),
                findViewById(R.id.leftItem4),
                findViewById(R.id.leftItem5)
        };

        rightItems = new TextView[]{
                findViewById(R.id.rightItem1),
                findViewById(R.id.rightItem2),
                findViewById(R.id.rightItem3),
                findViewById(R.id.rightItem4),
                findViewById(R.id.rightItem5)
        };

        for (TextView leftItem : leftItems) {
            leftItem.setOnClickListener(v -> selectLeftItem((TextView) v));
        }

        for (TextView rightItem : rightItems) {
            rightItem.setOnClickListener(v -> connectWithRightItem((TextView) v));
        }
    }

    private void selectLeftItem(TextView leftItem) {
        if (!leftItem.isEnabled()) {
            return;
        }

        if (selectedLeftItem == leftItem) {
            leftItem.setBackgroundResource(R.drawable.bg_stat_card);
            selectedLeftItem = null;
            return;
        }

        if (selectedLeftItem != null) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_stat_card);
        }

        selectedLeftItem = leftItem;
        selectedLeftItem.setBackgroundResource(R.drawable.bg_matching_selected);
    }

    private void connectWithRightItem(TextView rightItem) {
        if (selectedLeftItem == null) return;

        Integer correctRightId = correctMatches.get(selectedLeftItem.getId());
        boolean isCorrect = correctRightId != null && correctRightId == rightItem.getId();

        if (isCorrect) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            rightItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            playerScore += 10;
        } else {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            rightItem.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            playerScore -= 5;
        }

        selectedLeftItem.setEnabled(false);
        rightItem.setEnabled(false);

        connectedPairs++;

        TextView tvConnectedCount = findViewById(R.id.tvConnectedCount);
        tvConnectedCount.setText("🔗 " + connectedPairs + " / 5");

        TextView tvScore = findViewById(R.id.tvPlayerScore);
        tvScore.setText(playerScore + " pts");

        selectedLeftItem = null;

        if (connectedPairs == 5) {
            showEndDialog();
        }
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
                tvTimer.setText("⏱ " + seconds + "s");
            }

            @Override
            public void onFinish() {
                timeLeftMillis = 0;
                tvTimer.setText("⏱ 0s");
                showEndDialog();
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void showEndDialog() {
        if (timer != null) timer.cancel();

        new AlertDialog.Builder(this)
                .setTitle("Game finished")
                .setMessage("Your score: " + playerScore + " pts")
                .setPositiveButton("OK", (d, w) -> {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .show();
    }

    private void setupInfoButton() {
        TextView btnInfo = findViewById(R.id.btnMatchingInfo);

        btnInfo.setOnClickListener(v -> {
            pauseTimer();

            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle(R.string.matching_rules_title)
                    .setMessage(R.string.matching_rules_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();

                        if (timeLeftMillis > 0 && connectedPairs < 5) {
                            startTimer();
                        }
                    })
                    .show();
        });
    }
}