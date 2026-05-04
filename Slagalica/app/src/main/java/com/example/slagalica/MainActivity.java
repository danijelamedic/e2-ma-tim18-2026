package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.games.matching.MatchingActivity;
import com.example.slagalica.games.quiz.QuizActivity;
import com.example.slagalica.games.skocko.SkockoActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
}
