package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUid;
    private String currentUsername;
    private ListenerRegistration challengeListener;
    private ListenerRegistration myChallengeListener;

    private EditText etStars, etTokens;
    private Button btnPostChallenge;
    private LinearLayout challengesList;
    private String myChallengeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etStars = findViewById(R.id.etStars);
        etTokens = findViewById(R.id.etTokens);
        btnPostChallenge = findViewById(R.id.btnPostChallenge);
        challengesList = findViewById(R.id.challengesList);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    currentUsername = snapshot.getString("username");
                });

        btnPostChallenge.setOnClickListener(v -> postChallenge());
        listenForChallenges();
    }

    private void postChallenge() {
        String starsStr = etStars.getText().toString().trim();
        String tokensStr = etTokens.getText().toString().trim();

        if (starsStr.isEmpty() || tokensStr.isEmpty()) {
            Toast.makeText(this, "Enter stars and tokens to bid", Toast.LENGTH_SHORT).show();
            return;
        }

        int stars = Integer.parseInt(starsStr);
        int tokens = Integer.parseInt(tokensStr);

        if (stars > 10) {
            Toast.makeText(this, "Maximum 10 stars", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tokens > 2) {
            Toast.makeText(this, "Maximum 2 tokens", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    long myStars = snapshot.getLong("stars") != null ? snapshot.getLong("stars") : 0;
                    long myTokens = snapshot.getLong("tokens") != null ? snapshot.getLong("tokens") : 0;

                    if (myStars < stars) {
                        Toast.makeText(this, "Not enough stars", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (myTokens < tokens) {
                        Toast.makeText(this, "Not enough tokens", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users").document(currentUid)
                            .update("stars", FieldValue.increment(-stars),
                                    "tokens", FieldValue.increment(-tokens));

                    List<String> participants = new ArrayList<>();
                    participants.add(currentUid);

                    Map<String, Object> challenge = new HashMap<>();
                    challenge.put("creatorUid", currentUid);
                    challenge.put("creatorUsername", currentUsername);
                    challenge.put("starsStake", (long) stars);
                    challenge.put("tokensStake", (long) tokens);
                    challenge.put("participants", participants);
                    challenge.put("scores", new HashMap<String, Long>());
                    challenge.put("status", "open");
                    challenge.put("createdAt", System.currentTimeMillis());

                    db.collection("challenges").add(challenge)
                            .addOnSuccessListener(ref -> {
                                myChallengeId = ref.getId();
                                Toast.makeText(this, "Challenge posted! Waiting for players...",
                                        Toast.LENGTH_SHORT).show();
                                etStars.setText("");
                                etTokens.setText("");
                                listenForMyChallengeStart(myChallengeId, stars, tokens);
                            });
                });
    }

    private void listenForMyChallengeStart(String challengeId, int stars, int tokens) {
        myChallengeListener = db.collection("challenges").document(challengeId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");
                    List<String> participants = (List<String>) snapshot.get("participants");

                    if ("started".equals(status)) {
                        if (myChallengeListener != null) myChallengeListener.remove();
                        openChallengeGame(challengeId);
                    }
                });
    }

    private void listenForChallenges() {
        challengeListener = db.collection("challenges")
                .whereEqualTo("status", "open")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    challengesList.removeAllViews();

                    for (var doc : snapshots.getDocuments()) {
                        String creatorUid = doc.getString("creatorUid");
                        if (currentUid.equals(creatorUid)) continue;

                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null && participants.contains(currentUid)) continue;
                        if (participants != null && participants.size() >= 4) continue;

                        String challengeId = doc.getId();
                        String creatorUsername = doc.getString("creatorUsername");
                        long starsStake = doc.getLong("starsStake") != null ? doc.getLong("starsStake") : 0;
                        long tokensStake = doc.getLong("tokensStake") != null ? doc.getLong("tokensStake") : 0;

                        addChallengeView(challengeId, creatorUsername, starsStake, tokensStake,
                                participants != null ? participants.size() : 1);
                    }
                });
    }

    private void addChallengeView(String challengeId, String creatorUsername,
                                  long stars, long tokens, int currentPlayers) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_profile_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setPadding(40, 32, 40, 32);
        card.setElevation(6f);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚔️ " + creatorUsername + " challenges you!");
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(0xFF2D1B5E);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 12);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statsRow.setLayoutParams(statsParams);
        statsRow.setPadding(0, 0, 0, 12);

        TextView tvStars = new TextView(this);
        tvStars.setText("⭐ " + stars + " stars");
        tvStars.setTextSize(14);
        tvStars.setTextColor(0xFF5B2FC4);
        tvStars.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvStars.setLayoutParams(starParams);

        TextView tvTokens = new TextView(this);
        tvTokens.setText("🎟️ " + tokens + " tokens");
        tvTokens.setTextSize(14);
        tvTokens.setTextColor(0xFF5B2FC4);
        tvTokens.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tokenParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTokens.setLayoutParams(tokenParams);

        TextView tvPlayers = new TextView(this);
        tvPlayers.setText("👥 " + currentPlayers + "/4");
        tvPlayers.setTextSize(14);
        tvPlayers.setTextColor(0xFF5B2FC4);
        tvPlayers.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvPlayers.setLayoutParams(playerParams);

        statsRow.addView(tvStars);
        statsRow.addView(tvTokens);
        statsRow.addView(tvPlayers);

        TextView tvReward = new TextView(this);
        tvReward.setText("🏆 Winner gets " + Math.round((stars * currentPlayers + stars) * 0.75) + " stars");
        tvReward.setTextSize(12);
        tvReward.setTextColor(0xFF8B6FBF);
        tvReward.setPadding(0, 0, 0, 16);

        Button btnAccept = new Button(this);
        btnAccept.setText("⚔️ ACCEPT CHALLENGE");
        btnAccept.setBackgroundResource(R.drawable.bg_purple_button);
        btnAccept.setTextColor(0xFFFFFFFF);
        btnAccept.setTextSize(14f);
        btnAccept.setTypeface(null, android.graphics.Typeface.BOLD);
        btnAccept.setOnClickListener(v -> acceptChallenge(challengeId, stars, tokens));

        card.addView(tvTitle);
        card.addView(statsRow);
        card.addView(tvReward);
        card.addView(btnAccept);
        challengesList.addView(card);
    }

    private void acceptChallenge(String challengeId, long stars, long tokens) {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    long myStars = snapshot.getLong("stars") != null ? snapshot.getLong("stars") : 0;
                    long myTokens = snapshot.getLong("tokens") != null ? snapshot.getLong("tokens") : 0;

                    if (myStars < stars) {
                        Toast.makeText(this, "Not enough stars", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (myTokens < tokens) {
                        Toast.makeText(this, "Not enough tokens", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("challenges").document(challengeId).get()
                            .addOnSuccessListener(challengeDoc -> {
                                List<String> participants = (List<String>) challengeDoc.get("participants");
                                if (participants == null) participants = new ArrayList<>();

                                if (participants.contains(currentUid)) {
                                    Toast.makeText(this, "Already joined!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (participants.size() >= 4) {
                                    Toast.makeText(this, "Challenge is full", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                db.collection("users").document(currentUid)
                                        .update("stars", FieldValue.increment(-stars),
                                                "tokens", FieldValue.increment(-tokens));

                                List<String> updatedParticipants = new ArrayList<>(participants);
                                updatedParticipants.add(currentUid);

                                Map<String, Object> update = new HashMap<>();
                                update.put("participants", updatedParticipants);

                                if (updatedParticipants.size() >= 2) {
                                    update.put("status", "started");
                                }

                                db.collection("challenges").document(challengeId)
                                        .update(update)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(this, "Challenge accepted! Starting...",
                                                    Toast.LENGTH_SHORT).show();
                                            openChallengeGame(challengeId);
                                        });
                            });
                });
    }

    private void openChallengeGame(String challengeId) {
        if (challengeListener != null) challengeListener.remove();
        if (myChallengeListener != null) myChallengeListener.remove();

        Intent intent = new Intent(this, ChallengeGameActivity.class);
        intent.putExtra("challengeId", challengeId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (challengeListener != null) challengeListener.remove();
        if (myChallengeListener != null) myChallengeListener.remove();
    }
}