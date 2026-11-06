package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.time.ZoneId;

/** The two "try it now" buttons in the prayer settings. Uses the next real prayer's name and time. */
final class PrayerOverlayTest {
    private PrayerOverlayTest() {}

    private static PrayerTimes.Next next(Context c) {
        return PrayerTimes.next(System.currentTimeMillis(), ZoneId.systemDefault(), PrayerPrefs.lat(c), PrayerPrefs.lon(c), false);
    }

    static void banner(Context c) {
        PrayerTimes.Next n = next(c);
        android.content.Intent svc = new android.content.Intent(c, PrayerOverlayService.class)
                .putExtra(PrayerScheduler.EXTRA_INDEX, n.index).putExtra(PrayerScheduler.EXTRA_AT, n.at);
        c.startForegroundService(svc);
    }

    static void adhan(Context c) {
        Context app = c.getApplicationContext();
        PrayerTimes.Next n = next(app);
        new Handler(Looper.getMainLooper()).postDelayed(() -> PrayerAlert.fire(app, n.index, n.at), 3000L);
    }
}
