package com.example.slagalica.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.slagalica.ChatActivity;
import com.example.slagalica.R;
import com.example.slagalica.daily.DailyMissionsActivity;
import com.example.slagalica.friends.FriendsActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.slagalica.HomeActivity;
import android.os.CountDownTimer;
import com.example.slagalica.GameActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseNotificationsActivity extends AppCompatActivity {
    private final NotificationRepository repository = new NotificationRepository();
    private final List<AppNotification> notifications = new ArrayList<>();

    private ListenerRegistration listenerRegistration;
    private String currentUid;
    private LinearLayout notificationsContainer;
    private LinearLayout channelSection;
    private LinearLayout channelRow;
    private TextView emptyState;
    private TextView unreadCountView;
    private TextView filterAll;
    private TextView filterUnread;
    private TextView filterRead;
    private FilterMode currentFilter = FilterMode.ALL;

    protected abstract String getScreenTitle();

    protected String getTypeFilter() {
        return null;
    }

    protected boolean showChannels() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications_dynamic);
        NotificationChannelManager.createChannels(this);

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        bindViews();
        setupActions();
        setupBottomNavigation();
        listenForNotifications();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        ((TextView) findViewById(R.id.tvNotificationsTitle)).setText(getScreenTitle());
        notificationsContainer = findViewById(R.id.notificationsContainer);
        channelSection = findViewById(R.id.channelSection);
        channelRow = findViewById(R.id.channelRow);
        emptyState = findViewById(R.id.emptyState);
        unreadCountView = findViewById(R.id.tvUnreadCount);
        filterAll = findViewById(R.id.filterAll);
        filterUnread = findViewById(R.id.filterUnread);
        filterRead = findViewById(R.id.filterRead);
        channelSection.setVisibility(showChannels() ? View.VISIBLE : View.GONE);
        if (showChannels()) {
            renderChannels();
        }
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        filterAll.setOnClickListener(v -> applyFilter(FilterMode.ALL));
        filterUnread.setOnClickListener(v -> applyFilter(FilterMode.UNREAD));
        filterRead.setOnClickListener(v -> applyFilter(FilterMode.READ));
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> {
            if (currentUid != null) {
                repository.markAllAsRead(currentUid, notifications);
            }
        });
    }

    private void listenForNotifications() {
        if (currentUid == null) {
            Toast.makeText(this, "You must be logged in to see notifications.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listenerRegistration = repository.listen(currentUid, getTypeFilter(),
                new NotificationRepository.NotificationsCallback() {
                    @Override
                    public void onNotificationsLoaded(List<AppNotification> loaded) {
                        notifications.clear();
                        notifications.addAll(loaded);
                        renderNotifications();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(BaseNotificationsActivity.this,
                                "Failed to load notifications.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilter(FilterMode filterMode) {
        currentFilter = filterMode;
        setFilterStyle(filterAll, filterMode == FilterMode.ALL);
        setFilterStyle(filterUnread, filterMode == FilterMode.UNREAD);
        setFilterStyle(filterRead, filterMode == FilterMode.READ);
        renderNotifications();
    }

    private void setFilterStyle(TextView filterView, boolean active) {
        filterView.setBackgroundResource(active
                ? R.drawable.bg_notification_filter_active
                : R.drawable.bg_notification_filter_inactive);
        filterView.setTextColor(getColor(active ? android.R.color.white : R.color.notification_purple));
    }

    private void renderNotifications() {
        notificationsContainer.removeAllViews();
        int unreadCount = 0;
        int visibleCount = 0;

        for (AppNotification notification : notifications) {
            if (!notification.read) {
                unreadCount++;
            }
            if (!matchesFilter(notification)) {
                continue;
            }
            notificationsContainer.addView(createNotificationCard(notification));
            visibleCount++;
        }

        unreadCountView.setText(String.valueOf(unreadCount));
        emptyState.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private boolean matchesFilter(AppNotification notification) {
        if (currentFilter == FilterMode.UNREAD) {
            return !notification.read;
        }
        if (currentFilter == FilterMode.READ) {
            return notification.read;
        }
        return true;
    }

    private View createNotificationCard(AppNotification notification) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(notification.read
                ? R.drawable.bg_notification_card_read
                : R.drawable.bg_notification_card_unread);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText(getIconForType(notification.type));
        icon.setTextSize(24);
        icon.setGravity(Gravity.CENTER);
        header.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.setPadding(dp(8), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText(notification.title);
        title.setTextColor(getColor(R.color.notification_purple));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleColumn.addView(title);

        TextView time = new TextView(this);
        time.setText(formatTime(notification.createdAt));
        time.setTextColor(0xFF8B6FBF);
        time.setTextSize(12);
        titleColumn.addView(time);
        header.addView(titleColumn, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = new TextView(this);
        badge.setText(notification.read
                ? R.string.notifications_status_read
                : R.string.notifications_status_unread);
        badge.setGravity(Gravity.CENTER);
        badge.setTextSize(10);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setTextColor(notification.read ? getColor(R.color.notification_purple) : 0xFFFFFFFF);
        badge.setBackgroundResource(notification.read
                ? R.drawable.bg_notification_badge_read
                : R.drawable.bg_notification_badge_unread);
        header.addView(badge, new LinearLayout.LayoutParams(dp(72), dp(26)));
        card.addView(header);

        TextView message = new TextView(this);
        message.setText(notification.message);
        message.setTextColor(0xFF2D1B5E);
        message.setTextSize(14);
        message.setPadding(0, dp(8), 0, dp(8));
        card.addView(message);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button open = createActionButton("OPEN");
        open.setOnClickListener(v -> handleNotificationAction(notification));
        actions.addView(open, new LinearLayout.LayoutParams(0, dp(40), 1));

        if (!notification.read) {
            Button markRead = createActionButton("MARK READ");
            markRead.setOnClickListener(v -> repository.markAsRead(currentUid, notification.id));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
            params.setMargins(dp(8), 0, 0, 0);
            actions.addView(markRead, params);
        }

        card.addView(actions);
        card.setOnClickListener(v -> handleNotificationAction(notification));
        return card;
    }

    private Button createActionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(11);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundResource(R.drawable.bg_purple_button);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private void renderChannels() {
        channelRow.removeAllViews();
        addChannel("CHAT", AppNotification.TYPE_CHAT, ChatNotificationsActivity.class);
        addChannel("RANK", AppNotification.TYPE_RANKING, RankingNotificationsActivity.class);
        addChannel("REWARD", AppNotification.TYPE_REWARD, RewardsNotificationsActivity.class);
        addChannel("OTHER", AppNotification.TYPE_OTHER, OtherNotificationsActivity.class);
    }

    private void addChannel(String label, String type, Class<?> activityClass) {
        TextView channel = new TextView(this);
        channel.setText(getIconForType(type) + "\n" + label);
        channel.setGravity(Gravity.CENTER);
        channel.setTextColor(0xFF2D1B5E);
        channel.setTextSize(12);
        channel.setTypeface(null, android.graphics.Typeface.BOLD);
        channel.setBackgroundResource(R.drawable.bg_stat_card);
        channel.setOnClickListener(v -> startActivity(new Intent(this, activityClass)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(70), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        channelRow.addView(channel, params);
    }

    private void handleNotificationAction(AppNotification notification) {
        if (!notification.read) {
            repository.markAsRead(currentUid, notification.id);
        }

        switch (notification.actionType) {
            case AppNotification.ACTION_OPEN_CHAT:
                startActivity(new Intent(this, ChatActivity.class));
                break;
            case AppNotification.ACTION_OPEN_PROFILE:
                startActivity(new Intent(this, ProfileActivity.class));
                break;
            case AppNotification.ACTION_OPEN_REWARDS:
                showRewardDialog(notification);
                break;
            case AppNotification.ACTION_OPEN_RANKING:
                startActivity(new Intent(this, LeaderboardActivity.class));
                break;
            case AppNotification.ACTION_OPEN_DAILY_MISSIONS:
                startActivity(new Intent(this, DailyMissionsActivity.class));
                break;
            case AppNotification.ACTION_FRIEND_INVITE:
                showInviteDialog(notification);
                break;
            case AppNotification.ACTION_NONE:
            default:
                showSimpleDialog(notification.title, notification.message);
                break;
        }
    }

    private void showInviteDialog(AppNotification notification) {
        final String requestId = notification.actionData != null
                ? (String) notification.actionData.get("inviteId") : null;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(notification.title)
                .setMessage(notification.message)
                .setCancelable(false)
                .setPositiveButton("Accept", null)
                .setNegativeButton("Decline", null)
                .create();
        dialog.show();

        // 10s auto-odbijanje
        final CountDownTimer autoDecline = new CountDownTimer(10000, 1000) {
            @Override public void onTick(long ms) {
                dialog.setMessage(notification.message + "\n(" + (ms / 1000) + "s)");
            }
            @Override public void onFinish() {
                expireInvite(requestId);
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(BaseNotificationsActivity.this,
                        "Invite expired.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            autoDecline.cancel();
            dialog.dismiss();

            if (requestId != null) {
                acceptFriendlyInvite(requestId);
            } else {
                Toast.makeText(this, "Invite no longer valid.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            autoDecline.cancel();
            dialog.dismiss();
            declineInvite(requestId);
            Toast.makeText(this, "Invite declined.", Toast.LENGTH_SHORT).show();
        });
    }

    private void declineInvite(String requestId) {
        if (requestId == null) return;

        FirebaseFirestore.getInstance()
                .collection("friendlyRequests")
                .document(requestId)
                .update(
                        "status", "declined",
                        "respondedAt", FieldValue.serverTimestamp()
                );
    }

    private void acceptFriendlyInvite(String requestId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("friendlyRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(requestDoc -> {
                    if (!requestDoc.exists()) {
                        Toast.makeText(this, "Invite no longer exists.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String status = requestDoc.getString("status");

                    if (!"pending".equals(status)) {
                        Toast.makeText(this, "Invite is no longer active.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fromUid = requestDoc.getString("fromUid");
                    String toUid = requestDoc.getString("toUid");

                    if (fromUid == null || toUid == null) {
                        Toast.makeText(this, "Invalid invite.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> game = new HashMap<>();
                    game.put("player1", fromUid);
                    game.put("player2", toUid);
                    game.put("score1", 0L);
                    game.put("score2", 0L);
                    game.put("currentGame", 1L);
                    game.put("currentTurnUid", fromUid);
                    game.put("status", "active");
                    game.put("isFriendly", true);
                    game.put("createdAt", FieldValue.serverTimestamp());

                    for (int i = 1; i <= 6; i++) {
                        game.put("player1done_game" + i, false);
                        game.put("player2done_game" + i, false);
                    }

                    game.put("myNumberRound", 1L);
                    game.put("stepByStepRound", 1L);
                    game.put("stepByStepStatus", "");

                    db.collection("games")
                            .add(game)
                            .addOnSuccessListener(gameRef -> {
                                String gameId = gameRef.getId();

                                db.collection("friendlyRequests")
                                        .document(requestId)
                                        .update(
                                                "status", "accepted",
                                                "gameId", gameId,
                                                "respondedAt", FieldValue.serverTimestamp()
                                        );

                                db.collection("users").document(fromUid).update("inGame", true);
                                db.collection("users").document(toUid).update("inGame", true);

                                Intent intent = new Intent(this, GameActivity.class);
                                intent.putExtra("gameId", gameId);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to accept invite.", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    private void showRewardDialog(AppNotification notification) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_reward_notification, null);
        TextView title = dialogView.findViewById(R.id.tvRewardDialogTitle);
        TextView message = dialogView.findViewById(R.id.tvRewardDialogMessage);
        Button ok = dialogView.findViewById(R.id.btnRewardDialogOk);

        title.setText(notification.title);
        message.setText(notification.message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getIconForType(String type) {
        if (AppNotification.TYPE_CHAT.equals(type)) return "\uD83D\uDCAC";
        if (AppNotification.TYPE_RANKING.equals(type)) return "\uD83C\uDFC6";
        if (AppNotification.TYPE_REWARD.equals(type)) return "\uD83C\uDF81";
        return "\uD83D\uDD14";
    }

    private String formatTime(long createdAt) {
        if (createdAt <= 0L) {
            return "";
        }
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date(createdAt));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        super.onDestroy();
    }

    private enum FilterMode {
        ALL,
        UNREAD,
        READ
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                Toast.makeText(this, "You are already on Notifications", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.navStatistics).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.navFriends).setOnClickListener(v ->
                startActivity(new Intent(this, FriendsActivity.class)));

        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
    }

    private void expireInvite(String requestId) {
        if (requestId == null) return;

        FirebaseFirestore.getInstance()
                .collection("friendlyRequests")
                .document(requestId)
                .update(
                        "status", "expired",
                        "respondedAt", FieldValue.serverTimestamp()
                );
    }
}
