package com.sab.pawcare.ui.checklist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.databinding.ItemTaskBinding;
import com.sab.pawcare.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.Holder> {
    public interface Listener {
        void onToggle(ChecklistTask task);
        void onEdit(ChecklistTask task);
    }

    private final Listener listener;
    private final List<ChecklistTask> items = new ArrayList<>();

    public TaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ChecklistTask> tasks) {
        items.clear();
        if (tasks != null) items.addAll(tasks);
        notifyDataSetChanged();
    }

    public ChecklistTask get(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChecklistTask t = items.get(position);
        holder.binding.taskTitle.setText(t.title);
        holder.binding.taskMeta.setText(String.format(Locale.getDefault(), "%s • %s",
                t.category, DateUtils.formatTime(t.scheduledMinutes)));
        holder.binding.taskCheck.setOnCheckedChangeListener(null);
        holder.binding.taskCheck.setChecked(t.completed);
        holder.binding.taskTitle.setPaintFlags(t.completed
                ? holder.binding.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.binding.taskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.binding.taskCheck.setOnCheckedChangeListener((button, checked) -> {
            if (checked != t.completed) listener.onToggle(t);
        });
        holder.itemView.setOnClickListener(v -> listener.onEdit(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemTaskBinding binding;
        Holder(ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
