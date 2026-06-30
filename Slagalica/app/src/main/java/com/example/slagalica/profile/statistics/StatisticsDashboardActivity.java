package com.example.slagalica.profile.statistics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StatisticsDashboardActivity extends AppCompatActivity {

    private TextView tvOverallSuccess, tvPlayedMiniGames, tvBattles, tvWinsLosses;
    private TextView tvQuizPercent, tvMatchingPercent, tvAssociationsPercent;
    private TextView tvSkockoPercent, tvStepByStepPercent, tvMyNumberPercent;
    private TextView tvBestGame, tvWeakestGame;
    private ProgressBar progressOverall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_dashboard);

        bindViews();
        setupActions();
        setupBottomNavigation();
        loadStatistics();
    }

    private void bindViews() {
        tvOverallSuccess = findViewById(R.id.tvOverallSuccess);
        tvPlayedMiniGames = findViewById(R.id.tvPlayedMiniGames);
        tvBattles = findViewById(R.id.tvBattles);
        tvWinsLosses = findViewById(R.id.tvWinsLosses);

        tvQuizPercent = findViewById(R.id.tvQuizPercent);
        tvMatchingPercent = findViewById(R.id.tvMatchingPercent);
        tvAssociationsPercent = findViewById(R.id.tvAssociationsPercent);
        tvSkockoPercent = findViewById(R.id.tvSkockoPercent);
        tvStepByStepPercent = findViewById(R.id.tvStepByStepPercent);
        tvMyNumberPercent = findViewById(R.id.tvMyNumberPercent);

        tvBestGame = findViewById(R.id.tvBestGame);
        tvWeakestGame = findViewById(R.id.tvWeakestGame);
        progressOverall = findViewById(R.id.progressOverall);
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadStatistics() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showEmptyStatistics();
                        return;
                    }

                    long quizGames = getLong(document.getLong("quizGamesPlayed"));
                    long quizCorrect = getLong(document.getLong("quizCorrectAnswers"));
                    long quizTotal = getLong(document.getLong("quizTotalQuestions"));
                    int quizPercent = percent(quizCorrect, quizTotal);

                    long matchingGames = getLong(document.getLong("matchingGamesPlayed"));
                    long matchingCorrect = getLong(document.getLong("matchingCorrectMatches"));
                    long matchingTotal = getLong(document.getLong("matchingTotalMatches"));
                    int matchingPercent = percent(matchingCorrect, matchingTotal);

                    long associationsGames = getLong(document.getLong("associationsGamesPlayed"));
                    long associationsSolved = getLong(document.getLong("associationsSolved"));
                    int associationsPercent = percent(associationsSolved, associationsGames);

                    long skockoGames = getLong(document.getLong("skockoGamesPlayed"));
                    long skockoHits =
                            getLong(document.getLong("attempt1Hits")) +
                                    getLong(document.getLong("attempt2Hits")) +
                                    getLong(document.getLong("attempt3Hits")) +
                                    getLong(document.getLong("attempt4Hits")) +
                                    getLong(document.getLong("attempt5Hits")) +
                                    getLong(document.getLong("attempt6Hits"));
                    int skockoPercent = percent(skockoHits, skockoGames);

                    long stepGames = getLong(document.getLong("stepByStepGamesPlayed"));
                    long stepHits =
                            getLong(document.getLong("step1Hits")) +
                                    getLong(document.getLong("step2Hits")) +
                                    getLong(document.getLong("step3Hits")) +
                                    getLong(document.getLong("step4Hits")) +
                                    getLong(document.getLong("step5Hits")) +
                                    getLong(document.getLong("step6Hits")) +
                                    getLong(document.getLong("step7Hits"));
                    int stepPercent = percent(stepHits, stepGames);

                    long myNumberGames = getLong(document.getLong("myNumberGamesPlayed"));
                    long myNumberExact = getLong(document.getLong("myNumberExactHits"));
                    int myNumberPercent = percent(myNumberExact, myNumberGames);

                    long playedMiniGames = quizGames + matchingGames + associationsGames
                            + skockoGames + stepGames + myNumberGames;

                    long totalBattles = getLong(document.getLong("totalBattlesPlayed"));
                    long battlesWon = getLong(document.getLong("battlesWon"));
                    long battlesLost = getLong(document.getLong("battlesLost"));

                    int overallSuccess = averageOfExisting(
                            new int[]{quizPercent, matchingPercent, associationsPercent,
                                    skockoPercent, stepPercent, myNumberPercent},
                            new long[]{quizGames, matchingGames, associationsGames,
                                    skockoGames, stepGames, myNumberGames}
                    );

                    tvOverallSuccess.setText(overallSuccess + "%");
                    progressOverall.setProgress(overallSuccess);
                    tvPlayedMiniGames.setText("Played mini-games: " + playedMiniGames);
                    tvBattles.setText("Total battles: " + totalBattles);
                    tvWinsLosses.setText("Wins: " + battlesWon + "   Losses: " + battlesLost);

                    tvQuizPercent.setText("Quiz: " + quizPercent + "%");
                    tvMatchingPercent.setText("Matching: " + matchingPercent + "%");
                    tvAssociationsPercent.setText("Associations: " + associationsPercent + "%");
                    tvSkockoPercent.setText("Skočko: " + skockoPercent + "%");
                    tvStepByStepPercent.setText("Step by step: " + stepPercent + "%");
                    tvMyNumberPercent.setText("My number: " + myNumberPercent + "%");

                    showBestAndWeakest(
                            new String[]{"Quiz", "Matching", "Associations", "Skočko", "Step by step", "My number"},
                            new int[]{quizPercent, matchingPercent, associationsPercent, skockoPercent, stepPercent, myNumberPercent},
                            new long[]{quizGames, matchingGames, associationsGames, skockoGames, stepGames, myNumberGames}
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load statistics.", Toast.LENGTH_SHORT).show()
                );
    }

    private void showEmptyStatistics() {
        tvOverallSuccess.setText("0%");
        progressOverall.setProgress(0);
        tvPlayedMiniGames.setText("Played mini-games: 0");
        tvBattles.setText("Total battles: 0");
        tvWinsLosses.setText("Wins: 0   Losses: 0");
        tvBestGame.setText("Best game: -");
        tvWeakestGame.setText("Needs practice: -");
    }

    private void showBestAndWeakest(String[] names, int[] values, long[] games) {
        int bestIndex = -1;
        int weakestIndex = -1;

        for (int i = 0; i < values.length; i++) {
            if (games[i] <= 0) continue;

            if (bestIndex == -1 || values[i] > values[bestIndex]) {
                bestIndex = i;
            }

            if (weakestIndex == -1 || values[i] < values[weakestIndex]) {
                weakestIndex = i;
            }
        }

        tvBestGame.setText(bestIndex == -1
                ? "Best game: -"
                : "Best game: " + names[bestIndex] + " (" + values[bestIndex] + "%)");

        tvWeakestGame.setText(weakestIndex == -1
                ? "Needs practice: -"
                : "Needs practice: " + names[weakestIndex] + " (" + values[weakestIndex] + "%)");
    }

    private int averageOfExisting(int[] values, long[] games) {
        int sum = 0;
        int count = 0;

        for (int i = 0; i < values.length; i++) {
            if (games[i] > 0) {
                sum += values[i];
                count++;
            }
        }

        return count > 0 ? Math.round(sum * 1.0f / count) : 0;
    }

    private int percent(long value, long total) {
        return total > 0 ? (int) Math.round(value * 100.0 / total) : 0;
    }

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class)));

        findViewById(R.id.navFriends).setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));

        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.navStatistics).setOnClickListener(v ->
                Toast.makeText(this, "You are already on Statistics", Toast.LENGTH_SHORT).show());
    }
}