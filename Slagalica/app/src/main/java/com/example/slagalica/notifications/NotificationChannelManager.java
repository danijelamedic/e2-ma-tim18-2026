package com.example.slagalica.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationChannelManager {
    public static final String CHANNEL_CHAT = "chat_notifications";
    public static final String CHANNEL_RANKING = "ranking_notifications";
    public static final String CHANNEL_REWARDS = "reward_notifications";
    public static final String CHANNEL_OTHER = "other_notifications";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_CHAT,
                "Chat notifications",
                NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_RANKING,
                "Ranking notifications",
                NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_REWARDS,
                "Reward notifications",
                NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_OTHER,
                "Other notifications",
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static String channelForType(String type) {
        if (AppNotification.TYPE_CHAT.equals(type)) {
            return CHANNEL_CHAT;
        }
        if (AppNotification.TYPE_RANKING.equals(type)) {
            return CHANNEL_RANKING;
        }
        if (AppNotification.TYPE_REWARD.equals(type)) {
            return CHANNEL_REWARDS;
        }
        return CHANNEL_OTHER;
    }
}
