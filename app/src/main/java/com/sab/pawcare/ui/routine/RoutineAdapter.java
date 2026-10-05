package com.sab.pawcare.ui.routine;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.databinding.ItemRoutineBinding;
import com.sab.pawcare.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.Holder> {
    public interface Listener {
        void onClick(Routine routine);
    }

    private final Listener listener;
    private final List<Routine> items = new ArrayList<>();
    private final java.util.Map<Long, String> petNames = new java.util.HashMap<>();

    public RoutineAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setPetNames(java.util.Map<Long, String> names) {
        petNames.clear();
        if (names != null) petNames.putAll(names);
        notifyDataSetChanged();
    }

    public void submit(List<Routine> routines) {
        items.clear();
        if (routines != null) items.addAll(routines);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRoutineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Routine r = items.get(position);
        holder.binding.routineTitle.setText(r.title);
        String pet = petNames.getOrDefault(r.petId, "Pet");
        String freq = Routine.FREQ_DAILY.equals(r.frequency)
                ? "Daily"
                : "Weekly • " + DateUtils.weekdayLabel(r.weekdaysMask);
        holder.binding.routineMeta.setText(pet + " • " + r.category + " • " + freq + " • " + DateUtils.formatTime(r.scheduledMinutes));
        holder.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemRoutineBinding binding;
        Holder(ItemRoutineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
