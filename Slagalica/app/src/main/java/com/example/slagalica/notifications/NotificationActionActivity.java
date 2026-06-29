package com.example.slagalica.notifications;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.ChatActivity;
import com.example.slagalica.GameActivity;
import com.example.slagalica.R;
import com.example.slagalica.daily.DailyMissionsActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.example.slagalica.ranking.RankingRewardDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationActionActivity extends AppCompatActivity {

    public static final String EXTRA_NOTIFICATION_ID = "notificationId";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ACTION_TYPE = "actionType";
    public static final String EXTRA_INVITE_ID = "inviteId";

    private final NotificationRepository repository = new NotificationRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppNotification notification = notificationFromIntent(getIntent());
        markAsRead(notification);
        handleNotificationAction(notification);
    }

    private AppNotification notificationFromIntent(Intent intent) {
        AppNotification notification = new AppNotification(
                currentUid(),
                intent.getStringExtra(EXTRA_TYPE),
                valueOrDefault(intent.getStringExtra(EXTRA_TITLE), "Notification"),
                valueOrDefault(intent.getStringExtra(EXTRA_MESSAGE), ""),
                valueOrDefault(intent.getStringExtra(EXTRA_ACTION_TYPE), AppNotification.ACTION_NONE),
                actionDataFromIntent(intent)
        );
        notification.id = intent.getStringExtra(EXTRA_NOTIFICATION_ID);
        return notification;
    }

    private void handleNotificationAction(AppNotification notification) {
        switch (notification.actionType) {
            case AppNotification.ACTION_OPEN_CHAT:
                openAndFinish(new Intent(this, ChatActivity.class));
                break;
            case AppNotification.ACTION_OPEN_PROFILE:
                openAndFinish(new Intent(this, ProfileActivity.class));
                break;
            case AppNotification.ACTION_OPEN_REWARDS:
                showRewardDialog(notification);
                break;
            case AppNotification.ACTION_OPEN_RANKING:
                openAndFinish(new Intent(this, LeaderboardActivity.class));
                break;
            case AppNotification.ACTION_OPEN_DAILY_MISSIONS:
                openAndFinish(new Intent(this, DailyMissionsActivity.class));
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

    private void showRewardDialog(AppNotification notification) {
        if ("Ranking reward".equals(notification.title)) {
            RankingRewardDialog.show(this, notification.message, this::finish);
            return;
        }

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_reward_notification, null);
        TextView title = dialogView.findViewById(R.id.tvRewardDialogTitle);
        TextView message = dialogView.findViewById(R.id.tvRewardDialogMessage);
        Button ok = dialogView.findViewById(R.id.btnRewardDialogOk);

        title.setText(notification.title);
        message.setText(notification.message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setOnDismissListener(d -> finish())
                .create();
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setOnDismissListener(dialog -> finish())
                .show();
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

        final CountDownTimer autoDecline = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long ms) {
                dialog.setMessage(notification.message + "\n(" + (ms / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                expireInvite(requestId);
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(NotificationActionActivity.this,
                        "Invite expired.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.start();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            autoDecline.cancel();
            dialog.dismiss();

            if (requestId != null) {
                acceptFriendlyInvite(requestId);
            } else {
                Toast.makeText(this, "Invite no longer valid.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            autoDecline.cancel();
            dialog.dismiss();
            declineInvite(requestId);
            Toast.makeText(this, "Invite declined.", Toast.LENGTH_SHORT).show();
            finish();
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

    private void acceptFriendlyInvite(String requestId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("friendlyRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(requestDoc -> {
                    if (!requestDoc.exists()) {
                        Toast.makeText(this, "Invite no longer exists.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String status = requestDoc.getString("status");
                    if (!"pending".equals(status)) {
                        Toast.makeText(this, "Invite is no longer active.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String fromUid = requestDoc.getString("fromUid");
                    String toUid = requestDoc.getString("toUid");
                    if (fromUid == null || toUid == null) {
                        Toast.makeText(this, "Invalid invite.", Toast.LENGTH_SHORT).show();
                        finish();
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
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to accept invite.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }

    private void openAndFinish(Intent intent) {
        startActivity(intent);
        finish();
    }

    private void markAsRead(AppNotification notification) {
        String uid = currentUid();
        if (uid != null && notification.id != null) {
            repository.markAsRead(uid, notification.id);
        }
    }

    private String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private Map<String, Object> actionDataFromIntent(Intent intent) {
        Map<String, Object> actionData = new HashMap<>();
        String inviteId = intent.getStringExtra(EXTRA_INVITE_ID);
        if (inviteId != null) {
            actionData.put("inviteId", inviteId);
        }
        return actionData;
    }
}
