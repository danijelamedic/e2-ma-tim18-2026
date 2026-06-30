package com.example.slagalica.friends;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.GameActivity;
import com.example.slagalica.GameSessionActivity;
import com.example.slagalica.HomeActivity;
import com.example.slagalica.R;
import com.example.slagalica.leagues.League;
import com.example.slagalica.leagues.LeagueManager;
import com.example.slagalica.notifications.NotificationCenterActivity;
import com.example.slagalica.notifications.NotificationFactory;
import com.example.slagalica.profile.ProfileActivity;
import com.example.slagalica.ranking.LeaderboardActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity {

    private EditText searchUsernameEditText;
    private TextView searchPlayerButton;
    private TextView searchResultTextView;
    private LinearLayout friendsContainer;
    private FirebaseFirestore db;
    private String currentUid;
    private final NotificationFactory notificationFactory = new NotificationFactory();
    private ListenerRegistration friendlyRequestListener;
    private String activeRequestId = null;
    private androidx.appcompat.app.AlertDialog pendingRequestDialog;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {

                if (result.getContents() == null) {
                    return;
                }

                String scannedUid = result.getContents().trim();

                addFriendByQr(scannedUid);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, com.example.slagalica.LoginActivity.class));
            finish();
            return;
        }

        currentUid = user.getUid();

        searchUsernameEditText = findViewById(R.id.searchUsernameEditText);
        searchPlayerButton = findViewById(R.id.searchPlayerButton);
        searchResultTextView = findViewById(R.id.searchResultTextView);
        friendsContainer = findViewById(R.id.friendsContainer);

        searchPlayerButton.setOnClickListener(v -> searchAndAddFriend());
        findViewById(R.id.cardScanQr).setOnClickListener(v -> openQrScanner());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUid)
                    .update("online", false)
                    .addOnCompleteListener(task -> {
                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(FriendsActivity.this, com.example.slagalica.LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
        });

        setupBottomNavigation();
        loadFriends();
    }

    private void openQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan friend's QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);

        qrLauncher.launch(options);
    }

    private void addFriendByQr(String friendId) {
        if (friendId == null || friendId.isEmpty()) {
            showSearchMessage("Invalid QR code.");
            return;
        }

        if (friendId.equals(currentUid)) {
            showSearchMessage("You cannot add yourself.");
            return;
        }

        db.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showSearchMessage("Player not found.");
                        return;
                    }

                    String username = document.getString("username");
                    String avatar = document.getString("avatar");
                    String region = document.getString("region");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");

                    addFriend(
                            friendId,
                            username != null ? username : "Unknown",
                            avatar != null ? avatar : "owl",
                            region != null ? region : "",
                            stars,
                            league
                    );
                })
                .addOnFailureListener(e ->
                        showSearchMessage("Failed to scan QR code.")
                );
    }

    private void searchAndAddFriend() {
        String username = searchUsernameEditText.getText().toString().trim();

        if (username.isEmpty()) {
            showSearchMessage("Enter username first.");
            return;
        }

        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showSearchMessage("Player not found.");
                        return;
                    }

                    String friendId = querySnapshot.getDocuments().get(0).getId();

                    if (friendId.equals(currentUid)) {
                        showSearchMessage("You cannot add yourself.");
                        return;
                    }

                    String friendUsername = querySnapshot.getDocuments().get(0).getString("username");
                    String avatar = querySnapshot.getDocuments().get(0).getString("avatar");
                    String region = querySnapshot.getDocuments().get(0).getString("region");

                    Long stars = querySnapshot.getDocuments().get(0).getLong("stars");
                    Long league = querySnapshot.getDocuments().get(0).getLong("league");

                    addFriend(friendId, friendUsername, avatar, region, stars, league);
                })
                .addOnFailureListener(e ->
                        showSearchMessage("Search failed.")
                );
    }

    private void addFriend(String friendId, String username, String avatar,
                           String region, Long stars, Long league) {

        db.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(friendId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        showSearchMessage("This player is already your friend.");
                        return;
                    }

                    Map<String, Object> friend = new HashMap<>();
                    friend.put("friendId", friendId);
                    friend.put("username", username);
                    friend.put("avatar", avatar != null ? avatar : "owl");
                    friend.put("region", region != null ? region : "");
                    friend.put("stars", stars != null ? stars : 0);
                    friend.put("league", league != null ? league : 0);
                    friend.put("createdAt", Timestamp.now());

                    addFriendBothWays(friendId, friend);
                });
    }

    private void showSearchMessage(String message) {
        searchResultTextView.setVisibility(View.VISIBLE);
        searchResultTextView.setText(message);
    }

    private void loadFriends() {
        friendsContainer.removeAllViews();

        db.collection("users")
                .document(currentUid)
                .collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showEmptyFriends();
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String username = document.getString("username");
                        String avatar = document.getString("avatar");
                        Long stars = document.getLong("stars");
                        Long league = document.getLong("league");

                        String friendId = document.getString("friendId");

                        addFriendCard(
                                friendId != null ? friendId : document.getId(),
                                username != null ? username : "Unknown",
                                avatar != null ? avatar : "owl",
                                stars != null ? stars : 0,
                                league != null ? league : 0
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show()
                );
    }

    private void showEmptyFriends() {
        TextView empty = new TextView(this);

        empty.setText("No friends yet.\nSearch by username or scan QR code.");
        empty.setTextColor(android.graphics.Color.parseColor("#6F50B5"));
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
        empty.setBackgroundResource(R.drawable.bg_profile_card);
        empty.setPadding(16, 24, 16, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        empty.setLayoutParams(params);

        friendsContainer.addView(empty);
    }

    private void addFriendCard(String friendId, String username, String avatar, long stars, long league) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_profile_card);
        card.setPadding(18, 18, 18, 18);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cardParams);

        ImageView avatarView = new ImageView(this);
        avatarView.setImageResource(getAvatarResource(avatar));
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarView.setBackgroundResource(R.drawable.bg_avatar_circle);
        avatarView.setPadding(5, 5, 5, 5);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(dpToPx(76), dpToPx(76));

        avatarParams.setMargins(0,0,18,0);
        avatarView.setLayoutParams(avatarParams);
        card.addView(avatarView);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        infoLayout.setLayoutParams(infoParams);

        TextView usernameView = new TextView(this);
        usernameView.setText(username);
        usernameView.setTextColor(android.graphics.Color.parseColor("#2D1457"));
        usernameView.setTextSize(18);
        usernameView.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
        infoLayout.addView(usernameView);

        TextView leagueView = new TextView(this);
        League friendLeague = LeagueManager.getLeague((int) league);
        leagueView.setText(friendLeague.getIcon() + " " + friendLeague.getName());
        leagueView.setTextColor(android.graphics.Color.parseColor("#6F50B5"));
        leagueView.setTextSize(13);
        leagueView.setTypeface(null, android.graphics.Typeface.ITALIC);
        infoLayout.addView(leagueView);

        TextView starsView = new TextView(this);
        starsView.setText("⭐ " + stars);
        starsView.setTextColor(android.graphics.Color.parseColor("#2D1457"));
        starsView.setTextSize(14);
        starsView.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(starsView);

        TextView monthlyRankView = new TextView(this);
        monthlyRankView.setText("🏆 Monthly rank: loading...");
        monthlyRankView.setTextColor(android.graphics.Color.parseColor("#6F50B5"));
        monthlyRankView.setTextSize(12);
        monthlyRankView.setTypeface(null, android.graphics.Typeface.ITALIC);
        infoLayout.addView(monthlyRankView);

        loadFriendMonthlyRank(friendId, monthlyRankView);

        card.addView(infoLayout);

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        //play button
        TextView playButton = new TextView(this);
        playButton.setText("PLAY");
        playButton.setTextColor(android.graphics.Color.WHITE);
        playButton.setTextSize(16);
        playButton.setTypeface(null, android.graphics.Typeface.BOLD_ITALIC);
        playButton.setGravity(android.view.Gravity.CENTER);
        playButton.setBackgroundResource(R.drawable.bg_purple_button);
        playButton.setClickable(true);
        playButton.setFocusable(true);

        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                dpToPx(96),
                dpToPx(48)
        );
        playParams.setMargins(dpToPx(12), 0, dpToPx(6), 0);
        playButton.setLayoutParams(playParams);

        playButton.setOnClickListener(v -> sendFriendlyMatchRequest(friendId, username));

        //three dots
        TextView moreButton = new TextView(this);
        moreButton.setText("⋮");
        moreButton.setTextColor(android.graphics.Color.parseColor("#5A31C8"));
        moreButton.setTextSize(28);
        moreButton.setTypeface(null, android.graphics.Typeface.BOLD);
        moreButton.setGravity(android.view.Gravity.CENTER);
        moreButton.setClickable(true);
        moreButton.setFocusable(true);

        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                dpToPx(36),
                dpToPx(48)
        );
        moreButton.setLayoutParams(moreParams);

        moreButton.setOnClickListener(v -> {
            android.widget.PopupMenu popupMenu =
                    new android.widget.PopupMenu(FriendsActivity.this, moreButton);

            popupMenu.getMenu().add("Remove friend");

            popupMenu.setOnMenuItemClickListener(item -> {
                removeFriend(friendId);
                return true;
            });

            popupMenu.show();
        });

        buttonsLayout.addView(playButton);
        buttonsLayout.addView(moreButton);

        card.addView(buttonsLayout);

        friendsContainer.addView(card);
    }

    private int getAvatarResource(String avatar) {
        if (avatar == null) {
            return R.drawable.avatar_owl;
        }

        switch (avatar) {
            case "fox":
                return R.drawable.avatar_fox;
            case "penguin":
                return R.drawable.avatar_penguin;
            case "wolf":
                return R.drawable.avatar_wolf;
            case "cat":
                return R.drawable.avatar_cat;
            case "dog":
                return R.drawable.avatar_dog;
            case "guest":
                return R.drawable.avatar_guest;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(FriendsActivity.this, HomeActivity.class)));

        findViewById(R.id.navFriends).setOnClickListener(v ->
                Toast.makeText(this, "You are already on Friends", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(FriendsActivity.this, ProfileActivity.class)));

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(FriendsActivity.this, NotificationCenterActivity.class)));

        findViewById(R.id.navStatistics).setOnClickListener(v ->
                startActivity(new Intent(FriendsActivity.this, ProfileActivity.class)));

        findViewById(R.id.navLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(FriendsActivity.this, LeaderboardActivity.class)));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void addFriendBothWays(String friendId, Map<String, Object> friendData) {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentUserDoc -> {
                    String myUsername = currentUserDoc.getString("username");
                    String myAvatar = currentUserDoc.getString("avatar");
                    String myRegion = currentUserDoc.getString("region");
                    Long myStars = currentUserDoc.getLong("stars");
                    Long myLeague = currentUserDoc.getLong("league");

                    Map<String, Object> myDataForFriend = new HashMap<>();
                    myDataForFriend.put("friendId", currentUid);
                    myDataForFriend.put("username", myUsername != null ? myUsername : "Unknown");
                    myDataForFriend.put("avatar", myAvatar != null ? myAvatar : "owl");
                    myDataForFriend.put("region", myRegion != null ? myRegion : "");
                    myDataForFriend.put("stars", myStars != null ? myStars : 0);
                    myDataForFriend.put("league", myLeague != null ? myLeague : 0);
                    myDataForFriend.put("createdAt", Timestamp.now());

                    db.collection("users")
                            .document(currentUid)
                            .collection("friends")
                            .document(friendId)
                            .set(friendData);

                    db.collection("users")
                            .document(friendId)
                            .collection("friends")
                            .document(currentUid)
                            .set(myDataForFriend)
                            .addOnSuccessListener(unused -> {
                                showSearchMessage("Friend added.");
                                searchUsernameEditText.setText("");
                                loadFriends();
                            })
                            .addOnFailureListener(e ->
                                    showSearchMessage("Failed to add friend.")
                            );
                });
    }

    private void removeFriend(String friendId) {
        db.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(friendId)
                .delete()
                .addOnSuccessListener(unused ->
                        db.collection("users")
                                .document(friendId)
                                .collection("friends")
                                .document(currentUid)
                                .delete()
                                .addOnSuccessListener(unused2 -> {
                                    Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show();
                                    loadFriends();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Removed only from your list", Toast.LENGTH_SHORT).show()
                                )
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove friend", Toast.LENGTH_SHORT).show()
                );
    }

    private void sendFriendlyMatchRequest(String friendId, String friendUsername) {
        if (friendId == null || friendId.isEmpty()) {
            Toast.makeText(this, "Friend not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(friendId).get()
                .addOnSuccessListener(friendDoc -> {
                    if (!friendDoc.exists()) {
                        Toast.makeText(this, "Friend profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Boolean online = friendDoc.getBoolean("online");

                    if (!Boolean.TRUE.equals(online)) {
                        Toast.makeText(this, friendUsername + " is not online.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Boolean inGame = friendDoc.getBoolean("inGame");

                    if (Boolean.TRUE.equals(inGame)) {
                        Toast.makeText(this, friendUsername + " is already in a game.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createFriendlyRequest(friendId, friendUsername);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to check friend status.", Toast.LENGTH_SHORT).show()
                );
    }

    private void createFriendlyRequest(String friendId, String friendUsername) {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(myDoc -> {
                    String loadedUsername = myDoc.getString("username");
                    final String myUsername = (loadedUsername != null && !loadedUsername.isEmpty())
                            ? loadedUsername
                            : "Your friend";

                    Map<String, Object> request = new HashMap<>();
                    request.put("fromUid", currentUid);
                    request.put("fromUsername", myUsername);
                    request.put("toUid", friendId);
                    request.put("toUsername", friendUsername);
                    request.put("status", "pending");
                    request.put("friendly", true);
                    request.put("createdAt", FieldValue.serverTimestamp());

                    long expiresAt = System.currentTimeMillis() + 10000;

                    request.put("expiresAt", expiresAt);

                    db.collection("friendlyRequests")
                            .add(request)
                            .addOnSuccessListener(documentReference -> {
                                String requestId = documentReference.getId();
                                activeRequestId = requestId;
                                showCancelRequestDialog(requestId, friendUsername);
                                listenForFriendlyRequestResponse(requestId, friendId, friendUsername);

                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    db.collection("friendlyRequests")
                                            .document(requestId)
                                            .get()
                                            .addOnSuccessListener(doc -> {
                                                if (doc.exists() && "pending".equals(doc.getString("status"))) {
                                                    doc.getReference().update(
                                                            "status", "expired",
                                                            "respondedAt", FieldValue.serverTimestamp()
                                                    );
                                                }
                                            });
                                }, 10000);

                                notificationFactory.sendFriendInvite(
                                        FriendsActivity.this,
                                        friendId,
                                        myUsername,
                                        requestId
                                );

                                Toast.makeText(
                                        this,
                                        "Request sent to " + friendUsername + ".",
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load your profile.", Toast.LENGTH_SHORT).show()
                );
    }

    private void listenForFriendlyRequestResponse(String requestId, String friendId, String friendUsername) {
        Toast.makeText(this, "Waiting for " + friendUsername + "...", Toast.LENGTH_SHORT).show();

        if (friendlyRequestListener != null) {
            friendlyRequestListener.remove();
            friendlyRequestListener = null;
        }

        friendlyRequestListener = db.collection("friendlyRequests")
                .document(requestId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String status = snapshot.getString("status");

                    if ("accepted".equals(status)) {

                        String gameId = snapshot.getString("gameId");

                        activeRequestId = null;
                        dismissPendingRequestDialog();
                        stopFriendlyRequestListener();

                        if (gameId == null) {
                            Toast.makeText(this, "Game not found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Intent intent = new Intent(FriendsActivity.this, GameActivity.class);
                        intent.putExtra("gameId", gameId);
                        startActivity(intent);
                    } else if ("declined".equals(status)) {
                        stopFriendlyRequestListener();
                        activeRequestId = null;
                        dismissPendingRequestDialog();
                        Toast.makeText(this, friendUsername + " declined the invite.", Toast.LENGTH_SHORT).show();
                    } else if ("expired".equals(status)) {
                        stopFriendlyRequestListener();
                        activeRequestId = null;
                        dismissPendingRequestDialog();
                        Toast.makeText(this, "Invite expired.", Toast.LENGTH_SHORT).show();
                    } else if ("cancelled".equals(status)) {
                        stopFriendlyRequestListener();
                        activeRequestId = null;
                        dismissPendingRequestDialog();
                        Toast.makeText(this, "Invite cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void stopFriendlyRequestListener() {
        if (friendlyRequestListener != null) {
            friendlyRequestListener.remove();
            friendlyRequestListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopFriendlyRequestListener();
        super.onDestroy();
    }

    private void showCancelRequestDialog(String requestId, String friendUsername) {
        pendingRequestDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Waiting for " + friendUsername)
                .setMessage("Friendly match request is pending.")
                .setNegativeButton("Cancel request", (dialog, which) -> cancelFriendlyRequest(requestId))
                .setPositiveButton("Keep waiting", null)
                .create();

        pendingRequestDialog.show();
    }

    private void dismissPendingRequestDialog() {
        if (pendingRequestDialog != null && pendingRequestDialog.isShowing()) {
            pendingRequestDialog.dismiss();
        }
        pendingRequestDialog = null;
    }

    private void cancelFriendlyRequest(String requestId) {
        if (requestId == null) return;

        db.collection("friendlyRequests")
                .document(requestId)
                .update(
                        "status", "cancelled",
                        "respondedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    activeRequestId = null;
                    stopFriendlyRequestListener();
                    Toast.makeText(this, "Request cancelled.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel request.", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadFriendMonthlyRank(String friendId, TextView monthlyRankView) {
        String monthlyCycleId = getCurrentMonthlyCycleId();

        db.collection("leaderboards")
                .document(monthlyCycleId)
                .collection("entries")
                .orderBy("stars", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int rank = 1;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (doc.getId().equals(friendId)) {
                            monthlyRankView.setText("🏆 Monthly rank: #" + rank);
                            return;
                        }
                        rank++;
                    }

                    monthlyRankView.setText("🏆 Monthly rank: Not ranked");
                })
                .addOnFailureListener(e ->
                        monthlyRankView.setText("🏆 Monthly rank: unavailable")
                );
    }

    private String getCurrentMonthlyCycleId() {
        Calendar calendar = Calendar.getInstance();

        return String.format(
                Locale.ROOT,
                "monthly_%04d_%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1
        );
    }
}
