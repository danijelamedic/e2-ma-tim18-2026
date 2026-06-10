package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BattleResultActivity extends AppCompatActivity {

    private TextView tvTotalScore;
    private Button btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_result);

        int totalScore = getIntent().getIntExtra("totalScore", 0);

        tvTotalScore = findViewById(R.id.tvTotalScore);
        btnBackHome = findViewById(R.id.btnBackHome);

        tvTotalScore.setText(totalScore + " pts");

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}