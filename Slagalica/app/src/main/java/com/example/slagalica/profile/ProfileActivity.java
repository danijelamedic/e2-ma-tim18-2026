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
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvQuizOverviewPercent;
    private TextView tvMatchingOverviewPercent;
    private TextView tvOverallSuccess;
    private TextView tvOverallPlayedGames;
    private TextView tvOverallWinLoss;

    private ProgressBar progressQuizOverview;
    private ProgressBar progressMatchingOverview;

    private TextView tvUsername;
    private TextView tvLeague;
    private TextView tvTokens;
    private TextView tvStars;
    private TextView tvEmail;
    private TextView tvRegion;
    private TextView tvAvatar;

    private ImageView imgQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvUsername = findViewById(R.id.tvUsername);
        tvLeague = findViewById(R.id.tvLeague);
        tvTokens = findViewById(R.id.tvTokens);
        tvStars = findViewById(R.id.tvStars);
        tvEmail = findViewById(R.id.tvEmail);
        tvRegion = findViewById(R.id.tvRegion);
        tvAvatar = findViewById(R.id.tvAvatar);
        imgQrCode = findViewById(R.id.imgQrCode);

        tvQuizOverviewPercent = findViewById(R.id.tvQuizOverviewPercent);
        tvMatchingOverviewPercent = findViewById(R.id.tvMatchingOverviewPercent);

        progressQuizOverview = findViewById(R.id.progressQuizOverview);
        progressMatchingOverview = findViewById(R.id.progressMatchingOverview);

        tvOverallSuccess = findViewById(R.id.tvOverallSuccess);
        tvOverallPlayedGames = findViewById(R.id.tvOverallPlayedGames);
        tvOverallWinLoss = findViewById(R.id.tvOverallWinLoss);

        loadOverviewStatistics();
        loadOverallStatistics();
        loadUserProfile();

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

    private void loadUserProfile() {

        String userId = "jMwwl0MoswM7u5nifYChTng97jj1";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String username = document.getString("username");
                    String email = document.getString("email");
                    String region = document.getString("region");
                    String avatarUrl = document.getString("avatarUrl");

                    Long tokens = document.getLong("tokens");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");

                    tvUsername.setText(username != null ? username : "Unknown user");
                    tvEmail.setText(email != null ? "Email: " + email : "Email: /");
                    tvRegion.setText(region != null ? "Region: " + region : "Region: /");

                    tvTokens.setText(String.valueOf(tokens != null ? tokens : 0));
                    tvStars.setText(String.valueOf(stars != null ? stars : 0));

                    tvLeague.setText("League " + (league != null ? league : 0));

                    generateQrCode(userId);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        tvAvatar.setText("👤");
                    } else {
                        tvAvatar.setText("👤");
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void generateQrCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();

        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 300, 300);

            Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565);

            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            imgQrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }
}