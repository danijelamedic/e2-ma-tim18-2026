package com.example.slagalica.tournament;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.daily.DailyMission;
import com.example.slagalica.daily.DailyMissionRepository;
import com.example.slagalica.notifications.NotificationFactory;
import com.google.firebase.auth.FirebaseAuth;

public class TournamentResultActivity extends AppCompatActivity {

    private final TournamentRepository repository = new TournamentRepository();
    private String tournamentId;
    private String round;
    private String currentUid;
    private Button continueButton;
    private boolean won;
    private boolean abandonedMatch;
    private boolean rewardNotificationSent = false;
    private long myScore;
    private final DailyMissionRepository dailyMissionRepository = new DailyMissionRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tournamentId = getIntent().getStringExtra("tournamentId");
        round = getIntent().getStringExtra("tournamentRound");

        myScore = getIntent().getLongExtra("myScore", 0);
        long opponentScore = getIntent().getLongExtra("opponentScore", 0);
        abandonedMatch = getIntent().getBooleanExtra("abandonedMatch", false);
        boolean opponentAbandoned = getIntent().getBooleanExtra("opponentAbandoned", false);
        won = opponentAbandoned || (!abandonedMatch && myScore >= opponentScore);

        buildLayout(won, abandonedMatch, opponentAbandoned, myScore, opponentScore);
        reportResult();
    }

    private void buildLayout(boolean won, boolean abandonedMatch, boolean opponentAbandoned,
                             long myScore, long opponentScore) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(0xFFF6F3FF);

        TextView title = new TextView(this);
        title.setText(resultTitle(won, abandonedMatch, opponentAbandoned));
        title.setTextColor(won ? 0xFF126A32 : 0xFF7A1F1F);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_in));
        root.addView(title, matchWrap());

        TextView score = new TextView(this);
        score.setText("Your score: " + myScore + "\nOpponent score: " + opponentScore);
        score.setTextColor(0xFF2D1B5E);
        score.setTextSize(18);
        score.setGravity(Gravity.CENTER);
        score.setPadding(0, dp(18), 0, dp(18));
        root.addView(score, matchWrap());

        TextView rewards = new TextView(this);
        rewards.setText(rewardText(won, abandonedMatch));
        rewards.setTextColor(0xFF4C2A91);
        rewards.setTextSize(16);
        rewards.setGravity(Gravity.CENTER);
        rewards.setPadding(0, 0, 0, dp(18));
        root.addView(rewards, matchWrap());

        continueButton = new Button(this);
        continueButton.setText("Saving result...");
        continueButton.setEnabled(false);
        continueButton.setTextColor(0xFFFFFFFF);
        continueButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        continueButton.setBackgroundResource(R.drawable.bg_purple_button);
        root.addView(continueButton, matchWrap());

        setContentView(root);
    }

    private String resultTitle(boolean won, boolean abandonedMatch, boolean opponentAbandoned) {
        if (abandonedMatch) return "You left the tournament";
        if (opponentAbandoned) return "Opponent left - you advance";
        if (won) return TournamentRepository.ROUND_FINAL.equals(round)
                ? "Tournament champion!" : "You won the semifinal!";
        return TournamentRepository.ROUND_FINAL.equals(round)
                ? "Final finished" : "You are eliminated";
    }

    private String rewardText(boolean won, boolean abandonedMatch) {
        if (abandonedMatch) {
            return "No rewards and tournament entry tokens are not refunded.";
        }
        if (TournamentRepository.ROUND_SEMIFINAL.equals(round)) {
            return won
                    ? "+2 tokens and regular winner stars. Final awaits."
                    : "Semifinal loss gives no tournament rewards.";
        }
        return won
                ? "+3 tokens, regular winner stars, and +10 bonus stars."
                : "Final loss gives regular loser stars.";
    }

    private void reportResult() {
        String gameId = getIntent().getStringExtra("gameId");
        if (gameId == null) {
            enableContinue();
            return;
        }

        repository.reportMatchResult(gameId, new TournamentRepository.MatchReportCallback() {
            @Override
            public void onComplete(TournamentRepository.MatchReportResult result) {
                if (result.resultRecorded && won && !abandonedMatch) {
                    dailyMissionRepository.completeMission(TournamentResultActivity.this,
                            currentUid, DailyMission.WIN_TOURNAMENT_MATCH);
                }
                sendRewardNotificationIfNeeded();
                enableContinue();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TournamentResultActivity.this, message, Toast.LENGTH_LONG).show();
                enableContinue();
            }
        });
    }

    private void sendRewardNotificationIfNeeded() {
        if (rewardNotificationSent || abandonedMatch) {
            return;
        }

        String message = null;
        int regularStars = regularStars(won, myScore);
        if (TournamentRepository.ROUND_SEMIFINAL.equals(round) && won) {
            message = "You won the tournament semifinal and earned +2 tokens and "
                    + formatStars(regularStars) + " regular winner stars.";
        } else if (TournamentRepository.ROUND_FINAL.equals(round) && won) {
            message = "You won the tournament and earned +3 tokens, "
                    + formatStars(regularStars) + " regular winner stars, and +10 bonus stars.";
        } else if (TournamentRepository.ROUND_FINAL.equals(round)) {
            message = "You lose the tournament final and received "
                    + formatStars(regularStars) + " stars.";
        }

        if (message != null) {
            rewardNotificationSent = true;
            String notificationId = "tournament_reward_" + tournamentId + "_" + round + "_" + currentUid;
            new NotificationFactory().sendTournamentReward(this, currentUid, notificationId, message);
        }
    }

    private int regularStars(boolean won, long score) {
        int fromScore = (int) (score / 40);
        return won ? 10 + fromScore : -10 + fromScore;
    }

    private String formatStars(int stars) {
        return stars >= 0 ? "+" + stars : String.valueOf(stars);
    }

    private void enableContinue() {
        continueButton.setEnabled(true);
        continueButton.setText(tournamentId != null ? "Open bracket" : "Home");
        continueButton.setOnClickListener(v -> {
            Intent intent = tournamentId != null
                    ? new Intent(this, TournamentActivity.class).putExtra("tournamentId", tournamentId)
                    : new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(8));
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
