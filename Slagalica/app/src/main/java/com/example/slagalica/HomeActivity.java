package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.games.associations.AssociationsActivity;
import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;
import android.view.View;

public class HomeActivity extends AppCompatActivity {

    private Button btnPlay;
    private Button btnProfile;
    private Button btnLeaderboard;
    private Button btnNotifications;
    private View btnQuiz;
    private View btnMatching;
    private View btnAssociations;
    private View btnSkocko;
    private View btnStepByStep;
    private View btnMyNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupClickListeners();
    }

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
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v ->
                Toast.makeText(this, "Match flow will be added later", Toast.LENGTH_SHORT).show()
        );

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        btnLeaderboard.setOnClickListener(v ->
                Toast.makeText(this, "Leaderboard screen will be added later", Toast.LENGTH_SHORT).show()
        );

        btnQuiz.setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class))
        );

        btnMatching.setOnClickListener(v ->
                startActivity(new Intent(this, MatchingActivity.class))
        );

        btnAssociations.setOnClickListener(v ->
                startActivity(new Intent(this, AssociationsActivity.class))
        );

        btnSkocko.setOnClickListener(v ->
                startActivity(new Intent(this, SkockoActivity.class))
        );

        btnStepByStep.setOnClickListener(v ->
                startActivity(new Intent(this, StepByStepActivity.class))
        );

        btnMyNumber.setOnClickListener(v ->
                startActivity(new Intent(this, MyNumberActivity.class))
        );

        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class))
        );
    }
}