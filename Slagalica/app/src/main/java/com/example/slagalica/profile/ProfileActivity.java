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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.slagalica.ResetPasswordActivity;
import com.example.slagalica.HomeActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;

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
    private ImageView imgAvatar;
    private String currentAvatar = "owl";
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
        imgAvatar = findViewById(R.id.imgAvatar);
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
            FirebaseAuth.getInstance().signOut();

            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnResetPasswordProfile).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, ResetPasswordActivity.class))
        );

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

        imgAvatar.setOnClickListener(v -> openChangeAvatar());

        findViewById(R.id.btnEditAvatar).setOnClickListener(v -> openChangeAvatar());

        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class)));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "You are already on Profile", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, NotificationCenterActivity.class)));

        findViewById(R.id.navStatistics).setOnClickListener(v ->
                Toast.makeText(this, "Statistics screen coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navFriends).setOnClickListener(v ->
                Toast.makeText(this, "Friends screen coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                Toast.makeText(this, "Leaderboard screen coming soon", Toast.LENGTH_SHORT).show());
    }

    private void loadOverviewStatistics() {

        String userId = getCurrentUserId();

        if (userId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document(userId)
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

        String userId = getCurrentUserId();

        if (userId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("statistics")
                .document(userId)
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

                    long totalPlayed =
                            document.getLong("totalBattlesPlayed") == null
                                    ? 0
                                    : document.getLong("totalBattlesPlayed");

                    long wins =
                            document.getLong("battlesWon") == null
                                    ? 0
                                    : document.getLong("battlesWon");

                    long losses =
                            document.getLong("battlesLost") == null
                                    ? 0
                                    : document.getLong("battlesLost");

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

        String userId = getCurrentUserId();

        if (userId == null) {
            return;
        }

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
                    String avatar = document.getString("avatar");
                    currentAvatar = avatar != null ? avatar : "owl";

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

                    imgAvatar.setImageResource(getAvatarResource(avatar));
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

    private int getAvatarResource(String avatar) {
        if (avatar == null) {
            return R.drawable.avatar_owl;
        }

        switch (avatar) {
            case "fox":
                return R.drawable.avatar_fox;
            case "penguin":
                return R.drawable.avatar_penguin;
            case "wolf":
                return R.drawable.avatar_wolf;
            case "cat":
                return R.drawable.avatar_cat;
            case "dog":
                return R.drawable.avatar_dog;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }

    private void openChangeAvatar() {
        Intent intent = new Intent(ProfileActivity.this, ChangeAvatarActivity.class);
        intent.putExtra("currentAvatar", currentAvatar);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        loadOverviewStatistics();
        loadOverallStatistics();
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return null;
        }

        return user.getUid();
    }
}