package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** The prayer and adhan choices, kept apart from the launcher's own settings file. */
final class PrayerPrefs {
    private PrayerPrefs() {}

    static final String FILE = "islamic";
    /** Default place: the coordinates the TV owner gave. Change with the keys below over adb if the TV moves. */
    static final double DEFAULT_LAT = 41.0605086, DEFAULT_LON = 28.8355530;

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static double lat(Context c) {
        return parse(get(c).getString("lat", null), DEFAULT_LAT);
    }

    static double lon(Context c) {
        return parse(get(c).getString("lon", null), DEFAULT_LON);
    }

    /** The prayer strip and the verse card on Home. */
    static boolean panel(Context c) { return get(c).getBoolean("panel", true); }

    /** The banner over whatever is playing, at each prayer. */
    static boolean banner(Context c) { return get(c).getBoolean("banner", true); }

    /** The full adhan: takes the screen, pauses other audio, plays the chosen voice. */
    static boolean adhan(Context c) { return get(c).getBoolean("adhan", true); }

    static String voice(Context c) { return get(c).getString("voice", ""); }

    static boolean anyAlert(Context c) { return banner(c) || adhan(c); }

    private static double parse(String s, double fallback) {
        if (s == null) return fallback;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
