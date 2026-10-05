package com.sab.pawcare.notify;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.sab.pawcare.R;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Appointment;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.Routine;
import com.sab.pawcare.ui.main.MainActivity;
import com.sab.pawcare.util.DateUtils;

import java.util.Calendar;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "pawcare_care";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet care reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Feeding, medication, and appointment reminders");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    public static void rescheduleAll(Context context, long ownerId) {
        ensureChannel(context);
        AppDatabase db = AppDatabase.get(context);
        List<Routine> routines = db.routineDao().listByOwner(ownerId);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (Routine r : routines) {
            if (!r.reminderEnabled) continue;
            if (!Routine.CAT_FEEDING.equals(r.category) && !Routine.CAT_MEDICATION.equals(r.category)) {
                continue;
            }
            Pet pet = db.petDao().findById(r.petId);
            String title = r.category + " reminder";
            String body = (pet != null ? pet.name + ": " : "") + r.title + " at " + DateUtils.formatTime(r.scheduledMinutes);
            scheduleNext(context, am, (int) (200000 + r.id), r.scheduledMinutes, title, body);
        }
        List<Appointment> upcoming = db.appointmentDao().upcoming(ownerId, System.currentTimeMillis() - 60_000);
        for (Appointment a : upcoming) {
            if (!a.reminderEnabled) continue;
            Pet pet = db.petDao().findById(a.petId);
            String title = a.type + " reminder";
            String body = (pet != null ? pet.name + ": " : "") + a.title;
            scheduleAt(context, am, (int) (300000 + a.id), a.startsAt - 3_600_000L, title, body);
        }
    }

    private static void scheduleNext(Context context, AlarmManager am, int requestCode, int minutes, String title, String body) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, minutes / 60);
        c.set(Calendar.MINUTE, minutes % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        scheduleAt(context, am, requestCode, c.getTimeInMillis(), title, body);
    }

    private static void scheduleAt(Context context, AlarmManager am, int requestCode, long when, String title, String body) {
        if (when < System.currentTimeMillis()) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_BODY, body);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        if (title == null) title = "PawCare";
        if (body == null) body = "You have a pet care reminder.";
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(
                context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_paw)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
