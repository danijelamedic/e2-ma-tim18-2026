package com.example.slagalica;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.slagalica.data.FirebaseSeeder;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;
    private static final int REQUEST_NOTIFICATIONS = 2;
    private static final String TAG = "HomeActivity";

<<<<<<< Updated upstream
    private Button btnPlay;
    private View btnLogout;
    private View navHome;
    private View navLeaderboard;
    private View navNotifications;
    private View navProfile;
=======
    private Button btnPlay, btnProfile, btnLeaderboard, btnNotifications;
    private View btnQuiz, btnMatching, btnAssociations, btnSkocko, btnStepByStep, btnMyNumber;
    private View btnChallenge;
    private FirebaseFirestore db;
    private String currentUid;
>>>>>>> Stashed changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

<<<<<<< Updated upstream
        FirebaseSeeder.seedQuizQuestions();
        FirebaseSeeder.seedMatchingGames();
=======
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
>>>>>>> Stashed changes

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

    private void initializeViews() {
        btnPlay = findViewById(R.id.btnPlay);
        btnLogout = findViewById(R.id.btnLogout);

        navHome = findViewById(R.id.navHome);
        navLeaderboard = findViewById(R.id.navLeaderboard);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(this, GameSessionActivity.class))
        );

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
<<<<<<< Updated upstream
=======

    private void initializeViews() {
        btnPlay = findViewById(R.id.btnPlay);
        btnProfile = findViewById(R.id.btnProfile);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnQuiz = findViewById(R.id.btnQuiz);
        btnMatching = findViewById(R.id.btnMatching);
        btnAssociations = findViewById(R.id.btnAssociations);
        btnSkocko = findViewById(R.id.btnSkocko);
        btnStepByStep = findViewById(R.id.btnStepByStep);
        btnMyNumber = findViewById(R.id.btnMyNumber);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnChallenge = findViewById(R.id.btnChallenge);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(this, MatchmakingActivity.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));

        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class)));

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

        if (btnChallenge != null) {
            btnChallenge.setOnClickListener(v ->
                    startActivity(new Intent(this, ChallengeActivity.class)));
        }
    }
>>>>>>> Stashed changes
}