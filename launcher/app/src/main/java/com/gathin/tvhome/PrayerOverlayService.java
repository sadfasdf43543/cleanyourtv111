package com.gathin.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.ZoneId;

/**
 * Shows the prayer banner in a window above every app, for twenty seconds, without taking focus or keys:
 * the show underneath carries on. Needs "display over other apps" (see the README: one adb command).
 */
public final class PrayerOverlayService extends Service {
    private static final String CHANNEL = "prayer";
    private static final int NOTIFICATION_ID = 7002;
    private static final long SHOW_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private View banner;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();
        int index = intent == null ? PrayerTimes.FAJR : intent.getIntExtra(PrayerScheduler.EXTRA_INDEX, PrayerTimes.FAJR);
        long at = intent == null ? System.currentTimeMillis() : intent.getLongExtra(PrayerScheduler.EXTRA_AT, System.currentTimeMillis());
        show(index, at);
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Prayer times", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("حان وقت الصلاة").build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION_ID, n);
    }

    private void show(int index, long at) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        windows = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding((int) (26 * d), (int) (16 * d), (int) (30 * d), (int) (16 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2062B1E);
        bg.setCornerRadius(30 * d);
        bg.setStroke(Math.max(1, (int) (1.5f * d)), 0xFFE8C766);
        card.setBackground(bg);

        card.addView(new CrescentView(this, 0xFFE8C766), new LinearLayout.LayoutParams((int) (54 * d), (int) (54 * d)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tl.setMarginEnd((int) (18 * d));
        card.addView(texts, tl);

        TextView title = new TextView(this);
        title.setText("حان الآن موعد صلاة " + PrayerTimes.NAMES_AR[index]);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setTypeface(UiFont.get(this, R.string.font_bold));
        title.setIncludeFontPadding(false);
        texts.addView(title);

        TextView sub = new TextView(this);
        sub.setText(PrayerTimes.format(at, ZoneId.systemDefault()) + "  ·  حيَّ على الصلاة");
        sub.setTextColor(0xFFE8C766);
        sub.setTextSize(16);
        sub.setTypeface(UiFont.get(this, R.string.font_regular));
        sub.setIncludeFontPadding(false);
        sub.setPadding(0, (int) (6 * d), 0, 0);
        texts.addView(sub);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = (int) (40 * d);
        try {
            windows.addView(card, lp);
            banner = card;
            card.setAlpha(0f);
            card.setTranslationY(-40 * d);
            card.animate().alpha(1f).translationY(0f).setDuration(350).start();
        } catch (RuntimeException e) {
            stopSelf();
            return;
        }
        handler.postDelayed(this::dismiss, SHOW_MS);
    }

    private void dismiss() {
        removeBanner();
        stopForeground(true);
        stopSelf();
    }

    private void removeBanner() {
        if (banner != null && windows != null) {
            try {
                windows.removeViewImmediate(banner);
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
        banner = null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeBanner();
        super.onDestroy();
    }
}
