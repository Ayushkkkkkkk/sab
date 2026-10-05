package com.sab.pawcare.ui.pet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.databinding.FragmentPetsBinding;
import com.sab.pawcare.session.SessionManager;

public class PetsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentPetsBinding binding = FragmentPetsBinding.inflate(inflater, container, false);
        long userId = new SessionManager(requireContext()).getUserId();
        PetAdapter adapter = new PetAdapter(pet -> {
            Intent i = new Intent(requireContext(), PetDetailActivity.class);
            i.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id);
            startActivity(i);
        });
        binding.recycler.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), PetEditActivity.class)));
        AppDatabase.get(requireContext()).petDao().observeByOwner(userId).observe(getViewLifecycleOwner(), pets -> {
            adapter.submit(pets);
            binding.empty.setVisibility(pets == null || pets.isEmpty() ? View.VISIBLE : View.GONE);
        });
        return binding.getRoot();
    }
}
