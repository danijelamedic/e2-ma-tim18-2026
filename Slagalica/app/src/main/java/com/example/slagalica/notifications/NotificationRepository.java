package com.example.slagalica.notifications;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public interface NotificationsCallback {
        void onNotificationsLoaded(List<AppNotification> notifications);
        void onError(Exception exception);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration listen(String uid, String type, NotificationsCallback callback) {
        Query query = db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }

            List<AppNotification> notifications = new ArrayList<>();
            if (snapshot != null) {
                snapshot.getDocuments().forEach(document -> {
                    AppNotification notification = AppNotification.fromDocument(document);
                    if (type == null || type.equals(notification.type)) {
                        notifications.add(notification);
                    }
                });
            }
            callback.onNotificationsLoaded(notifications);
        });
    }

    public void create(String uid, AppNotification notification) {
        notification.userId = uid;
        if (notification.createdAt == 0L) {
            notification.createdAt = System.currentTimeMillis();
        }

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .add(notification.toMap());
    }

    public void markAsRead(String uid, String notificationId) {
        db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .update("read", true);
    }

    public void markAllAsRead(String uid, List<AppNotification> notifications) {
        for (AppNotification notification : notifications) {
            if (!notification.read) {
                markAsRead(uid, notification.id);
            }
        }
    }
}
