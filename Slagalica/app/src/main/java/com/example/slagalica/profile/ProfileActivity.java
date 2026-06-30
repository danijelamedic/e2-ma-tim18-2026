package com.example.slagalica.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.LoginActivity;
import com.example.slagalica.R;
import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueActivity;
import com.example.slagalica.leagues.LeagueManager;
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
import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.ranking.LeaderboardActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvQuizOverviewPercent;
    private TextView tvMatchingOverviewPercent;
    private TextView tvAssociationsOverviewPercent;
    private TextView tvSkockoOverviewPercent;
    private TextView tvStepByStepOverviewPercent;
    private TextView tvMyNumberOverviewPercent;
    private TextView tvOverallSuccess;
    private TextView tvOverallPlayedGames;
    private TextView tvOverallWinLoss;

    private ProgressBar progressQuizOverview;
    private ProgressBar progressMatchingOverview;
    private ProgressBar progressAssociationsOverview;
    private ProgressBar progressSkockoOverview;
    private ProgressBar progressStepByStepOverview;
    private ProgressBar progressMyNumberOverview;

    private TextView tvUsername;
    private TextView tvLeague;
    private TextView tvTokens;
    private TextView tvStars;
    private TextView tvEmail;
    private TextView tvRegion;
    private ImageView imgAvatar;
    private String currentAvatar = "owl";
    private ImageView imgQrCode;
    private View avatarBorderFrame;


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
        avatarBorderFrame = findViewById(R.id.avatarBorderFrame);
        imgQrCode = findViewById(R.id.imgQrCode);

        tvQuizOverviewPercent = findViewById(R.id.tvQuizOverviewPercent);
        tvMatchingOverviewPercent = findViewById(R.id.tvMatchingOverviewPercent);
        tvAssociationsOverviewPercent = findViewById(R.id.tvAssociationsOverviewPercent);
        tvSkockoOverviewPercent = findViewById(R.id.tvSkockoOverviewPercent);
        tvStepByStepOverviewPercent = findViewById(R.id.tvStepByStepOverviewPercent);
        tvMyNumberOverviewPercent = findViewById(R.id.tvMyNumberOverviewPercent);

        progressQuizOverview = findViewById(R.id.progressQuizOverview);
        progressMatchingOverview = findViewById(R.id.progressMatchingOverview);
        progressAssociationsOverview = findViewById(R.id.progressAssociationsOverview);
        progressSkockoOverview = findViewById(R.id.progressSkockoOverview);
        progressStepByStepOverview = findViewById(R.id.progressStepByStepOverview);
        progressMyNumberOverview = findViewById(R.id.progressMyNumberOverview);

        tvOverallSuccess = findViewById(R.id.tvOverallSuccess);
        tvOverallPlayedGames = findViewById(R.id.tvOverallPlayedGames);
        tvOverallWinLoss = findViewById(R.id.tvOverallWinLoss);

        loadOverviewStatistics();
        loadOverallStatistics();
        loadUserProfile();

        tvLeague.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, LeagueActivity.class))
        );

        TextView btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            String uid = getCurrentUserId();

            if (uid == null) {
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("online", false)
                    .addOnCompleteListener(task -> {
                        FirebaseAuth.getInstance().signOut();

                        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
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
                startActivity(new Intent(ProfileActivity.this, FriendsActivity.class)));

        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, LeaderboardActivity.class)));
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

                    int quizPercent = percent(
                            getLong(document.getLong("quizCorrectAnswers")),
                            getLong(document.getLong("quizTotalQuestions"))
                    );

                    int matchingPercent = percent(
                            getLong(document.getLong("matchingCorrectMatches")),
                            getLong(document.getLong("matchingTotalMatches"))
                    );

                    long associationsGames = getLong(document.getLong("associationsGamesPlayed"));
                    int associationsPercent = percent(
                            getLong(document.getLong("associationsSolved")),
                            associationsGames
                    );

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

                    int myNumberPercent = percent(
                            getLong(document.getLong("myNumberExactHits")),
                            getLong(document.getLong("myNumberGamesPlayed"))
                    );

                    tvQuizOverviewPercent.setText(quizPercent + "%");
                    progressQuizOverview.setProgress(quizPercent);

                    tvMatchingOverviewPercent.setText(matchingPercent + "%");
                    progressMatchingOverview.setProgress(matchingPercent);

                    tvAssociationsOverviewPercent.setText(associationsPercent + "%");
                    progressAssociationsOverview.setProgress(associationsPercent);

                    tvSkockoOverviewPercent.setText(skockoPercent + "%");
                    progressSkockoOverview.setProgress(skockoPercent);

                    tvStepByStepOverviewPercent.setText(stepPercent + "%");
                    progressStepByStepOverview.setProgress(stepPercent);

                    tvMyNumberOverviewPercent.setText(myNumberPercent + "%");
                    progressMyNumberOverview.setProgress(myNumberPercent);
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

                    long totalPlayed = getLong(document.getLong("totalBattlesPlayed"));
                    long wins = getLong(document.getLong("battlesWon"));
                    long losses = getLong(document.getLong("battlesLost"));

                    long totalFinished = wins + losses;

                    int winPercent = percent(wins, totalFinished);
                    int lossPercent = totalFinished > 0 ? 100 - winPercent : 0;

                    int quizAverage = average(
                            getLong(document.getLong("quizTotalScore")),
                            getLong(document.getLong("quizGamesPlayed"))
                    );

                    int matchingAverage = average(
                            getLong(document.getLong("matchingTotalScore")),
                            getLong(document.getLong("matchingGamesPlayed"))
                    );

                    int associationsAverage = average(
                            getLong(document.getLong("associationsTotalScore")),
                            getLong(document.getLong("associationsGamesPlayed"))
                    );

                    int skockoAverage = average(
                            getLong(document.getLong("skockoTotalScore")),
                            getLong(document.getLong("skockoGamesPlayed"))
                    );

                    int stepAverage = average(
                            getLong(document.getLong("stepByStepTotalScore")),
                            getLong(document.getLong("stepByStepGamesPlayed"))
                    );

                    int myNumberAverage = average(
                            getLong(document.getLong("myNumberTotalScore")),
                            getLong(document.getLong("myNumberGamesPlayed"))
                    );

                    int totalPercent = 0;
                    int gamesCount = 0;

                    if (getLong(document.getLong("quizGamesPlayed")) > 0) {
                        totalPercent += percent(
                                getLong(document.getLong("quizCorrectAnswers")),
                                getLong(document.getLong("quizTotalQuestions"))
                        );
                        gamesCount++;
                    }

                    if (getLong(document.getLong("matchingGamesPlayed")) > 0) {
                        totalPercent += percent(
                                getLong(document.getLong("matchingCorrectMatches")),
                                getLong(document.getLong("matchingTotalMatches"))
                        );
                        gamesCount++;
                    }

                    if (getLong(document.getLong("associationsGamesPlayed")) > 0) {

                        int associationsPercent = percent(
                                getLong(document.getLong("associationsSolved")),
                                getLong(document.getLong("associationsGamesPlayed"))
                        );

                        totalPercent += associationsPercent;
                        gamesCount++;
                    }

                    if (getLong(document.getLong("skockoGamesPlayed")) > 0) {

                        long hits =
                                getLong(document.getLong("attempt1Hits")) +
                                        getLong(document.getLong("attempt2Hits")) +
                                        getLong(document.getLong("attempt3Hits")) +
                                        getLong(document.getLong("attempt4Hits")) +
                                        getLong(document.getLong("attempt5Hits")) +
                                        getLong(document.getLong("attempt6Hits"));

                        int skockoPercent = percent(
                                hits,
                                getLong(document.getLong("skockoGamesPlayed"))
                        );

                        totalPercent += skockoPercent;
                        gamesCount++;
                    }

                    if (getLong(document.getLong("stepByStepGamesPlayed")) > 0) {

                        long hits =
                                getLong(document.getLong("step1Hits")) +
                                        getLong(document.getLong("step2Hits")) +
                                        getLong(document.getLong("step3Hits")) +
                                        getLong(document.getLong("step4Hits")) +
                                        getLong(document.getLong("step5Hits")) +
                                        getLong(document.getLong("step6Hits")) +
                                        getLong(document.getLong("step7Hits"));

                        int stepPercent = percent(
                                hits,
                                getLong(document.getLong("stepByStepGamesPlayed"))
                        );

                        totalPercent += stepPercent;
                        gamesCount++;
                    }

                    if (getLong(document.getLong("myNumberGamesPlayed")) > 0) {

                        int myNumberPercent = percent(
                                getLong(document.getLong("myNumberExactHits")),
                                getLong(document.getLong("myNumberGamesPlayed"))
                        );

                        totalPercent += myNumberPercent;
                        gamesCount++;
                    }

                    int overallSuccess = gamesCount > 0
                            ? totalPercent / gamesCount
                            : 0;

                    tvOverallSuccess.setText(
                            "Overall success: " + overallSuccess + "%"
                    );

                    tvOverallPlayedGames.setText("Played battles: " + totalPlayed);

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
                    String avatarBorder = document.getString("avatarBorder");
                    currentAvatar = avatar != null ? avatar : "owl";

                    Long tokens = document.getLong("tokens");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");

                    long leagueLevel = league != null ? league : 0;

                    League currentLeague = LeagueManager.getLeague(leagueLevel);

                    tvLeague.setText(
                            currentLeague.getIcon() + " " + currentLeague.getName()
                    );

                    tvUsername.setText(username != null ? username : "Unknown user");
                    tvEmail.setText(email != null ? "Email: " + email : "Email: /");
                    tvRegion.setText(region != null ? "Region: " + region : "Region: /");

                    tvTokens.setText(String.valueOf(tokens != null ? tokens : 0));
                    tvStars.setText(String.valueOf(stars != null ? stars : 0));


                    generateQrCode(userId);

                    imgAvatar.setImageResource(getAvatarResource(avatar));
                    applyAvatarBorder(avatarBorder);
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
            case "guest":
                return R.drawable.avatar_guest;
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

    private long getLong(Long value) {
        return value == null ? 0 : value;
    }

    private int percent(long value, long total) {
        return total > 0 ? (int) Math.round((value * 100.0) / total) : 0;
    }

    private int average(long totalScore, long gamesPlayed) {
        return gamesPlayed > 0 ? (int) Math.round((totalScore * 1.0) / gamesPlayed) : 0;
    }

    private void applyAvatarBorder(String avatarBorder) {
        if (avatarBorderFrame == null) return;

        if (avatarBorder == null) {
            avatarBorderFrame.setBackgroundResource(R.drawable.avatar_border_none);
            return;
        }

        switch (avatarBorder) {
            case "gold":
                avatarBorderFrame.setBackgroundResource(R.drawable.avatar_border_gold);
                break;
            case "silver":
                avatarBorderFrame.setBackgroundResource(R.drawable.avatar_border_silver);
                break;
            case "bronze":
                avatarBorderFrame.setBackgroundResource(R.drawable.avatar_border_bronze);
                break;
            case "none":
            default:
                avatarBorderFrame.setBackgroundResource(R.drawable.avatar_border_none);
                break;
        }
    }
}
