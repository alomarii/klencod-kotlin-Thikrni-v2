package com.alikhwa.thikrni;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** شريط تنقل سفلي عائم بمؤشر منزلق وأيقونات متحركة. */
public class BottomNav extends FrameLayout {
    public interface OnSelect {
        void onSelect(int index);
    }

    private static final String[] LABELS = {"الرئيسية", "إحصائياتي", "الصوتيات", "الإعدادات"};

    private final Ui ui;
    private final View pill;
    private final LinearLayout row;
    private final LinearLayout[] items = new LinearLayout[4];
    private final NavIcon[] icons = new NavIcon[4];
    private final TextView[] labels = new TextView[4];
    private int sel;
    private final OnSelect cb;

    public BottomNav(Context c, Ui ui, int initial, OnSelect cb) {
        super(c);
        this.ui = ui;
        this.cb = cb;
        this.sel = initial;
        Theme t = ui.t;

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.navBg);
        bg.setCornerRadius(ui.dp(28));
        bg.setStroke(ui.dp(1), Theme.a(t.accent, 0x66));
        setBackground(bg);
        setElevation(ui.dp(10));

        pill = new View(c);
        GradientDrawable pg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Theme.a(t.accent, 0x44), Theme.a(t.accent, 0x18)});
        pg.setCornerRadius(ui.dp(22));
        pg.setStroke(ui.dp(1), Theme.a(t.accent, 0x77));
        pill.setBackground(pg);
        addView(pill, new LayoutParams(ui.dp(70), ui.dp(54), Gravity.LEFT | Gravity.CENTER_VERTICAL));

        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(ui.dp(6), 0, ui.dp(6), 0);
        addView(row, new LayoutParams(-1, -1));

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            NavIcon ic = new NavIcon(c, i, t.sub, t.accent);
            ic.setSelectedAnimated(i == sel, false);
            item.addView(ic, new LinearLayout.LayoutParams(ui.dp(30), ui.dp(26)));
            TextView lb = ui.text(LABELS[i], 11, i == sel ? t.accent : t.sub, true);
            lb.setPadding(0, ui.dp(2), 0, 0);
            item.addView(lb);
            item.setOnClickListener(v -> {
                if (idx != sel) {
                    select(idx, true);
                    this.cb.onSelect(idx);
                }
            });
            row.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            items[i] = item;
            icons[i] = ic;
            labels[i] = lb;
        }

        addOnLayoutChangeListener((v, l, tp, r, b, ol, ot, or, ob) -> placePill(false));
    }

    private void placePill(boolean animate) {
        View item = items[sel];
        if (item.getWidth() == 0) return;
        int w = item.getWidth() - ui.dp(10);
        if (pill.getLayoutParams().width != w) {
            pill.getLayoutParams().width = w;
            pill.requestLayout();
        }
        float x = row.getLeft() + item.getLeft() + ui.dp(5);
        if (animate) {
            pill.animate().translationX(x).setDuration(420)
                    .setInterpolator(new OvershootInterpolator(0.85f)).start();
        } else {
            pill.animate().cancel();
            pill.setTranslationX(x);
        }
    }

    public void select(int i, boolean animate) {
        if (i == sel && !animate) {
            placePill(false);
            return;
        }
        sel = i;
        for (int k = 0; k < 4; k++) {
            icons[k].setSelectedAnimated(k == i, animate);
            labels[k].setTextColor(k == i ? ui.t.accent : ui.t.sub);
        }
        placePill(animate);
    }
}
