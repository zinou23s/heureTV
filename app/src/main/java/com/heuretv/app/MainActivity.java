package com.heuretv.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    static final String PREFS = "heuretv";
    static final String KEY_INTERVAL = "interval"; // 15, 30, 50 ou 60 (minutes)
    static final String KEY_ENABLED = "enabled";

    private static final int[] CHOICES = {15, 30, 50, 60};

    private SharedPreferences prefs;
    private TextView status;
    private Button btnPerm, btnStop;
    private final Button[] choiceButtons = new Button[CHOICES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(80), dp(30), dp(80), dp(30));

        TextView title = new TextView(this);
        title.setText("Heure TV");
        title.setTextSize(36);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp());

        status = new TextView(this);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(10), 0, dp(18));
        root.addView(status, lp());

        btnPerm = button(root);
        btnPerm.setOnClickListener(v -> askPermission());

        for (int i = 0; i < CHOICES.length; i++) {
            final int minutes = CHOICES[i];
            Button b = button(root);
            b.setOnClickListener(v -> choose(minutes));
            choiceButtons[i] = b;
        }

        btnStop = button(root);
        btnStop.setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply();
            stopService(new Intent(this, ClockService.class));
            refresh();
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si déjà configuré, on s'assure que le service tourne.
        if (prefs.getBoolean(KEY_ENABLED, false) && ClockOverlay.canDraw(this)) {
            startClockService();
        }
        refresh();
        if (!ClockOverlay.canDraw(this)) {
            btnPerm.requestFocus();
        } else {
            int cur = prefs.getInt(KEY_INTERVAL, 30);
            for (int i = 0; i < CHOICES.length; i++) {
                if (CHOICES[i] == cur) choiceButtons[i].requestFocus();
            }
        }
    }

    /** Un seul clic : enregistre l'intervalle et active l'affichage. */
    private void choose(int minutes) {
        prefs.edit().putInt(KEY_INTERVAL, minutes).putBoolean(KEY_ENABLED, true).apply();
        if (!ClockOverlay.canDraw(this)) {
            askPermission();
        } else {
            startClockService();
            ClockOverlay.show(this, 5000); // aperçu immédiat
        }
        refresh();
    }

    private void startClockService() {
        Intent svc = new Intent(this, ClockService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    private void refresh() {
        boolean ok = ClockOverlay.canDraw(this);
        int interval = prefs.getInt(KEY_INTERVAL, 30);
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);

        btnPerm.setText(ok
                ? "Autorisation par-dessus les autres applis : OK"
                : "1. Autoriser l'affichage par-dessus les autres applis");

        for (int i = 0; i < CHOICES.length; i++) {
            boolean sel = enabled && CHOICES[i] == interval;
            choiceButtons[i].setText((sel ? "✓  " : "")
                    + "Toutes les " + CHOICES[i] + " minutes");
        }
        btnStop.setText("Arrêter");

        if (!ok) {
            status.setText("Étape 1 : autorise l'affichage. Étape 2 : choisis un intervalle.");
            status.setTextColor(0xFFFFB74D);
        } else if (enabled) {
            status.setText("Actif : l'heure s'affiche toutes les " + interval
                    + " minutes, 10 secondes. Tu peux quitter cette appli.");
            status.setTextColor(0xFF81C784);
        } else {
            status.setText("Choisis un intervalle pour démarrer.");
            status.setTextColor(0xFFBDBDBD);
        }
    }

    private void askPermission() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            status.setText("Cet écran n'existe pas sur cette TV. Depuis un PC, lance :\n"
                    + "adb shell appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            status.setTextColor(0xFFFFB74D);
        }
    }

    private Button button(LinearLayout parent) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setFocusable(true);
        parent.addView(b, lp());
        return b;
    }

    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.topMargin = dp(4);
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
