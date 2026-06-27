package com.termux.app.feature;

import android.content.Context;
import android.content.SharedPreferences;

public class TermnxFeaturePrefs {

    private static final String PREFS_NAME = "termnx_features";
    private static final String KEY_APP_LOCK = "app_lock";
    private static final String KEY_STATUS_BAR = "status_bar";

    private final SharedPreferences prefs;

    public TermnxFeaturePrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply();
    }

    public boolean isStatusBarEnabled() {
        return prefs.getBoolean(KEY_STATUS_BAR, false);
    }

    public void setStatusBarEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_STATUS_BAR, enabled).apply();
    }
}
