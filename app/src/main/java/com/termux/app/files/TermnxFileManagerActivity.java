package com.termux.app.files;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.shared.termux.TermuxConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TermnxFileManagerActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_CARD = 0xFF161B22;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;
    private static final int COLOR_DIR = 0xFF58A6FF;
    private static final long MAX_EDIT_BYTES = 1024 * 1024;

    private File currentDir;
    private TextView pathView;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Files");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        currentDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
        if (!currentDir.exists()) {
            currentDir = new File("/");
        }
        setupBrowserView();
    }

    private void setupBrowserView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        pathView = new TextView(this);
        pathView.setTextColor(COLOR_DIM);
        pathView.setTextSize(12f);
        pathView.setPadding(dp(14), dp(12), dp(14), dp(8));
        root.addView(pathView);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(10), dp(2), dp(10), dp(10));
        scroll.addView(listContainer);
        root.addView(scroll);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(10), dp(4), dp(10), dp(12));

        Button homeButton = new Button(this);
        homeButton.setText("Home");
        homeButton.setOnClickListener(v -> {
            currentDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH);
            renderList();
        });
        homeButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button newFileButton = new Button(this);
        newFileButton.setText("New file");
        newFileButton.setOnClickListener(v -> showNewFileDialog());
        newFileButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button newFolderButton = new Button(this);
        newFolderButton.setText("New folder");
        newFolderButton.setOnClickListener(v -> showNewFolderDialog());
        newFolderButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        actions.addView(homeButton);
        actions.addView(newFileButton);
        actions.addView(newFolderButton);
        root.addView(actions);

        setContentView(root);
        renderList();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        File parent = currentDir.getParentFile();
        if (parent != null && currentDir.getAbsolutePath().length() > 1) {
            currentDir = parent;
            renderList();
        } else {
            super.onBackPressed();
        }
    }

    private void renderList() {
        pathView.setText(currentDir.getAbsolutePath());
        listContainer.removeAllViews();

        File parent = currentDir.getParentFile();
        if (parent != null && currentDir.getAbsolutePath().length() > 1) {
            listContainer.addView(buildRow("..", COLOR_DIR, v -> {
                currentDir = parent;
                renderList();
            }, null));
        }

        File[] files = currentDir.listFiles();
        if (files == null) {
            TextView denied = new TextView(this);
            denied.setText("Cannot read this folder.");
            denied.setTextColor(COLOR_DIM);
            denied.setPadding(dp(6), dp(10), dp(6), dp(10));
            listContainer.addView(denied);
            return;
        }

        List<File> entries = new ArrayList<>();
        Collections.addAll(entries, files);
        Collections.sort(entries, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (final File file : entries) {
            boolean isDir = file.isDirectory();
            String name = (isDir ? "[D] " : "[F] ") + file.getName();
            listContainer.addView(buildRow(name, isDir ? COLOR_DIR : COLOR_TEXT, v -> {
                if (isDir) {
                    currentDir = file;
                    renderList();
                } else {
                    openFile(file);
                }
            }, file));
        }
    }

    private LinearLayout buildRow(String text, int color, android.view.View.OnClickListener click, final File file) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(COLOR_CARD);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        row.setLayoutParams(params);

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(13f);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        row.setOnClickListener(click);
        if (file != null) {
            row.setOnLongClickListener(v -> {
                showEntryMenu(file);
                return true;
            });
        }
        return row;
    }

    private void showEntryMenu(final File file) {
        String[] options = {"Copy path", "Rename", "Delete"};
        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    copyToClipboard(file.getAbsolutePath());
                    Toast.makeText(this, "Path copied.", Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    showRenameDialog(file);
                } else {
                    confirmDelete(file);
                }
            })
            .show();
    }

    private void confirmDelete(final File file) {
        new AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete " + file.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (deleteRecursive(file)) {
                    Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Could not delete.", Toast.LENGTH_SHORT).show();
                }
                renderList();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private void showRenameDialog(final File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    File target = new File(currentDir, newName);
                    if (file.renameTo(target)) {
                        Toast.makeText(this, "Renamed.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not rename.", Toast.LENGTH_SHORT).show();
                    }
                    renderList();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showNewFileDialog() {
        final EditText input = new EditText(this);
        input.setHint("name.txt");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
            .setTitle("New file")
            .setView(input)
            .setPositiveButton("Create", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    File target = new File(currentDir, name);
                    try {
                        if (target.createNewFile()) {
                            renderList();
                            openFile(target);
                        } else {
                            Toast.makeText(this, "Already exists.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not create: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showNewFolderDialog() {
        final EditText input = new EditText(this);
        input.setHint("folder");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
            .setTitle("New folder")
            .setView(input)
            .setPositiveButton("Create", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    File target = new File(currentDir, name);
                    if (target.mkdirs()) {
                        renderList();
                    } else {
                        Toast.makeText(this, "Could not create.", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openFile(final File file) {
        if (file.length() > MAX_EDIT_BYTES) {
            new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage("This file is large. Copy its path to open it in the terminal instead?")
                .setPositiveButton("Copy path", (dialog, which) -> {
                    copyToClipboard(file.getAbsolutePath());
                    Toast.makeText(this, "Path copied.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }
        String content;
        try {
            content = readFile(file);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        openEditor(file, content);
    }

    private void openEditor(final File file, String content) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        TextView header = new TextView(this);
        header.setText(file.getAbsolutePath());
        header.setTextColor(COLOR_DIM);
        header.setTextSize(12f);
        header.setPadding(dp(14), dp(12), dp(14), dp(8));
        root.addView(header);

        final EditText editor = new EditText(this);
        editor.setText(content);
        editor.setTextColor(COLOR_TEXT);
        editor.setBackgroundColor(COLOR_CARD);
        editor.setGravity(Gravity.TOP);
        editor.setTypeface(android.graphics.Typeface.MONOSPACE);
        editor.setTextSize(13f);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scroll.addView(editor);
        root.addView(scroll);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(10), dp(4), dp(10), dp(12));

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            try {
                writeFile(file, editor.getText().toString());
                Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        save.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button back = new Button(this);
        back.setText("Back");
        back.setOnClickListener(v -> {
            rebuildBrowser();
        });
        back.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        actions.addView(save);
        actions.addView(back);
        root.addView(actions);

        setContentView(root);
    }

    private void rebuildBrowser() {
        setupBrowserView();
    }

    private String readFile(File file) throws Exception {
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

    private void writeFile(File file, String content) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("path", text));
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
