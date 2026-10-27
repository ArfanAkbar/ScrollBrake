package com.scrollbrake.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Saved settings and today's usage numbers. */
final class Prefs {

    /**
     * Each row: the name shown to people, followed by that app's Android package name(s).
     * YouTube is left out on purpose: it is used a lot for learning and long videos.
     */
    static final String[][] APPS = {
            {"Instagram", "com.instagram.android"},
            {"TikTok", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"},
            {"Facebook", "com.facebook.katana", "com.facebook.lite"},
            {"Snapchat", "com.snapchat.android"},
            {"X (Twitter)", "com.twitter.android"},
            {"Threads", "com.instagram.barcelona"},
            {"Reddit", "com.reddit.frontpage"},
    };

    static final int DEFAULT_LIMIT_MIN = 12;
    static final int MIN_LIMIT_MIN = 5;
    static final int MAX_LIMIT_MIN = 30;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("scrollbrake", Context.MODE_PRIVATE);
    }

    /** Returns the display name for a package, or null if it isn't a social app we know about. */
    static String labelFor(String pkg) {
        for (String[] app : APPS) {
            for (int i = 1; i < app.length; i++) {
                if (app[i].equals(pkg)) return app[0];
            }
        }
        return null;
    }

    static int getLimitMin(Context c) {
        return sp(c).getInt("limit_min", DEFAULT_LIMIT_MIN);
    }

    static void setLimitMin(Context c, int minutes) {
        sp(c).edit().putInt("limit_min", minutes).apply();
    }

    static boolean isWatched(Context c, String label) {
        return sp(c).getBoolean("watch_" + label, true);
    }

    static void setWatched(Context c, String label, boolean watched) {
        sp(c).edit().putBoolean("watch_" + label, watched).apply();
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static void addUsageSeconds(Context c, String label, long seconds) {
        if (seconds <= 0) return;
        String key = "use_" + today() + "_" + label;
        SharedPreferences p = sp(c);
        p.edit().putLong(key, p.getLong(key, 0) + seconds).apply();
    }

    static long getUsageSecondsToday(Context c, String label) {
        return sp(c).getLong("use_" + today() + "_" + label, 0);
    }
}
