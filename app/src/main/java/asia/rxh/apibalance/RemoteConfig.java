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
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仓库根目录 app.json 的远程公告。启动时刷新一次，网络失败时继续使用上次缓存，
 * 不阻塞启动，也不把错误弹到用户面前。
 */
public final class RemoteConfig {

    private static final String PREFS = "remote_config";
    private static final String URL =
            "https://raw.githubusercontent.com/RXHrxh520/APIBalance/main/app.json";
    private static volatile Config cached;
    private static final AtomicBoolean loading = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RemoteConfig() {
    }

    public static final class Config {
        public final String notice;
        public final String noticeUrl;

        Config(String notice, String noticeUrl) {
            this.notice = notice == null ? "" : notice;
            this.noticeUrl = noticeUrl == null ? "" : noticeUrl;
        }
    }

    public interface Callback {
        void onConfig(Config config);
    }

    public static void refresh(Context ctx) {
        final Context app = ctx.getApplicationContext();
        if (!loading.compareAndSet(false, true)) return;
        AppExecutors.io().execute(() -> {
            try {
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(URL)
                        .header("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME)
                        .build();
                try (okhttp3.Response response = NetworkClient.http().newCall(request).execute()) {
                    if (response.code() == 200 && response.body() != null) {
                        String text = response.body().string();
                        Config parsed = parse(text);
                        if (parsed != null) {
                            cached = parsed;
                            save(app, parsed);
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                loading.set(false);
            }
        });
    }

    public static void get(Context ctx, Callback callback) {
        Config local = cached;
        if (local == null) local = load(ctx);
        final Config hit = local;
        if (hit != null) {
            if (callback != null) MAIN.post(() -> callback.onConfig(hit));
            return;
        }
        if (callback == null) return;
        AppExecutors.io().execute(() -> {
            Config parsed = cached;
            if (parsed == null) {
                try {
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(URL)
                            .header("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME)
                            .build();
                    try (okhttp3.Response response = NetworkClient.http().newCall(request).execute()) {
                        if (response.code() == 200 && response.body() != null) {
                            parsed = parse(response.body().string());
                            if (parsed != null) cached = parsed;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            final Config result = parsed;
            MAIN.post(() -> callback.onConfig(result));
        });
    }

    /**
     * 公告解析。兼容两种写法：
     * 1）老格式：{"notice": "...", "notice_url": "..."}
     * 2）新格式：{"notices": [ { "id": "...", "enabled": true, "text": "...",
     *              "url": "...", "start": "2026-01-01", "end": "" } ]}
     * 新格式按顺序取第一条「启用且在有效期内的」，全部过期就回退到 notice 字段。
     * 字段含义见 docs/announcement.md。
     */
    private static void save(Context ctx, Config config) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("notice", config.notice)
                .putString("notice_url", config.noticeUrl)
                .apply();
    }

    private static Config load(Context ctx) {
        android.content.SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String notice = sp.getString("notice", "");
        if (notice == null || notice.isEmpty()) return null;
        return new Config(notice, sp.getString("notice_url", ""));
    }

    private static Config parse(String text) {
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            Config fromList = parseList(root);
            if (fromList != null) return fromList;
            String notice = string(root, "notice");
            if (notice.isEmpty()) notice = string(root, "announcement");
            if (notice.isEmpty()) notice = string(root, "text");
            String url = string(root, "notice_url");
            if (url.isEmpty()) url = string(root, "noticeUrl");
            // notice 也可能是对象 {text,url}
            JsonElement noticeEl = root.get("notice");
            if (noticeEl != null && noticeEl.isJsonObject()) {
                JsonObject o = noticeEl.getAsJsonObject();
                if (string(o, "text").length() > 0) notice = string(o, "text");
                if (url.isEmpty()) url = string(o, "url");
            }
            return new Config(notice, url);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Config parseList(JsonObject root) {
        JsonElement el = root.get("notices");
        if (el == null || !el.isJsonArray()) return null;
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
        for (JsonElement item : el.getAsJsonArray()) {
            if (item == null || !item.isJsonObject()) continue;
            JsonObject o = item.getAsJsonObject();
            if (!bool(o, "enabled", true)) continue;
            String start = string(o, "start");
            String end = string(o, "end");
            // 日期都按 yyyy-MM-dd 字符串比较；留空表示不限
            if (!start.isEmpty() && today.compareTo(start) < 0) continue;
            if (!end.isEmpty() && today.compareTo(end) > 0) continue;
            String text = string(o, "text");
            if (text.isEmpty()) continue;
            String url = string(o, "url");
            if (url.isEmpty()) url = string(o, "notice_url");
            // 只允许 https，避免仓库被改后把用户引到 http 钓鱼站
            if (!url.isEmpty() && !url.startsWith("https://")) url = "";
            return new Config(text, url);
        }
        return new Config("", "");
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            JsonElement e = o.get(key);
            return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()
                    ? e.getAsBoolean() : def;
        } catch (Throwable t) {
            return def;
        }
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()
                ? e.getAsString() : "";
    }
}
