package com.example.slagalica.leagues;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LeagueActivity extends AppCompatActivity {

    private TextView btnBack;
    private TextView txtCurrentLeague;
    private TextView txtLeagueProgress;
    private TextView txtLeagueStars;
    private TextView txtLeagueTokens;
    private TextView btnLeagueInfo;
    private ProgressBar progressLeague;

    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_league);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnBack = findViewById(R.id.btnBack);
        txtCurrentLeague = findViewById(R.id.txtCurrentLeague);
        txtLeagueProgress = findViewById(R.id.txtLeagueProgress);
        txtLeagueStars = findViewById(R.id.txtLeagueStars);
        txtLeagueTokens = findViewById(R.id.txtLeagueTokens);
        btnLeagueInfo = findViewById(R.id.btnLeagueInfo);
        progressLeague = findViewById(R.id.progressLeague);

        btnBack.setOnClickListener(v -> finish());
        btnLeagueInfo.setOnClickListener(v -> showLeagueInfoDialog());

        loadLeagueData();
    }

    private void loadLeagueData() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;

                    long stars = document.getLong("stars") != null
                            ? document.getLong("stars")
                            : 0;

                    long tokens = document.getLong("tokens") != null
                            ? document.getLong("tokens")
                            : 0;

                    txtLeagueStars.setText(String.valueOf(stars));
                    txtLeagueTokens.setText(String.valueOf(tokens));

                    League currentLeague = LeagueManager.getLeagueForStars((int) stars);
                    League nextLeague = LeagueManager.getNextLeague(currentLeague.getLevel());

                    txtCurrentLeague.setText(
                            currentLeague.getIcon() + " " + currentLeague.getName()
                    );

                    if (nextLeague == null) {
                        txtLeagueProgress.setText("⭐ " + stars + " stars • Maximum league reached");
                        progressLeague.setProgress(100);
                    } else {
                        int needed = nextLeague.getMinStars() - (int) stars;
                        int range = nextLeague.getMinStars() - currentLeague.getMinStars();
                        int currentProgress = (int) stars - currentLeague.getMinStars();
                        int percent = (int) ((currentProgress * 100.0f) / range);

                        txtLeagueProgress.setText(
                                "⭐ " + stars + " stars • " + needed + " stars to "
                                        + nextLeague.getName()
                        );
                        progressLeague.setProgress(percent);
                    }
                });
    }

    private void showLeagueInfoDialog() {
        StringBuilder message = new StringBuilder();

        message.append("Players progress through leagues by collecting stars.\n\n");
        message.append("League thresholds:\n");

        for (League league : LeagueManager.getAllLeagues()) {
            message.append(league.getIcon())
                    .append(" ")
                    .append(league.getName())
                    .append(" - ")
                    .append(league.getMinStars())
                    .append("+ stars")
                    .append("\n");
        }

        message.append("\nDaily login bonus:\n");
        message.append("Every player receives 5 tokens per day.\n");
        message.append("Each league level adds +1 extra daily token.\n\n");
        message.append("Example: Gold League gives 5 + 3 = 8 daily tokens.");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("League rules")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}