package com.example.slagalica.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.slagalica.R;

public class LocalNotificationSender {

    public static void show(Context context, AppNotification notification, Intent intent) {
        NotificationChannelManager.createChannels(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notification.id != null ? notification.id.hashCode() : (int) System.currentTimeMillis(),
                intent,
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
