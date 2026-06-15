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

public class AssociationsStatisticsActivity extends AppCompatActivity {

    private TextView tvAssociationsSuccess;
    private TextView tvAssociationsSolved;
    private TextView tvAssociationsUnsolved;
    private TextView tvAssociationsGamesPlayed;
    private TextView tvAssociationsAverageScore;
    private ProgressBar progressAssociations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_associations_statistics);

        tvAssociationsSuccess = findViewById(R.id.tvAssociationsSuccess);
        tvAssociationsSolved = findViewById(R.id.tvAssociationsSolved);
        tvAssociationsUnsolved = findViewById(R.id.tvAssociationsUnsolved);
        tvAssociationsGamesPlayed = findViewById(R.id.tvAssociationsGamesPlayed);
        tvAssociationsAverageScore = findViewById(R.id.tvAssociationsAverageScore);
        progressAssociations = findViewById(R.id.progressAssociations);

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

                    long gamesPlayed = getLong(document.getLong("associationsGamesPlayed"));
                    long solved = getLong(document.getLong("associationsSolved"));
                    long unsolved = getLong(document.getLong("associationsUnsolved"));
                    long totalScore = getLong(document.getLong("associationsTotalScore"));

                    int successPercent = 0;
                    if (gamesPlayed > 0) {
                        successPercent = (int) Math.round((solved * 100.0) / gamesPlayed);
                    }

                    int averageScore = 0;
                    if (gamesPlayed > 0) {
                        averageScore = (int) Math.round((totalScore * 1.0) / gamesPlayed);
                    }

                    tvAssociationsSuccess.setText(successPercent + "%");
                    progressAssociations.setProgress(successPercent);

                    tvAssociationsSolved.setText("Solved associations: " + solved);
                    tvAssociationsUnsolved.setText("Unsolved associations: " + unsolved);
                    tvAssociationsGamesPlayed.setText("Played associations games: " + gamesPlayed);
                    tvAssociationsAverageScore.setText("Average score: " + averageScore + "/60");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load associations statistics.", Toast.LENGTH_SHORT).show()
                );
    }

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }
}