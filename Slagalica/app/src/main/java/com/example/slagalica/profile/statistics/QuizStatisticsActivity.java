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
import com.google.firebase.firestore.FirebaseFirestore;

public class QuizStatisticsActivity extends AppCompatActivity {

    private TextView tvQuizSuccess;
    private TextView tvCorrectAnswers;
    private TextView tvWrongAnswers;
    private TextView tvQuizGamesPlayed;
    private ProgressBar progressQuiz;
    private TextView tvQuizWinLoss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_statistics);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvQuizSuccess = findViewById(R.id.tvQuizSuccess);
        tvCorrectAnswers = findViewById(R.id.tvCorrectAnswers);
        tvWrongAnswers = findViewById(R.id.tvWrongAnswers);
        tvQuizGamesPlayed = findViewById(R.id.tvQuizGamesPlayed);
        progressQuiz = findViewById(R.id.progressQuiz);
        tvQuizWinLoss = findViewById(R.id.tvQuizWinLoss);

        loadQuizStatistics();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadQuizStatistics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("statistics")
                .document("player1")
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    long gamesPlayed = getLongValue(document.getLong("quizGamesPlayed"));
                    long correctAnswers = getLongValue(document.getLong("quizCorrectAnswers"));
                    long totalQuestions = getLongValue(document.getLong("quizTotalQuestions"));
                    long wrongAnswers = totalQuestions - correctAnswers;

                    long gamesWon = getLongValue(document.getLong("quizGamesWon"));
                    long gamesLost = getLongValue(document.getLong("quizGamesLost"));
                    long totalGames = gamesWon + gamesLost;

                    int successPercent = 0;
                    if (totalQuestions > 0) {
                        successPercent = (int) Math.round((correctAnswers * 100.0) / totalQuestions);
                    }

                    int winPercent = 0;
                    int lossPercent = 0;
                    if (totalGames > 0) {
                        winPercent = (int) Math.round((gamesWon * 100.0) / totalGames);
                        lossPercent = 100 - winPercent;
                    }

                    tvQuizSuccess.setText(successPercent + "%");
                    progressQuiz.setProgress(successPercent);
                    tvCorrectAnswers.setText("Correct answers: " + correctAnswers);
                    tvWrongAnswers.setText("Wrong answers: " + wrongAnswers);
                    tvQuizGamesPlayed.setText("Played quiz games: " + gamesPlayed);
                    tvQuizWinLoss.setText("Wins: " + winPercent + "% Losses: " + lossPercent + "%");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load quiz statistics.", Toast.LENGTH_SHORT).show()
                );
    }
    private long getLongValue(Long value) {
        return value == null ? 0 : value;
    }
}