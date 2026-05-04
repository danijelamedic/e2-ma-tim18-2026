package com.example.slagalica.notifications;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.R;

public class RankingNotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ranking_notifications);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupDemoReadState();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupDemoReadState() {
        View cardOne = findViewById(R.id.cardRankingOne);
        TextView badgeOne = findViewById(R.id.badgeRankingOne);
        Button markOne = findViewById(R.id.btnMarkReadRankingOne);

        markOne.setOnClickListener(v -> markCardAsRead(cardOne, badgeOne, markOne));

        findViewById(R.id.btnMarkAllReadRanking).setOnClickListener(v ->
                markCardAsRead(cardOne, badgeOne, markOne));
    }

    private void markCardAsRead(View card, TextView badge, Button markReadButton) {
        card.setBackgroundResource(R.drawable.bg_notification_card_read);
        badge.setBackgroundResource(R.drawable.bg_notification_badge_read);
        badge.setText(R.string.notifications_status_read);
        badge.setTextColor(getColor(R.color.notification_purple));
        markReadButton.setVisibility(View.GONE);
    }
}
