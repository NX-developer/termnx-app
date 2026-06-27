package com.termux.app.quick;

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

import java.util.ArrayList;
import java.util.List;

public class TermnxQuickCommandsActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_CARD = 0xFF161B22;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private TermnxQuickPrefs prefs;
    private final List<String[]> commands = new ArrayList<>();
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxQuickPrefs(this);
        setTitle("Quick Commands");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        commands.clear();
        commands.addAll(prefs.getCommands());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        TextView info = new TextView(this);
        info.setText("These commands appear on the home-screen widget. Tap one there to run it "
            + "in the terminal. Up to " + TermnxQuickPrefs.MAX_SLOTS + " commands. "
            + "Add the widget from your launcher's widgets list.");
        info.setTextColor(COLOR_DIM);
        info.setTextSize(12f);
        info.setPadding(dp(14), dp(12), dp(14), dp(8));
        root.addView(info);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(10), dp(2), dp(10), dp(10));
        scroll.addView(list);
        root.addView(scroll);

        Button add = new Button(this);
        add.setText("Add command");
        add.setOnClickListener(v -> {
            if (commands.size() >= TermnxQuickPrefs.MAX_SLOTS) {
                Toast.makeText(this, "Maximum " + TermnxQuickPrefs.MAX_SLOTS + " reached.", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddDialog();
        });
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(10), dp(4), dp(10), dp(12));
        add.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        actions.addView(add);
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

    private void save() {
        prefs.setCommands(commands);
        TermnxWidgetProvider.refresh(this);
    }

    private void renderList() {
        list.removeAllViews();
        if (commands.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No commands yet.");
            empty.setTextColor(COLOR_DIM);
            empty.setPadding(dp(6), dp(10), dp(6), dp(10));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < commands.size(); i++) {
            final int index = i;
            String[] entry = commands.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(COLOR_CARD);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(4);
            row.setLayoutParams(params);

            TextView text = new TextView(this);
            text.setText(entry[0] + "\n" + entry[1]);
            text.setTextColor(COLOR_TEXT);
            text.setTextSize(13f);
            text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = new Button(this);
            remove.setText("\u2715");
            remove.setOnClickListener(v -> {
                commands.remove(index);
                save();
                renderList();
            });

            row.addView(text);
            row.addView(remove);
            list.addView(row);
        }
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText labelField = new EditText(this);
        labelField.setHint("Label (e.g. Update)");
        labelField.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText commandField = new EditText(this);
        commandField.setHint("Command (e.g. pkg upgrade -y)");
        commandField.setInputType(InputType.TYPE_CLASS_TEXT);

        layout.addView(labelField);
        layout.addView(commandField);

        new AlertDialog.Builder(this)
            .setTitle("Add command")
            .setView(layout)
            .setPositiveButton("Add", (dialog, which) -> {
                String command = commandField.getText().toString().trim();
                String label = labelField.getText().toString().trim();
                if (!command.isEmpty()) {
                    commands.add(new String[]{label.isEmpty() ? command : label, command});
                    save();
                    renderList();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
