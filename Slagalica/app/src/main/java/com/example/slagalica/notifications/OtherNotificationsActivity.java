package com.example.slagalica.notifications;

public class OtherNotificationsActivity extends BaseNotificationsActivity {
    @Override
    protected String getScreenTitle() {
        return "OTHER NOTIFICATIONS";
    }

    @Override
    protected String getTypeFilter() {
        return AppNotification.TYPE_OTHER;
    }
}
