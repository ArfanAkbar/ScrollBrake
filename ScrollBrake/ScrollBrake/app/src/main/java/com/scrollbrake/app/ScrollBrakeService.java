package com.scrollbrake.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The heart of ScrollBrake.
 *
 * 1. Watches which app is on screen.
 * 2. Counts how long you've been in a social media app.
 * 3. After your limit (10-15 min by default), lays a grey "fade" over the app that gets
 *    stronger every minute, so the feed looks dull and less tempting.
 * 4. Every few minutes past the limit, shows a short "take a break" pause card.
 */
public class ScrollBrakeService extends AccessibilityService {

    /** If you stay away from an app this long, its timer starts over. */
    private static final long SESSION_RESET_MS = 30 * 60 * 1000L;
    /** How often (in minutes past the limit) the pause card comes back. */
    private static final int PAUSE_EVERY_MIN = 3;
    /** How long the pause card makes you wait before "Continue" works. */
    private static final int PAUSE_WAIT_SECONDS = 10;
    /** Fade starts at this strength and grows each minute, up to the maximum. */
    private static final float FADE_START = 0.35f;
    private static final float FADE_PER_MIN = 0.05f;
    private static final float FADE_MAX = 0.80f;

    private static final String[] BREAK_IDEAS = {
            "Stand up and stretch for one minute.",
            "Drink a glass of water.",
            "Message a friend directly instead of scrolling.",
            "Step outside or look out of a window for a bit.",
            "Read a few pages of a book.",
            "Take five slow, deep breaths.",
            "Write down one thing you want to get done today.",
            "Put the phone in another room for 10 minutes.",
    };

    private static class Session {
        long seconds;          // time spent in this app during the current session
        long lastSeenMs;       // when we last saw you in this app
        long nextPauseAtSec;   // when the next pause card should appear
        long unsavedSeconds;   // usage not yet written to storage
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Session> sessions = new HashMap<>();
    private final Random random = new Random();

    private WindowManager windowManager;
    private PowerManager powerManager;

    private String currentLabel;   // e.g. "Instagram" while Instagram is open, otherwise null

    private FrameLayout fadeView;
    private TextView fadeText;
    private View pauseView;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            onTick();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkgChars = event.getPackageName();
        if (pkgChars == null) return;
        String pkg = pkgChars.toString();
        if (shouldIgnore(pkg)) return;

        String label = Prefs.labelFor(pkg);
        if (label != null && Prefs.isWatched(this, label)) {
            enterApp(label);
        } else {
            leaveApp();
        }
    }

    /** Things like the keyboard, notification shade or our own overlay shouldn't count as "leaving". */
    private boolean shouldIgnore(String pkg) {
        return pkg.equals(getPackageName())
                || pkg.equals("android")
                || pkg.equals("com.android.systemui")
                || pkg.contains("inputmethod")
                || pkg.contains("keyboard");
    }

