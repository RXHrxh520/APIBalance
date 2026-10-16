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
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 余额历史记录：把每次查询结果保存为本地快照，供趋势页展示。
 * 数据格式：JSON 数组 [{ts, provider, currency, total, granted, topped}, ...]
 */
public final class BalanceHistoryStore {

    private static final String PREFS = "ds_balance_history";
    private static final String KEY_DATA = "history";
    private static final int MAX_ITEMS = 200;

    private BalanceHistoryStore() {
    }

    public static void add(Context ctx, int providerId, ProviderConfig.BalanceResult result) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY_DATA, "[]"));
            long now = System.currentTimeMillis();
            // 去重：同一服务商、同一金额且在 30 秒内的多次写入视为同一次查询（余额页+悬浮球等入口重复记录），
            // 只更新时间戳、不追加，避免趋势图/柱状图出现重复数据。
            if (arr.length() > 0) {
                JSONObject last = arr.getJSONObject(arr.length() - 1);
                long lastTs = last.optLong("ts", 0);
                int lastPid = last.optInt("provider", -1);
                double lastTotal = last.optDouble("total", 0);
                // 只要金额没变就不新增数据点，仅刷新时间戳（外加 6 小时兜底，
                // 保证长时间不变的余额在趋势图上仍有一个当日锚点）。
                if (lastPid == providerId
                        && Math.abs(lastTotal - result.totalBalance) < 1e-6
                        && Math.abs(now - lastTs) < 6 * 3600_000L) {
                    last.put("ts", now);
                    sp.edit().putString(KEY_DATA, arr.toString()).apply();
                    return;
                }
            }
            JSONObject item = new JSONObject();
            item.put("ts", now);
            item.put("provider", providerId);
            item.put("currency", result.currency);
            item.put("total", result.totalBalance);
            item.put("granted", result.grantedBalance);
            item.put("topped", result.toppedUpBalance);
            arr.put(item);
            // 只保留最近 MAX_ITEMS 条
            while (arr.length() > MAX_ITEMS) {
                arr.remove(0);
            }
            sp.edit().putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** 返回全部历史（按时间升序）。 */
    public static List<HistoryItem> getAll(Context ctx) {
        List<HistoryItem> list = new ArrayList<>();
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY_DATA, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                HistoryItem it = new HistoryItem();
                it.ts = o.optLong("ts", 0);
                it.providerId = o.optInt("provider", ProviderConfig.PROVIDER_DEEPSEEK);
                it.currency = o.optString("currency", "CNY");
                it.total = o.optDouble("total", 0);
                it.granted = o.optDouble("granted", 0);
                it.topped = o.optDouble("topped", 0);
                list.add(it);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /** 一键清掉重复数据：相邻的「同服务商 + 同金额」只留一条。返回清掉多少条。 */
    public static int dedupe(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY_DATA, "[]"));
            JSONArray out = new JSONArray();
            int removed = 0;
            // 判定为重复的条件（三个都满足）：
            //   同一服务商、余额各项数值完全一致、时间相差不超过 10 分钟。
            // 之前要求时间戳一模一样，而实际重复记录的时间戳总是差几秒，
            // 所以「明明有重复却清不掉」；只按金额合并又会误删正常的持平历史。
            final long WINDOW = 10 * 60 * 1000L;
            java.util.HashMap<String, Long> lastTs = new java.util.HashMap<String, Long>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long ts = o.optLong("ts", 0);
                String key = o.optInt("provider", -1) + "|" + o.optDouble("total", 0)
                        + "|" + o.optDouble("granted", 0) + "|" + o.optDouble("topped", 0);
                Long prev = lastTs.get(key);
                if (prev != null && Math.abs(ts - prev) <= WINDOW) {
                    removed++;
                    continue;
                }
                lastTs.put(key, ts);
                out.put(o);
            }
            sp.edit().putString(KEY_DATA, out.toString()).apply();
            return removed;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DATA).apply();
    }

    public static final class HistoryItem {
        public long ts;
        public int providerId;
        public String currency;
        public double total;
        public double granted;
        public double topped;
    }
}
