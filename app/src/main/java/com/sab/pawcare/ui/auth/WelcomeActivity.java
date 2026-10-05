package com.sab.pawcare.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sab.pawcare.databinding.ActivityWelcomeBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.main.MainActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        binding.btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
