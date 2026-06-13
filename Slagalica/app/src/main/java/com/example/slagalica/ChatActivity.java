package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.notifications.AppNotification;
import com.example.slagalica.notifications.LocalNotificationSender;
import com.example.slagalica.notifications.NotificationChannelManager;
import com.example.slagalica.notifications.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private DatabaseReference chatRef;
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private String currentUid;
    private String currentUsername;
    private String currentRegion;
    private LinearLayout messagesContainer;
    private ScrollView scrollView;
    private EditText etMessage;
    private Button btnSend;
    private boolean isFirstLoad = true;
    private long lastMessageTime = 0;
    private boolean isInForeground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        NotificationChannelManager.createChannels(this);

        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        messagesContainer = findViewById(R.id.messagesContainer);
        scrollView = findViewById(R.id.scrollView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(snapshot -> {
                    currentUsername = snapshot.getString("username");
                    currentRegion = snapshot.getString("region");
                    if (currentRegion == null) currentRegion = "general";

                    chatRef = FirebaseDatabase.getInstance("https://slagalica-7a144-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("chats")
                            .child(currentRegion.replaceAll("\\s+", "_").toLowerCase());

                    listenForMessages();
                });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInForeground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInForeground = false;
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm",
                Locale.getDefault()).format(new Date());

        Map<String, Object> message = new HashMap<>();
        message.put("uid", currentUid);
        message.put("username", currentUsername);
        message.put("text", text);
        message.put("timestamp", timestamp);
        message.put("timeMillis", System.currentTimeMillis());

        chatRef.push().setValue(message);
        createChatNotificationsForRegion(text);
        etMessage.setText("");
    }

    private void createChatNotificationsForRegion(String text) {
        db.collection("users")
                .whereEqualTo("region", currentRegion)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot user : snapshot.getDocuments()) {
                        String uid = user.getId();
                        if (uid.equals(currentUid)) {
                            continue;
                        }
                        AppNotification notification = new AppNotification(
                                uid,
                                AppNotification.TYPE_CHAT,
                                "New message from " + currentUsername,
                                text,
                                AppNotification.ACTION_OPEN_CHAT,
                                null
                        );
                        notificationRepository.create(uid, notification);
                    }
                });
    }

    private void listenForMessages() {
        chatRef.orderByChild("timeMillis").limitToLast(50)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        messagesContainer.removeAllViews();
                        long latestTime = 0;
                        String latestSenderUid = null;
                        String latestSenderName = null;
                        String latestText = null;

                        for (DataSnapshot msg : snapshot.getChildren()) {
                            String uid = msg.child("uid").getValue(String.class);
                            String username = msg.child("username").getValue(String.class);
                            String text = msg.child("text").getValue(String.class);
                            String timestamp = msg.child("timestamp").getValue(String.class);
                            Long timeMillis = msg.child("timeMillis").getValue(Long.class);

                            addMessageView(uid, username, text, timestamp);

                            if (timeMillis != null && timeMillis > latestTime) {
                                latestTime = timeMillis;
                                latestSenderUid = uid;
                                latestSenderName = username;
                                latestText = text;
                            }
                        }

                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

                        if (!isFirstLoad && latestTime > lastMessageTime
                                && latestSenderUid != null
                                && !latestSenderUid.equals(currentUid)
                                && !isInForeground) {
                            showLocalChatNotification(latestSenderName, latestText);
                        }

                        if (latestTime > 0) lastMessageTime = latestTime;
                        isFirstLoad = false;
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void showLocalChatNotification(String senderName, String text) {
        AppNotification notification = new AppNotification(
                currentUid,
                AppNotification.TYPE_CHAT,
                "New message from " + senderName,
                text,
                AppNotification.ACTION_OPEN_CHAT,
                null
        );
        LocalNotificationSender.show(this, notification, new Intent(this, ChatActivity.class));
    }

    private void addMessageView(String uid, String username,
                                String text, String timestamp) {
        boolean isMe = currentUid.equals(uid);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapperParams.setMargins(0, 8, 0, 8);
        wrapper.setLayoutParams(wrapperParams);

        TextView tvUsername = new TextView(this);
        tvUsername.setText(username + " • " + timestamp);
        tvUsername.setTextSize(11);
        tvUsername.setTextColor(0xFF8B6FBF);
        LinearLayout.LayoutParams usernameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        usernameParams.setMargins(isMe ? 0 : 16, 0, isMe ? 16 : 0, 2);
        if (isMe) usernameParams.gravity = android.view.Gravity.END;
        tvUsername.setLayoutParams(usernameParams);

        TextView tvMessage = new TextView(this);
        tvMessage.setText(text);
        tvMessage.setTextSize(15);
        tvMessage.setTextColor(isMe ? 0xFFFFFFFF : 0xFF1E0A3C);
        tvMessage.setBackgroundResource(isMe ?
                R.drawable.bg_button_selector : R.drawable.bg_card_premium);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        msgParams.setMargins(isMe ? 80 : 16, 0, isMe ? 16 : 80, 0);
        if (isMe) msgParams.gravity = android.view.Gravity.END;
        tvMessage.setLayoutParams(msgParams);
        tvMessage.setPadding(24, 16, 24, 16);

        wrapper.addView(tvUsername);
        wrapper.addView(tvMessage);
        messagesContainer.addView(wrapper);
    }
}
