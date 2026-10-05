package com.sab.pawcare.ui.checklist;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.databinding.FragmentChecklistBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.task.TaskEditActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.ChecklistGenerator;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.ShakeDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChecklistFragment extends Fragment {
    private FragmentChecklistBinding binding;
    private TaskAdapter adapter;
    private long userId;
    private boolean weekly;
    private String query = "";
    private String filter = "ALL";
    private List<ChecklistTask> dailyTasks = new ArrayList<>();
    private List<ChecklistTask> weeklyTasks = new ArrayList<>();
    private ShakeDetector shakeDetector;
    private SensorManager sensorManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChecklistBinding.inflate(inflater, container, false);
        userId = new SessionManager(requireContext()).getUserId();
        adapter = new TaskAdapter(new TaskAdapter.Listener() {
            @Override
            public void onToggle(ChecklistTask task) {
                toggle(task);
            }

            @Override
            public void onEdit(ChecklistTask task) {
                Intent i = new Intent(requireContext(), TaskEditActivity.class);
                i.putExtra(TaskEditActivity.EXTRA_TASK_ID, task.id);
                startActivity(i);
            }
        });
        binding.recycler.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), TaskEditActivity.class)));
        binding.toggleScope.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            weekly = checkedId == binding.btnWeekly.getId();
            applyFilter();
        });
        binding.search.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                query = q == null ? "" : q;
                applyFilter();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                query = q == null ? "" : q;
                applyFilter();
                return true;
            }
        });
        binding.chipAll.setOnClickListener(v -> setFilter("ALL"));
        binding.chipPending.setOnClickListener(v -> setFilter("PENDING"));
        binding.chipDone.setOnClickListener(v -> setFilter("DONE"));
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos < 0 || pos >= adapter.getItemCount()) return;
                ChecklistTask task = adapter.get(pos);
                if (direction == ItemTouchHelper.RIGHT) {
                    if (!task.completed) toggle(task);
                    else adapter.notifyItemChanged(pos);
                    Snackbar.make(binding.getRoot(), "Marked completed", Snackbar.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(pos);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete task?")
                            .setMessage("This removes \"" + task.title + "\" from the checklist.")
                            .setNegativeButton("Cancel", (d, w) -> { })
                            .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() ->
                                    AppDatabase.get(requireContext()).checklistTaskDao().delete(task)))
                            .show();
                }
            }
        });
        helper.attachToRecyclerView(binding.recycler);
        shakeDetector = new ShakeDetector(this::confirmReset);
        observe();
        return binding.getRoot();
    }

    private void setFilter(String f) {
        filter = f;
        binding.chipAll.setChecked("ALL".equals(f));
        binding.chipPending.setChecked("PENDING".equals(f));
        binding.chipDone.setChecked("DONE".equals(f));
        applyFilter();
    }

    private void observe() {
        AppDatabase db = AppDatabase.get(requireContext());
        db.checklistTaskDao().observeByDate(userId, DateUtils.todayKey())
                .observe(getViewLifecycleOwner(), list -> {
                    dailyTasks = list == null ? new ArrayList<>() : list;
                    applyFilter();
                });
        db.checklistTaskDao().observeByDate(userId, DateUtils.weekKey(System.currentTimeMillis()))
                .observe(getViewLifecycleOwner(), list -> {
                    weeklyTasks = list == null ? new ArrayList<>() : list;
                    applyFilter();
                });
        AppExecutors.IO.execute(() -> ChecklistGenerator.generateForToday(db, userId));
    }

    private void applyFilter() {
        List<ChecklistTask> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.US);
        for (ChecklistTask t : weekly ? weeklyTasks : dailyTasks) {
            if ("PENDING".equals(filter) && t.completed) continue;
            if ("DONE".equals(filter) && !t.completed) continue;
            if (!q.isEmpty()) {
                String hay = (t.title + " " + t.category + " " + (t.notes == null ? "" : t.notes)).toLowerCase(Locale.US);
                if (!hay.contains(q)) continue;
            }
            out.add(t);
        }
        adapter.submit(out);
        binding.empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        binding.hint.setText(weekly
                ? "Weekly checklist • swipe right to complete, left to delete, shake to reset today"
                : "Daily checklist • swipe right to complete, left to delete, shake to reset today");
    }

    private void toggle(ChecklistTask task) {
        AppExecutors.IO.execute(() -> {
            task.completed = !task.completed;
            task.completedAt = task.completed ? System.currentTimeMillis() : 0;
            AppDatabase.get(requireContext()).checklistTaskDao().update(task);
        });
    }

    private void confirmReset() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset today's checklist?")
                .setMessage("All of today's tasks will be marked incomplete.")
                .setNegativeButton("Cancel", (d, w) -> { })
                .setPositiveButton("Reset", (d, w) -> AppExecutors.IO.execute(() ->
                        AppDatabase.get(requireContext()).checklistTaskDao().resetDate(userId, DateUtils.todayKey())))
                .show());
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) shakeDetector.register(sensorManager);
    }

    @Override
    public void onPause() {
        if (sensorManager != null) shakeDetector.unregister(sensorManager);
        super.onPause();
    }
}
