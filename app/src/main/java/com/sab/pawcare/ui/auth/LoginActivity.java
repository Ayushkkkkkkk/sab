package com.sab.pawcare.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.User;
import com.sab.pawcare.databinding.ActivityLoginBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.main.MainActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.Validation;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.linkRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void attemptLogin() {
        String email = String.valueOf(binding.inputEmail.getText()).trim();
        String password = String.valueOf(binding.inputPassword.getText());
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        boolean ok = true;
        if (!Validation.isValidEmail(email)) {
            binding.layoutEmail.setError("Enter a valid email");
            ok = false;
        }
        if (!Validation.isValidPassword(password)) {
            binding.layoutPassword.setError("Password must be at least 6 characters");
            ok = false;
        }
        if (!ok) return;
        String hash = Validation.sha256(password);
        AppExecutors.IO.execute(() -> {
            User user = AppDatabase.get(this).userDao().findByEmail(email.toLowerCase());
            runOnUiThread(() -> {
                if (user == null || !hash.equals(user.passwordHash)) {
                    Snackbar.make(binding.getRoot(), "Incorrect email or password", Snackbar.LENGTH_LONG).show();
                    return;
                }
                new SessionManager(this).login(user.id);
                Toast.makeText(this, "Welcome back, " + user.fullName, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });
    }
}
