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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private static final int FILTER_ALL_PROVIDER = -1;
    private static final int FILTER_ALL_TIME = 0;
    private static final int FILTER_7D = 1;
    private static final int FILTER_30D = 2;
    private static final int FILTER_90D = 3;

    private static final int MAX_MONTHS = 6;

    private int filterProvider = FILTER_ALL_PROVIDER;
    private int filterTime = FILTER_ALL_TIME;

    private TextView btnFilterProvider;
    private TextView btnFilterTime;
    private TextView tvStats;
    private TrendChartView chart;
    private TrendBarChartView barChart;
    private TextView tvEmpty;
    private LinearLayout historyContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView btnClear = view.findViewById(R.id.btn_clear);
        chart = view.findViewById(R.id.trend_chart);
        barChart = view.findViewById(R.id.month_bar_chart);
        tvEmpty = view.findViewById(R.id.tv_trend_empty);
        historyContainer = view.findViewById(R.id.history_container);
        btnFilterProvider = view.findViewById(R.id.btn_filter_provider);
        btnFilterTime = view.findViewById(R.id.btn_filter_time);
        tvStats = view.findViewById(R.id.tv_stats);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null && getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
                return;
            }
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.switchTab(0);
        });

        View btnDedupe = view.findViewById(R.id.btn_dedupe);
        if (btnDedupe != null) {
            btnDedupe.setOnClickListener(v -> {
                int n = BalanceHistoryStore.dedupe(getActivity());
                android.widget.Toast.makeText(getActivity(),
                        getString(R.string.hist_dedupe_done) + " " + n,
                        android.widget.Toast.LENGTH_SHORT).show();
                refresh();
            });
        }

        btnClear.setOnClickListener(v -> {
            BalanceHistoryStore.clear(getActivity().getApplicationContext());
            refresh();
            Toast.makeText(getActivity(), getString(R.string.j_150), Toast.LENGTH_SHORT).show();
        });

        TextView btnExport = view.findViewById(R.id.btn_export);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportHistory());
        }

        btnFilterProvider.setOnClickListener(v -> {
            String[] options = new String[ProviderConfig.count() + 1];
            options[0] = getString(R.string.j_151);
            for (int i = 0; i < ProviderConfig.count(); i++) {
                options[i + 1] = ProviderConfig.get(i).name;
            }
            int sel = filterProvider + 1;
            UiDialog.showPicker(getActivity(), getString(R.string.j_152), options, sel, w -> {
                filterProvider = w - 1;
                refresh();
            });
        });

        btnFilterTime.setOnClickListener(v -> {
            String[] options = {getString(R.string.j_153), getString(R.string.j_154), getString(R.string.j_155), getString(R.string.j_156)};
            UiDialog.showPicker(getActivity(), getString(R.string.j_157), options, filterTime, w -> {
                filterTime = w;
                refresh();
            });
        });

        refresh();

        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.decelerate_cubic))
                .start();

        ThemeUtils.apply(view);
        return view;
    }

    private void exportHistory() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        List<BalanceHistoryStore.HistoryItem> all = BalanceHistoryStore.getAll(act);
        if (all.isEmpty()) {
            Toast.makeText(act, getString(R.string.h_046), Toast.LENGTH_SHORT).show();
            return;
        }
        String[] formats = {"CSV", "JSON"};
        UiDialog.showPicker(act, getString(R.string.h_047), formats, -1, w -> {
            StringBuilder sb = new StringBuilder();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            if (w == 0) {
                sb.append(getString(R.string.x_001));
                for (BalanceHistoryStore.HistoryItem it : all) {
                    sb.append(sdf.format(new java.util.Date(it.ts))).append(',')
                      .append(csvEscape(ProviderConfig.get(it.providerId).name)).append(',')
                      .append(csvEscape(it.currency)).append(',')
                      .append(it.total).append(',')
                      .append(it.granted).append(',')
                      .append(it.topped).append('\n');
                }
            } else {
                sb.append("{\"history\":[");
                for (int i = 0; i < all.size(); i++) {
                    BalanceHistoryStore.HistoryItem it = all.get(i);
                    if (i > 0) sb.append(',');
                    sb.append("{\"ts\":").append(it.ts)
                      .append(",\"provider\":").append(it.providerId)
                      .append(",\"currency\":\"").append(jsonEscape(it.currency))
                      .append("\",\"total\":").append(it.total)
                      .append(",\"granted\":").append(it.granted)
                      .append(",\"topped\":").append(it.topped).append('}');
                }
                sb.append("]}");
            }
            // 直接落盘到「下载」，并明确告诉用户存哪了。
            // 原来用 ACTION_SEND 发文本，很多机型选不出目标，也没有任何反馈。
            String name = "APIBalance_history_" + System.currentTimeMillis()
                    + (w == 0 ? ".csv" : ".json");
            String mime = w == 0 ? "text/csv" : "application/json";
            String where = null;
            try {
                where = Exporter.saveText(act, sb.toString(), name, mime);
            } catch (Throwable t) {
                where = null;
            }
            Toast.makeText(act, where == null
                            ? getString(R.string.export_failed)
                            : getString(R.string.export_saved) + "\n" + where,
                    Toast.LENGTH_LONG).show();
        });
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    private double toDisplayTotal(BalanceHistoryStore.HistoryItem it) {
        int mode = AppPrefs.getDisplayCurrency(getActivity());
        double rate = AppPrefs.getUsdCnyRate(getActivity());
        double v = it.total;
        if (mode == 1 && "USD".equals(it.currency)) v *= rate;
        else if (mode == 2 && "CNY".equals(it.currency)) v /= rate;
        return v;
    }

    private void refresh() {
        try {
            List<BalanceHistoryStore.HistoryItem> all =
                    BalanceHistoryStore.getAll(getActivity().getApplicationContext());
            List<BalanceHistoryStore.HistoryItem> items = new ArrayList<>();
            long now = System.currentTimeMillis();
            long limit = 0;
            if (filterTime == FILTER_7D) limit = 7L * 24 * 3600 * 1000;
            else if (filterTime == FILTER_30D) limit = 30L * 24 * 3600 * 1000;
            else if (filterTime == FILTER_90D) limit = 90L * 24 * 3600 * 1000;

            for (BalanceHistoryStore.HistoryItem it : all) {
                if (filterProvider != FILTER_ALL_PROVIDER && it.providerId != filterProvider) continue;
                if (limit > 0 && now - it.ts > limit) continue;
                items.add(it);
            }

            btnFilterProvider.setText(filterProvider == FILTER_ALL_PROVIDER ? getString(R.string.j_151)
                    : ProviderConfig.get(filterProvider).name);
            btnFilterTime.setText(filterTime == FILTER_ALL_TIME ? getString(R.string.j_153)
                    : filterTime == FILTER_7D ? getString(R.string.j_154) : filterTime == FILTER_30D ? getString(R.string.j_155) : getString(R.string.j_156));

            List<Double> totals = new ArrayList<>();
            for (BalanceHistoryStore.HistoryItem it : items) totals.add(toDisplayTotal(it));
            if (totals.size() >= 2) {
                chart.setValues(totals);
                chart.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                chart.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }

            if (barChart != null) {
                java.util.LinkedHashMap<String, List<Double>> byMonth2 = new java.util.LinkedHashMap<>();
                SimpleDateFormat mf = new SimpleDateFormat("yy-MM", Locale.getDefault());
                for (BalanceHistoryStore.HistoryItem it : items) {
                    String mk = mf.format(new Date(it.ts));
                    List<Double> lst = byMonth2.get(mk);
                    if (lst == null) { lst = new ArrayList<>(); byMonth2.put(mk, lst); }
                    lst.add(toDisplayTotal(it));
                }
                while (byMonth2.size() > MAX_MONTHS) {
                    String first = byMonth2.keySet().iterator().next();
                    byMonth2.remove(first);
                }
                if (byMonth2.size() >= 2) {
                    List<Double> monthlyValues = new ArrayList<>();
                    List<String> monthLabels = new ArrayList<>();
                    for (java.util.Map.Entry<String, List<Double>> e : byMonth2.entrySet()) {
                        monthLabels.add(e.getKey());
                        List<Double> vals = e.getValue();
                        monthlyValues.add(vals.get(vals.size() - 1));
                    }
                    barChart.setData(monthlyValues, monthLabels);
                    barChart.setVisibility(View.VISIBLE);
                } else {
                    barChart.setVisibility(View.GONE);
                }
            }

            if (!items.isEmpty()) {
                java.util.LinkedHashMap<String, List<Double>> byMonth = new java.util.LinkedHashMap<>();
                SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                for (BalanceHistoryStore.HistoryItem it : items) {
                    String mk = monthFmt.format(new Date(it.ts));
                    List<Double> lst = byMonth.get(mk);
                    if (lst == null) { lst = new ArrayList<>(); byMonth.put(mk, lst); }
                    lst.add(toDisplayTotal(it));
                }
                while (byMonth.size() > MAX_MONTHS) {
                    String first = byMonth.keySet().iterator().next();
                    byMonth.remove(first);
                }
                StringBuilder stats = new StringBuilder();
                for (java.util.Map.Entry<String, List<Double>> e : byMonth.entrySet()) {
                    List<Double> vals = e.getValue();
                    double last = vals.get(vals.size() - 1);
                    stats.append(e.getKey()).append(" x").append(vals.size())
                         .append(" end ").append(String.format(Locale.getDefault(), "%.2f", last)).append(" | ");
                }
                stats.append("total ").append(items.size()).append(" records / ").append(byMonth.size()).append(" months");
                tvStats.setText(stats.toString());
                tvStats.setVisibility(View.VISIBLE);
            } else {
                tvStats.setVisibility(View.GONE);
            }

            historyContainer.removeAllViews();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            boolean dark = ThemeUtils.isDarkTheme(getActivity());
            for (int i = items.size() - 1; i >= 0; i--) {
                BalanceHistoryStore.HistoryItem it = items.get(i);
                LinearLayout row = new LinearLayout(getActivity());
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(16), dp(12), dp(16), dp(12));
                row.setBackgroundResource(R.drawable.bg_card_balance);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                row.setLayoutParams(lp);

                TextView tvProvider = new TextView(getActivity());
                tvProvider.setText(ProviderConfig.get(it.providerId).name + "  ·  " + sdf.format(new Date(it.ts)));
                tvProvider.setTextColor(ThemePrefs.getColor(getActivity()));
                tvProvider.setTextSize(12);
                tvProvider.setTypeface(null, android.graphics.Typeface.BOLD);
                row.addView(tvProvider);

                TextView tvBalance = new TextView(getActivity());
                tvBalance.setText(String.format(Locale.getDefault(),
                        getString(R.string.j_158),
                        it.total, it.currency, it.granted, it.topped));
                tvBalance.setTextColor(dark ? 0xFFE8E6F0 : 0xFF1C1B1F);
                tvBalance.setTextSize(14);
                tvBalance.setPadding(0, dp(4), 0, 0);
                row.addView(tvBalance);

                historyContainer.addView(row);
            }
        } catch (Exception e) {
            tvEmpty.setText(getString(R.string.j_159) + (e.getMessage() == null ? "" : e.getMessage()));
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
