package com.termux.app.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BackupManager {

    private static final int VERSION = 1;

    private static final String[] CONFIG_FILES = {
        ".termux/termux.properties",
        ".termux/colors.properties",
        ".termux/font.ttf",
        ".bashrc",
        ".bash_profile",
        ".profile",
        ".vimrc",
        ".nanorc",
        ".inputrc",
        ".gitconfig"
    };

    public static String exportToStream(Context context, OutputStream output) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", VERSION);
        root.put("createdAt", System.currentTimeMillis());
        root.put("app", "termnx");

        JSONObject prefsObject = new JSONObject();
        int prefsCount = 0;
        File sharedPrefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        File[] prefsFiles = sharedPrefsDir.listFiles();
        if (prefsFiles != null) {
            for (File file : prefsFiles) {
                String name = file.getName();
                if (!name.endsWith(".xml")) continue;
                String prefsName = name.substring(0, name.length() - 4);
                SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                JSONObject entries = serializePrefs(prefs);
                if (entries.length() > 0) {
                    prefsObject.put(prefsName, entries);
                    prefsCount++;
                }
            }
        }
        root.put("prefs", prefsObject);

        JSONObject filesObject = new JSONObject();
        int filesCount = 0;
        File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        for (String relative : CONFIG_FILES) {
            File file = new File(home, relative);
            if (file.exists() && file.isFile()) {
                byte[] bytes = readAll(file);
                filesObject.put(relative, Base64.encodeToString(bytes, Base64.NO_WRAP));
                filesCount++;
            }
        }
        root.put("files", filesObject);

        output.write(root.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
        return prefsCount + " ayar grubu, " + filesCount + " yapılandırma dosyası dışa aktarıldı.";
    }

    public static String importFromStream(Context context, InputStream input) throws Exception {
        byte[] data = readAll(input);
        JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));

        int prefsCount = 0;
        JSONObject prefsObject = root.optJSONObject("prefs");
        if (prefsObject != null) {
            for (java.util.Iterator<String> it = prefsObject.keys(); it.hasNext(); ) {
                String prefsName = it.next();
                JSONObject entries = prefsObject.getJSONObject(prefsName);
                restorePrefs(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE), entries);
                prefsCount++;
            }
        }

        int filesCount = 0;
        JSONObject filesObject = root.optJSONObject("files");
        File home = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        if (filesObject != null) {
            for (java.util.Iterator<String> it = filesObject.keys(); it.hasNext(); ) {
                String relative = it.next();
                String encoded = filesObject.getString(relative);
                byte[] bytes = Base64.decode(encoded, Base64.NO_WRAP);
                File target = new File(home, relative);
                File parent = target.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                writeAll(target, bytes);
                filesCount++;
            }
        }
        return prefsCount + " ayar grubu, " + filesCount + " dosya geri yüklendi. Tam etki için uygulamayı yeniden başlat.";
    }

    private static JSONObject serializePrefs(SharedPreferences prefs) throws Exception {
        JSONObject result = new JSONObject();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            JSONObject node = new JSONObject();
            if (value instanceof Boolean) {
                node.put("t", "bool");
                node.put("v", (Boolean) value);
            } else if (value instanceof Integer) {
                node.put("t", "int");
                node.put("v", (Integer) value);
            } else if (value instanceof Long) {
                node.put("t", "long");
                node.put("v", (Long) value);
            } else if (value instanceof Float) {
                node.put("t", "float");
                node.put("v", ((Float) value).doubleValue());
            } else if (value instanceof String) {
                node.put("t", "string");
                node.put("v", (String) value);
            } else if (value instanceof Set) {
                node.put("t", "set");
                JSONArray array = new JSONArray();
                for (Object item : (Set<?>) value) {
                    array.put(String.valueOf(item));
                }
                node.put("v", array);
            } else {
                continue;
            }
            result.put(entry.getKey(), node);
        }
        return result;
    }

    private static void restorePrefs(SharedPreferences prefs, JSONObject entries) throws Exception {
        SharedPreferences.Editor editor = prefs.edit();
        for (java.util.Iterator<String> it = entries.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONObject node = entries.getJSONObject(key);
            String type = node.optString("t", "");
            switch (type) {
                case "bool":
                    editor.putBoolean(key, node.getBoolean("v"));
                    break;
                case "int":
                    editor.putInt(key, node.getInt("v"));
                    break;
                case "long":
                    editor.putLong(key, node.getLong("v"));
                    break;
                case "float":
                    editor.putFloat(key, (float) node.getDouble("v"));
                    break;
                case "string":
                    editor.putString(key, node.getString("v"));
                    break;
                case "set":
                    JSONArray array = node.getJSONArray("v");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < array.length(); i++) {
                        set.add(array.getString(i));
                    }
                    editor.putStringSet(key, set);
                    break;
                default:
                    break;
            }
        }
        editor.apply();
    }

    private static byte[] readAll(File file) throws Exception {
        try (InputStream in = new java.io.FileInputStream(file)) {
            return readAll(in);
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void writeAll(File file, byte[] bytes) throws Exception {
        try (OutputStream out = new java.io.FileOutputStream(file)) {
            out.write(bytes);
            out.flush();
        }
    }
}
