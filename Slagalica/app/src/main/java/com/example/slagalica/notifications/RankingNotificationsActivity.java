package com.example.slagalica.notifications;

public class RankingNotificationsActivity extends BaseNotificationsActivity {
    @Override
    protected String getScreenTitle() {
        return "RANKING NOTIFICATIONS";
    }

    @Override
    protected String getTypeFilter() {
        return AppNotification.TYPE_RANKING;
    }
}
