package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at a prayer time (not exported), and re-arms the next one. */
public final class PrayerAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrayerScheduler.schedule(context);   // the next prayer, first, so a failure below never loses it
        if (intent == null || !PrayerScheduler.ACTION_ALARM.equals(intent.getAction())) return;
        long at = intent.getLongExtra(PrayerScheduler.EXTRA_AT, 0L);
        // An alarm delivered long after its time (the TV was off) would sound the adhan for a prayer that is over.
        if (Math.abs(System.currentTimeMillis() - at) > 5 * 60_000L) return;
        PrayerAlert.fire(context, intent.getIntExtra(PrayerScheduler.EXTRA_INDEX, PrayerTimes.FAJR), at);
    }
}
