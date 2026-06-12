package com.example.slagalica.notifications;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AppNotification {
    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_RANKING = "ranking";
    public static final String TYPE_REWARD = "reward";
    public static final String TYPE_OTHER = "other";

    public static final String ACTION_OPEN_CHAT = "OPEN_CHAT";
    public static final String ACTION_OPEN_PROFILE = "OPEN_PROFILE";
    public static final String ACTION_OPEN_REWARDS = "OPEN_REWARDS";
    public static final String ACTION_OPEN_RANKING = "OPEN_RANKING";
    public static final String ACTION_FRIEND_INVITE = "FRIEND_INVITE";
    public static final String ACTION_NONE = "NONE";

    public String id;
    public String userId;
    public String type;
    public String title;
    public String message;
    public long createdAt;
    public boolean read;
    public String actionType;
    public Map<String, Object> actionData;

    public AppNotification() {
        actionData = new HashMap<>();
    }

    public AppNotification(String userId, String type, String title, String message,
                           String actionType, Map<String, Object> actionData) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.actionType = actionType != null ? actionType : ACTION_NONE;
        this.actionData = actionData != null ? actionData : new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.read = false;
    }

    @SuppressWarnings("unchecked")
    public static AppNotification fromDocument(DocumentSnapshot document) {
        AppNotification notification = new AppNotification();
        notification.id = document.getId();
        notification.userId = document.getString("userId");
        notification.type = document.getString("type");
        notification.title = document.getString("title");
        notification.message = document.getString("message");
        notification.actionType = document.getString("actionType");
        notification.actionData = document.get("actionData") instanceof Map
                ? (Map<String, Object>) document.get("actionData")
                : new HashMap<>();

        Long createdAt = document.getLong("createdAt");
        notification.createdAt = createdAt != null ? createdAt : 0L;
        notification.read = Boolean.TRUE.equals(document.getBoolean("read"));

        if (notification.type == null) notification.type = TYPE_OTHER;
        if (notification.title == null) notification.title = "Notification";
        if (notification.message == null) notification.message = "";
        if (notification.actionType == null) notification.actionType = ACTION_NONE;
        return notification;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("type", type);
        data.put("title", title);
        data.put("message", message);
        data.put("createdAt", createdAt);
        data.put("read", read);
        data.put("actionType", actionType);
        data.put("actionData", actionData != null ? actionData : new HashMap<>());
        return data;
    }
}
