package com.sab.pawcare.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sab.pawcare.data.dao.AppointmentDao;
import com.sab.pawcare.data.dao.ChecklistTaskDao;
import com.sab.pawcare.data.dao.ExpenseDao;
import com.sab.pawcare.data.dao.PetDao;
import com.sab.pawcare.data.dao.PetPhotoDao;
import com.sab.pawcare.data.dao.RoutineDao;
import com.sab.pawcare.data.dao.UserDao;
import com.sab.pawcare.data.entity.Appointment;
import com.sab.pawcare.data.entity.ChecklistTask;
import com.sab.pawcare.data.entity.Expense;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.PetPhoto;
import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.data.entity.User;

@Database(
        entities = {
                User.class,
                Pet.class,
                PetPhoto.class,
                Routine.class,
                ChecklistTask.class,
                Expense.class,
                Appointment.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract PetDao petDao();
    public abstract PetPhotoDao petPhotoDao();
    public abstract RoutineDao routineDao();
    public abstract ChecklistTaskDao checklistTaskDao();
    public abstract ExpenseDao expenseDao();
    public abstract AppointmentDao appointmentDao();

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "pawcare.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
