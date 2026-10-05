package com.sab.pawcare.ui.more;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.User;
import com.sab.pawcare.databinding.FragmentMoreBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.ui.appointment.AppointmentsActivity;
import com.sab.pawcare.ui.auth.WelcomeActivity;
import com.sab.pawcare.ui.expense.ExpensesActivity;
import com.sab.pawcare.ui.sms.SmsDelegateActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.DateUtils;

public class MoreFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentMoreBinding binding = FragmentMoreBinding.inflate(inflater, container, false);
        SessionManager session = new SessionManager(requireContext());
        AppExecutors.IO.execute(() -> {
            User user = AppDatabase.get(requireContext()).userDao().findById(session.getUserId());
            long from = DateUtils.startOfMonth(System.currentTimeMillis());
            long to = DateUtils.endOfMonth(System.currentTimeMillis());
            double spent = AppDatabase.get(requireContext()).expenseDao().sumBetween(session.getUserId(), from, to);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (user != null) {
                    binding.userName.setText(user.fullName);
                    binding.userEmail.setText(user.email);
                }
                binding.monthSpend.setText(String.format(java.util.Locale.US, "This month: $%.2f", spent));
            });
        });
        binding.rowSms.setOnClickListener(v -> startActivity(new Intent(requireContext(), SmsDelegateActivity.class)));
        binding.rowExpenses.setOnClickListener(v -> startActivity(new Intent(requireContext(), ExpensesActivity.class)));
        binding.rowAppointments.setOnClickListener(v -> startActivity(new Intent(requireContext(), AppointmentsActivity.class)));
        binding.rowLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("You can sign back in with the same account.")
                .setNegativeButton("Cancel", (d, w) -> { })
                .setPositiveButton("Log out", (d, w) -> {
                    session.logout();
                    Intent i = new Intent(requireContext(), WelcomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .show());
        return binding.getRoot();
    }
}
