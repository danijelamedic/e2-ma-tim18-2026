package com.example.slagalica.notifications;

public class ChatNotificationsActivity extends BaseNotificationsActivity {
    @Override
    protected String getScreenTitle() {
        return "CHAT NOTIFICATIONS";
    }

    @Override
    protected String getTypeFilter() {
        return AppNotification.TYPE_CHAT;
    }
}
