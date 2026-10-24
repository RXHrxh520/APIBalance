package asia.rxh.apibalance;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * 独立的安全中心：先扫一遍，再逐条列出结果，每条都写清楚实测到什么、为什么重要。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public class SecurityCenterActivity extends Activity {

    private LinearLayout list;
    private ProgressBar bar;
    private TextView status;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppPrefs.wrapLocale(base));
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_center);
        ThemeUtils.apply(findViewById(android.R.id.content));

        View back = findViewById(R.id.sc_back);
        if (back != null) back.setOnClickListener(v -> finish());
        View rescan = findViewById(R.id.sc_rescan);
        if (rescan != null) rescan.setOnClickListener(v -> scan());

        list = findViewById(R.id.sc_list);
        bar = findViewById(R.id.sc_bar);
        status = findViewById(R.id.sc_status);
        scan();
    }

    private void scan() {
        if (list == null) return;
        list.removeAllViews();
        if (status != null) status.setText(getString(R.string.sec_scanning));
        if (bar != null) {
            bar.setProgress(0);
            bar.animate().alpha(1f).setDuration(150).start();
        }
        final List<String[]> rows;
        try {
            rows = EnvChecker.scanAll(this);
        } catch (Throwable t) {
            if (status != null) status.setText(getString(R.string.sec_scan_failed));
            return;
        }
        // 扫描进度条走一遍，再逐条淡入
        final int total = Math.max(rows.size(), 1);
        for (int i = 0; i < total; i++) {
            final int n = i;
            list.postDelayed(() -> {
                if (bar != null) bar.setProgress((int) ((n + 1) * 100f / total));
                if (n < rows.size()) addRow(rows.get(n), n * 40);
                if (n == total - 1) {
                    if (bar != null) bar.animate().alpha(0f).setDuration(300).start();
                    int ok = 0, warn = 0, unk = 0;
                    for (String[] p : rows) {
                        if ("ok".equals(p[1])) ok++;
                        else if ("warn".equals(p[1])) warn++;
                        else unk++;
                    }
                    if (status != null) {
                        status.setText(getString(R.string.sec_done) + "  ✓ " + ok
                                + "   ✗ " + warn + "   ? " + unk);
                    }
                }
            }, 60L * i);
        }
    }

    private void addRow(String[] row, int delay) {
        String label = row.length > 0 ? row[0] : "";
        String state = row.length > 1 ? row[1] : "unknown";
        String found = row.length > 2 ? row[2] : "";
        String why = row.length > 3 ? row[3] : "";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_card_balance);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);

        TextView mk = new TextView(this);
        mk.setText(EnvChecker.mark(state, this));
        mk.setTextSize(18);
        mk.setTextColor(EnvChecker.colorOf(state));
        mk.setGravity(Gravity.CENTER);
        mk.setWidth(dp(30));
        card.addView(mk);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), 0, 0, 0);
        card.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setTextColor(ThemeUtils.textPrimary(this));
        t1.setTextSize(15);
        t1.setTypeface(null, android.graphics.Typeface.BOLD);
        t1.setText(label + "   " + EnvChecker.stateText(this, state));
        col.addView(t1);

        if (found != null && !found.isEmpty()) {
            TextView t2 = new TextView(this);
            t2.setText(getString(R.string.sec_found) + found);
            t2.setTextColor(EnvChecker.colorOf(state));
            t2.setTextSize(12);
            t2.setPadding(0, dp(3), 0, 0);
            col.addView(t2);
        }
        if (why != null && !why.isEmpty()) {
            TextView t3 = new TextView(this);
            t3.setText(getString(R.string.sec_why) + why);
            t3.setTextColor(ThemeUtils.textSecondary(this));
            t3.setTextSize(11);
            t3.setPadding(0, dp(4), 0, 0);
            t3.setLineSpacing(dp(2), 1f);
            col.addView(t3);
        }

        card.setAlpha(0f);
        card.setTranslationY(dp(14));
        list.addView(card);
        card.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(240).start();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}

