package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .update("online", true)
                    .addOnCompleteListener(task -> {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    });

            return;
        }

        android.view.View layoutHero = findViewById(R.id.layoutHero);
        android.view.View layoutLoginCard = findViewById(R.id.layoutLoginCard);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);

        Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        slideUp.setStartOffset(120);
        layoutHero.startAnimation(bounceIn);
        layoutLoginCard.startAnimation(slideUp);

        android.view.View.OnFocusChangeListener focusAnim = (v, hasFocus) ->
                v.animate().scaleX(hasFocus ? 1.02f : 1.0f)
                        .scaleY(hasFocus ? 1.02f : 1.0f)
                        .setDuration(200).start();
        etEmail.setOnFocusChangeListener(focusAnim);
        etPassword.setOnFocusChangeListener(focusAnim);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String emailOrUsername = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (emailOrUsername.contains("@")) {
                loginWithEmail(emailOrUsername, password);
            } else {
                loginWithUsername(emailOrUsername, password);
            }
        });

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

           }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser().isEmailVerified()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            db.collection("users")
                                    .document(uid)
                                    .update("online", true)
                                    .addOnCompleteListener(updateTask -> {
                                        startActivity(new Intent(this, HomeActivity.class));
                                        finish();
                                    });
                        } else {
                            Toast.makeText(this, "Please verify your email first!", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginWithUsername(String username, String password) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String email = querySnapshot.getDocuments().get(0).getString("email");
                        loginWithEmail(email, password);
                    } else {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}