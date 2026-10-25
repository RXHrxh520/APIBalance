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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 汇率获取工具：从用户提供的 URL 拉取约定的 JSON 格式：
 * {"usd_cny": 7.20, "updated_at": 1735689600}
 * 拉取成功后缓存到 AppPrefs，余额换算使用缓存值（离线可用）。
 */
public final class FxRateFetcher {

    private FxRateFetcher() {
    }

    /** 同步拉取并缓存汇率；成功返回汇率值，失败返回 -1。 */
    public static double fetch(Context ctx) {
        String url = AppPrefs.getRateUrl(ctx);
        if (url == null || url.trim().isEmpty()) return -1;
        url = url.trim();
        final boolean allowHttp = AppPrefs.isRateHttpAllowed(ctx);
        if (!url.startsWith("https://")
                && !(allowHttp && url.startsWith("http://"))) return -1;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME);
            if (conn.getResponseCode() != 200) return -1;
            String proto = conn.getURL().getProtocol();
            if (!"https".equalsIgnoreCase(proto)
                    && !(allowHttp && "http".equalsIgnoreCase(proto))) return -1;
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                if (sb.length() + line.length() > 256 * 1024) return -1;
                sb.append(line);
            }
            rd.close();
            org.json.JSONObject o = new org.json.JSONObject(sb.toString());
            double rate = o.optDouble("usd_cny", 0);
            if (rate <= 0 || rate > 100) return -1;
            // 缓存：带上时间戳
            org.json.JSONObject cache = new org.json.JSONObject();
            cache.put("usd_cny", rate);
            cache.put("updated_at", System.currentTimeMillis() / 1000);
            AppPrefs.setRateJson(ctx, cache.toString());
            return rate;
        } catch (Exception e) {
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
