package com.example.slagalica.profile.statistics;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SkockoStatisticsActivity extends AppCompatActivity {

    private TextView tvSkockoSuccess, tvSkockoGamesPlayed, tvSkockoAverageScore;
    private TextView tvAttempt1Percent, tvAttempt2Percent, tvAttempt3Percent;
    private TextView tvAttempt4Percent, tvAttempt5Percent, tvAttempt6Percent;
    private ProgressBar progressSkocko;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skocko_statistics);

        tvSkockoSuccess = findViewById(R.id.tvSkockoSuccess);
        progressSkocko = findViewById(R.id.progressSkocko);

        tvAttempt1Percent = findViewById(R.id.tvAttempt1Percent);
        tvAttempt2Percent = findViewById(R.id.tvAttempt2Percent);
        tvAttempt3Percent = findViewById(R.id.tvAttempt3Percent);
        tvAttempt4Percent = findViewById(R.id.tvAttempt4Percent);
        tvAttempt5Percent = findViewById(R.id.tvAttempt5Percent);
        tvAttempt6Percent = findViewById(R.id.tvAttempt6Percent);

        tvSkockoGamesPlayed = findViewById(R.id.tvSkockoGamesPlayed);
        tvSkockoAverageScore = findViewById(R.id.tvSkockoAverageScore);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadStatistics();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadStatistics() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    long gamesPlayed = getLong(document.getLong("skockoGamesPlayed"));
                    long totalScore = getLong(document.getLong("skockoTotalScore"));

                    long attempt1 = getLong(document.getLong("attempt1Hits"));
                    long attempt2 = getLong(document.getLong("attempt2Hits"));
                    long attempt3 = getLong(document.getLong("attempt3Hits"));
                    long attempt4 = getLong(document.getLong("attempt4Hits"));
                    long attempt5 = getLong(document.getLong("attempt5Hits"));
                    long attempt6 = getLong(document.getLong("attempt6Hits"));

                    long totalHits = attempt1 + attempt2 + attempt3 + attempt4 + attempt5 + attempt6;

                    int successPercent = gamesPlayed > 0
                            ? (int) Math.round((totalHits * 100.0) / gamesPlayed)
                            : 0;

                    int averageScore = gamesPlayed > 0
                            ? (int) Math.round((totalScore * 1.0) / gamesPlayed)
                            : 0;

                    tvSkockoSuccess.setText(successPercent + "%");
                    progressSkocko.setProgress(successPercent);

                    tvAttempt1Percent.setText("Attempt 1: " + percent(attempt1, gamesPlayed) + "%");
                    tvAttempt2Percent.setText("Attempt 2: " + percent(attempt2, gamesPlayed) + "%");
                    tvAttempt3Percent.setText("Attempt 3: " + percent(attempt3, gamesPlayed) + "%");
                    tvAttempt4Percent.setText("Attempt 4: " + percent(attempt4, gamesPlayed) + "%");
                    tvAttempt5Percent.setText("Attempt 5: " + percent(attempt5, gamesPlayed) + "%");
                    tvAttempt6Percent.setText("Attempt 6: " + percent(attempt6, gamesPlayed) + "%");

                    tvSkockoGamesPlayed.setText("Played Skocko games: " + gamesPlayed);
                    tvSkockoAverageScore.setText("Average score: " + averageScore + "/40");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load Skocko statistics.", Toast.LENGTH_SHORT).show()
                );
    }

    private int percent(long value, long total) {
        return total > 0 ? (int) Math.round((value * 100.0) / total) : 0;
    }

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }
}