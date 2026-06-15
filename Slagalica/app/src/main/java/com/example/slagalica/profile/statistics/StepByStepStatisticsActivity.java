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

public class StepByStepStatisticsActivity extends AppCompatActivity {

    private TextView tvStepByStepSuccess, tvStepByStepGamesPlayed, tvStepByStepAverageScore;
    private TextView tvStep1Percent, tvStep2Percent, tvStep3Percent, tvStep4Percent;
    private TextView tvStep5Percent, tvStep6Percent, tvStep7Percent;
    private ProgressBar progressStepByStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_step_by_step_statistics);

        tvStepByStepSuccess = findViewById(R.id.tvStepByStepSuccess);
        progressStepByStep = findViewById(R.id.progressStepByStep);
        tvStepByStepGamesPlayed = findViewById(R.id.tvStepByStepGamesPlayed);
        tvStepByStepAverageScore = findViewById(R.id.tvStepByStepAverageScore);

        tvStep1Percent = findViewById(R.id.tvStep1Percent);
        tvStep2Percent = findViewById(R.id.tvStep2Percent);
        tvStep3Percent = findViewById(R.id.tvStep3Percent);
        tvStep4Percent = findViewById(R.id.tvStep4Percent);
        tvStep5Percent = findViewById(R.id.tvStep5Percent);
        tvStep6Percent = findViewById(R.id.tvStep6Percent);
        tvStep7Percent = findViewById(R.id.tvStep7Percent);

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

                    long gamesPlayed = getLong(document.getLong("stepByStepGamesPlayed"));
                    long totalScore = getLong(document.getLong("stepByStepTotalScore"));

                    long step1 = getLong(document.getLong("step1Hits"));
                    long step2 = getLong(document.getLong("step2Hits"));
                    long step3 = getLong(document.getLong("step3Hits"));
                    long step4 = getLong(document.getLong("step4Hits"));
                    long step5 = getLong(document.getLong("step5Hits"));
                    long step6 = getLong(document.getLong("step6Hits"));
                    long step7 = getLong(document.getLong("step7Hits"));

                    long totalHits = step1 + step2 + step3 + step4 + step5 + step6 + step7;

                    int successPercent = gamesPlayed > 0
                            ? (int) Math.round((totalHits * 100.0) / gamesPlayed)
                            : 0;

                    int averageScore = gamesPlayed > 0
                            ? (int) Math.round((totalScore * 1.0) / gamesPlayed)
                            : 0;

                    tvStepByStepSuccess.setText(successPercent + "%");
                    progressStepByStep.setProgress(successPercent);

                    tvStepByStepGamesPlayed.setText("Played Step By Step games: " + gamesPlayed);
                    tvStepByStepAverageScore.setText("Average score: " + averageScore + "/40");

                    tvStep1Percent.setText("Step 1: " + percent(step1, gamesPlayed) + "%");
                    tvStep2Percent.setText("Step 2: " + percent(step2, gamesPlayed) + "%");
                    tvStep3Percent.setText("Step 3: " + percent(step3, gamesPlayed) + "%");
                    tvStep4Percent.setText("Step 4: " + percent(step4, gamesPlayed) + "%");
                    tvStep5Percent.setText("Step 5: " + percent(step5, gamesPlayed) + "%");
                    tvStep6Percent.setText("Step 6: " + percent(step6, gamesPlayed) + "%");
                    tvStep7Percent.setText("Step 7: " + percent(step7, gamesPlayed) + "%");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load Step By Step statistics.", Toast.LENGTH_SHORT).show()
                );
    }

    private int percent(long value, long total) {
        return total > 0 ? (int) Math.round((value * 100.0) / total) : 0;
    }

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }
}