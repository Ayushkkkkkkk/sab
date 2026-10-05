package com.sab.pawcare.ui.task;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.databinding.ActivityTaskEditBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.Validation;

import java.util.ArrayList;
import java.util.List;

public class TaskEditActivity extends AppCompatActivity {
    public static final String EXTRA_TASK_ID = "task_id";
    private ActivityTaskEditBinding binding;
    private long taskId = -1;
    private ChecklistTask existing;
    private List<Pet> pets = new ArrayList<>();
    private int minutes = 9 * 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        String[] cats = new String[]{
                Routine.CAT_FEEDING, Routine.CAT_WALKING, Routine.CAT_GROOMING,
                Routine.CAT_MEDICATION, Routine.CAT_HEALTHCARE, Routine.CAT_CLEANING
        };
        binding.inputCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cats));
        binding.inputTime.setOnClickListener(v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(minutes / 60)
                    .setMinute(minutes % 60)
                    .setTitleText("Task time")
                    .build();
            picker.addOnPositiveButtonClickListener(x -> {
                minutes = picker.getHour() * 60 + picker.getMinute();
                binding.inputTime.setText(DateUtils.formatTime(minutes));
            });
            picker.show(getSupportFragmentManager(), "time");
        });
        binding.inputTime.setText(DateUtils.formatTime(minutes));
        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setVisibility(taskId > 0 ? View.VISIBLE : View.GONE);
        binding.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this task?")
                .setNegativeButton("Cancel", (d, w) -> { })
                .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() -> {
                    if (existing != null) AppDatabase.get(this).checklistTaskDao().delete(existing);
                    runOnUiThread(this::finish);
                }))
                .show());
        AppExecutors.IO.execute(() -> {
            pets = AppDatabase.get(this).petDao().listByOwner(new SessionManager(this).getUserId());
            if (taskId > 0) existing = AppDatabase.get(this).checklistTaskDao().findById(taskId);
            runOnUiThread(this::bind);
        });
    }

    private void bind() {
        List<String> names = new ArrayList<>();
        for (Pet p : pets) names.add(p.name);
        binding.inputPet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        if (existing != null) {
            binding.toolbar.setTitle("Edit task");
            binding.inputTitle.setText(existing.title);
            binding.inputCategory.setText(existing.category, false);
            minutes = existing.scheduledMinutes;
            binding.inputTime.setText(DateUtils.formatTime(minutes));
            binding.inputNotes.setText(existing.notes);
            binding.inputSupplies.setText(existing.supplies);
            binding.switchDone.setChecked(existing.completed);
            for (Pet p : pets) {
                if (p.id == existing.petId) {
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
        for (Pet p : pets) if (p.name.equals(name)) return p;
        return pets.isEmpty() ? null : pets.get(0);
    }

    private void save() {
        String title = String.valueOf(binding.inputTitle.getText()).trim();
        String category = String.valueOf(binding.inputCategory.getText()).trim();
        Pet pet = selectedPet();
        if (Validation.isBlank(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }
        if (pet == null) {
            binding.layoutPet.setError("Select a pet");
            return;
        }
        if (Validation.isBlank(category)) category = Routine.CAT_HEALTHCARE;
        String cat = category;
        boolean weekly = binding.switchWeekly.isChecked();
        AppExecutors.IO.execute(() -> {
            ChecklistTask t = existing != null ? existing : new ChecklistTask();
            t.ownerId = new SessionManager(this).getUserId();
            t.petId = pet.id;
            t.title = title;
            t.category = cat;
            t.notes = String.valueOf(binding.inputNotes.getText()).trim();
            t.supplies = String.valueOf(binding.inputSupplies.getText()).trim();
            t.scheduledMinutes = minutes;
            t.scope = weekly ? ChecklistTask.SCOPE_WEEKLY : ChecklistTask.SCOPE_DAILY;
            t.taskDate = weekly ? DateUtils.weekKey(System.currentTimeMillis()) : DateUtils.todayKey();
            t.completed = binding.switchDone.isChecked();
            t.completedAt = t.completed ? System.currentTimeMillis() : 0;
            if (existing == null) {
                t.customTask = true;
                t.routineId = null;
                AppDatabase.get(this).checklistTaskDao().insert(t);
            } else {
                AppDatabase.get(this).checklistTaskDao().update(t);
            }
            runOnUiThread(() -> {
                Snackbar.make(binding.getRoot(), "Task saved", Snackbar.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
