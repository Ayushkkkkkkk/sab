package com.sab.pawcare.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "pawcare_session";
    private static final String KEY_USER_ID = "user_id";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }
}
