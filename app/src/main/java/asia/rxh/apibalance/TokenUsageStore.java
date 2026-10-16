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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Token 用量记录：按天累计估算的 token 数。
 * 数据结构：JSON 数组 [{date:"2026-08-28", tokens: 12345}, ...]
 */
public final class TokenUsageStore {

    private static final String PREFS = "ds_token_usage";
    private static final String KEY_DATA = "usage";

    private TokenUsageStore() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 记录一次估算用量（按天累加）。 */
    public static void add(Context c, int tokens) {
        if (tokens <= 0) return;
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            boolean updated = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (today.equals(o.optString("date"))) {
                    o.put("tokens", o.optLong("tokens", 0) + tokens);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                JSONObject n = new JSONObject();
                n.put("date", today);
                n.put("tokens", (long) tokens);
                arr.put(n);
            }
            // 只保留最近 90 天
            while (arr.length() > 90) {
                arr.remove(0);
            }
            sp(c).edit().putString(KEY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** 今日累计 token 数。 */
    public static long today(Context c) {
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                if (today.equals(o.optString("date"))) {
                    return o.optLong("tokens", 0);
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /** 近 N 天累计 token 数（按自然日，含今天）。 */
    public static long lastDays(Context c, int days) {
        try {
            // 对齐到今天 00:00 再往前推 (days-1) 天，避免用当前时刻导致跨天 off-by-one
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long limit = cal.getTimeInMillis() - (long) (days - 1) * 24L * 3600 * 1000;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            long total = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                try {
                    Date d = sdf.parse(o.optString("date"));
                    if (d != null && d.getTime() >= limit) {
                        total += o.optLong("tokens", 0);
                    }
                } catch (Exception ignored) {
                }
            }
            return total;
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static void clear(Context c) {
        sp(c).edit().remove(KEY_DATA).apply();
    }
}
