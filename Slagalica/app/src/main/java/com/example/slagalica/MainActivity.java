package com.example.slagalica;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }

        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnQuiz = findViewById(R.id.btnQuiz);
        Button btnMatching = findViewById(R.id.btnMatching);
        Button btnSkocko = findViewById(R.id.btnSkocko);
        Button btnNotifications = findViewById(R.id.btnNotifications);

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class))
        );
        btnQuiz.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, QuizActivity.class))
        );
        btnMatching.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MatchingActivity.class))
        );
        btnSkocko.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SkockoActivity.class))
        );
        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NotificationCenterActivity.class))
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO: permission granted
            } else {
                // TODO: permission denied
            }
        }
    }

    @Override
    protected void onStart() { super.onStart(); }
    @Override
    protected void onResume() { super.onResume(); }
    @Override
    protected void onPause() { super.onPause(); }
    @Override
    protected void onStop() { super.onStop(); }
    @Override
    protected void onDestroy() { super.onDestroy(); }
    @Override
    protected void onRestart() { super.onRestart(); }
}