package com.termux.app.keys;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

public class TermnxKeysActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_CARD = 0xFF161B22;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private TermnxKeysPrefs prefs;
    private final List<TermnxKeysPrefs.CustomKey> working = new ArrayList<>();
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxKeysPrefs(this);
        setTitle("Extra Keys");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        working.clear();
        if (prefs.isCustomized()) {
            working.addAll(prefs.getKeys());
        } else {
            working.addAll(TermnxKeysPrefs.defaultSeed());
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        TextView info = new TextView(this);
        info.setText("These keys appear in the terminal's bottom key row. Reorder with the arrows, "
            + "remove keys you don't want, and add your own. A value can be a special key "
            + "(ENTER, TAB, ESC, CTRL, ALT, UP, DOWN, LEFT, RIGHT, HOME, END, PGUP, PGDN), "
            + "a single character (/, |, ~), or a short text/command. Tap Save to apply.");
        info.setTextColor(COLOR_DIM);
        info.setTextSize(12f);
        info.setPadding(dp(14), dp(12), dp(14), dp(8));

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(10), dp(4), dp(10), dp(10));
        scroll.addView(list);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(10), dp(4), dp(10), dp(12));

        Button addButton = new Button(this);
        addButton.setText("Add");
        addButton.setOnClickListener(v -> showAddDialog());
        addButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button saveButton = new Button(this);
        saveButton.setText("Save");
        saveButton.setOnClickListener(v -> {
            prefs.save(working);
            Toast.makeText(this, "Saved. Returning to the terminal applies it.", Toast.LENGTH_SHORT).show();
        });
        saveButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button resetButton = new Button(this);
        resetButton.setText("Reset");
        resetButton.setOnClickListener(v -> {
            prefs.reset();
            working.clear();
            working.addAll(TermnxKeysPrefs.defaultSeed());
            renderList();
            Toast.makeText(this, "Reset to default.", Toast.LENGTH_SHORT).show();
        });
        resetButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        actions.addView(addButton);
        actions.addView(saveButton);
        actions.addView(resetButton);

        root.addView(info);
        root.addView(scroll);
        root.addView(actions);
        setContentView(root);

        renderList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Add");
        menu.add(Menu.NONE, 2, Menu.NONE, "Save");
        MenuItem hide = menu.add(Menu.NONE, 3, Menu.NONE, "Hide bar");
        hide.setCheckable(true);
        hide.setChecked(prefs.isHidden());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            showAddDialog();
            return true;
        } else if (item.getItemId() == 2) {
            prefs.save(working);
            Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == 3) {
            boolean newValue = !item.isChecked();
            item.setChecked(newValue);
            prefs.setHidden(newValue);
            Toast.makeText(this, newValue
                ? "Key bar hidden. Your keys are kept."
                : "Key bar shown.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderList() {
        list.removeAllViews();
        if (working.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No keys. Tap Add to create one.");
            empty.setTextColor(COLOR_DIM);
            empty.setPadding(dp(6), dp(10), dp(6), dp(10));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < working.size(); i++) {
            final int index = i;
            TermnxKeysPrefs.CustomKey key = working.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(COLOR_CARD);
            card.setPadding(dp(12), dp(6), dp(8), dp(6));
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(4);
            cardParams.bottomMargin = dp(4);
            card.setLayoutParams(cardParams);

            TextView text = new TextView(this);
            String shown = key.label.equals(key.value) ? key.value : key.label + "  \u2192  " + key.value;
            if (key.macro) shown = shown + "   (combo)";
            text.setText(shown);
            text.setTextColor(COLOR_TEXT);
            text.setTypeface(Typeface.MONOSPACE);
            text.setTextSize(13f);
            text.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button up = smallButton("\u25B2");
            up.setOnClickListener(v -> {
                if (index > 0) {
                    java.util.Collections.swap(working, index, index - 1);
                    renderList();
                }
            });
            Button down = smallButton("\u25BC");
            down.setOnClickListener(v -> {
                if (index < working.size() - 1) {
                    java.util.Collections.swap(working, index, index + 1);
                    renderList();
                }
            });
            Button remove = smallButton("\u2715");
            remove.setOnClickListener(v -> {
                working.remove(index);
                renderList();
            });

            card.addView(text);
            card.addView(up);
            card.addView(down);
            card.addView(remove);
            list.addView(card);
        }
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setMinWidth(dp(44));
        button.setMinimumWidth(dp(44));
        button.setPadding(dp(6), 0, dp(6), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(2);
        button.setLayoutParams(params);
        return button;
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText labelField = new EditText(this);
        labelField.setHint("Name (e.g. OK)");
        labelField.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText valueField = new EditText(this);
        valueField.setHint("Value (e.g. ls -la, SHIFT+TAB)");
        valueField.setInputType(InputType.TYPE_CLASS_TEXT);

        final android.widget.CheckBox macroCheck = new android.widget.CheckBox(this);
        macroCheck.setText("Run as key/combo (e.g. SHIFT+TAB, CTRL+C)");
        macroCheck.setTextColor(COLOR_TEXT);
        macroCheck.setPadding(0, dp(8), 0, 0);

        TextView hint = new TextView(this);
        hint.setText("Off: types the text. On: sends it as a key or key combination. "
            + "Special keys: ENTER, TAB, ESC, CTRL, ALT, UP, DOWN, LEFT, RIGHT, HOME, END, PGUP, PGDN.");
        hint.setTextColor(COLOR_DIM);
        hint.setTextSize(11f);
        hint.setPadding(0, dp(8), 0, 0);

        layout.addView(labelField);
        layout.addView(valueField);
        layout.addView(macroCheck);
        layout.addView(hint);

        new AlertDialog.Builder(this)
            .setTitle("Add key")
            .setView(layout)
            .setPositiveButton("Add", (dialog, which) -> {
                String rawValue = valueField.getText().toString().trim();
                String label = labelField.getText().toString().trim();
                boolean macro = macroCheck.isChecked();
                String value = macro ? normalizeMacro(rawValue) : rawValue;
                if (!value.isEmpty()) {
                    working.add(new TermnxKeysPrefs.CustomKey(label.isEmpty() ? value : label, value, macro));
                    renderList();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String normalizeMacro(String value) {
        String replaced = value.replace("+", " ").replace(",", " ");
        String[] parts = replaced.trim().split("\\s+");
        java.util.Set<String> special = new java.util.HashSet<>(java.util.Arrays.asList(
            "SHIFT", "CTRL", "ALT", "FN", "ESC", "TAB", "ENTER", "UP", "DOWN", "LEFT", "RIGHT",
            "HOME", "END", "PGUP", "PGDN", "DEL", "BKSP", "SPACE", "INS", "MENU"));
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String token = part;
            if (special.contains(part.toUpperCase())) {
                token = part.toUpperCase();
            } else if (part.matches("(?i)f[0-9]{1,2}")) {
                token = part.toUpperCase();
            }
            if (builder.length() > 0) builder.append(" ");
            builder.append(token);
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
