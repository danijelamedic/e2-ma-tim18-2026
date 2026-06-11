package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ChallengeResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_result);

        long myScore   = getIntent().getLongExtra("myScore", 0);
        String resultMsg = getIntent().getStringExtra("resultMsg");
        long[] scores  = getIntent().getLongArrayExtra("playerScores");

        TextView tvResult  = findViewById(R.id.tvChallengeResult);
        TextView tvMyScore = findViewById(R.id.tvChallengeMyScore);
        LinearLayout tvScoresList = findViewById(R.id.scoresList);

        tvResult.setText(resultMsg);
        tvMyScore.setText("Your score: " + myScore);

        if (scores != null) {
            for (int i = 0; i < scores.length; i++) {
                TextView tv = new TextView(this);
                tv.setText((i + 1) + ". Player: " + scores[i] + " pts");
                tv.setTextSize(15);
                tv.setTextColor(i == 0 ? 0xFFFFD700 : 0xFF1E0A3C);
                tv.setPadding(0, 8, 0, 8);
                tvScoresList.addView(tv);
            }
        }

        findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}