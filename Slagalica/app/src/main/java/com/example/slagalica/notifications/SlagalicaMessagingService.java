package com.example.slagalica.notifications;

import android.content.Intent;

import com.example.slagalica.ChatActivity;
import com.example.slagalica.daily.DailyMissionsActivity;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SlagalicaMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();
        String type = valueOrDefault(data.get("type"), AppNotification.TYPE_OTHER);
        String title = valueOrDefault(data.get("title"), "Slagalica");
        String body = valueOrDefault(data.get("message"), "");
        String actionType = valueOrDefault(data.get("actionType"), AppNotification.ACTION_NONE);
        String notificationId = data.get("notificationId");

        AppNotification notification = new AppNotification(
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : "",
                type,
                title,
                body,
                actionType,
                actionDataFromJson(data.get("actionData"))
        );
        notification.id = notificationId;

        if (!"true".equals(data.get("stored"))) {
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            if (uid != null) {
                new NotificationRepository().create(uid, notification);
            }
        }

        LocalNotificationSender.show(this, notification, intentForAction(actionType));
    }

    private Intent intentForAction(String actionType) {
        if (AppNotification.ACTION_OPEN_CHAT.equals(actionType)) {
            return new Intent(this, ChatActivity.class);
        }
        if (AppNotification.ACTION_OPEN_PROFILE.equals(actionType)) {
            return new Intent(this, ProfileActivity.class);
        }
        if (AppNotification.ACTION_OPEN_REWARDS.equals(actionType)) {
            return new Intent(this, RewardsNotificationsActivity.class);
        }
        if (AppNotification.ACTION_OPEN_DAILY_MISSIONS.equals(actionType)) {
            return new Intent(this, DailyMissionsActivity.class);
        }
        if (AppNotification.ACTION_OPEN_RANKING.equals(actionType)) {
            return new Intent(this, LeaderboardActivity.class);
        }
        return new Intent(this, NotificationCenterActivity.class);
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private Map<String, Object> actionDataFromJson(String json) {
        Map<String, Object> actionData = new HashMap<>();
        if (json == null || json.isEmpty()) {
            return actionData;
        }

        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                actionData.put(key, object.optString(key));
            }
        } catch (JSONException ignored) {
            // Notification actions still work for types that do not need actionData.
        }
        return actionData;
    }
}
