package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.ZoneId;

/**
 * The adhan: a full-screen black-and-gold screen in its own task, so whatever was on screen stays underneath
 * and comes back when this closes. It takes exclusive audio focus (players that honour it pause), swallows
 * every remote key but the volume, and Back (or Escape, Delete, Stop) ends it early.
 */
public final class AdhanActivity extends Activity {
    static final String EXTRA_INDEX = "prayer_index";
    static final String EXTRA_AT = "prayer_at";
    private static final int GOLD = 0xFFE8C766;
    private static final long SAFETY_MS = 10 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private boolean pausedMedia;
    private boolean ended;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        int index = getIntent().getIntExtra(EXTRA_INDEX, PrayerTimes.FAJR);
        long at = getIntent().getLongExtra(EXTRA_AT, System.currentTimeMillis());
        setContentView(build(index, at));
        startAudio();
        handler.postDelayed(this::finishAdhan, SAFETY_MS);
    }

    private View build(int index, long at) {
        float d = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF06251A, 0xFF020A07, 0xFF000000});
        root.setBackground(bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(col, colLp);

        col.addView(new CrescentView(this, GOLD), new LinearLayout.LayoutParams((int) (110 * d), (int) (110 * d)));
        col.addView(text("حان الآن موعد أذان", 26, 0xCCFFFFFF, R.string.font_light, (int) (18 * d)));
        col.addView(text(PrayerTimes.NAMES_AR[index], 96, GOLD, R.string.font_bold, (int) (4 * d)));
        col.addView(text(PrayerTimes.format(at, ZoneId.systemDefault()), 40, Color.WHITE, R.string.font_medium, (int) (2 * d)));

        View line = new View(this);
        line.setBackgroundColor(0x55E8C766);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams((int) (220 * d), Math.max(1, (int) d));
        lineLp.topMargin = (int) (26 * d);
        lineLp.bottomMargin = (int) (22 * d);
        col.addView(line, lineLp);

        TextView verse = text("\uFD3F إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا \uFD3E", 24, GOLD,
                R.string.font_light, 0);
        verse.setPadding((int) (120 * d), 0, (int) (120 * d), 0);
        col.addView(verse);

        TextView hint = text("اضغط رجوع (Back / Exit) للخروج والعودة إلى ما كنت تشاهده", 16, 0x99FFFFFF,
                R.string.font_regular, 0);
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        hintLp.bottomMargin = (int) (36 * d);
        root.addView(hint, hintLp);
        return root;
    }

    private TextView text(String s, float sp, int color, int fontString, int topPx) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setTypeface(UiFont.get(this, fontString));
        t.setPadding(0, topPx, 0, 0);
        return t;
    }

    private void startAudio() {
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        pausedMedia = audio.isMusicActive();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(change -> { })
                .build();
        audio.requestAudioFocus(focus);
        // Players that ignore audio focus still hear the pause key. Only sent when something was playing.
        if (pausedMedia) mediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);

        AdhanVoices.Voice voice = AdhanVoices.chosen(this);
        if (voice == null) {
            handler.postDelayed(this::finishAdhan, 20_000L);
            return;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            if (voice.file != null) {
                player.setDataSource(voice.file);
            } else if (voice.url != null) {
                player.setDataSource(voice.url);
            } else {
                AssetFileDescriptor afd = getResources().openRawResourceFd(voice.rawId);
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            }
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp -> finishAdhan());
            player.setOnErrorListener((mp, what, extra) -> {
                finishAdhan();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            handler.postDelayed(this::finishAdhan, 15_000L);
        }
    }

    private void mediaKey(int code) {
        if (audio == null) return;
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    private void finishAdhan() {
        if (ended) return;
        ended = true;
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // not started yet
            }
            player.release();
            player = null;
        }
        if (audio != null && focus != null) audio.abandonAudioFocusRequest(focus);
        if (pausedMedia) mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);   // only what the adhan itself paused
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.dispatchKeyEvent(e);
        }
        if (k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_ESCAPE || k == KeyEvent.KEYCODE_DEL
                || k == KeyEvent.KEYCODE_MEDIA_STOP || k == KeyEvent.KEYCODE_MEDIA_CLOSE) {
            if (e.getAction() == KeyEvent.ACTION_UP) finishAdhan();
            return true;
        }
        return true;   // the adhan has the screen: no other key reaches anything
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!ended) finishAdhan();   // Home or another app took over: the adhan must not play on behind it
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finishAdhan();
    }
}
