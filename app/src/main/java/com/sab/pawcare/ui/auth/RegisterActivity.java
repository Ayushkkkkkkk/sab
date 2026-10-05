package com.sab.pawcare.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.User;
import com.sab.pawcare.databinding.ActivityRegisterBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.main.MainActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.Validation;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.linkLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String name = String.valueOf(binding.inputName.getText()).trim();
        String email = String.valueOf(binding.inputEmail.getText()).trim();
        String password = String.valueOf(binding.inputPassword.getText());
        String confirm = String.valueOf(binding.inputConfirm.getText());
        binding.layoutName.setError(null);
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.layoutConfirm.setError(null);
        boolean ok = true;
        if (Validation.isBlank(name) || name.length() < 2) {
            binding.layoutName.setError("Enter your name");
            ok = false;
        }
        if (!Validation.isValidEmail(email)) {
            binding.layoutEmail.setError("Enter a valid email");
            ok = false;
        }
        if (!Validation.isValidPassword(password)) {
            binding.layoutPassword.setError("At least 6 characters");
            ok = false;
        }
        if (!password.equals(confirm)) {
            binding.layoutConfirm.setError("Passwords do not match");
            ok = false;
        }
        if (!ok) return;
        AppExecutors.IO.execute(() -> {
            AppDatabase db = AppDatabase.get(this);
            if (db.userDao().findByEmail(email.toLowerCase()) != null) {
                runOnUiThread(() -> binding.layoutEmail.setError("An account already uses this email"));
                return;
            }
            User user = new User();
            user.fullName = name;
            user.email = email.toLowerCase();
            user.passwordHash = Validation.sha256(password);
            user.createdAt = System.currentTimeMillis();
            long id = db.userDao().insert(user);
            runOnUiThread(() -> {
                new SessionManager(this).login(id);
                Snackbar.make(binding.getRoot(), "Account created", Snackbar.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });
    }
}
