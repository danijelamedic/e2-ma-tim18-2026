package com.example.slagalica.leagues;

import java.util.Arrays;
import java.util.List;

public class LeagueManager {

    private static final List<League> leagues = Arrays.asList(
            new League(0, "Beginner League", "🌱", 0, 0),
            new League(1, "Bronze League", "🥉", 100, 1),
            new League(2, "Silver League", "🥈", 200, 2),
            new League(3, "Gold League", "🥇", 400, 3),
            new League(4, "Diamond League", "💎", 800, 4),
            new League(5, "Master League", "👑", 1600, 5)
    );

    public static League getLeagueForStars(int stars) {
        League currentLeague = leagues.get(0);

        for (League league : leagues) {
            if (stars >= league.getMinStars()) {
                currentLeague = league;
            } else {
                break;
            }
        }

        return currentLeague;
    }

    public static League getNextLeague(int currentLevel) {
        if (currentLevel >= leagues.size() - 1) {
            return null;
        }

        return leagues.get(currentLevel + 1);
    }

    public static League getLeague(long level) {
        if (level < 0) {
            return leagues.get(0);
        }

        if (level >= leagues.size()) {
            return leagues.get(leagues.size() - 1);
        }

        return leagues.get((int) level);
    }

    public static int getStarsNeededForNextLeague(int stars) {
        League currentLeague = getLeagueForStars(stars);
        League nextLeague = getNextLeague(currentLeague.getLevel());

        if (nextLeague == null) {
            return 0;
        }

        return nextLeague.getMinStars() - stars;
    }

    public static List<League> getAllLeagues() {
        return leagues;
    }
}