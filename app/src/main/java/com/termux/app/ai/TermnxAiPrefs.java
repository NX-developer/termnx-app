package com.termux.app.ai;

import android.content.Context;
import android.content.SharedPreferences;

public class TermnxAiPrefs {

    private static final String PREFS_NAME = "termnx_ai";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_FULL_ACCESS = "auto_run";
    private static final String KEY_EDIT_MODE = "edit_mode";
    private static final String KEY_BASE_URL = "base_url";

    public static final String DEFAULT_MODEL = "anthropic/claude-3.5-sonnet";
    public static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final SharedPreferences prefs;

    public TermnxAiPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String value) {
        prefs.edit().putString(KEY_API_KEY, value == null ? "" : value.trim()).apply();
    }

    public String getModel() {
        String value = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        return value == null || value.trim().isEmpty() ? DEFAULT_MODEL : value.trim();
    }

    public void setModel(String value) {
        prefs.edit().putString(KEY_MODEL, value == null ? DEFAULT_MODEL : value.trim()).apply();
    }

    public String getBaseUrl() {
        String value = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        return value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value.trim();
    }

    public void setBaseUrl(String value) {
        prefs.edit().putString(KEY_BASE_URL, value == null ? DEFAULT_BASE_URL : value.trim()).apply();
    }

    public boolean isFullAccess() {
        return prefs.getBoolean(KEY_FULL_ACCESS, false);
    }

    public void setFullAccess(boolean value) {
        prefs.edit().putBoolean(KEY_FULL_ACCESS, value).apply();
    }

    public boolean isEditMode() {
        return prefs.getBoolean(KEY_EDIT_MODE, false);
    }

    public void setEditMode(boolean value) {
        prefs.edit().putBoolean(KEY_EDIT_MODE, value).apply();
    }
}
