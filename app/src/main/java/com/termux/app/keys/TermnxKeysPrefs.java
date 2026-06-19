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

    public void saveKeys(List<CustomKey> keys) {
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
        prefs.edit().putString(KEY_CUSTOM, array.toString()).apply();
    }

    public void addKey(String label, String value) {
        List<CustomKey> keys = getKeys();
        keys.add(new CustomKey(label, value));
        saveKeys(keys);
    }

    public void removeAt(int index) {
        List<CustomKey> keys = getKeys();
        if (index >= 0 && index < keys.size()) {
            keys.remove(index);
            saveKeys(keys);
        }
    }

    private static String sanitize(String value) {
        return value.replace("\\", "").replace("'", "").replace("\n", " ").trim();
    }

    public static String appendCustomRow(Context context, String layout) {
        if (layout == null) return null;
        List<CustomKey> keys = new TermnxKeysPrefs(context).getKeys();
        if (keys.isEmpty()) return layout;

        StringBuilder row = new StringBuilder("[");
        boolean first = true;
        for (CustomKey key : keys) {
            String value = sanitize(key.value);
            String label = sanitize(key.label);
            if (value.isEmpty()) continue;
            if (!first) row.append(", ");
            row.append("{key: '").append(value).append("', display: '")
                .append(label.isEmpty() ? value : label).append("'}");
            first = false;
        }
        row.append("]");
        if (first) return layout;

        int idx = layout.lastIndexOf(']');
        if (idx < 0) return layout;
        return layout.substring(0, idx) + ", " + row + "]";
    }
}
