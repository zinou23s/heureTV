package com.heuretv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import java.util.Calendar;

/** Service au premier plan : surveille l'horloge et affiche l'heure aux moments voulus. */
public class ClockService extends Service {

    private static final String CHANNEL_ID = "heuretv";

    private int lastKey = -1;
    private long lastShown = System.currentTimeMillis();

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            check();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
        registerReceiver(tick, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastShown = System.currentTimeMillis(); // repart de zéro (utile pour 50 min)
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(tick);
        } catch (Exception ignored) {
        }
        ClockOverlay.hide(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void check() {
        int interval = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .getInt(MainActivity.KEY_INTERVAL, 30);
        long now = System.currentTimeMillis();

        if (interval == 50) {
            // 50 ne tombe pas sur l'horloge : on compte 50 min depuis le dernier affichage.
            if (now - lastShown < 50 * 60_000L - 20_000L) return;
        } else {
            // 15, 30, 60 : aux heures rondes (:00, :15, :30, :45...).
            Calendar c = Calendar.getInstance();
            int minute = c.get(Calendar.MINUTE);
            if (minute % interval != 0) return;
            int key = c.get(Calendar.DAY_OF_YEAR) * 1440
                    + c.get(Calendar.HOUR_OF_DAY) * 60 + minute;
            if (key == lastKey) return;
            lastKey = key;
        }
        lastShown = now;

        long showMs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .getInt(MainActivity.KEY_DURATION, MainActivity.DEFAULT_DURATION) * 1000L;
        ClockOverlay.show(this, showMs);
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Heure TV", NotificationManager.IMPORTANCE_MIN));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("Heure TV")
                .setContentText("Affichage de l'heure actif")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
    }
}
