package asia.rxh.apibalance;

/*
 * Copyright (C) 2026 软小寒
 *
 * This file is part of APIBalance.
 *
 * APIBalance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * APIBalance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * 简易余额趋势折线图（无第三方库，Canvas 自绘）。
 * 传入历史 total 序列，绘制折线 + 圆点 + 首尾数值标签。
 */
public class TrendChartView extends View {

    private List<Double> values;
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TrendChartView(Context context) {
        this(context, null);
    }

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int primary = 0xFF6750A4;
        linePaint.setColor(primary);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setColor(primary);
        dotPaint.setStyle(Paint.Style.FILL);

        fillPaint.setColor(0x336750A4);
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFF49454F);
        textPaint.setTextSize(dp(11f));
    }

    public void setValues(List<Double> vals) {
        this.values = vals;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.size() < 2) {
            // 少于 2 点：画一个点
            if (values != null && values.size() == 1) {
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                canvas.drawCircle(cx, cy, dp(4), dotPaint);
                canvas.drawText(String.format("%.2f", values.get(0)), cx - dp(20), cy + dp(4), textPaint);
            }
            return;
        }
        int w = getWidth();
        int h = getHeight();
        float padL = dp(8), padR = dp(8), padT = dp(18), padB = dp(22);
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (max - min < 1e-9) { max = min + 1; }
        float span = (float) (max - min);

        int n = values.size();
        Path line = new Path();
        Path area = new Path();
        for (int i = 0; i < n; i++) {
            float x = padL + chartW * i / (n - 1);
            float y = padT + chartH * (1f - (float) ((values.get(i) - min) / span));
            if (i == 0) {
                line.moveTo(x, y);
                area.moveTo(x, y);
            } else {
                line.lineTo(x, y);
                area.lineTo(x, y);
            }
        }
        area.lineTo(padL + chartW, padT + chartH);
        area.lineTo(padL, padT + chartH);
        area.close();
        canvas.drawPath(area, fillPaint);
        canvas.drawPath(line, linePaint);

        // 圆点
        for (int i = 0; i < n; i++) {
            float x = padL + chartW * i / (n - 1);
            float y = padT + chartH * (1f - (float) ((values.get(i) - min) / span));
            canvas.drawCircle(x, y, dp(3.5f), dotPaint);
        }

        // 首尾值
        canvas.drawText(String.format("%.2f", values.get(0)), padL, h - dp(6), textPaint);
        String last = String.format("%.2f", values.get(n - 1));
        float lw = textPaint.measureText(last);
        canvas.drawText(last, w - padR - lw, h - dp(6), textPaint);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
