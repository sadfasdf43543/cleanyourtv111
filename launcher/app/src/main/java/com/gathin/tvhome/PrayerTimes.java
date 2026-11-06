package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Prayer times computed on the TV itself (no network): the standard solar equations, Fajr 18 and Isha 17
 * degrees, Asr with a shadow factor of 1 and the Diyanet safety offsets, which is the method Turkey uses.
 * Pure Java so it runs in the unit tests.
 */
final class PrayerTimes {
    private PrayerTimes() {}

    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    static final double FAJR_ANGLE = 18.0, ISHA_ANGLE = 17.0;
    /** Minutes added to each computed time (Diyanet): Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    private static final int[] OFFSET_MIN = {0, -7, 5, 4, 7, 0};

    static final String[] NAMES_AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    /** The event after {@code at} (epoch ms) and which prayer it is. */
    static final class Next {
        final int index;
        final long at;

        Next(int index, long at) {
            this.index = index;
            this.at = at;
        }
    }

    /** Minutes after local midnight for the six events of one day. */
    static double[] minutes(int y, int m, int d, double lat, double lon, double tzHours) {
        double jd = julian(y, m, d) - lon / (15.0 * 24.0);
        double t = (jd - 2451545.0) / 36525.0;
        double l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360;
        double mean = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        double c = Math.sin(rad(mean)) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(rad(2 * mean)) * (0.019993 - 0.000101 * t)
                + Math.sin(rad(3 * mean)) * 0.000289;
        double trueLon = l0 + c;
        double omega = 125.04 - 1934.136 * t;
        double lam = trueLon - 0.00569 - 0.00478 * Math.sin(rad(omega));
        double eps0 = 23 + (26 + ((21.448 - t * (46.815 + t * (0.00059 - t * 0.001813)))) / 60) / 60;
        double eps = eps0 + 0.00256 * Math.cos(rad(omega));
        double decl = deg(Math.asin(Math.sin(rad(eps)) * Math.sin(rad(lam))));
        double y2 = Math.pow(Math.tan(rad(eps / 2)), 2);
        double eqt = 4 * deg(y2 * Math.sin(2 * rad(l0)) - 2 * e * Math.sin(rad(mean))
                + 4 * e * y2 * Math.sin(rad(mean)) * Math.cos(2 * rad(l0))
                - 0.5 * y2 * y2 * Math.sin(4 * rad(l0)) - 1.25 * e * e * Math.sin(2 * rad(mean)));
        double noon = 720 - 4 * lon - eqt + tzHours * 60;

        double[] r = new double[6];
        r[FAJR] = noon - hourAngle(lat, decl, -FAJR_ANGLE) * 4;
        r[SUNRISE] = noon - hourAngle(lat, decl, -0.833) * 4;
        r[DHUHR] = noon;
        double asrAlt = deg(Math.atan(1.0 / (1.0 + Math.tan(rad(Math.abs(lat - decl))))));
        r[ASR] = noon + hourAngle(lat, decl, asrAlt) * 4;
        r[MAGHRIB] = noon + hourAngle(lat, decl, -0.833) * 4;
        r[ISHA] = noon + hourAngle(lat, decl, -ISHA_ANGLE) * 4;
        for (int i = 0; i < 6; i++) r[i] += OFFSET_MIN[i];
        return r;
    }

    /** Epoch milliseconds of the six events on {@code date} in {@code zone}. */
    static long[] millis(LocalDate date, ZoneId zone, double lat, double lon) {
        long noonOffsetSec = zone.getRules().getOffset(date.atTime(12, 0).atZone(zone).toInstant()).getTotalSeconds();
        double[] min = minutes(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), lat, lon, noonOffsetSec / 3600.0);
        long midnight = date.atStartOfDay(zone).toInstant().toEpochMilli();
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) out[i] = midnight + Math.round(min[i] * 60000.0);
        return out;
    }

    /** The first prayer strictly after {@code now}, today's or tomorrow's. Sunrise counts only when asked. */
    static Next next(long now, ZoneId zone, double lat, double lon, boolean includeSunrise) {
        LocalDate day = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        for (int offset = 0; offset < 3; offset++) {
            long[] t = millis(day.plusDays(offset), zone, lat, lon);
            for (int i = 0; i < 6; i++) {
                if (i == SUNRISE && !includeSunrise) continue;
                if (t[i] > now) return new Next(i, t[i]);
            }
        }
        return new Next(FAJR, now + 86_400_000L);   // not reached: three days always hold a prayer
    }

    /** "06:08", always with Western digits whatever the TV's language is. */
    static String format(long epochMs, ZoneId zone) {
        java.time.ZonedDateTime z = java.time.Instant.ofEpochMilli(epochMs).atZone(zone);
        long minutes = Math.round((z.getHour() * 3600 + z.getMinute() * 60 + z.getSecond()) / 60.0);
        return String.format(Locale.US, "%02d:%02d", (minutes / 60) % 24, minutes % 60);
    }

    private static double rad(double d) { return Math.toRadians(d); }

    private static double deg(double r) { return Math.toDegrees(r); }

    private static double hourAngle(double lat, double decl, double alt) {
        double c = (Math.sin(rad(alt)) - Math.sin(rad(lat)) * Math.sin(rad(decl)))
                / (Math.cos(rad(lat)) * Math.cos(rad(decl)));
        return deg(Math.acos(Math.max(-1, Math.min(1, c))));
    }

    private static double julian(int y, int m, int d) {
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        int a = y / 100, b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }
}
