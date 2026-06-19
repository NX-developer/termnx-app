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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class TermnxKeysActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_CARD = 0xFF161B22;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_GREEN = 0xFF39D353;
    private static final int COLOR_DIM = 0xFF768390;

    private TermnxKeysPrefs prefs;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxKeysPrefs(this);
        setTitle("Ekstra Tuşlar");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        TextView info = new TextView(this);
        info.setText("Buraya eklediğin tuşlar terminalin alt tuş satırında görünür. "
            + "Değer olarak ENTER, TAB, CTRL gibi özel tuşları, tek karakter (/, |) veya kısa bir metin/komut yazabilirsin. "
            + "İsim, tuşun üstünde görünen etikettir.");
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

        Button addButton = new Button(this);
        addButton.setText("Tuş ekle");
        addButton.setOnClickListener(v -> showAddDialog());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.setMargins(dp(12), dp(4), dp(12), dp(12));
        addButton.setLayoutParams(addParams);

        root.addView(info);
        root.addView(scroll);
        root.addView(addButton);
        setContentView(root);

        renderList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Tuş ekle");
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void renderList() {
        list.removeAllViews();
        List<TermnxKeysPrefs.CustomKey> keys = prefs.getKeys();
        if (keys.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Henüz özel tuş yok. \"Tuş ekle\" ile başla.");
            empty.setTextColor(COLOR_DIM);
            empty.setPadding(dp(6), dp(10), dp(6), dp(10));
            list.addView(empty);
            return;
        }
        for (int i = 0; i < keys.size(); i++) {
            final int index = i;
            TermnxKeysPrefs.CustomKey key = keys.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(COLOR_CARD);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(4);
            cardParams.bottomMargin = dp(4);
            card.setLayoutParams(cardParams);

            TextView text = new TextView(this);
            text.setText(key.label + "  →  " + key.value);
            text.setTextColor(COLOR_TEXT);
            text.setTypeface(Typeface.MONOSPACE);
            text.setTextSize(13f);
            text.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button delete = new Button(this);
            delete.setText("Sil");
            delete.setOnClickListener(v -> {
                prefs.removeAt(index);
                renderList();
            });

            card.addView(text);
            card.addView(delete);
            list.addView(card);
        }
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(10), dp(20), dp(10));

        final EditText labelField = new EditText(this);
        labelField.setHint("İsim (örn. OK)");
        labelField.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText valueField = new EditText(this);
        valueField.setHint("Değer (örn. ENTER, /, ls -la)");
        valueField.setInputType(InputType.TYPE_CLASS_TEXT);

        TextView hint = new TextView(this);
        hint.setText("Özel tuşlar: ENTER, TAB, ESC, CTRL, ALT, UP, DOWN, LEFT, RIGHT, HOME, END, PGUP, PGDN");
        hint.setTextColor(COLOR_DIM);
        hint.setTextSize(11f);
        hint.setPadding(0, dp(8), 0, 0);

        layout.addView(labelField);
        layout.addView(valueField);
        layout.addView(hint);

        new AlertDialog.Builder(this)
            .setTitle("Tuş ekle")
            .setView(layout)
            .setPositiveButton("Ekle", (dialog, which) -> {
                String value = valueField.getText().toString().trim();
                String label = labelField.getText().toString().trim();
                if (!value.isEmpty()) {
                    prefs.addKey(label.isEmpty() ? value : label, value);
                    renderList();
                }
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
