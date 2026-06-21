package com.termux.app.theme;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class TermnxThemeActivity extends AppCompatActivity {

    private static final int COLOR_BG = 0xFF0B0E14;
    private static final int COLOR_TEXT = 0xFFD7DEE8;
    private static final int COLOR_DIM = 0xFF768390;

    private TermnxThemePrefs prefs;
    private EditText bgField;
    private EditText fgField;
    private EditText keyTextField;
    private EditText keyActiveField;
    private TextView imageStatus;

    private static final int REQUEST_IMAGE = 5001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new TermnxThemePrefs(this);
        setTitle("Appearance");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(content);

        TextView info = new TextView(this);
        info.setText("Pick colors with a swatch or type a hex value (e.g. #101418). "
            + "Leave a field empty to keep the default. Changes apply when you return to the terminal.");
        info.setTextColor(COLOR_DIM);
        info.setTextSize(12f);
        info.setPadding(0, 0, 0, dp(8));
        content.addView(info);

        bgField = addSection(content, "Terminal background", prefs.getTerminalBackground(),
            new int[]{0xFF000000, 0xFF0B0E14, 0xFF101418, 0xFF1B1B2F, 0xFF0F1B0F, 0xFF1A1320});
        fgField = addSection(content, "Terminal text", prefs.getTerminalForeground(),
            new int[]{0xFFFFFFFF, 0xFFD7DEE8, 0xFF39D353, 0xFFE3B341, 0xFF58A6FF, 0xFFFF7B72});
        keyTextField = addSection(content, "Key button text", prefs.getExtraKeyTextColor(),
            new int[]{0xFFFFFFFF, 0xFFD7DEE8, 0xFF39D353, 0xFF58A6FF, 0xFFE3B341, 0xFF768390});
        keyActiveField = addSection(content, "Key pressed color", prefs.getExtraKeyActiveColor(),
            new int[]{0xFFFF7B72, 0xFFE3B341, 0xFF39D353, 0xFF58A6FF, 0xFFB392F0, 0xFFFFFFFF});

        TextView imageLabel = new TextView(this);
        imageLabel.setText("Background image");
        imageLabel.setTextColor(COLOR_TEXT);
        imageLabel.setTextSize(14f);
        imageLabel.setPadding(0, dp(16), 0, dp(2));
        content.addView(imageLabel);

        imageStatus = new TextView(this);
        imageStatus.setTextColor(COLOR_DIM);
        imageStatus.setTextSize(12f);
        imageStatus.setText(prefs.hasBackgroundImage() ? "An image is set." : "No image set (using color).");
        content.addView(imageStatus);

        LinearLayout imageRow = new LinearLayout(this);
        imageRow.setOrientation(LinearLayout.HORIZONTAL);
        imageRow.setPadding(0, dp(6), 0, 0);

        Button pickImage = new Button(this);
        pickImage.setText("Pick image");
        pickImage.setOnClickListener(v -> pickImage());
        pickImage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button removeImage = new Button(this);
        removeImage.setText("Remove image");
        removeImage.setOnClickListener(v -> {
            prefs.clearBackgroundImage();
            imageStatus.setText("No image set (using color).");
            Toast.makeText(this, "Image removed. Return to the terminal to apply.", Toast.LENGTH_SHORT).show();
        });
        removeImage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        imageRow.addView(pickImage);
        imageRow.addView(removeImage);
        content.addView(imageRow);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(12), dp(4), dp(12), dp(12));

        Button apply = new Button(this);
        apply.setText("Save");
        apply.setOnClickListener(v -> apply());
        apply.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button reset = new Button(this);
        reset.setText("Reset");
        reset.setOnClickListener(v -> {
            prefs.reset();
            try {
                prefs.writeTerminalColorsFile();
            } catch (Exception ignored) {
            }
            bgField.setText("");
            fgField.setText("");
            keyTextField.setText("");
            keyActiveField.setText("");
            Toast.makeText(this, "Reset. Return to the terminal to apply.", Toast.LENGTH_SHORT).show();
        });
        reset.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        actions.addView(apply);
        actions.addView(reset);

        root.addView(scroll);
        root.addView(actions);
        setContentView(root);
    }

    private EditText addSection(LinearLayout parent, String title, int current, int[] presets) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(COLOR_TEXT);
        label.setTextSize(14f);
        label.setPadding(0, dp(12), 0, dp(4));
        parent.addView(label);

        final EditText field = new EditText(this);
        field.setHint("#RRGGBB");
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setTextColor(COLOR_TEXT);
        field.setHintTextColor(COLOR_DIM);
        if (current != TermnxThemePrefs.UNSET) {
            field.setText(TermnxThemePrefs.toHex(current));
        }
        parent.addView(field);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setPadding(0, dp(6), 0, 0);
        for (final int preset : presets) {
            Button swatch = new Button(this);
            swatch.setBackgroundColor(preset);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(34));
            params.rightMargin = dp(6);
            swatch.setLayoutParams(params);
            swatch.setOnClickListener(v -> field.setText(TermnxThemePrefs.toHex(preset)));
            swatches.addView(swatch);
        }
        parent.addView(swatches);
        return field;
    }

    private int parseField(EditText field) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) return TermnxThemePrefs.UNSET;
        if (!text.startsWith("#")) text = "#" + text;
        try {
            return Color.parseColor(text) | 0xFF000000;
        } catch (Exception e) {
            return TermnxThemePrefs.UNSET;
        }
    }

    private void apply() {
        prefs.setTerminalBackground(parseField(bgField));
        prefs.setTerminalForeground(parseField(fgField));
        prefs.setExtraKeyTextColor(parseField(keyTextField));
        prefs.setExtraKeyActiveColor(parseField(keyActiveField));
        try {
            prefs.writeTerminalColorsFile();
        } catch (Exception e) {
            Toast.makeText(this, "Could not write colors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Saved. Return to the terminal to apply.", Toast.LENGTH_SHORT).show();
    }

    private void pickImage() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            | android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == android.app.Activity.RESULT_OK
            && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            prefs.setBackgroundImageUri(uri.toString());
            if (imageStatus != null) imageStatus.setText("An image is set.");
            Toast.makeText(this, "Image set. Return to the terminal to apply.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
