package com.sab.pawcare.ui.pet;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.databinding.ItemPetCardBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.Holder> {
    public interface Listener {
        void onPetClick(Pet pet);
    }

    private final Listener listener;
    private final List<Pet> items = new ArrayList<>();

    public PetAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Pet> pets) {
        items.clear();
        if (pets != null) items.addAll(pets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPetCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Pet pet = items.get(position);
        holder.binding.petName.setText(pet.name);
        holder.binding.petMeta.setText(pet.animalType + " • " + pet.breed + " • " + pet.ageYears + " yrs");
        if (pet.photoUri != null && !pet.photoUri.isEmpty()) {
            File f = new File(pet.photoUri);
            if (f.exists()) {
                holder.binding.petPhoto.setImageURI(Uri.fromFile(f));
            } else {
                holder.binding.petPhoto.setImageResource(com.sab.pawcare.R.drawable.ic_paw);
            }
        } else {
            holder.binding.petPhoto.setImageResource(com.sab.pawcare.R.drawable.ic_paw);
        }
        holder.itemView.setOnClickListener(v -> listener.onPetClick(pet));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPetCardBinding binding;
        Holder(ItemPetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
