package com.example.slagalica.notifications;

public class NotificationCenterActivity extends BaseNotificationsActivity {
    @Override
    protected String getScreenTitle() {
        return "NOTIFICATION CENTER";
    }

    @Override
    protected boolean showChannels() {
        return true;
    }
}
