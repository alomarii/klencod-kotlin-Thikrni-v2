package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/** زر الصلاة على النبي: حلقات نابضة + انفجار موجي عند الضغط. */
public class CounterView extends View {
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float pulse = 0f, burst = 1f, press = 1f;
    private ValueAnimator pulseAnim, burstAnim, pressAnim;
    private final float d;
    private final Theme th;

    public CounterView(Context c, Theme theme) {
        super(c);
        th = theme;
        d = c.getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2f * d);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(th.onAccent);

        pulseAnim = ValueAnimator.ofFloat(0f, 1f);
        pulseAnim.setDuration(2800);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setInterpolator(new LinearInterpolator());
        pulseAnim.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!pulseAnim.isStarted()) pulseAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        pulseAnim.cancel();
        super.onDetachedFromWindow();
    }

    /** موجة ضوئية عند كل ضغطة. */
    public void burst() {
        if (burstAnim != null) burstAnim.cancel();
        burstAnim = ValueAnimator.ofFloat(0f, 1f);
        burstAnim.setDuration(750);
        burstAnim.addUpdateListener(a -> {
            burst = (float) a.getAnimatedValue();
            invalidate();
        });
        burstAnim.start();
    }

    private void animatePress(float to, boolean overshoot) {
        if (pressAnim != null) pressAnim.cancel();
        pressAnim = ValueAnimator.ofFloat(press, to);
        pressAnim.setDuration(overshoot ? 380 : 120);
        if (overshoot) pressAnim.setInterpolator(new OvershootInterpolator(3f));
        pressAnim.addUpdateListener(a -> {
            press = (float) a.getAnimatedValue();
            invalidate();
        });
        pressAnim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animatePress(0.92f, false);
                return true;
            case MotionEvent.ACTION_UP:
                animatePress(1f, true);
                burst();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                animatePress(1f, true);
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float R = Math.min(getWidth(), getHeight()) / 2f * 0.56f * press;

        // حلقات نابضة
        for (int k = 0; k < 3; k++) {
            float f = (pulse + k / 3f) % 1f;
            ring.setColor(Theme.a(th.accent, (int) ((1f - f) * 110)));
            cv.drawCircle(cx, cy, R * (1.05f + 0.55f * f), ring);
        }
        // موجة الضغط
        if (burst < 1f) {
            ring.setStrokeWidth(4f * d * (1f - burst) + 1f);
            ring.setColor(((int) ((1f - burst) * 230) << 24) | 0xFFFFFF);
            cv.drawCircle(cx, cy, R * (1f + 0.9f * burst), ring);
            ring.setStrokeWidth(2f * d);
        }
        // هالة
        glow.setShader(new RadialGradient(cx, cy, R * 1.6f,
                Theme.a(th.accent, 0x66), Theme.a(th.accent, 0x00), Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, R * 1.6f, glow);
        // القرص الذهبي
        disc.setShader(new RadialGradient(cx - R * 0.3f, cy - R * 0.35f, R * 1.3f,
                new int[]{th.accentLight, th.accent, th.accentDark},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, R, disc);
        ring.setColor(0x88FFF6D0);
        cv.drawCircle(cx, cy, R * 0.93f, ring);

        // النص
        text.setTextSize(R * 0.34f);
        cv.drawText("صلِّ على", cx, cy - R * 0.05f, text);
        text.setTextSize(R * 0.40f);
        cv.drawText("النبي", cx, cy + R * 0.38f, text);
        text.setTextSize(R * 0.16f);
        cv.drawText("اضغط للعدّ", cx, cy - R * 0.45f, text);
    }
}
