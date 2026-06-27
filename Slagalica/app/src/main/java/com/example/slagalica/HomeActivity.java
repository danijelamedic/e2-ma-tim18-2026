package com.example.slagalica;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.slagalica.data.FirebaseSeeder;
import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.leagues.LeagueActivity;
import com.example.slagalica.notifications.AppNotification;
import com.example.slagalica.notifications.LocalNotificationSender;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.notifications.NotificationChannelManager;
import com.example.slagalica.notifications.NotificationRepository;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.example.slagalica.ranking.RankingRepository;
import com.example.slagalica.ranking.RankingRewardDialog;
import com.example.slagalica.tournament.TournamentActivity;
import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;
    private static final int REQUEST_NOTIFICATIONS = 2;
    private static final String TAG = "HomeActivity";

    private Button btnPlay;
    private View btnLogout;
    private View navHome;
    private View navLeaderboard;
    private View navNotifications;
    private View navProfile;
    private View btnChallenge;
    private View btnTournament;
    private View btnChat;
    private FirebaseFirestore db;
    private String currentUid;
    private TextView tvWelcomeUsername;
    private TextView txtHomeStatus;
    private NotificationRepository notificationRepository;
    private ListenerRegistration notificationListener;
    private final Set<String> seenNotificationIds = new HashSet<>();
    private boolean notificationFirstLoad = true;

    private View navStatistics;
    private View navFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        NotificationChannelManager.createChannels(this);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUid = currentUser.getUid();

        FirebaseSeeder.seedQuizQuestions();
        FirebaseSeeder.seedMatchingGames();
        FirebaseSeeder.seedAssociationGames();
        FirebaseSeeder.seedSkockoGames();
        FirebaseSeeder.seedStepByStepQuestions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATIONS);
            }
        }

        initializeViews();
        setupClickListeners();
        checkDailyTokens();
        loadCurrentUserName();
        saveFcmToken();
        startNotificationListener();
        // Defense demo: uncomment to preview ranking notification and animated reward dialog.
        // sendRankingNotificationPreview();
        new RankingRepository().checkPreviousCycleRewards(currentUid, this::showRankingRewardIfAny);
    }

    private void loadCurrentUserName() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    String username = document.getString("username");
                    if (username != null && tvWelcomeUsername != null) {
                        tvWelcomeUsername.setText("Welcome, " + username + "!");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                );
    }

    private void showRankingRewardIfAny(String message) {
        if (message == null || message.isEmpty() || isFinishing() || isDestroyed()) {
            return;
        }

        RankingRewardDialog.show(this, message);
    }

    private void sendRankingNotificationPreview() {
        AppNotification notification = new AppNotification(
                currentUid,
                AppNotification.TYPE_RANKING,
                "Ranking update",
                "You entered the weekly leaderboard at position #3.",
                AppNotification.ACTION_OPEN_RANKING,
                new HashMap<>()
        );
        notificationRepository.create(currentUid, notification);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                RankingRewardDialog.show(this,
                        "You finished #3 in the weekly ranking and earned 2 token(s).");
            }
        }, 800);
    }

    private void checkDailyTokens() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    long lastTokenDay = snapshot.getLong("lastTokenDay") != null ?
                            snapshot.getLong("lastTokenDay") : 0;

                    Calendar cal = Calendar.getInstance();
                    long today = cal.get(Calendar.YEAR) * 10000L
                            + cal.get(Calendar.MONTH) * 100L
                            + cal.get(Calendar.DAY_OF_MONTH);

                    if (today <= lastTokenDay) {
                        loadHomeStatus();
                        return;
                    }

                    long stars = snapshot.getLong("stars") != null ? snapshot.getLong("stars") : 0;
                    League leagueObj = LeagueManager.getLeagueForStars((int) stars);
                    long dailyTokens = 5 + leagueObj.getLevel();

                    db.collection("users").document(currentUid)
                            .update("tokens", FieldValue.increment(dailyTokens),
                                    "lastTokenDay", today,
                                    "league", leagueObj.getLevel())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this,
                                        "Daily tokens received: +" + dailyTokens,
                                        Toast.LENGTH_LONG).show();

                                loadHomeStatus();
                            });
                });
    }

    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> db.collection("users")
                        .document(currentUid)
                        .update("fcmToken", token));
    }

    private void startNotificationListener() {
        if (notificationListener != null) notificationListener.remove();

        notificationListener = notificationRepository.listen(currentUid, null,
                new NotificationRepository.NotificationsCallback() {
                    @Override
                    public void onNotificationsLoaded(List<AppNotification> notifications) {
                        if (notificationFirstLoad) {
                            for (AppNotification n : notifications) seenNotificationIds.add(n.id);
                            notificationFirstLoad = false;
                            return;
                        }
                        for (AppNotification n : notifications) {
                            if (n.read || n.id == null) continue;
                            if (seenNotificationIds.add(n.id)) {
                                Intent intent = new Intent(HomeActivity.this, NotificationCenterActivity.class);
                                LocalNotificationSender.show(HomeActivity.this, n, intent);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.w(TAG, "Failed to listen for notifications", exception);
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Microphone permission granted");
            }
        }
    }

    private void initializeViews() {
        btnPlay           = findViewById(R.id.btnPlay);
        btnChallenge      = findViewById(R.id.btnChallenge);
        btnTournament     = findViewById(R.id.btnTournament);
        btnChat = findViewById(R.id.btnChat);
        btnLogout         = findViewById(R.id.btnLogout);
        navHome           = findViewById(R.id.navHome);
        navLeaderboard    = findViewById(R.id.navLeaderboard);
        navNotifications  = findViewById(R.id.navNotifications);
        navProfile        = findViewById(R.id.navProfile);
        tvWelcomeUsername = findViewById(R.id.tvWelcomeUsername);
        navStatistics     = findViewById(R.id.navStatistics);
        navFriends        = findViewById(R.id.navFriends);
        txtHomeStatus = findViewById(R.id.txtHomeStatus);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(this, MatchmakingActivity.class)));

        if (btnChallenge != null) {
            btnChallenge.setOnClickListener(v ->
                    startActivity(new Intent(this, ChallengeActivity.class)));
        }
        if (btnTournament != null) {
            btnTournament.setOnClickListener(v ->
                    startActivity(new Intent(this, TournamentActivity.class)));
        }
        if (btnChat != null) {
            btnChat.setOnClickListener(v ->
                    startActivity(new Intent(this, ChatActivity.class)));
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUid)
                    .update("online", false)
                    .addOnCompleteListener(task -> {
                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(this, com.example.slagalica.LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });

        });

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show());

        if (txtHomeStatus != null) {
            txtHomeStatus.setOnClickListener(v ->
                    startActivity(new Intent(this, LeagueActivity.class)));
        }

        navLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        navNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class)));

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        navStatistics.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        navFriends.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));
    }

    @Override
    protected void onDestroy() {
        if (notificationListener != null) notificationListener.remove();
        super.onDestroy();
    }

    private void loadHomeStatus() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists() || txtHomeStatus == null) return;

                    long stars = document.getLong("stars") != null
                            ? document.getLong("stars")
                            : 0;

                    long tokens = document.getLong("tokens") != null
                            ? document.getLong("tokens")
                            : 0;

                    League league = LeagueManager.getLeagueForStars((int) stars);

                    txtHomeStatus.setText(
                            league.getIcon() + " " + league.getName()
                                    + "   🪙 " + tokens
                                    + "   ⭐ " + stars
                    );

                    document.getReference().update("league", league.getLevel());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load league data", Toast.LENGTH_SHORT).show()
                );
    }
}
