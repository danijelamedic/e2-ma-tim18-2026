package com.example.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        EditText etOldPassword = findViewById(R.id.etOldPassword);
        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etNewPasswordConfirm = findViewById(R.id.etNewPasswordConfirm);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String newPasswordConfirm = etNewPasswordConfirm.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || newPasswordConfirm.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_fields_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(newPasswordConfirm)) {
                Toast.makeText(this, getString(R.string.error_passwords_no_match), Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ResetPasswordActivity.this, HomeActivity.class);
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
