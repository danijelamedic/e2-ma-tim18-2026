package com.example.slagalica.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.LoginActivity;
import com.example.slagalica.R;
import com.example.slagalica.profile.statistics.QuizStatisticsActivity;
import com.example.slagalica.profile.statistics.MatchingStatisticsActivity;
import com.example.slagalica.profile.statistics.AssociationsStatisticsActivity;
import com.example.slagalica.profile.statistics.SkockoStatisticsActivity;
import com.example.slagalica.profile.statistics.StepByStepStatisticsActivity;
import com.example.slagalica.profile.statistics.MyNumberStatisticsActivity;
import android.widget.ProgressBar;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvQuizOverviewPercent;
    private TextView tvMatchingOverviewPercent;
    private TextView tvOverallSuccess;
    private TextView tvOverallPlayedGames;
    private TextView tvOverallWinLoss;

    private ProgressBar progressQuizOverview;
    private ProgressBar progressMatchingOverview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvQuizOverviewPercent = findViewById(R.id.tvQuizOverviewPercent);
        tvMatchingOverviewPercent = findViewById(R.id.tvMatchingOverviewPercent);

        progressQuizOverview = findViewById(R.id.progressQuizOverview);
        progressMatchingOverview = findViewById(R.id.progressMatchingOverview);

        tvOverallSuccess = findViewById(R.id.tvOverallSuccess);
        tvOverallPlayedGames = findViewById(R.id.tvOverallPlayedGames);
        tvOverallWinLoss = findViewById(R.id.tvOverallWinLoss);

        loadOverviewStatistics();
        loadOverallStatistics();

        TextView btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.cardQuizStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, QuizStatisticsActivity.class)));

        findViewById(R.id.cardMatchingStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, MatchingStatisticsActivity.class)));

        findViewById(R.id.cardAssociationsStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, AssociationsStatisticsActivity.class)));

        findViewById(R.id.cardSkockoStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, SkockoStatisticsActivity.class)));

        findViewById(R.id.cardStepByStepStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, StepByStepStatisticsActivity.class)));

        findViewById(R.id.cardMyNumberStatistics).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, MyNumberStatisticsActivity.class)));
    }

    private void loadOverviewStatistics() {

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document("player1")
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    long quizCorrect =
                            document.getLong("quizCorrectAnswers") == null
                                    ? 0
                                    : document.getLong("quizCorrectAnswers");

                    long quizTotal =
                            document.getLong("quizTotalQuestions") == null
                                    ? 0
                                    : document.getLong("quizTotalQuestions");

                    int quizPercent = 0;

                    if (quizTotal > 0) {
                        quizPercent =
                                (int) ((quizCorrect * 100) / quizTotal);
                    }

                    long matchingCorrect =
                            document.getLong("matchingCorrectMatches") == null
                                    ? 0
                                    : document.getLong("matchingCorrectMatches");

                    long matchingTotal =
                            document.getLong("matchingTotalMatches") == null
                                    ? 0
                                    : document.getLong("matchingTotalMatches");

                    int matchingPercent = 0;

                    if (matchingTotal > 0) {
                        matchingPercent =
                                (int) ((matchingCorrect * 100) / matchingTotal);
                    }

                    tvQuizOverviewPercent.setText(quizPercent + "%");
                    progressQuizOverview.setProgress(quizPercent);

                    tvMatchingOverviewPercent.setText(matchingPercent + "%");
                    progressMatchingOverview.setProgress(matchingPercent);
                });
    }

    private void loadOverallStatistics() {

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document("player1")
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    long quizCorrect = document.getLong("quizCorrectAnswers") == null
                            ? 0 : document.getLong("quizCorrectAnswers");

                    long quizTotal = document.getLong("quizTotalQuestions") == null
                            ? 0 : document.getLong("quizTotalQuestions");

                    long matchingCorrect = document.getLong("matchingCorrectMatches") == null
                            ? 0 : document.getLong("matchingCorrectMatches");

                    long matchingTotal = document.getLong("matchingTotalMatches") == null
                            ? 0 : document.getLong("matchingTotalMatches");

                    long totalCorrect = quizCorrect + matchingCorrect;
                    long totalQuestions = quizTotal + matchingTotal;

                    int successPercent = 0;

                    if (totalQuestions > 0) {
                        successPercent =
                                (int) Math.round((totalCorrect * 100.0) / totalQuestions);
                    }

                    long quizPlayed = document.getLong("quizGamesPlayed") == null
                            ? 0 : document.getLong("quizGamesPlayed");

                    long matchingPlayed = document.getLong("matchingGamesPlayed") == null
                            ? 0 : document.getLong("matchingGamesPlayed");

                    long totalPlayed = quizPlayed + matchingPlayed;

                    long wins =
                            (document.getLong("quizGamesWon") == null ? 0 : document.getLong("quizGamesWon"))
                                    +
                                    (document.getLong("matchingGamesWon") == null ? 0 : document.getLong("matchingGamesWon"));

                    long losses =
                            (document.getLong("quizGamesLost") == null ? 0 : document.getLong("quizGamesLost"))
                                    +
                                    (document.getLong("matchingGamesLost") == null ? 0 : document.getLong("matchingGamesLost"));

                    long totalFinished = wins + losses;

                    int winPercent = 0;
                    int lossPercent = 0;

                    if (totalFinished > 0) {
                        winPercent =
                                (int) Math.round((wins * 100.0) / totalFinished);
                        lossPercent = 100 - winPercent;
                    }

                    tvOverallSuccess.setText("Success: " + successPercent + "%");
                    tvOverallPlayedGames.setText("Played games: " + totalPlayed);
                    tvOverallWinLoss.setText(
                            "Wins: " + winPercent + "% Losses: " + lossPercent + "%"
                    );
                });
    }
}