package com.example.slagalica.notifications;

import android.content.Context;
import android.content.Intent;

import com.example.slagalica.ChatActivity;
import com.example.slagalica.profile.ProfileActivity;

import java.util.HashMap;
import java.util.Map;

public class NotificationFactory {
    private final NotificationRepository repository = new NotificationRepository();

    public void sendReward(Context context, String uid, int starsDelta, long tokensEarned) {
        String title = "Match reward";
        String message = buildRewardMessage(starsDelta, tokensEarned);
        AppNotification notification = new AppNotification(
                uid,
                AppNotification.TYPE_REWARD,
                title,
                message,
                AppNotification.ACTION_OPEN_REWARDS,
                null
        );
        repository.create(uid, notification);
        LocalNotificationSender.show(context, notification,
                new Intent(context, RewardsNotificationsActivity.class));
    }

    public void sendLeagueChange(Context context, String uid, long oldLeague, long newLeague) {
        String title = newLeague > oldLeague ? "League promotion" : "League update";
        String message = newLeague > oldLeague
                ? "You advanced from League " + oldLeague + " to League " + newLeague + "."
                : "Your league changed from League " + oldLeague + " to League " + newLeague + ".";
        AppNotification notification = new AppNotification(
                uid,
                AppNotification.TYPE_OTHER,
                title,
                message,
                AppNotification.ACTION_OPEN_PROFILE,
                null
        );
        repository.create(uid, notification);
        LocalNotificationSender.show(context, notification,
                new Intent(context, ProfileActivity.class));
    }

    public void sendRankingChange(Context context, String uid, long oldRank, long newRank) {
        String message = oldRank <= 0
                ? "You entered the ranking list at position #" + newRank + "."
                : "Your ranking changed from #" + oldRank + " to #" + newRank + ".";
        AppNotification notification = new AppNotification(
                uid,
                AppNotification.TYPE_RANKING,
                "Ranking update",
                message,
                AppNotification.ACTION_OPEN_RANKING,
                null
        );
        repository.create(uid, notification);
        LocalNotificationSender.show(context, notification,
                new Intent(context, RankingNotificationsActivity.class));
    }

    public void sendFriendInvite(Context context, String toUid, String fromUsername, String inviteId) {
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("inviteId", inviteId);

        AppNotification notification = new AppNotification(
                toUid,
                AppNotification.TYPE_OTHER,
                "Friendly match invite",
                fromUsername + " invited you to a friendly match.",
                AppNotification.ACTION_FRIEND_INVITE,
                actionData
        );
        repository.create(toUid, notification);
    }

    public void sendChat(Context context, String uid, String senderName, String text) {
        AppNotification notification = new AppNotification(
                uid,
                AppNotification.TYPE_CHAT,
                "New message from " + senderName,
                text,
                AppNotification.ACTION_OPEN_CHAT,
                null
        );
        repository.create(uid, notification);
        LocalNotificationSender.show(context, notification, new Intent(context, ChatActivity.class));
    }

    public void seedTestNotifications(String uid) {
        repository.create(uid, new AppNotification(
                uid,
                AppNotification.TYPE_CHAT,
                "Test chat message",
                "This is a test chat notification.",
                AppNotification.ACTION_OPEN_CHAT,
                null
        ));
        repository.create(uid, new AppNotification(
                uid,
                AppNotification.TYPE_REWARD,
                "Test reward",
                "You earned 3 test tokens.",
                AppNotification.ACTION_OPEN_REWARDS,
                null
        ));
        repository.create(uid, new AppNotification(
                uid,
                AppNotification.TYPE_OTHER,
                "Test league update",
                "You advanced to League 1.",
                AppNotification.ACTION_OPEN_PROFILE,
                null
        ));
    }

    private String buildRewardMessage(int starsDelta, long tokensEarned) {
        String starsText = starsDelta >= 0 ? "+" + starsDelta : String.valueOf(starsDelta);
        if (tokensEarned > 0) {
            return "You received " + starsText + " stars and +" + tokensEarned + " token(s).";
        }
        return "You received " + starsText + " stars.";
    }
}
