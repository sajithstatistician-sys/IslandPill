package com.islandpill.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean on = context.getSharedPreferences("pill", Context.MODE_PRIVATE)
                .getBoolean("enabled", false);
        if (on && Settings.canDrawOverlays(context)) {
            context.startForegroundService(new Intent(context, OverlayService.class));
        }
    }
}
