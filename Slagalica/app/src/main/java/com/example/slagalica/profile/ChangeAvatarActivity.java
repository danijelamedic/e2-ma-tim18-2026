package com.example.slagalica.profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangeAvatarActivity extends AppCompatActivity {

    private String selectedAvatar;

    private ImageView avatarOwl;
    private ImageView avatarFox;
    private ImageView avatarPenguin;
    private ImageView avatarWolf;
    private ImageView avatarCat;
    private ImageView avatarDog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_avatar);

        avatarOwl = findViewById(R.id.avatarOwl);
        avatarFox = findViewById(R.id.avatarFox);
        avatarPenguin = findViewById(R.id.avatarPenguin);
        avatarWolf = findViewById(R.id.avatarWolf);
        avatarCat = findViewById(R.id.avatarCat);
        avatarDog = findViewById(R.id.avatarDog);

        selectedAvatar = getIntent().getStringExtra("currentAvatar");

        if (selectedAvatar == null || selectedAvatar.isEmpty()) {
            selectedAvatar = "owl";
        }

        selectAvatar(selectedAvatar);

        avatarOwl.setOnClickListener(v -> selectAvatar("owl"));
        avatarFox.setOnClickListener(v -> selectAvatar("fox"));
        avatarPenguin.setOnClickListener(v -> selectAvatar("penguin"));
        avatarWolf.setOnClickListener(v -> selectAvatar("wolf"));
        avatarCat.setOnClickListener(v -> selectAvatar("cat"));
        avatarDog.setOnClickListener(v -> selectAvatar("dog"));

        findViewById(R.id.btnCancelAvatar).setOnClickListener(v -> finish());

        findViewById(R.id.btnSaveAvatar).setOnClickListener(v -> saveAvatar());
    }

    private void selectAvatar(String avatar) {
        selectedAvatar = avatar;

        resetAvatarBackgrounds();

        switch (avatar) {
            case "fox":
                avatarFox.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
            case "penguin":
                avatarPenguin.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
            case "wolf":
                avatarWolf.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
            case "cat":
                avatarCat.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
            case "dog":
                avatarDog.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
            case "owl":
            default:
                avatarOwl.setBackgroundResource(R.drawable.bg_avatar_selected);
                break;
        }
    }

    private void resetAvatarBackgrounds() {
        avatarOwl.setBackground(null);
        avatarFox.setBackground(null);
        avatarPenguin.setBackground(null);
        avatarWolf.setBackground(null);
        avatarCat.setBackground(null);
        avatarDog.setBackground(null);
    }

    private void saveAvatar() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("avatar", selectedAvatar)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Avatar changed", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to change avatar", Toast.LENGTH_SHORT).show()
                );
    }
}