package com.sab.pawcare.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) return;
        long userId = session.getUserId();
        AppExecutors.IO.execute(() -> ReminderReceiver.rescheduleAll(context.getApplicationContext(), userId));
    }
}
