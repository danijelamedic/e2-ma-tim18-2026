package com.example.slagalica.data;

import com.example.slagalica.models.SkockoGame;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkockoRepository {

    public interface SkockoCallback {
        void onSuccess(SkockoGame game);
        void onError(Exception exception);
    }

    public void loadRandomSkockoGame(SkockoCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("skockoGames")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SkockoGame> games = new ArrayList<>();
                    snapshot.forEach(document -> {
                        SkockoGame game = document.toObject(SkockoGame.class);
                        if (isValid(game)) {
                            games.add(game);
                        }
                    });

                    if (games.isEmpty()) {
                        callback.onError(new IllegalStateException("No skocko games found."));
                        return;
                    }

                    callback.onSuccess(games.get(new Random().nextInt(games.size())));
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean isValid(SkockoGame game) {
        if (game == null || game.getSecretCode() == null || game.getSecretCode().size() != 4) {
            return false;
        }

        for (Long symbol : game.getSecretCode()) {
            if (symbol == null || symbol < 0 || symbol > 5) {
                return false;
            }
        }

        return true;
    }
}
