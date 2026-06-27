package com.termux.app.quick;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TermnxQuickPrefs {

    public static final int MAX_SLOTS = 6;
    private static final String PREFS_NAME = "termnx_quick";
    private static final String KEY_COMMANDS = "commands";

    private final SharedPreferences prefs;

    public TermnxQuickPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<String[]> getCommands() {
        List<String[]> result = new ArrayList<>();
        String raw = prefs.getString(KEY_COMMANDS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) continue;
                String label = obj.optString("label", "");
                String command = obj.optString("command", "");
                if (!command.isEmpty()) {
                    result.add(new String[]{label.isEmpty() ? command : label, command});
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public void setCommands(List<String[]> commands) {
        JSONArray array = new JSONArray();
        try {
            for (String[] entry : commands) {
                JSONObject obj = new JSONObject();
                obj.put("label", entry[0]);
                obj.put("command", entry[1]);
                array.put(obj);
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putString(KEY_COMMANDS, array.toString()).apply();
    }
}
