package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MatchmakingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration gameListener;
    private String currentUid;
    private TextView tvStatus;
    private Button btnCancel;
    private boolean isFriendly;
    private String friendUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        isFriendly = getIntent().getBooleanExtra("isFriendly", false);
        friendUid = getIntent().getStringExtra("friendUid");

        tvStatus = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> {
            cancelMatchmaking();
            finish();
        });

        if (isFriendly) {
            startMatchmaking();
        } else {
            checkTokensAndStart();
        }
    }

    private void checkTokensAndStart() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    long tokens = snapshot.getLong("tokens") != null ?
                            snapshot.getLong("tokens") : 0;

                    if (tokens <= 0) {
                        Toast.makeText(this,
                                "You don't have enough tokens to play! You need at least 1 token.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    db.collection("users").document(currentUid)
                            .update("tokens", FieldValue.increment(-1))
                            .addOnSuccessListener(unused -> startMatchmaking());
                });
    }

    private void startMatchmaking() {
        tvStatus.setText("Looking for opponent...");

        if (isFriendly && friendUid != null) {
            createGame(friendUid, true);
            return;
        }

        db.collection("matchmaking")
                .whereEqualTo("status", "waiting")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty() &&
                            !querySnapshot.getDocuments().get(0).getId().equals(currentUid)) {
                        String opponentUid = querySnapshot.getDocuments().get(0).getId();
                        createGame(opponentUid, false);
                    } else {
                        addToWaitingPool();
                    }
                });
    }

    private void addToWaitingPool() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "waiting");
        data.put("timestamp", System.currentTimeMillis());
        data.put("isFriendly", false);

        db.collection("matchmaking").document(currentUid).set(data)
                .addOnSuccessListener(unused -> listenForGame());
    }

    private void listenForGame() {
        tvStatus.setText("Waiting for opponent...");

        gameListener = db.collection("matchmaking").document(currentUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        String gameId = snapshot.getString("gameId");
                        if (gameId != null) {
                            openGame(gameId);
                        }
                    }
                });
    }

    private void createGame(String opponentUid, boolean friendly) {
        Map<String, Object> game = new HashMap<>();
        game.put("player1", currentUid);
        game.put("player2", opponentUid);
        game.put("status", "active");
        game.put("currentGame", 3);
        game.put("currentTurnUid", currentUid); // player1 ide prvi
        game.put("score1", 0);
        game.put("score2", 0);
        game.put("isFriendly", friendly);
        game.put("createdAt", System.currentTimeMillis());

        db.collection("games").add(game)
                .addOnSuccessListener(gameRef -> {
                    String gameId = gameRef.getId();

                    Map<String, Object> update = new HashMap<>();
                    update.put("gameId", gameId);
                    update.put("status", "matched");

                    db.collection("matchmaking").document(currentUid).update(update);
                    if (!friendly) {
                        db.collection("matchmaking").document(opponentUid).update(update);
                    }

                    openGame(gameId);
                });
    }

    private void openGame(String gameId) {
        if (gameListener != null) gameListener.remove();
        db.collection("matchmaking").document(currentUid).delete();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameId);
        startActivity(intent);
        finish();
    }

    private void cancelMatchmaking() {
        if (gameListener != null) gameListener.remove();
        db.collection("matchmaking").document(currentUid).delete();
        if (!isFriendly) {
            db.collection("users").document(currentUid)
                    .update("tokens", FieldValue.increment(1));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelMatchmaking();
    }
}
