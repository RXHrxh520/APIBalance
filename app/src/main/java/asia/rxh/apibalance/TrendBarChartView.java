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
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * 月度余额柱状图（Canvas 自绘，零依赖）。
 * 传入月度总额序列，绘制柱状 + 数值标签。
 */
public class TrendBarChartView extends View {

    private List<Double> values;
    private List<String> labels;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TrendBarChartView(Context context) {
        this(context, null);
    }

    public TrendBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(0xFF6750A4);
        textPaint.setColor(0xFF8B949E);
        textPaint.setTextSize(dp(10f));
        gridPaint.setColor(0x22000000);
        gridPaint.setStrokeWidth(1);
    }

    public void setData(List<Double> vals, List<String> lbls) {
        this.values = vals;
        this.labels = lbls;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.isEmpty()) return;
        int w = getWidth();
        int h = getHeight();
        float padB = dp(18), padT = dp(14);
        float chartH = h - padB - padT;

        double max = 0;
        for (double v : values) {
            if (v > max) max = v;
        }
        if (max <= 0) max = 1;

        int n = values.size();
        float slot = w / (float) n;
        float barW = Math.min(slot * 0.6f, dp(40));

        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float bh = (float) (chartH * (values.get(i) / max));
            if (bh < dp(2)) bh = dp(2);
            canvas.drawRoundRect(cx - barW / 2, padT + chartH - bh, cx + barW / 2, padT + chartH, dp(4), dp(4), barPaint);
            if (labels != null && i < labels.size()) {
                canvas.drawText(labels.get(i), cx - textPaint.measureText(labels.get(i)) / 2, h - dp(4), textPaint);
            }
            String val = String.format("%.1f", values.get(i));
            canvas.drawText(val, cx - textPaint.measureText(val) / 2, padT + chartH - bh - dp(4), textPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
