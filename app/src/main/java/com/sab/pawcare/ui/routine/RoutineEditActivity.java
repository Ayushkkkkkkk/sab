package com.sab.pawcare.ui.routine;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.databinding.ActivityRoutineEditBinding;
import com.sab.pawcare.notify.ReminderReceiver;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.Validation;

import java.util.ArrayList;
import java.util.List;

public class RoutineEditActivity extends AppCompatActivity {
    public static final String EXTRA_ROUTINE_ID = "routine_id";
    public static final String EXTRA_PET_ID = "pet_id";

    private ActivityRoutineEditBinding binding;
    private long routineId = -1;
    private long presetPetId = -1;
    private Routine existing;
    private List<Pet> pets = new ArrayList<>();
    private int minutes = 8 * 60;
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoutineEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        routineId = getIntent().getLongExtra(EXTRA_ROUTINE_ID, -1);
        presetPetId = getIntent().getLongExtra(EXTRA_PET_ID, -1);
        String[] cats = new String[]{
                Routine.CAT_FEEDING, Routine.CAT_WALKING, Routine.CAT_GROOMING,
                Routine.CAT_MEDICATION, Routine.CAT_HEALTHCARE, Routine.CAT_CLEANING
        };
        binding.inputCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cats));
        for (int i = 0; i < DAYS.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(DAYS[i]);
            chip.setCheckable(true);
            chip.setTag(i);
            binding.dayChips.addView(chip);
        }
        binding.inputTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(minutes / 60)
                    .setMinute(minutes % 60)
                    .setTitleText("Scheduled time")
                    .build();
            picker.addOnPositiveButtonClickListener(x -> {
                minutes = picker.getHour() * 60 + picker.getMinute();
                binding.inputTime.setText(DateUtils.formatTime(minutes));
            });
            picker.show(getSupportFragmentManager(), "time");
        });
        binding.inputTime.setText(DateUtils.formatTime(minutes));
        binding.groupFreq.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) binding.dayChips.setVisibility(id == binding.btnWeekly.getId() ? View.VISIBLE : View.GONE);
        });
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnDelete.setVisibility(routineId > 0 ? View.VISIBLE : View.GONE);
        AppExecutors.IO.execute(() -> {
            long userId = new SessionManager(this).getUserId();
            pets = AppDatabase.get(this).petDao().listByOwner(userId);
            if (routineId > 0) existing = AppDatabase.get(this).routineDao().findById(routineId);
            runOnUiThread(this::bind);
        });
    }

    private void bind() {
        List<String> names = new ArrayList<>();
        for (Pet p : pets) names.add(p.name);
        binding.inputPet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        if (existing != null) {
            binding.toolbar.setTitle("Edit routine");
            binding.inputTitle.setText(existing.title);
            binding.inputCategory.setText(existing.category, false);
            minutes = existing.scheduledMinutes;
            binding.inputTime.setText(DateUtils.formatTime(minutes));
            binding.inputInstructions.setText(existing.instructions);
            binding.inputSupplies.setText(existing.supplies);
            binding.switchReminder.setChecked(existing.reminderEnabled);
            boolean weekly = Routine.FREQ_WEEKLY.equals(existing.frequency);
            binding.groupFreq.check(weekly ? binding.btnWeekly.getId() : binding.btnDaily.getId());
            binding.dayChips.setVisibility(weekly ? View.VISIBLE : View.GONE);
            for (int i = 0; i < binding.dayChips.getChildCount(); i++) {
                Chip chip = (Chip) binding.dayChips.getChildAt(i);
                int bit = 1 << i;
                chip.setChecked((existing.weekdaysMask & bit) != 0);
            }
            for (Pet p : pets) {
                if (p.id == existing.petId) {
                    binding.inputPet.setText(p.name, false);
                    break;
                }
            }
        } else if (presetPetId > 0) {
            for (Pet p : pets) {
                if (p.id == presetPetId) {
                    binding.inputPet.setText(p.name, false);
                    break;
                }
            }
        } else if (!pets.isEmpty()) {
            binding.inputPet.setText(pets.get(0).name, false);
        }
    }

    private Pet selectedPet() {
        String name = String.valueOf(binding.inputPet.getText()).trim();
        for (Pet p : pets) {
            if (p.name.equals(name)) return p;
        }
        return pets.isEmpty() ? null : pets.get(0);
    }

    private int weekdayMask() {
        int mask = 0;
        for (int i = 0; i < binding.dayChips.getChildCount(); i++) {
            Chip chip = (Chip) binding.dayChips.getChildAt(i);
            if (chip.isChecked()) mask |= (1 << i);
        }
        return mask;
    }

    private void save() {
        String title = String.valueOf(binding.inputTitle.getText()).trim();
        String category = String.valueOf(binding.inputCategory.getText()).trim();
        Pet pet = selectedPet();
        binding.layoutTitle.setError(null);
        binding.layoutCategory.setError(null);
        binding.layoutPet.setError(null);
        if (Validation.isBlank(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }
        if (Validation.isBlank(category)) {
            binding.layoutCategory.setError("Pick a category");
            return;
        }
        if (pet == null) {
            binding.layoutPet.setError("Assign this routine to a pet");
            return;
        }
        boolean weekly = binding.groupFreq.getCheckedButtonId() == binding.btnWeekly.getId();
        int mask = weekly ? weekdayMask() : 0;
        if (weekly && mask == 0) {
            Snackbar.make(binding.getRoot(), "Select at least one weekday", Snackbar.LENGTH_LONG).show();
            return;
        }
        AppExecutors.IO.execute(() -> {
            AppDatabase db = AppDatabase.get(this);
            Routine r = existing != null ? existing : new Routine();
            r.petId = pet.id;
            r.title = title;
            r.category = category;
            r.frequency = weekly ? Routine.FREQ_WEEKLY : Routine.FREQ_DAILY;
            r.scheduledMinutes = minutes;
            r.weekdaysMask = mask;
            r.instructions = String.valueOf(binding.inputInstructions.getText()).trim();
            r.supplies = String.valueOf(binding.inputSupplies.getText()).trim();
            r.reminderEnabled = binding.switchReminder.isChecked();
            if (existing == null) {
                r.createdAt = System.currentTimeMillis();
                db.routineDao().insert(r);
            } else {
                db.routineDao().update(r);
            }
            ReminderReceiver.rescheduleAll(getApplicationContext(), new SessionManager(this).getUserId());
            runOnUiThread(() -> {
                Snackbar.make(binding.getRoot(), "Routine saved", Snackbar.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete routine?")
                .setMessage("Generated checklist items stay unless you delete them separately.")
                .setNegativeButton("Cancel", (d, w) -> { })
                .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() -> {
                    if (existing != null) AppDatabase.get(this).routineDao().delete(existing);
                    runOnUiThread(this::finish);
                }))
                .show();
    }
}
