package com.example.slagalica.tournament;

import com.example.slagalica.notifications.AppNotification;
import com.example.slagalica.notifications.NotificationRepository;
import com.example.slagalica.ranking.RankingRepository;
import com.example.slagalica.leagues.LeagueManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRepository {

    public static final String ROUND_SEMIFINAL = "semifinal";
    public static final String ROUND_FINAL = "final";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository notificationRepository = new NotificationRepository();

    public interface SimpleCallback {
        void onSuccess(String id);
        void onError(String message);
    }

    public interface MatchReportCallback {
        void onComplete(MatchReportResult result);
        void onError(String message);
    }

    public static class MatchReportResult {
        public final boolean rewardsApplied;
        public final boolean resultRecorded;

        MatchReportResult(boolean rewardsApplied, boolean resultRecorded) {
            this.rewardsApplied = rewardsApplied;
            this.resultRecorded = resultRecorded;
        }
    }

    public interface TournamentSnapshotCallback {
        void onSnapshot(DocumentSnapshot snapshot);
        void onError(Exception exception);
    }

    public void joinTournament(String uid, SimpleCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(user -> {
                    long tokens = user.getLong("tokens") != null ? user.getLong("tokens") : 0;
                    if (tokens < 3) {
                        callback.onError("You need 3 tokens to enter a tournament.");
                        return;
                    }

                    db.collection("users").document(uid)
                            .update("tokens", FieldValue.increment(-3), "inGame", true)
                            .addOnSuccessListener(unused -> addToQueue(uid, callback))
                            .addOnFailureListener(e -> callback.onError("Failed to reserve tournament tokens."));
                })
                .addOnFailureListener(e -> callback.onError("Failed to load user data."));
    }

    private void addToQueue(String uid, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("status", "waiting");
        data.put("createdAt", System.currentTimeMillis());

        db.collection("tournamentQueue").document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(uid);
                    checkForReadyTournament();
                })
                .addOnFailureListener(e -> callback.onError("Failed to join tournament queue."));
    }

    public void cancelWaiting(String uid) {
        db.collection("tournamentQueue").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    String status = snapshot.getString("status");
                    if (!"waiting".equals(status)) return;

                    db.collection("tournamentQueue").document(uid).delete();
                    db.collection("users").document(uid)
                            .update("tokens", FieldValue.increment(3), "inGame", false);
                });
    }

    public ListenerRegistration listenQueue(String uid, TournamentSnapshotCallback callback) {
        return db.collection("tournamentQueue").document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    callback.onSnapshot(snapshot);
                });
    }

    public ListenerRegistration listenTournament(String tournamentId, TournamentSnapshotCallback callback) {
        return db.collection("tournaments").document(tournamentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    callback.onSnapshot(snapshot);
                });
    }

    public void checkForReadyTournament() {
        db.collection("tournamentQueue")
                .whereEqualTo("status", "waiting")
                .limit(4)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.size() < 4) return;

                    List<String> players = new ArrayList<>();
                    long firstCreatedAt = Long.MAX_VALUE;
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        players.add(doc.getId());
                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) firstCreatedAt = Math.min(firstCreatedAt, createdAt);
                    }

                    String tournamentId = "tournament_" + firstCreatedAt;
                    DocumentReference tournamentRef = db.collection("tournaments").document(tournamentId);
                    tournamentRef.get().addOnSuccessListener(existing -> {
                        if (existing.exists()) return;
                        createTournament(tournamentId, players);
                    });
                });
    }

    private void createTournament(String tournamentId, List<String> players) {
        Collections.shuffle(players);
        String p1 = players.get(0);
        String p2 = players.get(1);
        String p3 = players.get(2);
        String p4 = players.get(3);

        DocumentReference game1Ref = db.collection("games").document();
        DocumentReference game2Ref = db.collection("games").document();

        Map<String, Object> tournament = new HashMap<>();
        tournament.put("status", "semifinals");
        tournament.put("players", players);
        tournament.put("semifinal1Players", listOf(p1, p2));
        tournament.put("semifinal2Players", listOf(p3, p4));
        tournament.put("semifinal1GameId", game1Ref.getId());
        tournament.put("semifinal2GameId", game2Ref.getId());
        tournament.put("createdAt", System.currentTimeMillis());

        Map<String, Object> game1 = createTournamentGame(p1, p2, tournamentId, ROUND_SEMIFINAL, "semifinal1");
        Map<String, Object> game2 = createTournamentGame(p3, p4, tournamentId, ROUND_SEMIFINAL, "semifinal2");

        Map<String, Object> queueUpdate1 = queueMatchedData(tournamentId, game1Ref.getId(), "semifinal1");
        Map<String, Object> queueUpdate2 = queueMatchedData(tournamentId, game1Ref.getId(), "semifinal1");
        Map<String, Object> queueUpdate3 = queueMatchedData(tournamentId, game2Ref.getId(), "semifinal2");
        Map<String, Object> queueUpdate4 = queueMatchedData(tournamentId, game2Ref.getId(), "semifinal2");

        WriteBatch batch = db.batch();
        batch.set(db.collection("tournaments").document(tournamentId), tournament);
        batch.set(game1Ref, game1);
        batch.set(game2Ref, game2);
        batch.set(db.collection("tournamentQueue").document(p1), queueUpdate1, SetOptions.merge());
        batch.set(db.collection("tournamentQueue").document(p2), queueUpdate2, SetOptions.merge());
        batch.set(db.collection("tournamentQueue").document(p3), queueUpdate3, SetOptions.merge());
        batch.set(db.collection("tournamentQueue").document(p4), queueUpdate4, SetOptions.merge());
        batch.commit();
    }

    private Map<String, Object> createTournamentGame(String player1, String player2,
                                                     String tournamentId, String round,
                                                     String matchId) {
        Map<String, Object> game = new HashMap<>();
        game.put("player1", player1);
        game.put("player2", player2);
        game.put("status", "active");
        game.put("currentGame", 1);
        game.put("currentTurnUid", player1);
        game.put("score1", 0);
        game.put("score2", 0);
        game.put("isFriendly", false);
        game.put("isTournament", true);
        game.put("tournamentId", tournamentId);
        game.put("tournamentRound", round);
        game.put("tournamentMatchId", matchId);
        game.put("createdAt", System.currentTimeMillis());
        return game;
    }

    private Map<String, Object> queueMatchedData(String tournamentId, String gameId, String matchId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "matched");
        data.put("tournamentId", tournamentId);
        data.put("gameId", gameId);
        data.put("matchId", matchId);
        return data;
    }

    public void reportMatchResult(String gameId, MatchReportCallback callback) {
        DocumentReference gameRef = db.collection("games").document(gameId);
        gameRef.get()
                .addOnSuccessListener(game -> {
                    if (game == null || !game.exists()) {
                        callback.onError("Tournament game not found.");
                        return;
                    }

                    String tournamentId = game.getString("tournamentId");
                    String round = game.getString("tournamentRound");
                    String matchId = game.getString("tournamentMatchId");
                    String player1 = game.getString("player1");
                    String player2 = game.getString("player2");
                    String abandonedBy = game.getString("abandonedBy");
                    long score1 = valueOrZero(game.getLong("score1"));
                    long score2 = valueOrZero(game.getLong("score2"));

                    if (tournamentId == null || round == null || matchId == null
                            || player1 == null || player2 == null) {
                        callback.onError("Tournament metadata is incomplete.");
                        return;
                    }

                    String winnerUid;
                    String loserUid;
                    if (abandonedBy != null && abandonedBy.equals(player1)) {
                        winnerUid = player2;
                        loserUid = player1;
                    } else if (abandonedBy != null && abandonedBy.equals(player2)) {
                        winnerUid = player1;
                        loserUid = player2;
                    } else if (score1 >= score2) {
                        winnerUid = player1;
                        loserUid = player2;
                    } else {
                        winnerUid = player2;
                        loserUid = player1;
                    }

                    long winnerScore = winnerUid.equals(player1) ? score1 : score2;
                    long loserScore = loserUid.equals(player1) ? score1 : score2;
                    applyTournamentResult(tournamentId, round, matchId, winnerUid, loserUid,
                            winnerScore, loserScore, abandonedBy, callback);
                })
                .addOnFailureListener(e -> callback.onError("Failed to load tournament game."));
    }

    private void applyTournamentResult(String tournamentId, String round, String matchId,
                                       String winnerUid, String loserUid,
                                       long winnerScore, long loserScore,
                                       String abandonedBy, MatchReportCallback callback) {
        DocumentReference tournamentRef = db.collection("tournaments").document(tournamentId);
        DocumentReference finalGameRef = db.collection("games").document();

        db.runTransaction(transaction -> {
            DocumentSnapshot tournament = transaction.get(tournamentRef);
            if (tournament == null || !tournament.exists()) {
                return new MatchReportResult(false, false);
            }

            String winnerField = matchId + "Winner";
            String recordedWinner = tournament.getString(winnerField);
            if (recordedWinner != null) {
                return new MatchReportResult(false, recordedWinner.equals(winnerUid));
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(matchId + "Winner", winnerUid);
            updates.put(matchId + "Loser", loserUid);
            updates.put(matchId + "WinnerScore", winnerScore);
            updates.put(matchId + "LoserScore", loserScore);
            updates.put(matchId + "AbandonedBy", abandonedBy);

            if (ROUND_SEMIFINAL.equals(round)) {
                String otherWinner = "semifinal1".equals(matchId)
                        ? tournament.getString("semifinal2Winner")
                        : tournament.getString("semifinal1Winner");
                if (otherWinner != null) {
                    String finalPlayer1 = otherWinner;
                    String finalPlayer2 = winnerUid;
                    updates.put("status", "final");
                    updates.put("finalPlayers", listOf(finalPlayer1, finalPlayer2));
                    updates.put("finalGameId", finalGameRef.getId());
                    transaction.set(finalGameRef, createTournamentGame(
                            finalPlayer1, finalPlayer2, tournamentId, ROUND_FINAL, "final"));
                } else {
                    updates.put("status", "semifinals");
                }
            } else {
                updates.put("status", "finished");
                updates.put("winnerUid", winnerUid);
            }

            transaction.update(tournamentRef, updates);
            return new MatchReportResult(true, true);
        }).addOnSuccessListener(result -> {
            if (result.rewardsApplied) {
                grantRewards(round, winnerUid, loserUid, winnerScore, loserScore, abandonedBy);
            }
            callback.onComplete(result);
        }).addOnFailureListener(e -> callback.onError("Failed to save tournament result."));
    }

    private void grantRewards(String round, String winnerUid, String loserUid,
                              long winnerScore, long loserScore, String abandonedBy) {
        if (ROUND_SEMIFINAL.equals(round)) {
            updateStarsAndTokens(winnerUid, regularStars(true, winnerScore), 2);
            if (abandonedBy == null) {
                db.collection("users").document(loserUid).update("inGame", false);
            }
            return;
        }

        updateStarsAndTokens(winnerUid, regularStars(true, winnerScore) + 10, 3);
        if (abandonedBy == null || !abandonedBy.equals(loserUid)) {
            updateStarsAndTokens(loserUid, regularStars(false, loserScore), 0);
        } else {
            db.collection("users").document(loserUid).update("inGame", false);
        }
    }

    private int regularStars(boolean won, long score) {
        int fromScore = (int) (score / 40);
        return won ? 10 + fromScore : -10 + fromScore;
    }

    private void updateStarsAndTokens(String uid, int starsDelta, int tokenBonus) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(snapshot -> {
            long currentStars = valueOrZero(snapshot.getLong("stars"));
            long newStars = Math.max(0, currentStars + starsDelta);
            long regularTokenBonus = newStars / 50 - currentStars / 50;
            long oldLeague = valueOrZero(snapshot.getLong("league"));
            long newLeague = LeagueManager.getLeagueForStars((int) newStars).getLevel();

            Map<String, Object> updates = new HashMap<>();
            updates.put("stars", newStars);
            updates.put("league", newLeague);
            updates.put("inGame", false);
            updates.put("gamesPlayed", FieldValue.increment(1));
            long totalTokenBonus = regularTokenBonus + tokenBonus;
            if (totalTokenBonus != 0) {
                updates.put("tokens", FieldValue.increment(totalTokenBonus));
            }
            userRef.update(updates).addOnSuccessListener(unused -> {
                RankingRepository.recordRankedMatchForUser(uid, starsDelta);
                if (oldLeague != newLeague) {
                    sendLeagueChange(uid, oldLeague, newLeague);
                }
            });
        });
    }

    private void sendLeagueChange(String uid, long oldLeague, long newLeague) {
        boolean promoted = newLeague > oldLeague;
        String title = promoted ? "League promotion" : "League demotion";
        String message = promoted
                ? "Congratulations! You advanced from "
                + LeagueManager.getLeague(oldLeague).getName()
                + " to "
                + LeagueManager.getLeague(newLeague).getName()
                + "."
                : "You dropped from "
                + LeagueManager.getLeague(oldLeague).getName()
                + " to "
                + LeagueManager.getLeague(newLeague).getName()
                + ".";

        AppNotification notification = new AppNotification(
                uid,
                AppNotification.TYPE_OTHER,
                title,
                message,
                AppNotification.ACTION_OPEN_PROFILE,
                null
        );
        notificationRepository.create(uid, notification);
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0;
    }

    private List<String> listOf(String first, String second) {
        List<String> list = new ArrayList<>();
        list.add(first);
        list.add(second);
        return list;
    }
}
