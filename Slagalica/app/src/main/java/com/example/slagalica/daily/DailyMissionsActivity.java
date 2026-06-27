package com.example.slagalica.daily;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.HomeActivity;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.R;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DailyMissionsActivity extends AppCompatActivity {
    private final DailyMissionRepository repository = new DailyMissionRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUid;
    private LinearLayout missionsContainer;
    private TextView userName;
    private TextView userStatus;
    private TextView progress;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        buildLayout();
        loadUserHeader();
        listenMissions();
    }

    private void buildLayout() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xFFF6F3FF);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(110));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("📅 DAILY MISSIONS");
        title.setTextColor(0xFF2D1B5E);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC));
        root.addView(title, matchWrap());

        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        profileCard.setBackgroundResource(R.drawable.bg_profile_card);
        profileCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams profileParams = matchWrap();
        profileParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(profileCard, profileParams);

        userName = new TextView(this);
        userName.setText("Welcome");
        userName.setTextColor(0xFF2D1B5E);
        userName.setTextSize(20);
        userName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        profileCard.addView(userName, matchWrap());

        userStatus = new TextView(this);
        userStatus.setText("Loading league, tokens and stars...");
        userStatus.setTextColor(0xFF2E1A66);
        userStatus.setTextSize(15);
        userStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        userStatus.setGravity(Gravity.CENTER);
        userStatus.setBackgroundResource(R.drawable.bg_league_chip);
        userStatus.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, dp(8), 0, 0);
        profileCard.addView(userStatus, statusParams);

        progress = new TextView(this);
        progress.setTextColor(0xFF4C2A91);
        progress.setTextSize(16);
        progress.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        progress.setGravity(Gravity.CENTER);
        progress.setPadding(0, dp(12), 0, dp(12));
        root.addView(progress, matchWrap());

        missionsContainer = new LinearLayout(this);
        missionsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(missionsContainer, matchWrap());

        TextView bonus = new TextView(this);
        bonus.setText("Complete all four missions: +2 tokens and +3 extra stars");
        bonus.setTextColor(0xFF2D1B5E);
        bonus.setTextSize(15);
        bonus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bonus.setGravity(Gravity.CENTER);
        bonus.setBackgroundResource(R.drawable.bg_league_chip);
        bonus.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams bonusParams = matchWrap();
        bonusParams.setMargins(0, dp(10), 0, dp(12));
        root.addView(bonus, bonusParams);

        frame.addView(scroll);
        frame.addView(bottomNavigation());

        setContentView(frame);
    }

    private void loadUserHeader() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document == null || !document.exists()) return;
                    String username = document.getString("username");
                    long stars = document.getLong("stars") != null ? document.getLong("stars") : 0;
                    long tokens = document.getLong("tokens") != null ? document.getLong("tokens") : 0;
                    League league = LeagueManager.getLeagueForStars((int) stars);

                    userName.setText(username != null ? username : "Player");
                    userStatus.setText(league.getIcon() + " " + league.getName()
                            + "   🪙 " + tokens + "   ⭐ " + stars);
                });
    }

    private void listenMissions() {
        listener = repository.listenToday(currentUid, new DailyMissionRepository.MissionCallback() {
            @Override
            public void onLoaded(DailyMission mission) {
                render(mission);
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(DailyMissionsActivity.this,
                        "Failed to load daily missions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(DailyMission mission) {
        missionsContainer.removeAllViews();
        progress.setText(mission.completedCount() + "/4 completed today");
        addMission("🏆", "Win a regular match", mission.winMatch);
        addMission("💬", "Send a regional chat message", mission.sendChatMessage);
        addMission("🤝", "Play a friendly match", mission.playFriendlyMatch);
        addMission("🏟️", "Win a tournament match", mission.winTournamentMatch);
    }

    private void addMission(String icon, String text, boolean done) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_profile_card);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView textView = new TextView(this);
        textView.setText(icon + "  " + text + "\n⭐ Reward: +3 stars");
        textView.setTextColor(done ? 0xFF126A32 : 0xFF2D1B5E);
        textView.setTextSize(16);
        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(textView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView statusIcon = new TextView(this);
        statusIcon.setText(done ? "✅" : "❌");
        statusIcon.setTextSize(24);
        statusIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        statusParams.setMargins(dp(10), 0, 0, 0);
        row.addView(statusIcon, statusParams);

        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(10));
        missionsContainer.addView(row, params);
    }

    private LinearLayout bottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(8), dp(6), dp(8));
        nav.setBackgroundResource(R.drawable.bg_profile_card);
        nav.setElevation(dp(12));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(86),
                Gravity.BOTTOM);
        nav.setLayoutParams(params);

        addNavItem(nav, "🏠", v -> goHome());
        addNavItem(nav, "🔔", v -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        addNavItem(nav, "👥", v -> startActivity(new Intent(this, FriendsActivity.class)));
        addNavItem(nav, "🏆", v -> startActivity(new Intent(this, LeaderboardActivity.class)));
        addNavItem(nav, "📊", v -> startActivity(new Intent(this, ProfileActivity.class)));
        addNavItem(nav, "👤", v -> startActivity(new Intent(this, ProfileActivity.class)));
        return nav;
    }

    private void addNavItem(LinearLayout nav, String icon, View.OnClickListener listener) {
        TextView item = new TextView(this);
        item.setText(icon);
        item.setTextSize(24);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);
        nav.addView(item, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}
