package asia.rxh.apibalance;/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.animation.ValueAnimator;

/**
 * 骨架屏（v1.5）：一条斜向高光从左到右反复扫过。
 * 只做视觉占位，不持有任何数据。
 */
public class ShimmerView extends View {

    private final Paint base = new Paint(Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint sweep = new Paint(Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private float offset = -1f;
    private ValueAnimator anim;

    public ShimmerView(Context c) {
        this(c, null);
    }

    public ShimmerView(Context c, AttributeSet a) {
        super(c, a);
        radius = 8f * getResources().getDisplayMetrics().density;
        base.setColor(0xFFE6E6EA);
        sweep.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void start() {
        if (anim != null) return;
        anim = ValueAnimator.ofFloat(-1f, 2f);
        anim.setDuration(1300);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.RESTART);
        anim.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private void stop() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, base);
        float band = w * 0.42f;
        float x = offset * (w + band) - band;
        sweep.setShader(new LinearGradient(x, 0, x + band, h,
                new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, sweep);
    }
}
