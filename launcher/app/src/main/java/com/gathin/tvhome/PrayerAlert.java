package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Chooses what a prayer time does: the full adhan, or the banner over whatever is on. */
final class PrayerAlert {
    private PrayerAlert() {}

    static void fire(Context context, int index, long at) {
        Context app = context.getApplicationContext();
        boolean haveVoice = PrayerPrefs.adhan(app) && AdhanVoices.chosen(app) != null;
        try {
            if (haveVoice) {
                app.startActivity(new Intent(app, AdhanActivity.class)
                        .putExtra(AdhanActivity.EXTRA_INDEX, index)
                        .putExtra(AdhanActivity.EXTRA_AT, at)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
            }
            // No adhan wanted, or no voice to play: the banner, so the prayer never passes unnoticed.
            Intent svc = new Intent(app, PrayerOverlayService.class)
                    .putExtra(PrayerScheduler.EXTRA_INDEX, index)
                    .putExtra(PrayerScheduler.EXTRA_AT, at);
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(svc);
            else app.startService(svc);
        } catch (RuntimeException e) {
            // Background start refused (overlay permission missing): nothing more can be done from here.
        }
    }
}
