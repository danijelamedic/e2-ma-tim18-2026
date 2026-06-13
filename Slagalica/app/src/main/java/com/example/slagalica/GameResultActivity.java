package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.notifications.NotificationFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class GameResultActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private NotificationFactory notificationFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_result);

        db = FirebaseFirestore.getInstance();
        notificationFactory = new NotificationFactory();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        long myScore = getIntent().getLongExtra("myScore", 0);
        long opponentScore = getIntent().getLongExtra("opponentScore", 0);
        boolean isFriendly = getIntent().getBooleanExtra("isFriendly", false);

        TextView tvResult = findViewById(R.id.tvResult);
        TextView tvScores = findViewById(R.id.tvScores);
        TextView tvStars = findViewById(R.id.tvStars);

        boolean won = myScore > opponentScore;

        if (myScore > opponentScore) {
            tvResult.setText("You Win! 🏆");
        } else if (myScore < opponentScore) {
            tvResult.setText("You Lose!");
        } else {
            tvResult.setText("Draw!");
        }

        tvScores.setText("Your score: " + myScore + "\nOpponent score: " + opponentScore);

        if (!isFriendly) {
            updateStarsAndTokens(won, myScore, tvStars);
        } else {
            tvStars.setText("Friendly match - no stars awarded");
        }
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void updateStarsAndTokens(boolean won, long myScore, TextView tvStars) {
        int starsFromScore = (int)(myScore / 40);

        int starsDelta;
        if (won) {
            starsDelta = 10 + starsFromScore;
        } else {
            starsDelta = -10 + starsFromScore;
        }

        String resultText = won ?
                "+" + starsDelta + " stars" :
                (starsDelta >= 0 ? "+" + starsDelta : starsDelta) + " stars";
        tvStars.setText(resultText);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    long currentStars = snapshot.getLong("stars") != null ?
                            snapshot.getLong("stars") : 0;
                    long oldLeague = snapshot.getLong("league") != null ?
                            snapshot.getLong("league") : 0;

                    long newStars = Math.max(0, currentStars + starsDelta);

                    long tokensEarned = newStars / 50 - currentStars / 50;
                    int newLeague = calculateLeague(newStars);

                    db.collection("users").document(currentUid)
                            .update("stars", newStars,
                                    "tokens", FieldValue.increment(tokensEarned),
                                    "gamesPlayed", FieldValue.increment(1),
                                    "league", newLeague)
                            .addOnSuccessListener(unused -> {
                                if (tokensEarned > 0) {
                                    tvStars.append("\n+" + tokensEarned + " token(s) earned!");
                                }
                                notificationFactory.sendReward(this, currentUid, starsDelta, tokensEarned);
                                if (newLeague != oldLeague) {
                                    notificationFactory.sendLeagueChange(this, currentUid, oldLeague, newLeague);
                                }
                            });
                });
    }

    private int calculateLeague(long stars) {
        int[] leagueThresholds = {0, 100, 200, 400, 800, 1600};
        for (int i = leagueThresholds.length - 1; i >= 0; i--) {
            if (stars >= leagueThresholds[i]) {
                return i;
            }
        }
        return 0;
    }
}
