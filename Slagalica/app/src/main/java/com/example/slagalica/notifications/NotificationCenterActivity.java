package com.example.slagalica.notifications;

import android.content.Intent;
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

public class NotificationCenterActivity extends AppCompatActivity {
    private View cardInvite;
    private View cardReward;
    private View cardChat;
    private View cardLeague;
    private View cardChatMention;
    private View cardRankingRefresh;
    private View cardDailyMissionBonus;
    private View cardDailyTokensReminder;
    private View emptyState;
    private TextView unreadCountView;
    private TextView filterAll;
    private TextView filterUnread;
    private TextView filterRead;
    private TextView badgeInvite;
    private TextView badgeReward;
    private Button btnMarkReadInvite;
    private Button btnMarkReadReward;

    private boolean inviteRead = false;
    private boolean rewardRead = false;
    private boolean chatRead = true;
    private boolean leagueRead = true;
    private boolean chatMentionRead = true;
    private boolean rankingRefreshRead = true;
    private boolean dailyMissionBonusRead = true;
    private boolean dailyTokensReminderRead = true;
    private FilterMode currentFilter = FilterMode.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_center);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        bindViews();
        setupFilters();
        setupNavigation();
        applyFilter(FilterMode.ALL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        filterAll = findViewById(R.id.filterAll);
        filterUnread = findViewById(R.id.filterUnread);
        filterRead = findViewById(R.id.filterRead);
        unreadCountView = findViewById(R.id.tvUnreadCount);
        cardInvite = findViewById(R.id.cardInvite);
        cardReward = findViewById(R.id.cardReward);
        cardChat = findViewById(R.id.cardChat);
        cardLeague = findViewById(R.id.cardLeague);
        cardChatMention = findViewById(R.id.cardChatMention);
        cardRankingRefresh = findViewById(R.id.cardRankingRefresh);
        cardDailyMissionBonus = findViewById(R.id.cardDailyMissionBonus);
        cardDailyTokensReminder = findViewById(R.id.cardDailyTokensReminder);
        badgeInvite = findViewById(R.id.badgeInvite);
        badgeReward = findViewById(R.id.badgeReward);
        btnMarkReadInvite = findViewById(R.id.btnMarkReadInvite);
        btnMarkReadReward = findViewById(R.id.btnMarkReadReward);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupFilters() {
        filterAll.setOnClickListener(v -> applyFilter(FilterMode.ALL));
        filterUnread.setOnClickListener(v -> applyFilter(FilterMode.UNREAD));
        filterRead.setOnClickListener(v -> applyFilter(FilterMode.READ));
    }

    private void setupNavigation() {
        findViewById(R.id.channelChat).setOnClickListener(v ->
                startActivity(new Intent(this, ChatNotificationsActivity.class)));
        findViewById(R.id.channelRanking).setOnClickListener(v ->
                startActivity(new Intent(this, RankingNotificationsActivity.class)));
        findViewById(R.id.channelRewards).setOnClickListener(v ->
                startActivity(new Intent(this, RewardsNotificationsActivity.class)));
        findViewById(R.id.channelOther).setOnClickListener(v ->
                startActivity(new Intent(this, OtherNotificationsActivity.class)));

        cardInvite.setOnClickListener(v ->
                openOther());
        cardReward.setOnClickListener(v ->
                openRewards());
        cardChat.setOnClickListener(v ->
                openChat());
        cardLeague.setOnClickListener(v ->
                openOther());
        cardChatMention.setOnClickListener(v ->
                openChat());
        cardRankingRefresh.setOnClickListener(v ->
                openRanking());
        cardDailyMissionBonus.setOnClickListener(v ->
                openRewards());
        cardDailyTokensReminder.setOnClickListener(v ->
                openOther());

        findViewById(R.id.btnOpenInvite).setOnClickListener(v -> openOther());
        findViewById(R.id.btnOpenReward).setOnClickListener(v -> openRewards());
        findViewById(R.id.btnOpenChat).setOnClickListener(v -> openChat());
        findViewById(R.id.btnOpenLeague).setOnClickListener(v -> openOther());
        findViewById(R.id.btnOpenChatMention).setOnClickListener(v -> openChat());
        findViewById(R.id.btnOpenRankingRefresh).setOnClickListener(v -> openRanking());
        findViewById(R.id.btnOpenDailyMissionBonus).setOnClickListener(v -> openRewards());
        findViewById(R.id.btnOpenDailyTokensReminder).setOnClickListener(v -> openOther());

        btnMarkReadInvite.setOnClickListener(v -> markInviteAsRead());
        btnMarkReadReward.setOnClickListener(v -> markRewardAsRead());
    }

    private void applyFilter(FilterMode mode) {
        currentFilter = mode;
        updateFilterStyles(mode);

        cardInvite.setVisibility(getVisibilityForState(inviteRead, mode));
        cardReward.setVisibility(getVisibilityForState(rewardRead, mode));
        cardChat.setVisibility(getVisibilityForState(chatRead, mode));
        cardLeague.setVisibility(getVisibilityForState(leagueRead, mode));
        cardChatMention.setVisibility(getVisibilityForState(chatMentionRead, mode));
        cardRankingRefresh.setVisibility(getVisibilityForState(rankingRefreshRead, mode));
        cardDailyMissionBonus.setVisibility(getVisibilityForState(dailyMissionBonusRead, mode));
        cardDailyTokensReminder.setVisibility(getVisibilityForState(dailyTokensReminderRead, mode));

        boolean hasVisibleCards =
                cardInvite.getVisibility() == View.VISIBLE
                        || cardReward.getVisibility() == View.VISIBLE
                        || cardChat.getVisibility() == View.VISIBLE
                        || cardLeague.getVisibility() == View.VISIBLE
                        || cardChatMention.getVisibility() == View.VISIBLE
                        || cardRankingRefresh.getVisibility() == View.VISIBLE
                        || cardDailyMissionBonus.getVisibility() == View.VISIBLE
                        || cardDailyTokensReminder.getVisibility() == View.VISIBLE;
        emptyState.setVisibility(hasVisibleCards ? View.GONE : View.VISIBLE);
        updateUnreadCount();
    }

    private void updateFilterStyles(FilterMode mode) {
        setFilterStyle(filterAll, mode == FilterMode.ALL);
        setFilterStyle(filterUnread, mode == FilterMode.UNREAD);
        setFilterStyle(filterRead, mode == FilterMode.READ);
    }

    private void setFilterStyle(TextView filterView, boolean active) {
        filterView.setBackgroundResource(active
                ? R.drawable.bg_notification_filter_active
                : R.drawable.bg_notification_filter_inactive);
        filterView.setTextColor(getColor(active ? android.R.color.white : R.color.notification_purple));
    }

    private int getVisibilityForState(boolean isRead, FilterMode mode) {
        if (mode == FilterMode.ALL) {
            return View.VISIBLE;
        }
        if (mode == FilterMode.UNREAD) {
            return isRead ? View.GONE : View.VISIBLE;
        }
        return isRead ? View.VISIBLE : View.GONE;
    }

    private void updateUnreadCount() {
        int unreadCount = 0;
        if (!inviteRead) unreadCount++;
        if (!rewardRead) unreadCount++;
        if (!chatRead) unreadCount++;
        if (!leagueRead) unreadCount++;
        if (!chatMentionRead) unreadCount++;
        if (!rankingRefreshRead) unreadCount++;
        if (!dailyMissionBonusRead) unreadCount++;
        if (!dailyTokensReminderRead) unreadCount++;
        unreadCountView.setText(String.valueOf(unreadCount));
    }

    private void markInviteAsRead() {
        if (inviteRead) {
            return;
        }
        inviteRead = true;
        markCardAsRead(cardInvite, badgeInvite, btnMarkReadInvite);
        applyFilter(currentFilter);
    }

    private void markRewardAsRead() {
        if (rewardRead) {
            return;
        }
        rewardRead = true;
        markCardAsRead(cardReward, badgeReward, btnMarkReadReward);
        applyFilter(currentFilter);
    }

    private void markCardAsRead(View card, TextView badge, Button markReadButton) {
        card.setBackgroundResource(R.drawable.bg_notification_card_read);
        badge.setBackgroundResource(R.drawable.bg_notification_badge_read);
        badge.setText(R.string.notifications_status_read);
        badge.setTextColor(getColor(R.color.notification_purple));
        markReadButton.setVisibility(View.GONE);
    }

    private void openChat() {
        startActivity(new Intent(this, ChatNotificationsActivity.class));
    }

    private void openRanking() {
        startActivity(new Intent(this, RankingNotificationsActivity.class));
    }

    private void openRewards() {
        startActivity(new Intent(this, RewardsNotificationsActivity.class));
    }

    private void openOther() {
        startActivity(new Intent(this, OtherNotificationsActivity.class));
    }

    private enum FilterMode {
        ALL,
        UNREAD,
        READ
    }
}
