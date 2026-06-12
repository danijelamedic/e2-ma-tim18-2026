package com.example.slagalica.notifications;

public class RewardsNotificationsActivity extends BaseNotificationsActivity {
    @Override
    protected String getScreenTitle() {
        return "REWARD NOTIFICATIONS";
    }

    @Override
    protected String getTypeFilter() {
        return AppNotification.TYPE_REWARD;
    }
}
