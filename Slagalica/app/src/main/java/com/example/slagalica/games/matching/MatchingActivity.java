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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import com.example.slagalica.data.StatisticsRepository;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.ImageView;
import com.example.slagalica.models.MatchingPair;
import java.util.ArrayList;
import java.util.List;
import android.view.View;

public class MatchingActivity extends AppCompatActivity {

    private TextView selectedLeftItem = null;
    private int connectedPairs = 0;
    private final Map<String, String> correctMatches = new HashMap<>();
    private final List<MatchingPair> matchingPairs = new ArrayList<>();

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
    private boolean resultSaved = false;
    private String gameId;
    private boolean isMultiplayer;
    private boolean opponentAlreadyLeft;
    private boolean isFriendly;
    private String currentUid;
    private String currentTurnUid;
    private boolean isMyTurn = false;
    private int matchingRound = 1;
    private String matchingPhase = "starter";
    private String matchingStarterUid;
    private String matchingCurrentPlayerUid;
    private FirebaseFirestore db;
    private ListenerRegistration matchingListener;
    private String lastShownOpponentMatch = "";
    private int lastSeenMatchingRound = 0;
    private String lastSeenMatchingPhase = "";
    private String lastSeenCurrentPlayerUid = "";
    private int correctMatchesInCurrentRound = 0;
    private int initialScore = 0;

