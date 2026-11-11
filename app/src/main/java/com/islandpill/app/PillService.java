package com.islandpill.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Draws one static pill over the camera cutout using an accessibility overlay, which sits above
 * the status bar. It subscribes to NO accessibility events and reads nothing from the screen.
 */
public class PillService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private WindowManager wm;
    private View pill;
    private GradientDrawable shape;
    private WindowManager.LayoutParams lp;
    private SharedPreferences sp;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = 0; // receive no events at all
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        sp = getSharedPreferences("pill", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        removePill();
        pill = new View(this);
        shape = new GradientDrawable();
        shape.setCornerRadius(1000f); // clamped to a full pill shape
        pill.setBackground(shape);

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        lp = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (Build.VERSION.SDK_INT >= 30) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        lp.setTitle("IslandPill");

        applyLayout(false);
        wm.addView(pill, lp);
        refreshVisibility();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    private void applyLayout(boolean update) {
        lp.width = px(sp.getInt("w", 96));
        lp.height = px(sp.getInt("h", 30));
        lp.y = px(sp.getInt("y", 8));
        lp.alpha = 1f;
        int a = Math.round(sp.getInt("a", 100) * 255f / 100f);
        shape.setColor(Color.argb(a, sp.getInt("r", 0), sp.getInt("g", 0), sp.getInt("b", 0)));
        if (update && pill != null && pill.isAttachedToWindow()) {
            wm.updateViewLayout(pill, lp);
            pill.invalidate();
        }
    }

    /** Hidden when switched off in the app, and in landscape (camera moves to the side). */
    private void refreshVisibility() {
        if (pill == null) return;
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        boolean on = sp.getBoolean("enabled", true);
        pill.setVisibility(on && !landscape ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (pill == null || lp == null) return;
        applyLayout(true);
        refreshVisibility();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pill != null) {
            applyLayout(true);
            refreshVisibility();
        }
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void removePill() {
        if (sp != null) sp.unregisterOnSharedPreferenceChangeListener(this);
        if (pill != null && wm != null && pill.isAttachedToWindow()) wm.removeView(pill);
        pill = null;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override public void onInterrupt() { }

    @Override
    public boolean onUnbind(Intent intent) {
        removePill();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removePill();
        super.onDestroy();
    }
}
