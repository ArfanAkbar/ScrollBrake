package com.scrollbrake.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** The settings screen you see when you open ScrollBrake. */
public class MainActivity extends Activity {

    private static final int GREEN = Color.rgb(31, 111, 92);

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(Color.rgb(246, 248, 247));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(40));
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildScreen(); // refresh: the user may have just switched the service on
    }

    private void buildScreen() {
        root.removeAllViews();

        root.addView(text("ScrollBrake", 30, true, GREEN));
        root.addView(text("Scroll less, live more. After a few minutes in a social app, "
                + "the feed slowly fades and you get gentle nudges to take a break.", 15, false, Color.DKGRAY), top(6));

        // --- Status card
        boolean on = isServiceOn();
        LinearLayout status = card();
        status.addView(text(on ? "Protection is ON" : "Protection is OFF", 20, true,
                on ? GREEN : Color.rgb(190, 60, 50)));
        if (on) {
            status.addView(text("ScrollBrake is watching the apps you picked below.", 14, false, Color.DKGRAY), top(4));
        } else {
            status.addView(text("Tap the button, find \"ScrollBrake\" in the list (sometimes under "
                    + "\"Installed apps\" or \"Downloaded apps\") and turn it on.", 14, false, Color.DKGRAY), top(4));
            Button turnOn = button("Turn on ScrollBrake");
            turnOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }
            });
            status.addView(turnOn, top(12));

            status.addView(text("Is the switch greyed out or showing \"Restricted setting\"? Tap below, "
                    + "then tap the three dots (top right) and choose \"Allow restricted settings\". "
                    + "Then try again.", 13, false, Color.GRAY), top(12));
            Button appInfo = button("Open app info");
            appInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            });
            status.addView(appInfo, top(8));
        }
        root.addView(status, top(20));

        // --- Time limit
        LinearLayout limitCard = card();
        final TextView limitLabel = text("", 17, true, Color.BLACK);
        limitCard.addView(limitLabel);
        limitCard.addView(text("You can still use the app afterwards. It just gets duller and duller.",
                13, false, Color.GRAY), top(4));
        SeekBar bar = new SeekBar(this);
        bar.setMax(Prefs.MAX_LIMIT_MIN - Prefs.MIN_LIMIT_MIN);
        bar.setProgress(Prefs.getLimitMin(this) - Prefs.MIN_LIMIT_MIN);
        setLimitText(limitLabel, Prefs.getLimitMin(this));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int minutes = progress + Prefs.MIN_LIMIT_MIN;
                setLimitText(limitLabel, minutes);
                if (fromUser) Prefs.setLimitMin(MainActivity.this, minutes);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        limitCard.addView(bar, top(12));
        root.addView(limitCard, top(16));

        // --- Apps
        LinearLayout appsCard = card();
        appsCard.addView(text("Apps to slow down", 17, true, Color.BLACK));
        long totalToday = 0;
        for (String[] app : Prefs.APPS) {
            final String label = app[0];
            long secs = Prefs.getUsageSecondsToday(this, label);
            totalToday += secs;
            CheckBox cb = new CheckBox(this);
            cb.setText(label + "   ·   " + (secs / 60) + " min today");
            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            cb.setChecked(Prefs.isWatched(this, label));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean checked) {
                    Prefs.setWatched(MainActivity.this, label, checked);
                }
            });
            appsCard.addView(cb, top(6));
        }
        appsCard.addView(text("Total today: " + (totalToday / 60) + " min", 14, true, GREEN), top(12));
        root.addView(appsCard, top(16));

        // --- How it works
        LinearLayout how = card();
        how.addView(text("How it works", 17, true, Color.BLACK));
        how.addView(text("1. Use your apps normally until you reach your limit.\n"
                + "2. Then a grey layer starts covering the app, getting stronger every minute, "
                + "so photos and videos look less exciting.\n"
                + "3. Every few minutes a pause card asks if you want a break. You can leave or "
                + "keep going after 10 seconds.\n"
                + "4. The timer resets after 30 minutes away from the app.\n\n"
                + "ScrollBrake only checks which app is on screen. It never reads your posts, "
                + "messages or what you type, and nothing leaves your phone.",
                14, false, Color.DKGRAY), top(6));
        root.addView(how, top(16));
    }

    private void setLimitText(TextView tv, int minutes) {
        tv.setText("Start fading after " + minutes + " minutes");
    }

    private boolean isServiceOn() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(this, ScrollBrakeService.class);
        String list = enabled.toLowerCase();
        return list.contains(me.flattenToString().toLowerCase())
                || list.contains(me.flattenToShortString().toLowerCase());
    }

    // ---------------------------------------------------------------- tiny UI helpers

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GREEN);
        bg.setCornerRadius(dp(24));
        b.setBackground(bg);
        return b;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(225, 230, 228));
        c.setBackground(bg);
        return c;
    }

    private LinearLayout.LayoutParams top(int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(marginDp);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