    private void enterApp(String label) {
        if (label.equals(currentLabel)) return;
        leaveApp();

        long now = System.currentTimeMillis();
        Session s = sessions.get(label);
        if (s == null || now - s.lastSeenMs > SESSION_RESET_MS) {
            s = new Session();
            s.nextPauseAtSec = Prefs.getLimitMin(this) * 60L;
            sessions.put(label, s);
        }
        s.lastSeenMs = now;
        currentLabel = label;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 1000);
    }

    private void leaveApp() {
        if (currentLabel != null) {
            Session s = sessions.get(currentLabel);
            if (s != null) {
                s.lastSeenMs = System.currentTimeMillis();
                saveUsage(currentLabel, s);
            }
        }
        currentLabel = null;
        handler.removeCallbacks(tick);
        removeFade();
        removePause();
    }

    private void onTick() {
        if (currentLabel == null) return;
        Session s = sessions.get(currentLabel);
        if (s == null) return;

        // Only count time while the screen is actually on.
        if (powerManager == null || powerManager.isInteractive()) {
            s.seconds++;
            s.unsavedSeconds++;
            s.lastSeenMs = System.currentTimeMillis();
            if (s.unsavedSeconds >= 10) saveUsage(currentLabel, s);

            long limitSec = Prefs.getLimitMin(this) * 60L;
            if (s.seconds >= limitSec) {
                showOrUpdateFade(currentLabel, s.seconds, s.seconds - limitSec);
                if (s.seconds >= s.nextPauseAtSec && pauseView == null) {
                    showPause(currentLabel, s.seconds);
                    s.nextPauseAtSec = s.seconds + PAUSE_EVERY_MIN * 60L;
                }
            } else {
                removeFade();
            }
        }
        handler.postDelayed(tick, 1000);
    }

    private void saveUsage(String label, Session s) {
        Prefs.addUsageSeconds(this, label, s.unsavedSeconds);
        s.unsavedSeconds = 0;
    }

    // ---------------------------------------------------------------- the fading layer

    private void showOrUpdateFade(String label, long totalSec, long overSec) {
        if (windowManager == null) return;
        float strength = Math.min(FADE_MAX, FADE_START + (overSec / 60f) * FADE_PER_MIN);
        int alpha = Math.round(strength * 255);

        if (fadeView == null) {
            fadeView = new FrameLayout(this);
            fadeText = new TextView(this);
            fadeText.setTextColor(Color.WHITE);
            fadeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            fadeText.setPadding(dp(14), dp(6), dp(14), dp(6));
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(Color.argb(200, 20, 20, 20));
            pill.setCornerRadius(dp(20));
            fadeText.setBackground(pill);
            FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            textLp.topMargin = dp(40);
            fadeView.addView(fadeText, textLp);

            // NOT_TOUCHABLE: you can still use the app underneath, it just looks dull.
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            try {
                windowManager.addView(fadeView, lp);
            } catch (Exception e) {
                fadeView = null;
                fadeText = null;
                return;
            }
        }
        // A grey wash drains the colour out of photos and videos.
        fadeView.setBackgroundColor(Color.argb(alpha, 70, 70, 70));
        fadeText.setText((totalSec / 60) + " min on " + label + "  ·  the feed is fading");
    }

    private void removeFade() {
        if (fadeView != null && windowManager != null) {
            try {
                windowManager.removeView(fadeView);
            } catch (Exception ignored) {
            }
        }
        fadeView = null;
        fadeText = null;
    }

    // ---------------------------------------------------------------- the pause card

    private void showPause(final String label, long totalSec) {
        if (windowManager == null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(32), dp(32), dp(32), dp(32));
        card.setBackgroundColor(Color.argb(240, 18, 24, 22));
        // Swallow taps so the feed underneath can't be scrolled while the card is up.
        card.setClickable(true);

        TextView title = text("Time for a break?", 26, true);
        TextView body = text("You've spent " + (totalSec / 60) + " minutes on " + label
                + ". The longer you stay, the more the feed fades.", 16, false);
        TextView idea = text("Try this: " + BREAK_IDEAS[random.nextInt(BREAK_IDEAS.length)], 15, false);
        idea.setTextColor(Color.rgb(140, 220, 190));

        Button leave = new Button(this);
        leave.setText("Leave " + label);
        leave.setAllCaps(false);
        leave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePause();
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        });

        final Button stay = new Button(this);
        stay.setAllCaps(false);
        stay.setEnabled(false);
        stay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePause();
            }
        });

        card.addView(title, wrapLp(0));
        card.addView(body, wrapLp(dp(16)));
        card.addView(idea, wrapLp(dp(16)));
        card.addView(leave, wrapLp(dp(32)));
        card.addView(stay, wrapLp(dp(8)));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        try {
            windowManager.addView(card, lp);
            pauseView = card;
        } catch (Exception e) {
            pauseView = null;
            return;
        }

        // Countdown before "Continue" can be pressed.
        final int[] left = {PAUSE_WAIT_SECONDS};
        final View thisCard = card;
        stay.setText("Continue scrolling (" + left[0] + ")");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pauseView != thisCard) return; // card already closed
                left[0]--;
                if (left[0] > 0) {
                    stay.setText("Continue scrolling (" + left[0] + ")");
                    handler.postDelayed(this, 1000);
                } else {
                    stay.setText("Continue scrolling");
                    stay.setEnabled(true);
                }
            }
        }, 1000);
    }

    private void removePause() {
        if (pauseView != null && windowManager != null) {
            try {
                windowManager.removeView(pauseView);
            } catch (Exception ignored) {
            }
        }
        pauseView = null;
    }

    // ---------------------------------------------------------------- helpers

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams wrapLp(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onInterrupt() {
        // Nothing to do.
    }

    @Override
    public void onDestroy() {
        leaveApp();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
