package com.heuretv.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Relance le service au démarrage de la TV si l'utilisateur l'avait activé. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean enabled = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .getBoolean(MainActivity.KEY_ENABLED, false);
        if (!enabled || !ClockOverlay.canDraw(context)) return;

        Intent svc = new Intent(context, ClockService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }
}