    private ImageView imgOpponentAvatar;
    private TextView tvOpponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matching);

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

        db = FirebaseFirestore.getInstance();

        gameId = getIntent().getStringExtra("gameId");
        isMultiplayer = getIntent().getBooleanExtra("isMultiplayer", false);
        opponentAlreadyLeft = getIntent().getBooleanExtra("opponentAlreadyLeft", false);
        isFriendly = getIntent().getBooleanExtra("isFriendly", false);
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
        imgOpponentAvatar = findViewById(R.id.imgOpponentAvatar);
        tvOpponentName = findViewById(R.id.tvOpponentName);

        tvPlayerScore = findViewById(R.id.tvPlayerScore);
        initialScore = getIntent().getIntExtra("currentTotalScore", 0);
        playerScore = initialScore;
        tvPlayerScore.setText(playerScore + " pts");

        loadCurrentUserInfo();
        loadOpponentInfo();

        setupLeaveButton();
        setupSubmitButton();
        setupMatchingViews();
        setupMatchingClicks();
        setupInfoButton();
        listenForMatchingUpdates();

        loadMatchingGameFromFirebase("game1");
    }

    private void loadMatchingGameFromFirebase(String documentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("matchingGames")
                .document(documentId)
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

                    if (isMultiplayer && gameId != null) {
                        initializeMatchingState();
                    } else {
                        startTimer();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load matching game.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fillMatchingData(MatchingGame game) {
        matchingPairs.clear();

        matchingPairs.add(new MatchingPair(game.getLeft1(), game.getMatch1()));
        matchingPairs.add(new MatchingPair(game.getLeft2(), game.getMatch2()));
        matchingPairs.add(new MatchingPair(game.getLeft3(), game.getMatch3()));
        matchingPairs.add(new MatchingPair(game.getLeft4(), game.getMatch4()));
        matchingPairs.add(new MatchingPair(game.getLeft5(), game.getMatch5()));

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

        for (MatchingPair pair : matchingPairs) {
            correctMatches.put(pair.getLeft(), pair.getRight());
        }
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

                        if (isBattleMode || isMultiplayer) {
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
                    .setPositiveButton("YES", (dialog, which) -> finishMatchingTurn())
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

        if (isMultiplayer && gameId != null) {
            db.collection("games")
                    .document(gameId)
                    .update(
                            "matchingSelectedLeft", selectedLeftText,
                            "matchingSelectedRight", selectedRightText,
                            "matchingUpdatedByUid", currentUid,
                            "matchingConnectedPairs", connectedPairs + 1
                    );
        }

        String correctRightText = correctMatches.get(selectedLeftText);
        boolean isCorrect = correctRightText != null && correctRightText.equals(selectedRightText);

        if (isCorrect) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            rightItem.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            playerScore += 2;
            correctMatchesCount++;
            correctMatchesInCurrentRound++;
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
            finishMatchingTurn();
        }
    }

    private void startTimer() {

        if (isMultiplayer && !isMyTurn) {
            if (timer != null) {
                timer.cancel();
            }
            tvTimer.setText("⏱ Waiting");
            return;
        }

        if (timer != null) {
            timer.cancel();
        }

        timer = null;

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
                finishMatchingTurn();
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void showEndDialog() {
        if (resultSaved) {
            return;
        }

        resultSaved = true;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        boolean won = correctMatchesCount >= 3;
        int matchingOnlyScore = playerScore - initialScore;
        // Friendly matches do not count towards statistics.
        if (!isFriendly) {
            StatisticsRepository.saveMatchingResult(correctMatchesCount, 5, matchingOnlyScore, won);
        }

        if (isBattleMode || isMultiplayer) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("points", matchingOnlyScore);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            imgYourAvatar.setImageResource(R.drawable.avatar_owl);
            tvPlayerName.setText("Player");
            tvPlayerInfo.setText("🪙0 ⭐0 L0");
            return;
        }

        String userId = user.getUid();

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
            case "guest":
                return R.drawable.avatar_guest;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }

    private void listenForMatchingUpdates() {
        if (!isMultiplayer || gameId == null) {
            return;
        }

        matchingListener = db.collection("games")
                .document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String abandonedBy = snapshot.getString("abandonedBy");
                    if (abandonedBy != null && !abandonedBy.equals(currentUid)) {
                        opponentAlreadyLeft = true;
                    }

                    matchingRound = snapshot.getLong("matchingRound") != null
                            ? snapshot.getLong("matchingRound").intValue()
                            : 1;

                    matchingPhase = snapshot.getString("matchingPhase");

                    if ("finished".equals(matchingPhase)) {
                        showEndDialog();
                        return;
                    }
                    matchingStarterUid = snapshot.getString("matchingStarterUid");
                    matchingCurrentPlayerUid = snapshot.getString("matchingCurrentPlayerUid");

                    Long connectedFromDb = snapshot.getLong("matchingConnectedPairs");
                    connectedPairs = connectedFromDb != null ? connectedFromDb.intValue() : 0;

                    TextView tvConnectedCount = findViewById(R.id.tvConnectedCount);
                    tvConnectedCount.setText("🔗 " + connectedPairs + " / 5");

                    boolean roundChanged = matchingRound != lastSeenMatchingRound;
                    boolean phaseChanged = matchingPhase != null && !matchingPhase.equals(lastSeenMatchingPhase);
                    boolean playerChanged = matchingCurrentPlayerUid != null && !matchingCurrentPlayerUid.equals(lastSeenCurrentPlayerUid);

                    if (roundChanged) {
                        resetMatchingBoardForNewRound();

                        if (matchingRound == 2) {
                            loadMatchingGameFromFirebase("game2");
                        }
                    }

                    if (roundChanged || phaseChanged || playerChanged) {
                        timeLeftMillis = 30000;

                        if (timer != null) {
                            timer.cancel();
                            timer = null;
                        }
                    }

                    lastSeenMatchingRound = matchingRound;
                    lastSeenMatchingPhase = matchingPhase != null ? matchingPhase : "";
                    lastSeenCurrentPlayerUid = matchingCurrentPlayerUid != null ? matchingCurrentPlayerUid : "";

                    isMyTurn = opponentAlreadyLeft
                            || (currentUid != null && currentUid.equals(matchingCurrentPlayerUid));
                    updateTurnUi();

                    if (isMyTurn && timer == null && timeLeftMillis > 0) {
                        startTimer();
                    }

                    String selectedLeft = snapshot.getString("matchingSelectedLeft");
                    String selectedRight = snapshot.getString("matchingSelectedRight");
                    String updatedByUid = snapshot.getString("matchingUpdatedByUid");

                    if (!isMyTurn
                            && updatedByUid != null
                            && !updatedByUid.equals(currentUid)
                            && selectedLeft != null
                            && selectedRight != null) {
                        showOpponentMatch(selectedLeft, selectedRight);
                    }
                });
    }

    private void resetMatchingBoardForNewRound() {
        selectedLeftItem = null;
        connectedPairs = 0;
        correctMatchesInCurrentRound = 0;

        TextView tvConnectedCount = findViewById(R.id.tvConnectedCount);
        tvConnectedCount.setText("🔗 0 / 5");

        for (TextView leftItem : leftItems) {
            leftItem.setEnabled(false);
            leftItem.setBackgroundResource(R.drawable.bg_stat_card);
        }

        for (TextView rightItem : rightItems) {
            rightItem.setEnabled(false);
            rightItem.setBackgroundResource(R.drawable.bg_stat_card);
        }

        lastShownOpponentMatch = "";
    }

    private void updateTurnUi() {
        if (leftItems == null || rightItems == null) {
            return;
        }

        for (TextView leftItem : leftItems) {
            leftItem.setEnabled(isMyTurn);
        }

        for (TextView rightItem : rightItems) {
            rightItem.setEnabled(isMyTurn);
        }

        if (!isMyTurn) {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            tvTimer.setText("⏱ Waiting");
        } else {
            tvTimer.setText("⏱ " + (timeLeftMillis / 1000) + "s");
        }
    }

    private void showOpponentMatch(String selectedLeft, String selectedRight) {

        String matchKey = selectedLeft + "|" + selectedRight;

        if (matchKey.equals(lastShownOpponentMatch)) {
            return;
        }

        lastShownOpponentMatch = matchKey;

        TextView leftView = null;
        TextView rightView = null;

        for (TextView item : leftItems) {
            if (item.getText().toString().equals(selectedLeft)) {
                leftView = item;
                break;
            }
        }

        for (TextView item : rightItems) {
            if (item.getText().toString().equals(selectedRight)) {
                rightView = item;
                break;
            }
        }

        if (leftView == null || rightView == null) {
            return;
        }

        String correctRightText = correctMatches.get(selectedLeft);
        boolean isCorrect = correctRightText != null && correctRightText.equals(selectedRight);

        if (isCorrect) {
            leftView.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
            rightView.setBackgroundResource(R.drawable.bg_quiz_answer_correct);
        } else {
            leftView.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
            rightView.setBackgroundResource(R.drawable.bg_quiz_answer_wrong);
        }

        leftView.setEnabled(false);
        rightView.setEnabled(false);
    }

    private void initializeMatchingState() {

        db.collection("games")
                .document(gameId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    Long round = snapshot.getLong("matchingRound");

                    if (round != null) {
                        return;
                    }

                    String player1 = snapshot.getString("player1");
                    String starter = opponentAlreadyLeft ? currentUid : player1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("matchingRound", 1);
                    updates.put("matchingPhase", "starter");
                    updates.put("matchingStarterUid", starter);
                    updates.put("matchingCurrentPlayerUid", starter);

                    db.collection("games")
                            .document(gameId)
                            .update(updates);
                });
    }

    private void finishMatchingTurn() {
        if (!isMultiplayer || gameId == null) {
            showEndDialog();
            return;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        db.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        return;
                    }

                    String player1 = snapshot.getString("player1");
                    String player2 = snapshot.getString("player2");

                    if (player1 == null || player2 == null) {
                        return;
                    }

                    String opponentUid = currentUid.equals(player1) ? player2 : player1;

                    if ("starter".equals(matchingPhase) && correctMatchesInCurrentRound < 5)  {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("matchingPhase", "opponent_chance");
                        updates.put("matchingCurrentPlayerUid", opponentUid);
                        updates.put("matchingConnectedPairs", connectedPairs);
                        db.collection("games").document(gameId).update(updates);
                        return;
                    }

                    if (matchingRound == 1) {
                        String nextStarter = player2;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("matchingRound", 2);
                        updates.put("matchingPhase", "starter");
                        updates.put("matchingStarterUid", nextStarter);
                        updates.put("matchingCurrentPlayerUid", nextStarter);
                        updates.put("matchingConnectedPairs", 0);
                        updates.put("matchingSelectedLeft", null);
                        updates.put("matchingSelectedRight", null);
                        updates.put("matchingUpdatedByUid", null);

                        db.collection("games").document(gameId).update(updates);
                        return;
                    }



                    Map<String, Object> updates = new HashMap<>();
                    updates.put("matchingPhase", "finished");
                    updates.put("matchingCurrentPlayerUid", null);

                    db.collection("games").document(gameId)
                            .update(updates)
                            .addOnSuccessListener(unused -> showEndDialog());
                });
    }

    private void loadOpponentInfo() {
        if (!isMultiplayer || gameId == null || currentUid == null) {
            return;
        }

        db.collection("games")
                .document(gameId)
                .get()
                .addOnSuccessListener(gameSnapshot -> {
                    String player1 = gameSnapshot.getString("player1");
                    String player2 = gameSnapshot.getString("player2");

                    if (player1 == null || player2 == null) {
                        return;
                    }

                    String opponentUid = currentUid.equals(player1) ? player2 : player1;

                    db.collection("users")
                            .document(opponentUid)
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                if (!userSnapshot.exists()) {
                                    Toast.makeText(this, "Opponent user document not found", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                String username = userSnapshot.getString("username");
                                String avatar = userSnapshot.getString("avatar");

                                tvOpponentName.setText(username != null ? username : "Opponent");
                                imgOpponentAvatar.setImageResource(getAvatarResource(avatar));

                                Long tokens = userSnapshot.getLong("tokens");
                                Long stars = userSnapshot.getLong("stars");
                                Long league = userSnapshot.getLong("league");

                                long tokensValue = tokens != null ? tokens : 0;
                                long starsValue = stars != null ? stars : 0;
                                long leagueValue = league != null ? league : 0;

                                tvOpponentInfo.setText(
                                        "🪙" + tokensValue +
                                                " ⭐" + starsValue +
                                                " L" + leagueValue
                                );
                            });
                });
    }


}
