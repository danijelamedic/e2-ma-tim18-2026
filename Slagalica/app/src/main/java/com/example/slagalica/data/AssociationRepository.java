package com.example.slagalica.data;

import com.example.slagalica.models.AssociationGame;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AssociationRepository {

    public interface AssociationCallback {
        void onSuccess(AssociationGame game);
        void onError(Exception exception);
    }

    public void loadRandomAssociation(AssociationCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("associationGames")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AssociationGame> games = new ArrayList<>();
                    snapshot.forEach(document -> {
                        AssociationGame game = document.toObject(AssociationGame.class);
                        if (isValid(game)) {
                            games.add(game);
                        }
                    });

                    if (games.isEmpty()) {
                        callback.onError(new IllegalStateException("No association games found."));
                        return;
                    }

                    callback.onSuccess(games.get(new Random().nextInt(games.size())));
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean isValid(AssociationGame game) {
        if (game == null
                || game.getColumnA() == null
                || game.getColumnB() == null
                || game.getColumnC() == null
                || game.getColumnD() == null
                || game.getColumnSolutions() == null
                || game.getFinalSolution() == null
                || game.getColumnA().size() != 4
                || game.getColumnB().size() != 4
                || game.getColumnC().size() != 4
                || game.getColumnD().size() != 4
                || game.getColumnSolutions().size() != 4) {
            return false;
        }

        return true;
    }
}
