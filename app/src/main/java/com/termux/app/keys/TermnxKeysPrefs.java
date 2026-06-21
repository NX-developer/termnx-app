package com.termux.app.keys;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TermnxKeysPrefs {

    private static final String PREFS_NAME = "termnx_keys";
    private static final String KEY_CUSTOM = "custom_keys";
    private static final String KEY_CUSTOMIZED = "customized";

    public static class CustomKey {
        public final String label;
        public final String value;

        public CustomKey(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private final SharedPreferences prefs;

    public TermnxKeysPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isCustomized() {
        return prefs.getBoolean(KEY_CUSTOMIZED, false);
    }

    public List<CustomKey> getKeys() {
        List<CustomKey> keys = new ArrayList<>();
        String raw = prefs.getString(KEY_CUSTOM, "");
        if (raw == null || raw.isEmpty()) return keys;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String label = obj.optString("label", "");
                String value = obj.optString("value", "");
                if (!value.isEmpty()) {
                    keys.add(new CustomKey(label.isEmpty() ? value : label, value));
                }
            }
        } catch (Exception ignored) {
        }
        return keys;
    }

    public void save(List<CustomKey> keys) {
        JSONArray array = new JSONArray();
        for (CustomKey key : keys) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("label", key.label);
                obj.put("value", key.value);
                array.put(obj);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(KEY_CUSTOM, array.toString()).putBoolean(KEY_CUSTOMIZED, true).apply();
    }

    public void reset() {
        prefs.edit().remove(KEY_CUSTOM).putBoolean(KEY_CUSTOMIZED, false).apply();
    }

    public static List<CustomKey> defaultSeed() {
        String[] values = {"ESC", "/", "-", "HOME", "UP", "END", "PGUP",
            "TAB", "CTRL", "ALT", "LEFT", "DOWN", "RIGHT", "PGDN", "ENTER"};
        List<CustomKey> list = new ArrayList<>();
        for (String value : values) {
            list.add(new CustomKey(value, value));
        }
        return list;
    }

    private static String sanitize(String value) {
        return value.replace("\\", "").replace("'", "").replace("\n", " ").trim();
    }

    public static String buildLayout(Context context, String defaultLayout) {
        TermnxKeysPrefs prefs = new TermnxKeysPrefs(context);
        if (!prefs.isCustomized()) return defaultLayout;
        List<CustomKey> keys = prefs.getKeys();

        StringBuilder layout = new StringBuilder("[");
        int perRow = 7;
        int count = 0;
        boolean rowOpen = false;
        boolean firstRow = true;
        boolean firstInRow = true;
        for (CustomKey key : keys) {
            String value = sanitize(key.value);
            String label = sanitize(key.label);
            if (value.isEmpty()) continue;

            if (!rowOpen) {
                if (!firstRow) layout.append(", ");
                layout.append("[");
                rowOpen = true;
                firstInRow = true;
            }
            if (!firstInRow) layout.append(", ");
            if (label.isEmpty() || label.equals(value)) {
                layout.append("'").append(value).append("'");
            } else {
                layout.append("{key: '").append(value).append("', display: '").append(label).append("'}");
            }
            firstInRow = false;
            count++;

            if (count % perRow == 0) {
                layout.append("]");
                rowOpen = false;
                firstRow = false;
            }
        }
        if (rowOpen) {
            layout.append("]");
        }
        layout.append("]");
        if (count == 0) return "[]";
        return layout.toString();
    }
}
