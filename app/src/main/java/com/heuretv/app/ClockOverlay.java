package com.heuretv.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Affiche l'heure en haut à droite de l'écran, par-dessus toutes les applis. */
final class ClockOverlay {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static View current;
    private static Runnable hider;

    private ClockOverlay() {}

    static boolean canDraw(Context ctx) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx);
    }

    static void show(Context ctx, long durationMs) {
        final Context app = ctx.getApplicationContext();
        if (!canDraw(app)) return;
        hide(app);

        TextView tv = new TextView(app);
        tv.setText(new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date()));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(app, 28), dp(app, 12), dp(app, 28), dp(app, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB3000000);
        bg.setCornerRadius(dp(app, 16));
        tv.setBackground(bg);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = dp(app, 40);
        lp.y = dp(app, 30);

        try {
            wm(app).addView(tv, lp);
        } catch (Exception e) {
            return;
        }
        current = tv;
        hider = () -> hide(app);
        HANDLER.postDelayed(hider, durationMs);
    }

    static void hide(Context ctx) {
        if (hider != null) {
            HANDLER.removeCallbacks(hider);
            hider = null;
        }
        if (current != null) {
            try {
                wm(ctx.getApplicationContext()).removeView(current);
            } catch (Exception ignored) {
            }
            current = null;
        }
    }

    private static WindowManager wm(Context app) {
        return (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
