package com.sab.pawcare.ui.routine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.databinding.FragmentRoutinesBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutinesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentRoutinesBinding binding = FragmentRoutinesBinding.inflate(inflater, container, false);
        long userId = new SessionManager(requireContext()).getUserId();
        RoutineAdapter adapter = new RoutineAdapter(routine -> {
            Intent i = new Intent(requireContext(), RoutineEditActivity.class);
            i.putExtra(RoutineEditActivity.EXTRA_ROUTINE_ID, routine.id);
            startActivity(i);
        });
        binding.recycler.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> {
            AppExecutors.IO.execute(() -> {
                List<Pet> pets = AppDatabase.get(requireContext()).petDao().listByOwner(userId);
                requireActivity().runOnUiThread(() -> {
                    if (pets == null || pets.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "Add a pet before creating a routine", Snackbar.LENGTH_LONG).show();
                    } else {
                        startActivity(new Intent(requireContext(), RoutineEditActivity.class));
                    }
                });
            });
        });
        AppDatabase db = AppDatabase.get(requireContext());
        db.petDao().observeByOwner(userId).observe(getViewLifecycleOwner(), pets -> {
            Map<Long, String> names = new HashMap<>();
            if (pets != null) {
                for (Pet p : pets) names.put(p.id, p.name);
            }
            adapter.setPetNames(names);
        });
        db.routineDao().observeByOwner(userId).observe(getViewLifecycleOwner(), routines -> {
            adapter.submit(routines);
            binding.empty.setVisibility(routines == null || routines.isEmpty() ? View.VISIBLE : View.GONE);
        });
        return binding.getRoot();
    }
}
