package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * The Islamic strip on Home: today's five prayers (the next one lit green, with the time left), and under
 * them a verse or a hadith that changes every 45 seconds, with the Hijri date. Not focusable: the remote
 * keeps driving the dock. Digits are always 0-9.
 */
public final class IslamicPanel extends LinearLayout {
    private static final int GREEN = 0xFF1F7A4D, GOLD = 0xFFE8C766;
    private static final long TICK_MS = 15_000L, QUOTE_MS = 45_000L;
    private static final int[] SHOWN = {PrayerTimes.FAJR, PrayerTimes.DHUHR, PrayerTimes.ASR, PrayerTimes.MAGHRIB, PrayerTimes.ISHA};
    private static final String[] HIJRI_MONTHS = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى",
        "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView[] names = new TextView[SHOWN.length];
    private final TextView[] times = new TextView[SHOWN.length];
    private final TextView[] extra = new TextView[SHOWN.length];
    private final LinearLayout[] cards = new LinearLayout[SHOWN.length];
    private TextView quote, source, hijri;
    private int turn;
    private long lastQuote;

    public IslamicPanel(Context context) {
        this(context, null);
    }

    public IslamicPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClipChildren(false);
        build();
    }

    private void build() {
        float d = getResources().getDisplayMetrics().density;

        // Five cards. RTL, so the first (Fajr) sits at the right edge as in Arabic prayer tables.
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        for (int i = 0; i < SHOWN.length; i++) {
            LinearLayout c = new LinearLayout(getContext());
            c.setOrientation(VERTICAL);
            c.setGravity(Gravity.CENTER);
            c.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
            names[i] = label(PrayerTimes.NAMES_AR[SHOWN[i]], 14, 0xCCFFFFFF, R.string.font_medium);
            times[i] = label("--:--", 24, Color.WHITE, R.string.font_regular);
            extra[i] = label("", 12, 0xE6FFFFFF, R.string.font_regular);
            c.addView(names[i]);
            c.addView(times[i]);
            c.addView(extra[i]);
            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginEnd(0);
            lp.setMarginStart((int) (10 * d));
            row.addView(c, lp);
            cards[i] = c;
        }
        LayoutParams rowLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(row, rowLp);

        // The verse / hadith card.
        LinearLayout q = new LinearLayout(getContext());
        q.setOrientation(VERTICAL);
        q.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        q.setPadding((int) (22 * d), (int) (12 * d), (int) (22 * d), (int) (10 * d));
        q.setBackground(rounded(0x26FFFFFF, 18 * d, 0x33E8C766, d));
        quote = label("", 19, GOLD, R.string.font_light);
        quote.setMaxLines(3);
        quote.setLineSpacing(0f, 1.15f);
        q.addView(quote);

        LinearLayout foot = new LinearLayout(getContext());
        foot.setOrientation(HORIZONTAL);
        foot.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        source = label("", 13, 0xB3FFFFFF, R.string.font_regular);
        source.setGravity(Gravity.START);
        hijri = label("", 13, 0xB3FFFFFF, R.string.font_regular);
        hijri.setGravity(Gravity.END);
        foot.addView(source, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        foot.addView(hijri, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        LayoutParams fl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        fl.topMargin = (int) (6 * d);
        q.addView(foot, fl);

        LayoutParams ql = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ql.topMargin = (int) (10 * d);
        addView(q, ql);
    }

    private TextView label(String s, float sp, int color, int font) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setTypeface(UiFont.get(getContext(), font));
        t.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return t;
    }

    private static GradientDrawable rounded(int fill, float radius, int stroke, float d) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (stroke != 0) g.setStroke(Math.max(1, (int) d), stroke);
        return g;
    }

    /** Recomputes the five times, which one is next, and (when due) the quote. */
    void refresh() {
        Context c = getContext();
        float d = getResources().getDisplayMetrics().density;
        ZoneId zone = ZoneId.systemDefault();
        long now = System.currentTimeMillis();
        double lat = PrayerPrefs.lat(c), lon = PrayerPrefs.lon(c);
        long[] today = PrayerTimes.millis(LocalDate.now(zone), zone, lat, lon);
        PrayerTimes.Next next = PrayerTimes.next(now, zone, lat, lon, false);
        for (int i = 0; i < SHOWN.length; i++) {
            int p = SHOWN[i];
            times[i].setText(PrayerTimes.format(today[p], zone));
            boolean isNext = p == next.index;
            cards[i].setBackground(isNext ? rounded(GREEN, 18 * d, 0, d) : rounded(0x26FFFFFF, 18 * d, 0x1FFFFFFF, d));
            extra[i].setText(isNext ? remaining(next.at - now) : "");
            extra[i].setVisibility(isNext ? VISIBLE : INVISIBLE);
        }
        hijri.setText(hijriDate(LocalDate.now(zone)));
        if (lastQuote == 0 || now - lastQuote >= QUOTE_MS) {
            lastQuote = now;
            String[] e = IslamicContent.pick(turn++, LocalDate.now(zone).getDayOfYear());
            quote.setText(e[0]);
            source.setText(e[1]);
        }
    }

    private static String remaining(long ms) {
        long min = Math.max(0, ms / 60000L);
        if (min >= 60) return String.format(Locale.US, "بعد %d:%02d", min / 60, min % 60);
        return String.format(Locale.US, "بعد %d د", min);
    }

    private static String hijriDate(LocalDate g) {
        try {
            HijrahDate h = HijrahDate.from(g);
            return String.format(Locale.US, "%d %s %d هـ", h.get(ChronoField.DAY_OF_MONTH),
                    HIJRI_MONTHS[h.get(ChronoField.MONTH_OF_YEAR) - 1], h.get(ChronoField.YEAR));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        if (visibility != VISIBLE) handler.removeCallbacks(tick);
        else if (isAttachedToWindow()) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }
}
