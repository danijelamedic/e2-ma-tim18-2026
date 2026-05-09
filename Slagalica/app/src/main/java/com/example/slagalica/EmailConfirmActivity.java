package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EmailConfirmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_confirm);

        Button btnResend = findViewById(R.id.btnResend);
        Button btnNext = findViewById(R.id.btnNext);

        btnResend.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.resend_email), Toast.LENGTH_SHORT).show();
        });

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(EmailConfirmActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    protected void onPause() { super.onPause(); }

    @Override
    protected void onStop() { super.onStop(); }

    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onRestart() { super.onRestart(); }
}