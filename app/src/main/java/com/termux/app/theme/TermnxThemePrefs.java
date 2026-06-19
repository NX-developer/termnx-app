package com.termux.app.theme;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TermnxThemePrefs {

    private static final String PREFS_NAME = "termnx_theme";
    private static final String KEY_TERM_BG = "terminal_background";
    private static final String KEY_TERM_FG = "terminal_foreground";
    private static final String KEY_KEY_TEXT = "extra_key_text";
    private static final String KEY_KEY_ACTIVE = "extra_key_active";

    public static final int UNSET = 0;

    private final SharedPreferences prefs;

    public TermnxThemePrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTerminalBackground() {
        return prefs.getInt(KEY_TERM_BG, UNSET);
    }

    public int getTerminalForeground() {
        return prefs.getInt(KEY_TERM_FG, UNSET);
    }

    public int getExtraKeyTextColor() {
        return prefs.getInt(KEY_KEY_TEXT, UNSET);
    }

    public int getExtraKeyActiveColor() {
        return prefs.getInt(KEY_KEY_ACTIVE, UNSET);
    }

    public void setTerminalBackground(int color) {
        prefs.edit().putInt(KEY_TERM_BG, color).apply();
    }

    public void setTerminalForeground(int color) {
        prefs.edit().putInt(KEY_TERM_FG, color).apply();
    }

    public void setExtraKeyTextColor(int color) {
        prefs.edit().putInt(KEY_KEY_TEXT, color).apply();
    }

    public void setExtraKeyActiveColor(int color) {
        prefs.edit().putInt(KEY_KEY_ACTIVE, color).apply();
    }

    public void reset() {
        prefs.edit().clear().apply();
    }

    public static String toHex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public void writeTerminalColorsFile() throws Exception {
        int bg = getTerminalBackground();
        int fg = getTerminalForeground();
        if (bg == UNSET && fg == UNSET) {
            return;
        }
        File file = new File(TermuxConstants.TERMUX_COLOR_PROPERTIES_FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        List<String> lines = new ArrayList<>();
        if (file.exists()) {
            String existing = readFile(file);
            for (String line : existing.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("background") || trimmed.startsWith("foreground")) {
                    continue;
                }
                if (!trimmed.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        if (bg != UNSET) {
            lines.add("background=" + toHex(bg));
        }
        if (fg != UNSET) {
            lines.add("foreground=" + toHex(fg));
        }
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line).append('\n');
        }
        writeFile(file, out.toString());
    }

    private static String readFile(File file) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
        }
        return new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void writeFile(File file, String content) throws Exception {
        try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
            out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
