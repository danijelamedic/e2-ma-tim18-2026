package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        View layoutHero = findViewById(R.id.layoutHero);
        View layoutLoginCard = findViewById(R.id.layoutLoginCard);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);

        Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        slideUp.setStartOffset(120);

        layoutHero.startAnimation(bounceIn);
        layoutLoginCard.startAnimation(slideUp);

        View.OnFocusChangeListener focusAnim = (v, hasFocus) ->
                v.animate()
                        .scaleX(hasFocus ? 1.02f : 1.0f)
                        .scaleY(hasFocus ? 1.02f : 1.0f)
                        .setDuration(200)
                        .start();

        etEmail.setOnFocusChangeListener(focusAnim);
        etPassword.setOnFocusChangeListener(focusAnim);

        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvResetPassword = findViewById(R.id.tvResetPassword);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvResetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    @Override protected void onStart() { super.onStart(); }
    @Override protected void onResume() { super.onResume(); }
    @Override protected void onPause() { super.onPause(); }
    @Override protected void onStop() { super.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); }
    @Override protected void onRestart() { super.onRestart(); }
}