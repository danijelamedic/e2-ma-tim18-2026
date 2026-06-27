package com.example.slagalica.daily;

import com.google.firebase.firestore.DocumentSnapshot;

public class DailyMission {
    public static final String WIN_MATCH = "winMatch";
    public static final String SEND_CHAT_MESSAGE = "sendChatMessage";
    public static final String PLAY_FRIENDLY_MATCH = "playFriendlyMatch";
    public static final String WIN_TOURNAMENT_MATCH = "winTournamentMatch";

    public boolean winMatch;
    public boolean sendChatMessage;
    public boolean playFriendlyMatch;
    public boolean winTournamentMatch;
    public boolean allCompletedBonusClaimed;

    public static DailyMission fromDocument(DocumentSnapshot document) {
        DailyMission mission = new DailyMission();
        if (document == null || !document.exists()) {
            return mission;
        }

        mission.winMatch = Boolean.TRUE.equals(document.getBoolean(WIN_MATCH));
        mission.sendChatMessage = Boolean.TRUE.equals(document.getBoolean(SEND_CHAT_MESSAGE));
        mission.playFriendlyMatch = Boolean.TRUE.equals(document.getBoolean(PLAY_FRIENDLY_MATCH));
        mission.winTournamentMatch = Boolean.TRUE.equals(document.getBoolean(WIN_TOURNAMENT_MATCH));
        mission.allCompletedBonusClaimed = Boolean.TRUE.equals(document.getBoolean("allCompletedBonusClaimed"));
        return mission;
    }

    public int completedCount() {
        int count = 0;
        if (winMatch) count++;
        if (sendChatMessage) count++;
        if (playFriendlyMatch) count++;
        if (winTournamentMatch) count++;
        return count;
    }

    public boolean isAllCompleted() {
        return completedCount() == 4;
    }
}
