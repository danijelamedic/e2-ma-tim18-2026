package com.example.slagalica.games.matching;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;

public class MatchingActivity extends AppCompatActivity {

    private TextView selectedLeftItem = null;
    private int connectedPairs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matching);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupLeaveButton();
        setupSubmitButton();
        setupMatchingClicks();
        setupInfoButton();
    }

    private void setupLeaveButton() {
        Button btnLeave = findViewById(R.id.btnLeaveGame);

        btnLeave.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle("Leave Game")
                    .setMessage("Are you sure you want to leave the game?")
                    .setPositiveButton("YES", (dialog, which) -> finish())
                    .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void setupSubmitButton() {
        Button btnSubmit = findViewById(R.id.btnSubmitMatching);

        btnSubmit.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle("Submit Answers")
                    .setMessage("Are you sure you want to submit your answers?")
                    .setPositiveButton("YES", (dialog, which) -> showWinDialog())
                    .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void showWinDialog() {
        new AlertDialog.Builder(MatchingActivity.this)
                .setTitle("Yay!")
                .setMessage("You won the game!")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(MatchingActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void setupMatchingClicks() {
        TextView leftItem1 = findViewById(R.id.leftItem1);
        TextView leftItem2 = findViewById(R.id.leftItem2);
        TextView leftItem3 = findViewById(R.id.leftItem3);
        TextView leftItem4 = findViewById(R.id.leftItem4);
        TextView leftItem5 = findViewById(R.id.leftItem5);

        TextView rightItem1 = findViewById(R.id.rightItem1);
        TextView rightItem2 = findViewById(R.id.rightItem2);
        TextView rightItem3 = findViewById(R.id.rightItem3);
        TextView rightItem4 = findViewById(R.id.rightItem4);
        TextView rightItem5 = findViewById(R.id.rightItem5);

        leftItem1.setOnClickListener(v -> selectLeftItem(leftItem1));
        leftItem2.setOnClickListener(v -> selectLeftItem(leftItem2));
        leftItem3.setOnClickListener(v -> selectLeftItem(leftItem3));
        leftItem4.setOnClickListener(v -> selectLeftItem(leftItem4));
        leftItem5.setOnClickListener(v -> selectLeftItem(leftItem5));

        rightItem1.setOnClickListener(v -> connectWithRightItem(rightItem1));
        rightItem2.setOnClickListener(v -> connectWithRightItem(rightItem2));
        rightItem3.setOnClickListener(v -> connectWithRightItem(rightItem3));
        rightItem4.setOnClickListener(v -> connectWithRightItem(rightItem4));
        rightItem5.setOnClickListener(v -> connectWithRightItem(rightItem5));
    }

    private void selectLeftItem(TextView leftItem) {
        if (selectedLeftItem == leftItem) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_stat_card);
            selectedLeftItem = null;
            return;
        }

        if (selectedLeftItem != null) {
            selectedLeftItem.setBackgroundResource(R.drawable.bg_stat_card);
        }

        selectedLeftItem = leftItem;
        selectedLeftItem.setBackgroundResource(R.drawable.bg_matching_selected);
    }

    private void connectWithRightItem(TextView rightItem) {
        if (selectedLeftItem == null) {
            return;
        }

        selectedLeftItem.setBackgroundResource(R.drawable.bg_matching_connected);
        rightItem.setBackgroundResource(R.drawable.bg_matching_connected);

        selectedLeftItem.setEnabled(false);
        rightItem.setEnabled(false);

        connectedPairs++;
        TextView tvConnectedCount = findViewById(R.id.tvConnectedCount);
        tvConnectedCount.setText("🔗 " + connectedPairs + " / 5");

        selectedLeftItem = null;
    }

    private void setupInfoButton() {
        TextView btnInfo = findViewById(R.id.btnMatchingInfo);

        btnInfo.setOnClickListener(v -> {
            new AlertDialog.Builder(MatchingActivity.this)
                    .setTitle(R.string.matching_rules_title)
                    .setMessage(R.string.matching_rules_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }
}