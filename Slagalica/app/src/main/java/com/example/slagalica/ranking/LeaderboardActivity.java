package com.example.slagalica.ranking;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.data.PlayerProfileLoader;
import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueManager;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.profile.statistics.StatisticsDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private final RankingRepository repository = new RankingRepository();

    private LinearLayout leaderboardContainer;
    private TextView emptyState;
    private TextView cycleLabel;
    private TextView currentPlayerRank;
    private TextView playerStatus;
    private TextView tabWeekly;
    private TextView tabMonthly;
    private ListenerRegistration registration;
    private String selectedType = RankingRepository.TYPE_WEEKLY;
    private String currentUid;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadPlayerStatus();
            loadLeaderboard(selectedType);
            refreshHandler.postDelayed(this, 120000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        bindViews();
        setupActions();
        setupBottomNavigation();
        checkRewards();
        loadPlayerStatus();
        loadLeaderboard(RankingRepository.TYPE_WEEKLY);
        refreshHandler.postDelayed(refreshRunnable, 120000);
    }

    private void bindViews() {
        leaderboardContainer = findViewById(R.id.leaderboardContainer);
        emptyState = findViewById(R.id.tvEmptyLeaderboard);
        cycleLabel = findViewById(R.id.tvCycleLabel);
        currentPlayerRank = findViewById(R.id.tvCurrentPlayerRank);
        playerStatus = findViewById(R.id.tvPlayerStatus);
        tabWeekly = findViewById(R.id.tabWeekly);
        tabMonthly = findViewById(R.id.tabMonthly);
    }

    private void loadPlayerStatus() {
        if (currentUid == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    long tokens = document.getLong("tokens") != null ? document.getLong("tokens") : 0;
                    long stars = document.getLong("stars") != null ? document.getLong("stars") : 0;
                    long leagueLevel = document.getLong("league") != null ? document.getLong("league") : 0;
                    League league = LeagueManager.getLeague(leagueLevel);

                    playerStatus.setText(league.getIcon() + " " + league.getName()
                            + "   🪙 " + tokens
                            + "   ⭐ " + stars);
                });
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRankingInfo).setOnClickListener(v -> showInfo());
        tabWeekly.setOnClickListener(v -> loadLeaderboard(RankingRepository.TYPE_WEEKLY));
        tabMonthly.setOnClickListener(v -> loadLeaderboard(RankingRepository.TYPE_MONTHLY));
    }

    private void loadLeaderboard(String type) {
        selectedType = type;
        setTabState();

        RankingRepository.Cycle cycle = repository.getCurrentCycle(type);
        cycleLabel.setText("📅 " + (RankingRepository.TYPE_WEEKLY.equals(type) ? "Weekly" : "Monthly")
                + " cycle: " + cycle.label);

        if (registration != null) {
            registration.remove();
            registration = null;
        }

        registration = repository.listenToCurrentEntries(type,
                new RankingRepository.EntriesCallback() {
                    @Override
                    public void onLoaded(List<RankingRepository.RankingEntry> entries) {
                        renderEntries(entries);
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(LeaderboardActivity.this,
                                "Failed to load leaderboard.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderEntries(List<RankingRepository.RankingEntry> entries) {
        leaderboardContainer.removeAllViews();
        emptyState.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        RankingRepository.RankingEntry currentEntry = null;
        for (RankingRepository.RankingEntry entry : entries) {
            boolean mine = currentUid != null && currentUid.equals(entry.uid);
            if (mine) {
                currentEntry = entry;
            }
            leaderboardContainer.addView(createEntryRow(entry, mine));
        }

        if (currentEntry != null) {
            currentPlayerRank.setText("#" + currentEntry.rank + "  "
                    + currentEntry.username + "  "
                    + currentEntry.leagueIcon + " "
                    + currentEntry.leagueName + "  "
                    + "⭐ " + currentEntry.stars);
        } else {
            currentPlayerRank.setText("Your placement will appear after one ranked match in this cycle.");
        }
    }

    private View createEntryRow(RankingRepository.RankingEntry entry, boolean mine) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(mine ? R.drawable.bg_notification_card_unread : R.drawable.bg_profile_card);
        row.setMinimumHeight(dp(74));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        TextView rank = new TextView(this);
        rank.setText("#" + entry.rank);
        rank.setTextColor(0xFF2D1B5E);
        rank.setTextSize(18);
        rank.setTypeface(null, Typeface.BOLD);
        rank.setGravity(Gravity.CENTER);
        row.addView(rank, new LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(PlayerProfileLoader.getAvatarResource(entry.avatar));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
        avatar.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatarParams.setMargins(0, 0, dp(10), 0);
        row.addView(avatar, avatarParams);
        PlayerProfileLoader.load(entry.uid, summary -> avatar.setImageResource(summary.avatarResId));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView username = new TextView(this);
        username.setText(entry.username);
        username.setTextColor(0xFF2D1B5E);
        username.setTextSize(16);
        username.setTypeface(null, Typeface.BOLD);
        info.addView(username);

        TextView league = new TextView(this);
        league.setText(entry.leagueIcon + " " + entry.leagueName + " - "
                + entry.matchesPlayed + " match(es)");
        league.setTextColor(0xFF5B2FC4);
        league.setTextSize(12);
        info.addView(league);
        row.addView(info, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView stars = new TextView(this);
        stars.setText("⭐\n" + entry.stars);
        stars.setTextColor(0xFF2D1B5E);
        stars.setTextSize(15);
        stars.setTypeface(null, Typeface.BOLD);
        stars.setGravity(Gravity.CENTER);
        row.addView(stars, new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));

        return row;
    }

    private void setTabState() {
        boolean weekly = RankingRepository.TYPE_WEEKLY.equals(selectedType);
        tabWeekly.setBackgroundResource(weekly
                ? R.drawable.bg_notification_filter_active
                : R.drawable.bg_notification_filter_inactive);
        tabMonthly.setBackgroundResource(!weekly
                ? R.drawable.bg_notification_filter_active
                : R.drawable.bg_notification_filter_inactive);
        tabWeekly.setTextColor(weekly ? 0xFFFFFFFF : 0xFF5B2FC4);
        tabMonthly.setTextColor(!weekly ? 0xFFFFFFFF : 0xFF5B2FC4);
    }

    private void checkRewards() {
        if (currentUid == null) {
            return;
        }
        repository.checkPreviousCycleRewards(currentUid, this::showRewardDialog);
    }

    private void showRewardDialog(String message) {
        if (message == null || message.isEmpty() || isFinishing() || isDestroyed()) {
            return;
        }

        RankingRewardDialog.show(this, message);
    }

    private void showInfo() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Ranking rules")
                .setMessage("Weekly and monthly leaderboards include players who played at least one ranked match in the current cycle.\n\n"
                        + "Rewards are granted after a cycle ends: weekly #1 +5 tokens, #2 +3, #3 +2, #4-10 +1. Monthly rewards are doubled.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationCenterActivity.class)));
        findViewById(R.id.navFriends).setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));
        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                Toast.makeText(this, "You are already on Leaderboard", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navStatistics).setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsDashboardActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable);
        if (registration != null) {
            registration.remove();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
