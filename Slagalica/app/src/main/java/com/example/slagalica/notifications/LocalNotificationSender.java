package com.example.slagalica.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.slagalica.R;

import java.util.HashSet;
import java.util.Set;

public class LocalNotificationSender {

    private static final Set<String> shownNotificationIds = new HashSet<>();

    public static void show(Context context, AppNotification notification, Intent intent) {
        if (notification.id != null) {
            synchronized (shownNotificationIds) {
                if (!shownNotificationIds.add(notification.id)) {
                    return;
                }
            }
        }

        NotificationChannelManager.createChannels(context);

        Intent actionIntent = new Intent(context, NotificationActionActivity.class);
        actionIntent.putExtra(NotificationActionActivity.EXTRA_NOTIFICATION_ID, notification.id);
        actionIntent.putExtra(NotificationActionActivity.EXTRA_TYPE, notification.type);
        actionIntent.putExtra(NotificationActionActivity.EXTRA_TITLE, notification.title);
        actionIntent.putExtra(NotificationActionActivity.EXTRA_MESSAGE, notification.message);
        actionIntent.putExtra(NotificationActionActivity.EXTRA_ACTION_TYPE, notification.actionType);
        if (notification.actionData != null && notification.actionData.get("inviteId") != null) {
            actionIntent.putExtra(NotificationActionActivity.EXTRA_INVITE_ID,
                    String.valueOf(notification.actionData.get("inviteId")));
        }
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notification.id != null ? notification.id.hashCode() : (int) System.currentTimeMillis(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NotificationChannelManager.channelForType(notification.type))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) (notification.createdAt % Integer.MAX_VALUE), builder.build());
        }
    }
}
