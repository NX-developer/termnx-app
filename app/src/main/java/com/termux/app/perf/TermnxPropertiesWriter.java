package com.termux.app.perf;

import com.termux.shared.termux.TermuxConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TermnxPropertiesWriter {

    public static void setProperty(String key, String value) throws Exception {
        File file = new File(TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        List<String> lines = new ArrayList<>();
        if (file.exists()) {
            for (String line : readFile(file).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith(key + "=") || trimmed.startsWith(key + " =")
                    || trimmed.startsWith("#" + key)) {
                    continue;
                }
                lines.add(line);
            }
        }
        lines.add(key + "=" + value);

        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                out.append(line).append('\n');
            }
        }
        writeFile(file, out.toString());
    }

    private static String readFile(File file) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void writeFile(File file, String content) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
