package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

/** Remote-friendly settings for the prayer banner, the adhan and the Home strip. Built in code, OK toggles a row. */
public final class PrayerSettingsActivity extends Activity {
    private SharedPreferences prefs;
    private TextView bannerRow, adhanRow, voiceRow, panelRow, overlayRow, infoRow;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = PrayerPrefs.get(this);
        float d = getResources().getDisplayMetrics().density;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding((int) (72 * d), (int) (38 * d), (int) (72 * d), (int) (38 * d));

        TextView title = new TextView(this);
        title.setText("الصلاة والأذان");
        title.setTextColor(0xFFE8C766);
        title.setTextSize(34);
        title.setTypeface(UiFont.get(this, R.string.font_bold));
        col.addView(title);

        bannerRow = row(col, d, 24);
        adhanRow = row(col, d, 8);
        voiceRow = row(col, d, 8);
        panelRow = row(col, d, 8);
        overlayRow = row(col, d, 24);
        TextView test1 = row(col, d, 24);
        TextView test2 = row(col, d, 8);
        infoRow = new TextView(this);
        infoRow.setTextColor(0x99FFFFFF);
        infoRow.setTextSize(15);
        infoRow.setTypeface(UiFont.get(this, R.string.font_regular));
        infoRow.setPadding((int) (18 * d), (int) (24 * d), (int) (18 * d), 0);
        col.addView(infoRow);

        bannerRow.setOnClickListener(v -> toggle("banner", PrayerPrefs.banner(this)));
        adhanRow.setOnClickListener(v -> toggle("adhan", PrayerPrefs.adhan(this)));
        panelRow.setOnClickListener(v -> toggle("panel", PrayerPrefs.panel(this)));
        voiceRow.setOnClickListener(v -> nextVoice());
        overlayRow.setOnClickListener(v -> openOverlaySettings());
        test1.setText("تجربة الإشعار الآن");
        test1.setOnClickListener(v -> PrayerOverlayTest.banner(this));
        test2.setText("تجربة الأذان بعد 3 ثوانٍ");
        test2.setOnClickListener(v -> PrayerOverlayTest.adhan(this));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF101419);
        scroll.setFillViewport(true);
        scroll.addView(col);
        setContentView(scroll);
        bannerRow.requestFocus();
        render();
    }

    private TextView row(LinearLayout parent, float d, int topDp) {
        TextView t = new TextView(this, null, 0, R.style.SettingsRow);
        t.setTypeface(UiFont.get(this, R.string.font_medium));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
        lp.topMargin = (int) (topDp * d);
        parent.addView(t, lp);
        return t;
    }

    private void toggle(String key, boolean current) {
        prefs.edit().putBoolean(key, !current).apply();
        PrayerScheduler.schedule(this);
        render();
    }

    private void nextVoice() {
        List<AdhanVoices.Voice> all = AdhanVoices.list(this);
        if (all.isEmpty()) return;
        String cur = AdhanVoices.chosen(this).id;
        int i = 0;
        for (int k = 0; k < all.size(); k++) if (all.get(k).id.equals(cur)) i = k;
        prefs.edit().putString("voice", all.get((i + 1) % all.size()).id).apply();
        render();
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            // Many TVs hide this page: the README gives the adb command that grants it instead.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private static String onOff(boolean b) { return b ? "مفعّل" : "معطّل"; }

    private void render() {
        bannerRow.setText("إشعار وقت الصلاة فوق أي تطبيق:  " + onOff(PrayerPrefs.banner(this)));
        adhanRow.setText("تشغيل الأذان وإيقاف ما يعمل مؤقتاً:  " + onOff(PrayerPrefs.adhan(this)));
        AdhanVoices.Voice v = AdhanVoices.chosen(this);
        voiceRow.setText("صوت الأذان:  " + (v == null ? "لا يوجد صوت (انظر الملاحظة أدناه)" : v.name));
        panelRow.setText("عرض المواقيت والآيات في الرئيسية:  " + onOff(PrayerPrefs.panel(this)));
        overlayRow.setText("إذن العرض فوق التطبيقات:  " + (Settings.canDrawOverlays(this) ? "ممنوح" : "غير ممنوح — اضغط للفتح"));
        infoRow.setText(String.format(Locale.US,
                "الموقع: %.5f , %.5f  ·  طريقة الحساب: رئاسة الشؤون الدينية التركية (الفجر 18°، العشاء 17°)\n"
                        + "أصوات الأذان: ضع ملفات mp3 في  %s\nأو سطراً بصيغة  الاسم|https://...  في الملف sources.txt بالمجلد نفسه.",
                PrayerPrefs.lat(this), PrayerPrefs.lon(this), AdhanVoices.folder(this).getAbsolutePath()));
    }
}
