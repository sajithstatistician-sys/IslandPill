package com.islandpill.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences sp;
    private TextView status;
    private Button permButton;
    private Switch toggle;

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

        permButton = new Button(this);
        permButton.setText("1. Allow \"Display over other apps\"");
        permButton.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        root.addView(permButton);

        toggle = new Switch(this);
        toggle.setText("2. Show pill");
        toggle.setTextSize(18);
        toggle.setPadding(0, dp(16), 0, dp(16));
        toggle.setChecked(sp.getBoolean("enabled", false));
        toggle.setOnCheckedChangeListener((CompoundButton b, boolean on) -> {
            if (on && !Settings.canDrawOverlays(this)) {
                b.setChecked(false);
                Toast.makeText(this, "Allow the permission first", Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit().putBoolean("enabled", on).apply();
            Intent i = new Intent(this, OverlayService.class);
            if (on) startForegroundService(i); else stopService(i);
        });
        root.addView(toggle);

        TextView hint = new TextView(this);
        hint.setText("3. Adjust until the pill hides the camera hole exactly (changes apply live).");
        hint.setPadding(0, 0, 0, dp(8));
        root.addView(hint);

        addSlider(root, "Width", "w", 30, 260, 96);
        addSlider(root, "Height", "h", 14, 70, 30);
        addSlider(root, "Distance from top", "y", 0, 80, 8);

        TextView note = new TextView(this);
        note.setText("\nTip for POCO / HyperOS: Settings > Apps > Island Pill > Autostart ON, "
                + "Battery saver > No restrictions, and lock the app in Recents. "
                + "The pill hides in landscape.");
        note.setTextSize(13);
        root.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = Settings.canDrawOverlays(this);
        status.setText(ok ? "Permission: granted" : "Permission: not granted yet");
        permButton.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (!ok && toggle.isChecked()) toggle.setChecked(false);
    }

    private void addSlider(LinearLayout root, String label, String key, int min, int max, int def) {
        TextView t = new TextView(this);
        SeekBar s = new SeekBar(this);
        int v = sp.getInt(key, def);
        t.setText(label + ": " + v + " dp");
        s.setMax(max - min);
        s.setProgress(v - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int val = progress + min;
                t.setText(label + ": " + val + " dp");
                sp.edit().putInt(key, val).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        root.addView(t);
        root.addView(s);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
