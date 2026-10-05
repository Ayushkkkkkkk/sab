package com.sab.pawcare.ui.expense;

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
import com.sab.pawcare.R;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Expense;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.databinding.ActivityExpensesBinding;
import com.sab.pawcare.databinding.DialogExpenseBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;
import com.sab.pawcare.util.Validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensesActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "pet_id";
    private ActivityExpensesBinding binding;
    private long filterPetId;
    private final List<Expense> items = new ArrayList<>();
    private final Map<Long, String> petNames = new HashMap<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpensesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        filterPetId = getIntent().getLongExtra(EXTRA_PET_ID, -1);
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
        db.expenseDao().observeByOwner(userId).observe(this, list -> {
            items.clear();
            if (list != null) {
                for (Expense e : list) {
                    if (filterPetId <= 0 || e.petId == filterPetId) items.add(e);
                }
            }
            adapter.notifyDataSetChanged();
            binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            refreshTotal();
        });
    }

    private void refreshTotal() {
        AppExecutors.IO.execute(() -> {
            long from = DateUtils.startOfMonth(System.currentTimeMillis());
            long to = DateUtils.endOfMonth(System.currentTimeMillis());
            double sum;
            if (filterPetId > 0) {
                sum = AppDatabase.get(this).expenseDao().sumForPetBetween(filterPetId, from, to);
            } else {
                sum = AppDatabase.get(this).expenseDao().sumBetween(new SessionManager(this).getUserId(), from, to);
            }
            runOnUiThread(() -> binding.total.setText(String.format(Locale.US, "This month: $%.2f", sum)));
        });
    }

    private void showDialog(Expense existing) {
        DialogExpenseBinding d = DialogExpenseBinding.inflate(getLayoutInflater());
        List<String> names = new ArrayList<>(petNames.values());
        d.inputPet.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        d.inputCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.expense_categories)));
        final long[] when = {existing != null ? existing.spentAt : System.currentTimeMillis()};
        d.inputDate.setText(DateUtils.formatDate(when[0]));
        d.inputDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setSelection(when[0])
                    .setTitleText("Expense date")
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                when[0] = sel;
                d.inputDate.setText(DateUtils.formatDate(sel));
            });
            picker.show(getSupportFragmentManager(), "date");
        });
        if (existing != null) {
            d.inputTitle.setText(existing.title);
            d.inputAmount.setText(String.valueOf(existing.amount));
            d.inputCategory.setText(existing.category, false);
            d.inputNotes.setText(existing.notes);
            String pn = petNames.get(existing.petId);
            if (pn != null) d.inputPet.setText(pn, false);
        } else if (filterPetId > 0 && petNames.containsKey(filterPetId)) {
            d.inputPet.setText(petNames.get(filterPetId), false);
        } else if (!names.isEmpty()) {
            d.inputPet.setText(names.get(0), false);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add expense" : "Edit expense")
                .setView(d.getRoot())
                .setNegativeButton("Cancel", (di, w) -> { })
                .setPositiveButton("Save", (di, w) -> {
                    String title = String.valueOf(d.inputTitle.getText()).trim();
                    String amountS = String.valueOf(d.inputAmount.getText()).trim();
                    String petName = String.valueOf(d.inputPet.getText()).trim();
                    if (Validation.isBlank(title) || Validation.isBlank(amountS) || Validation.isBlank(petName)) {
                        Snackbar.make(binding.getRoot(), "Title, amount, and pet are required", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(amountS);
                    } catch (NumberFormatException e) {
                        Snackbar.make(binding.getRoot(), "Enter a valid amount", Snackbar.LENGTH_LONG).show();
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
                    Expense exp = existing != null ? existing : new Expense();
                    exp.petId = petId;
                    exp.title = title;
                    exp.amount = amount;
                    exp.category = String.valueOf(d.inputCategory.getText()).trim();
                    exp.notes = String.valueOf(d.inputNotes.getText()).trim();
                    exp.spentAt = when[0];
                    AppExecutors.IO.execute(() -> {
                        if (existing == null) AppDatabase.get(this).expenseDao().insert(exp);
                        else AppDatabase.get(this).expenseDao().update(exp);
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
            Expense e = items.get(position);
            holder.title.setText(String.format(Locale.US, "%s • $%.2f", e.title, e.amount));
            holder.sub.setText(petNames.getOrDefault(e.petId, "Pet") + " • " + e.category + " • " + DateUtils.formatDate(e.spentAt));
            holder.itemView.setOnClickListener(v -> showDialog(e));
            holder.itemView.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(ExpensesActivity.this)
                        .setTitle("Delete expense?")
                        .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() ->
                                AppDatabase.get(ExpensesActivity.this).expenseDao().delete(e)))
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
