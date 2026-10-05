package com.sab.pawcare.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.databinding.FragmentHomeBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.checklist.TaskAdapter;
import com.sab.pawcare.ui.pet.PetAdapter;
import com.sab.pawcare.ui.pet.PetDetailActivity;
import com.sab.pawcare.ui.pet.PetEditActivity;
import com.sab.pawcare.ui.task.TaskEditActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.ChecklistGenerator;
import com.sab.pawcare.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private PetAdapter petAdapter;
    private TaskAdapter pendingAdapter;
    private TaskAdapter doneAdapter;
    private long userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        userId = new SessionManager(requireContext()).getUserId();
        petAdapter = new PetAdapter(pet -> {
            Intent i = new Intent(requireContext(), PetDetailActivity.class);
            i.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id);
            startActivity(i);
        });
        pendingAdapter = new TaskAdapter(new TaskAdapter.Listener() {
            @Override
            public void onToggle(ChecklistTask task) {
                toggle(task);
            }

            @Override
            public void onEdit(ChecklistTask task) {
                openTask(task.id);
            }
        });
        doneAdapter = new TaskAdapter(new TaskAdapter.Listener() {
            @Override
            public void onToggle(ChecklistTask task) {
                toggle(task);
            }

            @Override
            public void onEdit(ChecklistTask task) {
                openTask(task.id);
            }
        });
        binding.petRecycler.setAdapter(petAdapter);
        binding.pendingRecycler.setAdapter(pendingAdapter);
        binding.doneRecycler.setAdapter(doneAdapter);
        binding.pendingRecycler.setNestedScrollingEnabled(false);
        binding.doneRecycler.setNestedScrollingEnabled(false);
        binding.petRecycler.setNestedScrollingEnabled(false);
        binding.fabAddPet.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PetEditActivity.class)));
        AppDatabase db = AppDatabase.get(requireContext());
        db.petDao().observeByOwner(userId).observe(getViewLifecycleOwner(), pets -> {
            petAdapter.submit(pets);
            binding.emptyPets.setVisibility(pets == null || pets.isEmpty() ? View.VISIBLE : View.GONE);
        });
        String today = DateUtils.todayKey();
        db.checklistTaskDao().observeByDate(userId, today).observe(getViewLifecycleOwner(), this::bindTasks);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        AppExecutors.IO.execute(() -> ChecklistGenerator.generateForToday(AppDatabase.get(requireContext()), userId));
        loadCounts();
    }

    private void bindTasks(List<ChecklistTask> all) {
        List<ChecklistTask> pending = new ArrayList<>();
        List<ChecklistTask> done = new ArrayList<>();
        if (all != null) {
            for (ChecklistTask t : all) {
                if (t.completed) done.add(t);
                else pending.add(t);
            }
        }
        pendingAdapter.submit(pending);
        doneAdapter.submit(done);
        binding.pendingTitle.setText("Pending (" + pending.size() + ")");
        binding.doneTitle.setText("Completed (" + done.size() + ")");
        binding.emptyPending.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyDone.setVisibility(done.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadCounts() {
        AppExecutors.IO.execute(() -> {
            AppDatabase db = AppDatabase.get(requireContext());
            int pets = db.petDao().listByOwner(userId).size();
            int pending = db.checklistTaskDao().countPending(userId, DateUtils.todayKey());
            int done = db.checklistTaskDao().countCompleted(userId, DateUtils.todayKey());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                binding.statPets.setText(String.valueOf(pets));
                binding.statPending.setText(String.valueOf(pending));
                binding.statDone.setText(String.valueOf(done));
            });
        });
    }

    private void toggle(ChecklistTask task) {
        AppExecutors.IO.execute(() -> {
            task.completed = !task.completed;
            task.completedAt = task.completed ? System.currentTimeMillis() : 0;
            AppDatabase.get(requireContext()).checklistTaskDao().update(task);
            loadCounts();
        });
    }

    private void openTask(long id) {
        Intent i = new Intent(requireContext(), TaskEditActivity.class);
        i.putExtra(TaskEditActivity.EXTRA_TASK_ID, id);
        startActivity(i);
    }
}
