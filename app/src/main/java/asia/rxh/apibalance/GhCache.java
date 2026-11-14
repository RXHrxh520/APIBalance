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

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * GitHub 公开接口的按天缓存。
 *
 * 开发者页、贡献页的数据一天之内不会变几次，每次进页面都重新拉一遍既慢又浪费配额
 * （未登录时 GitHub 每小时只给 60 次）。这里把响应体按 URL 存下来，同一天直接复用，
 * 跨过零点（换日期）才重新请求。
 */
public final class GhCache {

    private static final String PREFS = "gh_api_cache";
    private static final String KEY_BODY = "body_";
    private static final String KEY_DATE = "date_";

    private GhCache() {
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private static String hash(String url) {
        return Integer.toHexString(url.hashCode());
    }

    /** 取响应体：当天有缓存就用缓存，否则联网拉一次并存入。失败时抛出异常，由调用方兜底。 */
    public static String get(Context ctx, String url) throws Exception {
        if (ctx == null) throw new java.io.IOException("no context");
        Context app = ctx.getApplicationContext();
        SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String h = hash(url);
        String body = sp.getString(KEY_BODY + h, null);
        String date = sp.getString(KEY_DATE + h, null);
        if (body != null && body.length() > 1 && today().equals(date)) {
            return body;
        }
        String fresh = fetch(url);
        sp.edit().putString(KEY_BODY + h, fresh).putString(KEY_DATE + h, today()).apply();
        return fresh;
    }

    /** 强制联网刷新（下拉重试用），成功后覆盖当天缓存。 */
    public static String refresh(Context ctx, String url) throws Exception {
        if (ctx == null) throw new java.io.IOException("no context");
        Context app = ctx.getApplicationContext();
        String fresh = fetch(url);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_BODY + hash(url), fresh)
                .putString(KEY_DATE + hash(url), today())
                .apply();
        return fresh;
    }

    private static String fetch(String url) throws Exception {
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME)
                .build();
        try (okhttp3.Response resp = NetworkClient.http().newCall(req).execute()) {
            if (resp.code() != 200 || resp.body() == null) {
                throw new java.io.IOException("HTTP " + resp.code());
            }
            return resp.body().string();
        }
    }

    /** 供调试页展示缓存条目数量。 */
    public static int size(Context ctx) {
        if (ctx == null) return 0;
        SharedPreferences sp = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int n = 0;
        for (String k : sp.getAll().keySet()) if (k.startsWith(KEY_BODY)) n++;
        return n;
    }

    /** 清空缓存（调用可在设置里「清理缓存」时使用）。 */
    public static void clear(Context ctx) {
        if (ctx == null) return;
        ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
