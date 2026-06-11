package com.example.slagalica;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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

    private static final String CHANNEL_ID = "chat_notifications";
    private static final int NOTIF_ID = 1001;

    private DatabaseReference chatRef;
    private FirebaseFirestore db;
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

        createNotificationChannel();

        db = FirebaseFirestore.getInstance();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for new chat messages");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
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
        etMessage.setText("");
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
                            sendChatNotification(latestSenderName, latestText);
                        }

                        if (latestTime > 0) lastMessageTime = latestTime;
                        isFirstLoad = false;
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void sendChatNotification(String senderName, String text) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New message from " + senderName)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIF_ID, builder.build());
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