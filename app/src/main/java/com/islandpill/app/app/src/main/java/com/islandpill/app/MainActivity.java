package com.islandpill.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences sp;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("pill", MODE_PRIVATE);

        int p = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, dp(80), p, p);

        TextView title = new TextView(this);
        title.setText("Island Pill");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        Button access = new Button(this);
        access.setText("1. Open Accessibility settings");
        access.setAllCaps(false);
        access.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        Button restricted = new Button(this);
        restricted.setText("If the switch is greyed out: open App info");
        restricted.setAllCaps(false);
        restricted.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        root.addView(restricted);

        Switch toggle = new Switch(this);
        toggle.setText("2. Show pill");
        toggle.setTextSize(18);
        toggle.setPadding(0, dp(16), 0, dp(16));
        toggle.setChecked(sp.getBoolean("enabled", true));
        toggle.setOnCheckedChangeListener((b, on) ->
                sp.edit().putBoolean("enabled", on).apply());
        root.addView(toggle);

        TextView hint = new TextView(this);
        hint.setText("3. Adjust until the pill hides the camera hole exactly (changes apply live).");
        hint.setPadding(0, 0, 0, dp(8));
        root.addView(hint);

        addSlider(root, "Width", "w", 30, 260, 96, " dp");
        addSlider(root, "Height", "h", 14, 70, 30, " dp");
        addSlider(root, "Distance from top", "y", 0, 80, 8, " dp");

        TextView colTitle = new TextView(this);
        colTitle.setText("\nColour");
        colTitle.setTextSize(18);
        root.addView(colTitle);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        addSwatch(swatches, "Black", 0, 0, 0);
        addSwatch(swatches, "Dark", 25, 25, 25);
        addSwatch(swatches, "Grey", 70, 70, 70);
        addSwatch(swatches, "White", 255, 255, 255);
        root.addView(swatches);

        addSlider(root, "Red", "r", 0, 255, 0, "");
        addSlider(root, "Green", "g", 0, 255, 0, "");
        addSlider(root, "Blue", "b", 0, 255, 0, "");
        addSlider(root, "Opacity", "a", 10, 100, 100, " %");

        TextView note = new TextView(this);
        note.setText("\nHyperOS tips: Autostart ON for this app, Battery saver > No restrictions, "
                + "lock the app in Recents. The pill hides in landscape. "
                + "This app reads nothing from your screen.");
        note.setTextSize(13);
        root.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean on = enabled != null && enabled.contains(getPackageName());
        status.setText(on ? "Accessibility: ON - pill is active"
                : "Accessibility: OFF - turn on Island Pill under Installed apps");
    }

    private void addSlider(LinearLayout root, String label, String key, int min, int max,
                           int def, String unit) {
        TextView t = new TextView(this);
        SeekBar s = new SeekBar(this);
        int v = sp.getInt(key, def);
        t.setText(label + ": " + v + unit);
        s.setMax(max - min);
        s.setProgress(v - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = progress + min;
                t.setText(label + ": " + val + unit);
                sp.edit().putInt(key, val).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        root.addView(t);
        root.addView(s);
    }

    private void addSwatch(LinearLayout row, String name, int r, int g, int b) {
        Button btn = new Button(this);
        btn.setText(name);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> {
            sp.edit().putInt("r", r).putInt("g", g).putInt("b", b).putInt("a", 100).apply();
            recreate();
        });
        row.addView(btn, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
