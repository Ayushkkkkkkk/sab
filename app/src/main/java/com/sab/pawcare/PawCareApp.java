package com.sab.pawcare;

import android.app.Application;

import com.sab.pawcare.notify.ReminderReceiver;

public class PawCareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ReminderReceiver.ensureChannel(this);
    }
}
