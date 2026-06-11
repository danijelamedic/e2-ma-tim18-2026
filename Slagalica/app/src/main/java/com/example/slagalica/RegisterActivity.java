package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etRegion = findViewById(R.id.etRegion);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String region = etRegion.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || region.isEmpty()
                    || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("email", email);
                            user.put("region", region);
                            user.put("tokens", 5);
                            user.put("stars", 0);
                            user.put("league", 0);
                            user.put("avatarUrl", "");
                            user.put("createdAt", System.currentTimeMillis());

                            db.collection("users").document(uid).set(user)
                                    .addOnSuccessListener(unused -> {
                                        mAuth.getCurrentUser().sendEmailVerification()
                                                .addOnCompleteListener(emailTask -> {
                                                    mAuth.signOut();
                                                    Toast.makeText(this,
                                                            "Registration successful! Please check your email to verify your account.",
                                                            Toast.LENGTH_LONG).show();
                                                    startActivity(new Intent(this, LoginActivity.class));
                                                    finish();
                                                });
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Error: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show());
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}