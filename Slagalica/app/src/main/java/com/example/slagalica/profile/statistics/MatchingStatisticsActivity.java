package com.example.slagalica.profile.statistics;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.R;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MatchingStatisticsActivity extends AppCompatActivity {

    private TextView tvMatchingSuccess;
    private TextView tvSuccessfulPairs;
    private TextView tvFailedPairs;
    private TextView tvMatchingGamesPlayed;
    private TextView tvMatchingWinLoss;
    private ProgressBar progressMatching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matching_statistics);
        tvMatchingSuccess = findViewById(R.id.tvMatchingSuccess);
        tvSuccessfulPairs = findViewById(R.id.tvSuccessfulPairs);
        tvFailedPairs = findViewById(R.id.tvFailedPairs);
        tvMatchingGamesPlayed = findViewById(R.id.tvMatchingGamesPlayed);
        tvMatchingWinLoss = findViewById(R.id.tvMatchingWinLoss);
        progressMatching = findViewById(R.id.progressMatching);

        loadStatistics();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadStatistics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        db.collection("statistics")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        return;
                    }

                    long correctMatches =
                            document.getLong("matchingCorrectMatches") == null
                                    ? 0
                                    : document.getLong("matchingCorrectMatches");

                    long totalMatches =
                            document.getLong("matchingTotalMatches") == null
                                    ? 0
                                    : document.getLong("matchingTotalMatches");

                    long gamesPlayed =
                            document.getLong("matchingGamesPlayed") == null
                                    ? 0
                                    : document.getLong("matchingGamesPlayed");

                    long totalScore =
                            document.getLong("matchingTotalScore") == null
                                    ? 0
                                    : document.getLong("matchingTotalScore");

                    int averageScore = 0;
                    if (gamesPlayed > 0) {
                        averageScore = (int) Math.round((totalScore * 1.0) / gamesPlayed);
                    }

                    int successPercent = 0;

                    if (totalMatches > 0) {
                        successPercent =
                                (int) ((correctMatches * 100) / totalMatches);
                    }

                    int failedMatches =
                            (int) (totalMatches - correctMatches);

                    int wins = successPercent;
                    int losses = 100 - successPercent;

                    tvMatchingSuccess.setText(successPercent + "%");
                    progressMatching.setProgress(successPercent);

                    tvSuccessfulPairs.setText(
                            "Successfully connected terms: " + correctMatches);

                    tvFailedPairs.setText(
                            "Failed connections: " + failedMatches);

                    tvMatchingGamesPlayed.setText(
                            "Played matching games: " + gamesPlayed + "\nAverage score: " + averageScore + "/20");

                    tvMatchingWinLoss.setText(
                            "Wins: " + wins + "%  Losses: " + losses + "%");
                });
    }
}