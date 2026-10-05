package com.sab.pawcare.util;

import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;

import java.util.List;

public final class ChecklistGenerator {
    private ChecklistGenerator() {}

    public static void generateForToday(AppDatabase db, long ownerId) {
        long now = System.currentTimeMillis();
        String dayKey = DateUtils.dateKey(now);
        String weekKey = DateUtils.weekKey(now);
        int todayBit = DateUtils.weekdayBit(now);
        List<Pet> pets = db.petDao().listByOwner(ownerId);
        for (Pet pet : pets) {
            List<Routine> routines = db.routineDao().listByPet(pet.id);
            for (Routine r : routines) {
                boolean daily = Routine.FREQ_DAILY.equals(r.frequency);
                boolean weeklyDue = Routine.FREQ_WEEKLY.equals(r.frequency) && (r.weekdaysMask & todayBit) != 0;
                if (!daily && !weeklyDue) continue;
                String key = daily ? dayKey : weekKey;
                if (db.checklistTaskDao().countGenerated(ownerId, key, r.id) > 0) continue;
                ChecklistTask t = new ChecklistTask();
                t.ownerId = ownerId;
                t.petId = pet.id;
                t.routineId = r.id;
                t.title = r.title + " — " + pet.name;
                t.category = r.category;
                t.notes = r.instructions;
                t.supplies = r.supplies;
                t.scheduledMinutes = r.scheduledMinutes;
                t.taskDate = key;
                t.scope = daily ? ChecklistTask.SCOPE_DAILY : ChecklistTask.SCOPE_WEEKLY;
                t.completed = false;
                t.customTask = false;
                db.checklistTaskDao().insert(t);
            }
        }
    }
}
