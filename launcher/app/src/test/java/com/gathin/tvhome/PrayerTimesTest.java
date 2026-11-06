package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Test;

public class PrayerTimesTest {
    private static final double LAT = 41.0605086, LON = 28.8355530;
    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    private static String at(long[] t, int i) { return PrayerTimes.format(t[i], ISTANBUL); }

    @Test
    public void istanbulInNovember() {
        long[] t = PrayerTimes.millis(LocalDate.of(2026, 11, 7), ISTANBUL, LAT, LON);
        assertEquals("06:08", at(t, PrayerTimes.FAJR));
        assertEquals("12:53", at(t, PrayerTimes.DHUHR));
        assertEquals("18:01", at(t, PrayerTimes.MAGHRIB));
        assertEquals("19:23", at(t, PrayerTimes.ISHA));   // matches a published MWL 18/17 table for the day
    }

    @Test
    public void nextPrayerRollsOverToTomorrowsFajr() {
        long[] t = PrayerTimes.millis(LocalDate.of(2026, 11, 7), ISTANBUL, LAT, LON);
        PrayerTimes.Next n = PrayerTimes.next(t[PrayerTimes.ISHA] + 1000, ISTANBUL, LAT, LON, false);
        assertEquals(PrayerTimes.FAJR, n.index);
        assertTrue(n.at > t[PrayerTimes.ISHA]);
    }

    @Test
    public void sunriseIsSkippedUnlessAsked() {
        long[] t = PrayerTimes.millis(LocalDate.of(2026, 11, 7), ISTANBUL, LAT, LON);
        assertEquals(PrayerTimes.DHUHR, PrayerTimes.next(t[PrayerTimes.FAJR] + 1000, ISTANBUL, LAT, LON, false).index);
        assertEquals(PrayerTimes.SUNRISE, PrayerTimes.next(t[PrayerTimes.FAJR] + 1000, ISTANBUL, LAT, LON, true).index);
    }
}
