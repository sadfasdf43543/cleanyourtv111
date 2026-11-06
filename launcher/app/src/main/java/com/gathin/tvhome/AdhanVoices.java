package com.gathin.tvhome;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The adhan voices the viewer can pick. Three places, in this order:
 * <ol>
 * <li>audio files in Android/data/&lt;package&gt;/files/adhan/ on the TV (adb push, or any file manager);</li>
 * <li>lines "Name|https://address" in adhan/sources.txt in that folder, streamed when the adhan starts;</li>
 * <li>a res/raw/adhan_default.* file compiled into the build, if one exists.</li>
 * </ol>
 */
final class AdhanVoices {
    private AdhanVoices() {}

    static final class Voice {
        final String id;      // stable key kept in the settings
        final String name;    // what the settings row shows
        final String file;    // local path, or null
        final String url;     // https address, or null
        final int rawId;      // res/raw id, or 0

        Voice(String id, String name, String file, String url, int rawId) {
            this.id = id;
            this.name = name;
            this.file = file;
            this.url = url;
            this.rawId = rawId;
        }
    }

    static File folder(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File dir = new File(base, "adhan");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    static List<Voice> list(Context c) {
        List<Voice> out = new ArrayList<>();
        File dir = folder(c);
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".m4a") || n.endsWith(".wav")
                        || n.endsWith(".aac")) {
                    String base = f.getName().substring(0, f.getName().lastIndexOf('.'));
                    out.add(new Voice("file:" + f.getName(), base, f.getAbsolutePath(), null, 0));
                }
            }
        }
        File sources = new File(dir, "sources.txt");
        if (sources.isFile()) {
            try (BufferedReader r = new BufferedReader(new FileReader(sources))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    int bar = line.indexOf('|');
                    if (bar <= 0 || line.startsWith("#")) continue;
                    String name = line.substring(0, bar).trim();
                    String url = line.substring(bar + 1).trim();
                    if (!url.startsWith("https://") || name.isEmpty()) continue;   // https only
                    out.add(new Voice("url:" + name, name, null, url, 0));
                }
            } catch (Exception ignored) {
                // an unreadable list just offers fewer voices
            }
        }
        int raw = c.getResources().getIdentifier("adhan_default", "raw", c.getPackageName());
        if (raw != 0) out.add(new Voice("raw:adhan_default", "Adhan", null, null, raw));
        return out;
    }

    /** The saved voice, else the first one there is, else null (the alert then falls back to the banner). */
    static Voice chosen(Context c) {
        List<Voice> all = list(c);
        if (all.isEmpty()) return null;
        String id = PrayerPrefs.voice(c);
        for (Voice v : all) if (v.id.equals(id)) return v;
        return all.get(0);
    }
}
