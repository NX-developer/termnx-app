package com.termux.app.backup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TermnxBackupActivity extends AppCompatActivity {

    private static final int REQUEST_EXPORT = 4001;
    private static final int REQUEST_IMPORT = 4002;

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private TextView status;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Backup");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView info = new TextView(this);
        info.setText("Export all app settings, your ~/.termux configuration and key dotfiles (.bashrc, .vimrc, etc.) "
            + "into a single file. After a reinstall, pick that file to restore everything. "
            + "Save it outside the app (e.g. Downloads) so it survives uninstalling.");
        info.setTextColor(COLOR_DIM);
        info.setTextSize(13f);

        Button exportButton = new Button(this);
        exportButton.setText("Export");
        exportButton.setOnClickListener(v -> startExport());
        exportButton.setLayoutParams(buttonParams());

        Button importButton = new Button(this);
        importButton.setText("Import (restore)");
        importButton.setOnClickListener(v -> startImport());
        importButton.setLayoutParams(buttonParams());

        status = new TextView(this);
        status.setTextColor(COLOR_TEXT);
        status.setTextSize(13f);
        status.setPadding(0, dp(16), 0, 0);

        root.addView(info);
        root.addView(exportButton);
        root.addView(importButton);
        root.addView(status);
        setContentView(root);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        return params;
    }

    private void startExport() {
        String name = "termnx-backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, name);
        try {
            startActivityForResult(intent, REQUEST_EXPORT);
        } catch (Exception e) {
            status.setText("Could not open file picker: " + e.getMessage());
        }
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_IMPORT);
        } catch (Exception e) {
            status.setText("Could not open file picker: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) {
            runExport(uri);
        } else if (requestCode == REQUEST_IMPORT) {
            runImport(uri);
        }
    }

    private void runExport(final Uri uri) {
        status.setText("Exporting...");
        new Thread(() -> {
            String message;
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new RuntimeException("Could not open output stream");
                message = BackupManager.exportToStream(this, out);
            } catch (Exception e) {
                message = "Export error: " + e.getMessage();
            }
            final String result = message;
            main.post(() -> status.setText(result));
        }).start();
    }

    private void runImport(final Uri uri) {
        status.setText("Restoring...");
        new Thread(() -> {
            String message;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new RuntimeException("Could not open input stream");
                message = BackupManager.importFromStream(this, in);
            } catch (Exception e) {
                message = "Restore error: " + e.getMessage();
            }
            final String result = message;
            main.post(() -> status.setText(result));
        }).start();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
