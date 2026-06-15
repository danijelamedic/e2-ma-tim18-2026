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

public class MyNumberStatisticsActivity extends AppCompatActivity {

    private TextView tvMyNumberSuccess;
    private TextView tvMyNumberExactFound;
    private TextView tvMyNumberNotExact;
    private TextView tvMyNumberGamesPlayed;
    private TextView tvMyNumberAverageScore;
    private ProgressBar progressMyNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_number_statistics);

        tvMyNumberSuccess = findViewById(R.id.tvMyNumberSuccess);
        tvMyNumberExactFound = findViewById(R.id.tvMyNumberExactFound);
        tvMyNumberNotExact = findViewById(R.id.tvMyNumberNotExact);
        tvMyNumberGamesPlayed = findViewById(R.id.tvMyNumberGamesPlayed);
        tvMyNumberAverageScore = findViewById(R.id.tvMyNumberAverageScore);
        progressMyNumber = findViewById(R.id.progressMyNumber);

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

                    long gamesPlayed = getLong(document.getLong("myNumberGamesPlayed"));
                    long exactHits = getLong(document.getLong("myNumberExactHits"));
                    long totalScore = getLong(document.getLong("myNumberTotalScore"));

                    long notExact = gamesPlayed - exactHits;

                    int successPercent = 0;
                    if (gamesPlayed > 0) {
                        successPercent = (int) Math.round((exactHits * 100.0) / gamesPlayed);
                    }

                    int averageScore = 0;
                    if (gamesPlayed > 0) {
                        averageScore = (int) Math.round((totalScore * 1.0) / gamesPlayed);
                    }

                    tvMyNumberSuccess.setText(successPercent + "%");
                    progressMyNumber.setProgress(successPercent);

                    tvMyNumberExactFound.setText("Exact number found: " + exactHits);
                    tvMyNumberNotExact.setText("Not exact: " + notExact);
                    tvMyNumberGamesPlayed.setText("Played My Number games: " + gamesPlayed);
                    tvMyNumberAverageScore.setText("Average score: " + averageScore + "/20");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load My Number statistics.", Toast.LENGTH_SHORT).show()
                );
    }

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }
}