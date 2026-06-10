package com.example.slagalica;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;
    private static final String TAG = "HomeActivity";

    private Button btnPlay;
    private View btnLogout;
    private View navHome;
    private View navLeaderboard;
    private View navNotifications;
    private View navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseSeeder.seedQuizQuestions();
        FirebaseSeeder.seedMatchingGames();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }

        initializeViews();
        setupClickListeners();
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
            } else {
                Log.d(TAG, "Microphone permission denied");
            }
        }
    }
}