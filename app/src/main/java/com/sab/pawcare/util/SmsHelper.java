package com.sab.pawcare.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;

import java.util.List;

public final class SmsHelper {
    private SmsHelper() {}

    public static String buildMessage(Pet pet, List<Routine> routines, List<ChecklistTask> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("PawCare handoff for ").append(pet.name).append("\n");
        sb.append(pet.animalType);
        if (pet.breed != null && !pet.breed.isEmpty()) {
            sb.append(" • ").append(pet.breed);
        }
        sb.append("\n");
        if (pet.allergies != null && !pet.allergies.isEmpty()) {
            sb.append("Allergies: ").append(pet.allergies).append("\n");
        }
        if (pet.dietaryPreferences != null && !pet.dietaryPreferences.isEmpty()) {
            sb.append("Diet: ").append(pet.dietaryPreferences).append("\n");
        }
        if (pet.medicalNotes != null && !pet.medicalNotes.isEmpty()) {
            sb.append("Medical: ").append(pet.medicalNotes).append("\n");
        }
        sb.append("\nFeeding:\n");
        appendCategory(sb, routines, Routine.CAT_FEEDING);
        sb.append("\nWalking / Exercise:\n");
        appendCategory(sb, routines, Routine.CAT_WALKING);
        sb.append("\nMedication:\n");
        appendCategory(sb, routines, Routine.CAT_MEDICATION);
        sb.append("\nCare instructions:\n");
        boolean any = false;
        for (Routine r : routines) {
            if (r.instructions != null && !r.instructions.trim().isEmpty()) {
                sb.append("- ").append(r.title).append(": ").append(r.instructions).append("\n");
                any = true;
            }
        }
        if (!any) sb.append("- Follow the usual home routine.\n");
        if (tasks != null && !tasks.isEmpty()) {
            sb.append("\nToday's checklist:\n");
            for (ChecklistTask t : tasks) {
                sb.append(t.completed ? "[x] " : "[ ] ");
                sb.append(t.title).append(" @ ").append(DateUtils.formatTime(t.scheduledMinutes)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static void appendCategory(StringBuilder sb, List<Routine> routines, String category) {
        boolean any = false;
        for (Routine r : routines) {
            if (category.equals(r.category)) {
                sb.append("- ").append(r.title)
                        .append(" at ").append(DateUtils.formatTime(r.scheduledMinutes));
                if (Routine.FREQ_WEEKLY.equals(r.frequency)) {
                    sb.append(" (").append(DateUtils.weekdayLabel(r.weekdaysMask)).append(")");
                }
                if (r.instructions != null && !r.instructions.isEmpty()) {
                    sb.append(" — ").append(r.instructions);
                }
                sb.append("\n");
                any = true;
            }
        }
        if (!any) sb.append("- None listed\n");
    }

    public static void openSms(Context context, String phone, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phone));
        intent.putExtra("sms_body", body);
        context.startActivity(intent);
    }
}
