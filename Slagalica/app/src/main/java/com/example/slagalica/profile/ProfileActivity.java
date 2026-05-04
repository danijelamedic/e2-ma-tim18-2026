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

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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
}