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
import com.example.slagalica.games.MyNumber.MyNumberActivity;
import com.example.slagalica.games.StepByStep.StepByStepActivity;
import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.example.slagalica.notifications.AppNotification;
import com.example.slagalica.notifications.LocalNotificationSender;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.notifications.NotificationChannelManager;
import com.example.slagalica.notifications.NotificationRepository;
import com.example.slagalica.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Calendar;
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
    private View btnQuiz;
    private View btnMatching;
    private View btnAssociations;
    private View btnSkocko;
    private View btnStepByStep;
    private View btnMyNumber;
    private FirebaseFirestore db;
    private String currentUid;
    private TextView tvWelcomeUsername;
    private NotificationRepository notificationRepository;
    private ListenerRegistration notificationListener;
    private final Set<String> seenNotificationIds = new HashSet<>();
    private boolean notificationFirstLoad = true;

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

        //FirebaseSeeder.seedQuizQuestions();
        //FirebaseSeeder.seedMatchingGames();
        FirebaseSeeder.seedAssociationGames();
        FirebaseSeeder.seedSkockoGames();

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
        ensureTestNotifications();
    }

    private void loadCurrentUserName() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        return;
                    }

                    String username = document.getString("username");

                    if (username != null && tvWelcomeUsername != null) {
                        tvWelcomeUsername.setText("Welcome, " + username + "!");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                );
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

                    if (today <= lastTokenDay) return;

                    long league = snapshot.getLong("league") != null ?
                            snapshot.getLong("league") : 0;

                    long dailyTokens = 5 + league;

                    db.collection("users").document(currentUid)
                            .update("tokens", FieldValue.increment(dailyTokens),
                                    "lastTokenDay", today)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "Daily tokens received: +" + dailyTokens,
                                            Toast.LENGTH_LONG).show());
                });
    }

    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> db.collection("users")
                        .document(currentUid)
                        .update("fcmToken", token));
    }

    private void ensureTestNotifications() {
        ensureTestNotification("test_chat",
                new AppNotification(currentUid, AppNotification.TYPE_CHAT,
                        "Test chat message", "This is a test chat notification.",
                        AppNotification.ACTION_OPEN_CHAT, null),
                () -> ensureTestNotification("test_reward",
                        new AppNotification(currentUid, AppNotification.TYPE_REWARD,
                                "Test reward", "You earned 3 test tokens.",
                                AppNotification.ACTION_OPEN_REWARDS, null),
                        () -> ensureTestNotification("test_other",
                                new AppNotification(currentUid, AppNotification.TYPE_OTHER,
                                        "Test league update", "You advanced to League 1.",
                                        AppNotification.ACTION_OPEN_PROFILE, null),
                                this::startNotificationListener)));
    }

    private void ensureTestNotification(String documentId, AppNotification notification, Runnable onComplete) {
        com.google.firebase.firestore.DocumentReference ref = db.collection("users")
                .document(currentUid)
                .collection("notifications")
                .document(documentId);

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                java.util.Map<String, Object> data = notification.toMap();
                data.remove("read");
                data.remove("createdAt");
                ref.set(data, SetOptions.merge()).addOnCompleteListener(updateTask -> onComplete.run());
            } else {
                ref.set(notification.toMap()).addOnCompleteListener(createTask -> onComplete.run());
            }
        });
    }

    private void startNotificationListener() {
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = notificationRepository.listen(currentUid, null,
                new NotificationRepository.NotificationsCallback() {
                    @Override
                    public void onNotificationsLoaded(List<AppNotification> notifications) {
                        if (notificationFirstLoad) {
                            for (AppNotification notification : notifications) {
                                seenNotificationIds.add(notification.id);
                            }
                            notificationFirstLoad = false;
                            return;
                        }

                        for (AppNotification notification : notifications) {
                            if (notification.read || notification.id == null) {
                                continue;
                            }
                            if (seenNotificationIds.add(notification.id)) {
                                Intent intent = new Intent(HomeActivity.this, NotificationCenterActivity.class);
                                LocalNotificationSender.show(HomeActivity.this, notification, intent);
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
        btnPlay = findViewById(R.id.btnPlay);
        btnChallenge = findViewById(R.id.btnChallenge);
        btnQuiz = findViewById(R.id.btnQuiz);
        btnMatching = findViewById(R.id.btnMatching);
        btnAssociations = findViewById(R.id.btnAssociations);
        btnSkocko = findViewById(R.id.btnSkocko);
        btnStepByStep = findViewById(R.id.btnStepByStep);
        btnMyNumber = findViewById(R.id.btnMyNumber);
        btnLogout = findViewById(R.id.btnLogout);
        navHome = findViewById(R.id.navHome);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);
        tvWelcomeUsername = findViewById(R.id.tvWelcomeUsername);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(this, MatchmakingActivity.class)));

        //btnPlay.setOnClickListener(v ->
        //        startActivity(new Intent(this, GameSessionActivity.class))
        //);

        if (btnChallenge != null) {
            btnChallenge.setOnClickListener(v ->
                    startActivity(new Intent(this, ChallengeActivity.class)));
        }

        btnQuiz.setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class)));

        btnMatching.setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class)));

        btnAssociations.setOnClickListener(v ->
                startActivity(new Intent(this, AssociationsActivity.class)));

        btnSkocko.setOnClickListener(v ->
                startActivity(new Intent(this, SkockoActivity.class)));

        btnStepByStep.setOnClickListener(v ->
                startActivity(new Intent(this, StepByStepActivity.class)));

        btnMyNumber.setOnClickListener(v ->
                startActivity(new Intent(this, MyNumberActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show()
        );

        navLeaderboard.setOnClickListener(v ->
                Toast.makeText(this, "Leaderboard screen will be added later", Toast.LENGTH_SHORT).show()
        );

        navNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class))
        );

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    @Override
    protected void onDestroy() {
        if (notificationListener != null) {
            notificationListener.remove();
        }
        super.onDestroy();
    }
}
