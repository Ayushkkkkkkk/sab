package com.sab.pawcare.ui.sms;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.databinding.ActivitySmsBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.SmsHelper;
import com.sab.pawcare.util.Validation;

import java.util.ArrayList;
import java.util.List;

public class SmsDelegateActivity extends AppCompatActivity {
    private ActivitySmsBinding binding;
    private List<Pet> pets = new ArrayList<>();
    private String preview = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySmsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.inputPet.setOnItemClickListener((p, v, pos, id) -> rebuild());
        binding.switchIncludeChecklist.setOnCheckedChangeListener((b, c) -> rebuild());
        binding.btnSend.setOnClickListener(v -> send());
        AppExecutors.IO.execute(() -> {
            pets = AppDatabase.get(this).petDao().listByOwner(new SessionManager(this).getUserId());
            runOnUiThread(() -> {
                List<String> names = new ArrayList<>();
                for (Pet pet : pets) names.add(pet.name);
                binding.inputPet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
                if (!pets.isEmpty()) {
                    binding.inputPet.setText(pets.get(0).name, false);
                    rebuild();
                }
            });
        });
    }

    private Pet selected() {
        String name = String.valueOf(binding.inputPet.getText()).trim();
        for (Pet p : pets) if (p.name.equals(name)) return p;
        return pets.isEmpty() ? null : pets.get(0);
    }

    private void rebuild() {
        Pet pet = selected();
        if (pet == null) {
            binding.preview.setText("Add a pet first.");
            return;
        }
        boolean include = binding.switchIncludeChecklist.isChecked();
        AppExecutors.IO.execute(() -> {
            List<Routine> routines = AppDatabase.get(this).routineDao().listByPet(pet.id);
            List<ChecklistTask> tasks = include
                    ? AppDatabase.get(this).checklistTaskDao().listByDate(new SessionManager(this).getUserId(), DateUtils.todayKey())
                    : new ArrayList<>();
            List<ChecklistTask> forPet = new ArrayList<>();
            for (ChecklistTask t : tasks) if (t.petId == pet.id) forPet.add(t);
            preview = SmsHelper.buildMessage(pet, routines, forPet);
            runOnUiThread(() -> binding.preview.setText(preview));
        });
    }

    private void send() {
        String phone = String.valueOf(binding.inputPhone.getText()).trim();
        binding.layoutPhone.setError(null);
        if (!Validation.isValidPhone(phone)) {
            binding.layoutPhone.setError("Enter a valid phone number");
            return;
        }
        if (preview == null || preview.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Generate a message first", Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            SmsHelper.openSms(this, phone, preview);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "No SMS app is available on this device", Snackbar.LENGTH_LONG).show();
        }
    }
}
