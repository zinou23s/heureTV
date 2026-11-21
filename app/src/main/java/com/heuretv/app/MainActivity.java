package com.heuretv.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    static final String PREFS = "heuretv";
    static final String KEY_INTERVAL = "interval"; // 15, 30, 50 ou 60 (minutes)
    static final String KEY_ENABLED = "enabled";
    static final String KEY_DURATION = "duration"; // durée d'affichage en secondes
    static final int DEFAULT_DURATION = 30;

    private static final int[] CHOICES = {15, 30, 50, 60};
    private static final int[] DURATIONS = {10, 30, 60, 120, 150};

    private SharedPreferences prefs;
    private TextView status;
    private Button btnPerm, btnStop;
    private final Button[] choiceButtons = new Button[CHOICES.length];
    private final Button[] durationButtons = new Button[DURATIONS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(60), dp(24), dp(60), dp(24));

        TextView title = new TextView(this);
        title.setText("Heure TV");
        title.setTextSize(34);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp());

        status = new TextView(this);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(12));
        root.addView(status, lp());

        btnPerm = button(root);
        btnPerm.setOnClickListener(v -> askPermission());

        // Deux colonnes : intervalle à gauche, durée d'affichage à droite.
        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        columns.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER_HORIZONTAL;
        cp.topMargin = dp(10);
        root.addView(columns, cp);

        LinearLayout left = column(columns, "Afficher l'heure…");
        LinearLayout right = column(columns, "…pendant");

        for (int i = 0; i < CHOICES.length; i++) {
            final int minutes = CHOICES[i];
            Button b = button(left);
            b.setOnClickListener(v -> choose(minutes));
            choiceButtons[i] = b;
        }

        for (int i = 0; i < DURATIONS.length; i++) {
            final int seconds = DURATIONS[i];
            Button b = button(right);
            b.setOnClickListener(v -> chooseDuration(seconds));
            durationButtons[i] = b;
        }

        btnStop = button(root);
        btnStop.setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply();
            stopService(new Intent(this, ClockService.class));
            refresh();
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
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

    /** Enregistre la durée d'affichage et montre un aperçu de 5 secondes. */
    private void chooseDuration(int seconds) {
        prefs.edit().putInt(KEY_DURATION, seconds).apply();
        if (ClockOverlay.canDraw(this)) {
            ClockOverlay.show(this, 5000);
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
        int duration = prefs.getInt(KEY_DURATION, DEFAULT_DURATION);
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);

        btnPerm.setText(ok
                ? "Autorisation par-dessus les autres applis : OK"
                : "1. Autoriser l'affichage par-dessus les autres applis");

        for (int i = 0; i < CHOICES.length; i++) {
            boolean sel = enabled && CHOICES[i] == interval;
            choiceButtons[i].setText((sel ? "✓  " : "")
                    + "Toutes les " + CHOICES[i] + " minutes");
            choiceButtons[i].setSelected(sel);
        }
        for (int i = 0; i < DURATIONS.length; i++) {
            boolean sel = DURATIONS[i] == duration;
            durationButtons[i].setText((sel ? "✓  " : "") + durLabel(DURATIONS[i]));
            durationButtons[i].setSelected(sel);
        }
        btnStop.setText("Arrêter");

        if (!ok) {
            status.setText("Étape 1 : autorise l'affichage. Étape 2 : choisis un intervalle.");
            status.setTextColor(0xFFFFB74D);
        } else if (enabled) {
            status.setText("Actif : l'heure s'affiche toutes les " + interval
                    + " minutes, pendant " + durLabel(duration)
                    + ". Tu peux quitter cette appli.");
            status.setTextColor(0xFF81C784);
        } else {
            status.setText("Choisis un intervalle pour démarrer.");
            status.setTextColor(0xFFBDBDBD);
        }
    }

    private static String durLabel(int s) {
        if (s < 60) return s + " secondes";
        if (s == 60) return "1 minute";
        if (s % 60 == 0) return (s / 60) + " minutes";
        return (s / 60) + " min " + (s % 60);
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

    private LinearLayout column(LinearLayout parent, String heading) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextSize(18);
        h.setTextColor(0xFFBDBDBD);
        h.setGravity(Gravity.CENTER);
        col.addView(h, lp());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = dp(16);
        p.rightMargin = dp(16);
        parent.addView(col, p);
        return col;
    }

    private GradientDrawable shape(int fill, int strokeColor, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(12));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    /** Bouton très visible à la télécommande : le bouton ciblé devient jaune, grand, bordure blanche. */
    private Button button(LinearLayout parent) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(22);
        b.setTextColor(Color.WHITE);
        b.setFocusable(true);
        b.setMinHeight(dp(56));
        b.setMinWidth(dp(300));
        b.setPadding(dp(20), dp(8), dp(20), dp(8));

        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused},
                shape(0xFFFFC107, Color.WHITE, 4));          // ciblé : jaune vif
        bg.addState(new int[]{android.R.attr.state_selected},
                shape(0xFF2E7D32, 0xFF81C784, 2));           // choix actif : vert
        bg.addState(new int[]{}, shape(0xFF37474F, 0xFF607D8B, 1)); // normal : gris foncé
        b.setBackground(bg);

        b.setOnFocusChangeListener((v, hasFocus) -> {
            b.setTextColor(hasFocus ? Color.BLACK : Color.WHITE);
            v.animate().scaleX(hasFocus ? 1.08f : 1f)
                    .scaleY(hasFocus ? 1.08f : 1f).setDuration(120).start();
        });

        LinearLayout.LayoutParams p = lp();
        p.topMargin = dp(8);
        parent.addView(b, p);
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
