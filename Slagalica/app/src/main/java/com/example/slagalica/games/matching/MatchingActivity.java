package com.example.slagalica.games.matching;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.example.slagalica.models.MatchingGame;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;
import android.widget.ImageView;

public class MatchingActivity extends AppCompatActivity {

    private TextView selectedLeftItem = null;
    private int connectedPairs = 0;
    private final Map<String, String> correctMatches = new HashMap<>();

    private int playerScore = 0;
    private int correctMatchesCount = 0;
    private CountDownTimer timer;
    private long timeLeftMillis = 30000;

    private TextView tvTimer;
    private TextView[] leftItems;
    private TextView[] rightItems;
    private TextView tvPlayerName;
    private ImageView imgYourAvatar;
    private TextView tvPlayerInfo;
    private TextView tvOpponentInfo;
    private TextView tvPlayerScore;
    private boolean isBattleMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matching);

        isBattleMode = getIntent().getBooleanExtra("isBattleMode", false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTimer = findViewById(R.id.tvTimer);
        imgYourAvatar = findViewById(R.id.imgYourAvatar);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvPlayerInfo = findViewById(R.id.tvPlayerInfo);
        tvOpponentInfo = findViewById(R.id.tvOpponentInfo);
        tvOpponentInfo.setText("🪙0 ⭐0 L0");

        tvPlayerScore = findViewById(R.id.tvPlayerScore);
        playerScore = getIntent().getIntExtra("currentTotalScore", 0);
        tvPlayerScore.setText(playerScore + " pts");

        loadCurrentUserInfo();

        setupLeaveButton();
        setupSubmitButton();
        setupMatchingViews();
        setupMatchingClicks();
        setupInfoButton();

        loadMatchingGameFromFirebase();
    }

    private void loadMatchingGameFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("matchingGames")
                .document("game1")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Matching game not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    MatchingGame game = documentSnapshot.toObject(MatchingGame.class);

                    if (game == null) {
                        Toast.makeText(this, "Failed to load matching game.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    fillMatchingData(game);
                    startTimer();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load matching game.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fillMatchingData(MatchingGame game) {
        leftItems[0].setText(game.getLeft1());
        leftItems[1].setText(game.getLeft2());
        leftItems[2].setText(game.getLeft3());
        leftItems[3].setText(game.getLeft4());
        leftItems[4].setText(game.getLeft5());

        rightItems[0].setText(game.getRight1());
        rightItems[1].setText(game.getRight2());
        rightItems[2].setText(game.getRight3());
        rightItems[3].setText(game.getRight4());
        rightItems[4].setText(game.getRight5());

        correctMatches.clear();
        correctMatches.put(game.getLeft1(), game.getMatch1());
        correctMatches.put(game.getLeft2(), game.getMatch2());
        correctMatches.put(game.getLeft3(), game.getMatch3());
        correctMatches.put(game.getLeft4(), game.getMatch4());
        correctMatches.put(game.getLeft5(), game.getMatch5());
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveGame);

        btnLeave.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle("Leave battle")
                    .setMessage("If you leave now, you will lose the entire battle. Are you sure?")
                    .setPositiveButton("YES", (dialog, which) -> {
                        if (timer != null) {
                            timer.cancel();
                        }

                        if (isBattleMode) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("battleLost", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            finish();
                        }
                    })
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

    private void setupMatchingViews() {
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
    }

    private void setupMatchingClicks() {
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

        String selectedLeftText = selectedLeftItem.getText().toString();
        String selectedRightText = rightItem.getText().toString();

        String correctRightText = correctMatches.get(selectedLeftText);
        boolean isCorrect = correctRightText != null && correctRightText.equals(selectedRightText);

        if (isCorrect) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            rightItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            playerScore += 2;
            correctMatchesCount++;
        } else {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            rightItem.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
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
        if (timer != null) {
            timer.cancel();
        }

        if (isBattleMode) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("score", playerScore);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Game finished")
                .setMessage("Your score: " + playerScore + " pts")
                .setPositiveButton("OK", (dialog, which) -> {
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

    private void loadCurrentUserInfo() {
        String userId = "jMwwl0MoswM7u5nifYChTng97jj1";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        imgYourAvatar.setImageResource(R.drawable.avatar_owl);
                        tvPlayerName.setText("Player");
                        tvPlayerInfo.setText("🪙0 ⭐0 L0");
                        return;
                    }

                    String username = document.getString("username");
                    String avatar = document.getString("avatar");

                    Long tokens = document.getLong("tokens");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");

                    long tokensValue = tokens != null ? tokens : 0;
                    long starsValue = stars != null ? stars : 0;
                    long leagueValue = league != null ? league : 0;

                    tvPlayerName.setText(username != null ? username : "Player");
                    imgYourAvatar.setImageResource(getAvatarResource(avatar));

                    tvPlayerInfo.setText(
                            "🪙" + tokensValue +
                                    " ⭐" + starsValue +
                                    " L" + leagueValue
                    );
                })
                .addOnFailureListener(e -> {
                    imgYourAvatar.setImageResource(R.drawable.avatar_owl);
                    tvPlayerName.setText("Player");
                    tvPlayerInfo.setText("🪙0 ⭐0 L0");
                });
    }

    private int getAvatarResource(String avatar) {
        if (avatar == null) {
            return R.drawable.avatar_owl;
        }

        switch (avatar) {
            case "fox":
                return R.drawable.avatar_fox;
            case "penguin":
                return R.drawable.avatar_penguin;
            case "wolf":
                return R.drawable.avatar_wolf;
            case "cat":
                return R.drawable.avatar_cat;
            case "dog":
                return R.drawable.avatar_dog;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }
}