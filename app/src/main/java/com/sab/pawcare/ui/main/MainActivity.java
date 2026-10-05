package com.sab.pawcare.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.sab.pawcare.R;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.databinding.ActivityMainBinding;
import com.sab.pawcare.notify.ReminderReceiver;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.checklist.ChecklistFragment;
import com.sab.pawcare.ui.home.HomeFragment;
import com.sab.pawcare.ui.more.MoreFragment;
import com.sab.pawcare.ui.pet.PetsFragment;
import com.sab.pawcare.ui.routine.RoutinesFragment;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.ChecklistGenerator;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState == null) {
            show(new HomeFragment());
        }
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) show(new HomeFragment());
            else if (id == R.id.nav_pets) show(new PetsFragment());
            else if (id == R.id.nav_routines) show(new RoutinesFragment());
            else if (id == R.id.nav_checklist) show(new ChecklistFragment());
            else if (id == R.id.nav_more) show(new MoreFragment());
            return true;
        });
        requestNotifications();
        AppExecutors.IO.execute(() -> {
            ChecklistGenerator.generateForToday(AppDatabase.get(this), session.getUserId());
            ReminderReceiver.rescheduleAll(getApplicationContext(), session.getUserId());
        });
    }

    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
