package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Boot, an app update, a clock or time-zone change: arm the next prayer again. Only reschedules. */
public final class PrayerBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrayerScheduler.schedule(context);
    }
}
