package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 版本更新说明：从 GitHub Release API 读取最新发布说明，每个版本只展示一次。
 * 网络失败时每 6 小时重试，最多三次；失败不弹错误、不影响启动。
 */
public final class UpdateNotice {

    private static final String API =
            "https://api.github.com/repos/RXHrxh520/APIBalance/releases/latest";
    private static final String PREFS = "update_notice";
    private static final long RETRY_MS = 6L * 60 * 60 * 1000;
    private static final int MAX_ATTEMPTS = 3;

    private UpdateNotice() {
    }

    public static void check(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        final Context app = activity.getApplicationContext();
        final SharedPreferences sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final String version = BuildConfig.VERSION_NAME;
        if (version.equals(sp.getString("shown_version", ""))) return;
        long now = System.currentTimeMillis();
        if (now < sp.getLong("next_try", 0)) return;
        int attempts = sp.getInt("attempts", 0);
        if (attempts >= MAX_ATTEMPTS) return;

        AppExecutors.io().execute(() -> {
            String body = null;
            String tag = null;
            String url = null;
            try {
                // 与 UpdateChecker 共用 GhCache，同一天只打一次 GitHub，避免双请求被限流
                String text = GhCache.get(activity, API);
                JSONObject obj = new JSONObject(text);
                tag = obj.optString("tag_name", "");
                body = obj.optString("body", "");
                url = obj.optString("html_url", "");
            } catch (Throwable ignored) {
            }
            final String noticeBody = body;
            final String noticeTag = tag;
            final String noticeUrl = url;
            if (noticeBody == null) {
                long next = System.currentTimeMillis() + RETRY_MS;
                sp.edit().putInt("attempts", attempts + 1)
                        .putLong("next_try", next).apply();
                return;
            }
            activity.runOnUiThread(() -> {
                if (activity.isFinishing()) return;
                if (noticeBody.trim().isEmpty()) {
                    sp.edit().putString("shown_version", version)
                            .putInt("attempts", 0).remove("next_try").apply();
                    return;
                }
                String title = activity.getString(R.string.update_notice_title_fmt,
                        noticeTag == null || noticeTag.isEmpty() ? version : noticeTag);
                String message = noticeBody.trim();
                UiDialog.showConfirm(activity, title, message,
                        activity.getString(R.string.update_notice_ok), null,
                        () -> sp.edit().putString("shown_version", version)
                                .putInt("attempts", 0).remove("next_try").apply(),
                        null);
            });
        });
    }
}
