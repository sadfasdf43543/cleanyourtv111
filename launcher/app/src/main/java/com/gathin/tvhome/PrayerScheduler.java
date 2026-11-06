package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.ZoneId;

/** Arms one alarm, for the next prayer. Every alarm, boot and settings change arms the one after it. */
final class PrayerScheduler {
    private PrayerScheduler() {}

    static final String ACTION_ALARM = "com.gathin.tvhome.PRAYER_ALARM";
    static final String EXTRA_INDEX = "prayer_index";
    static final String EXTRA_AT = "prayer_at";
    private static final int REQUEST = 7001;

    static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent base = new Intent(app, PrayerAlarmReceiver.class).setAction(ACTION_ALARM);
        if (!PrayerPrefs.anyAlert(app)) {
            am.cancel(PendingIntent.getBroadcast(app, REQUEST, base, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            return;
        }
        PrayerTimes.Next next = PrayerTimes.next(System.currentTimeMillis(), ZoneId.systemDefault(),
                PrayerPrefs.lat(app), PrayerPrefs.lon(app), false);
        base.putExtra(EXTRA_INDEX, next.index).putExtra(EXTRA_AT, next.at);
        PendingIntent pi = PendingIntent.getBroadcast(app, REQUEST, base,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi);   // a few minutes late at worst
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi);
        }
    }
}
