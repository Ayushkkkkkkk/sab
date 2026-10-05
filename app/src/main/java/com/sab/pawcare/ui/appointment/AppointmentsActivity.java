package com.sab.pawcare.ui.appointment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.sab.pawcare.R;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Appointment;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.databinding.ActivityAppointmentsBinding;
import com.sab.pawcare.databinding.DialogAppointmentBinding;
import com.sab.pawcare.notify.ReminderReceiver;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.Validation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentsActivity extends AppCompatActivity {
    private ActivityAppointmentsBinding binding;
    private final List<Appointment> items = new ArrayList<>();
    private final Map<Long, String> petNames = new HashMap<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppointmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        adapter = new Adapter();
        binding.recycler.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> showDialog(null));
        long userId = new SessionManager(this).getUserId();
        AppDatabase db = AppDatabase.get(this);
        db.petDao().observeByOwner(userId).observe(this, pets -> {
            petNames.clear();
            if (pets != null) for (Pet p : pets) petNames.put(p.id, p.name);
            adapter.notifyDataSetChanged();
        });
        db.appointmentDao().observeByOwner(userId).observe(this, list -> {
            items.clear();
            if (list != null) items.addAll(list);
            adapter.notifyDataSetChanged();
            binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showDialog(Appointment existing) {
        DialogAppointmentBinding d = DialogAppointmentBinding.inflate(getLayoutInflater());
        List<String> names = new ArrayList<>(petNames.values());
        d.inputPet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        d.inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{
                Appointment.TYPE_VET, Appointment.TYPE_VACCINATION, Appointment.TYPE_GROOMING, Appointment.TYPE_OTHER
        }));
        final long[] when = {existing != null ? existing.startsAt : System.currentTimeMillis() + 3_600_000};
        d.inputWhen.setText(DateUtils.formatDateTime(when[0]));
        d.inputWhen.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setSelection(when[0])
                    .setTitleText("Appointment date")
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(when[0]);
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                MaterialTimePicker tp = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_12H)
                        .setHour(hour)
                        .setMinute(minute)
                        .build();
                tp.addOnPositiveButtonClickListener(x -> {
                    when[0] = DateUtils.combineDateAndMinutes(sel, tp.getHour() * 60 + tp.getMinute());
                    d.inputWhen.setText(DateUtils.formatDateTime(when[0]));
                });
                tp.show(getSupportFragmentManager(), "time");
            });
            picker.show(getSupportFragmentManager(), "date");
        });
        if (existing != null) {
            d.inputTitle.setText(existing.title);
            d.inputType.setText(existing.type, false);
            d.inputLocation.setText(existing.location);
            d.inputNotes.setText(existing.notes);
            d.switchReminder.setChecked(existing.reminderEnabled);
            String pn = petNames.get(existing.petId);
            if (pn != null) d.inputPet.setText(pn, false);
        } else if (!names.isEmpty()) {
            d.inputPet.setText(names.get(0), false);
            d.inputType.setText(Appointment.TYPE_VET, false);
            d.switchReminder.setChecked(true);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add appointment" : "Edit appointment")
                .setView(d.getRoot())
                .setNegativeButton("Cancel", (di, w) -> { })
                .setPositiveButton("Save", (di, w) -> {
                    String title = String.valueOf(d.inputTitle.getText()).trim();
                    String petName = String.valueOf(d.inputPet.getText()).trim();
                    if (Validation.isBlank(title) || Validation.isBlank(petName)) {
                        Snackbar.make(binding.getRoot(), "Title and pet are required", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Long petId = null;
                    for (Map.Entry<Long, String> e : petNames.entrySet()) {
                        if (e.getValue().equals(petName)) petId = e.getKey();
                    }
                    if (petId == null) {
                        Snackbar.make(binding.getRoot(), "Pick a registered pet", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Appointment a = existing != null ? existing : new Appointment();
                    a.petId = petId;
                    a.title = title;
                    a.type = String.valueOf(d.inputType.getText()).trim();
                    a.location = String.valueOf(d.inputLocation.getText()).trim();
                    a.notes = String.valueOf(d.inputNotes.getText()).trim();
                    a.startsAt = when[0];
                    a.reminderEnabled = d.switchReminder.isChecked();
                    AppExecutors.IO.execute(() -> {
                        if (existing == null) AppDatabase.get(this).appointmentDao().insert(a);
                        else AppDatabase.get(this).appointmentDao().update(a);
                        ReminderReceiver.rescheduleAll(getApplicationContext(), new SessionManager(this).getUserId());
                    });
                })
                .show();
    }

    class Adapter extends RecyclerView.Adapter<Adapter.H> {
        @NonNull
        @Override
        public H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_two_line, parent, false);
            return new H(v);
        }

        @Override
        public void onBindViewHolder(@NonNull H holder, int position) {
            Appointment a = items.get(position);
            holder.title.setText(a.title + " • " + a.type);
            holder.sub.setText(petNames.getOrDefault(a.petId, "Pet") + " • " + DateUtils.formatDateTime(a.startsAt));
            holder.itemView.setOnClickListener(v -> showDialog(a));
            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(AppointmentsActivity.this)
                        .setTitle("Delete appointment?")
                        .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() ->
                                AppDatabase.get(AppointmentsActivity.this).appointmentDao().delete(a)))
                        .setNegativeButton("Cancel", (d, w) -> { })
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class H extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView sub;
            H(View v) {
                super(v);
                title = v.findViewById(R.id.lineTitle);
                sub = v.findViewById(R.id.lineSub);
            }
        }
    }
}
