package com.islandpill.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/** Draws one static black pill over the camera cutout. No timers, no polling, no animation. */
public class OverlayService extends Service
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private WindowManager wm;
    private View pill;
    private WindowManager.LayoutParams lp;
    private SharedPreferences sp;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences("pill", MODE_PRIVATE);
        startInForeground();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        pill = new View(this);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.BLACK);
        shape.setCornerRadius(1000f); // clamped to a full pill shape
        pill.setBackground(shape);

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        lp = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        updateOrientation();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    private void applyLayout(boolean update) {
        lp.width = px(sp.getInt("w", 96));
        lp.height = px(sp.getInt("h", 30));
        lp.y = px(sp.getInt("y", 8));
        if (update && pill != null && pill.isAttachedToWindow()) {
            wm.updateViewLayout(pill, lp);
        }
    }

    /** In landscape the camera moves to the side, so hide the pill instead of misplacing it. */
    private void updateOrientation() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        pill.setVisibility(landscape ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pill != null) {
            updateOrientation();
            applyLayout(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (pill != null && lp != null) applyLayout(true);
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void startInForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(
                "pill", "Island Pill", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, "pill")
                .setContentTitle("Island Pill is on")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, n);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sp != null) sp.unregisterOnSharedPreferenceChangeListener(this);
        if (pill != null && pill.isAttachedToWindow()) wm.removeView(pill);
        super.onDestroy();
    }
}
