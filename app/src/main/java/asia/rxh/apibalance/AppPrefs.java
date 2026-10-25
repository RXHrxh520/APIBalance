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

/**
 * 统一设置项管理：超时、重试、主题、隐私开关、多币种、汇率等。
 * 避免设置项散落在各个类里。
 */
public final class AppPrefs {

    private static final String PREFS = "ds_settings";

    private static final String KEY_TIMEOUT = "timeout_sec";
    private static final String KEY_RETRY = "retry_count";
    private static final String KEY_CURRENCY = "display_currency"; // 0=自动 1=CNY 2=USD
    private static final String KEY_PROXY = "proxy_enabled";
    private static final String KEY_PROXY_HOST = "proxy_host";
    private static final String KEY_PROXY_PORT = "proxy_port";
    private static final String KEY_NO_SCREENSHOT = "priv_noscreenshot";
    private static final String KEY_NO_NOTIFY = "priv_nonotify";
    private static final String KEY_HIDE_RECENTS = "priv_hiderecents";
    private static final String KEY_ALERT_ENABLED = "alert_enabled";
    private static final String KEY_ALERT_THRESHOLD = "alert_threshold";
    private static final String KEY_AUTO_REFRESH = "auto_refresh";
    private static final String KEY_QUIET_START = "quiet_start";
    private static final String KEY_QUIET_END = "quiet_end";
    private static final String KEY_QUIET_ENABLED = "quiet_enabled";
    private static final String KEY_STICKY_NOTIFY = "sticky_notify";
    private static final String KEY_USER_AGENT = "user_agent";
    private static final String KEY_REQ_LOG = "req_log";
    private static final String KEY_FONT_SIZE = "font_size"; // 0=小 1=中 2=大
    private static final String KEY_BG_PATH = "bg_path";
    private static final String KEY_FLOAT = "float_win";
    private static final String KEY_REFRESH_INTERVAL = "refresh_interval_sec";

    private AppPrefs() {
    }

    /** 按用户选择的语言包一层 Configuration，给各 Activity 的 attachBaseContext 用。 */
    public static Context wrapLocale(Context base) {
        try {
            int lang = getLanguage(base);
            if (lang < 1 || lang > 4) return base;
            android.content.res.Configuration cfg =
                    new android.content.res.Configuration(base.getResources().getConfiguration());
            if (lang == 2) {
                cfg.setLocale(new java.util.Locale("en"));
            } else if (lang == 3) {
                cfg.setLocale(java.util.Locale.TRADITIONAL_CHINESE);
            } else if (lang == 4) {
                cfg.setLocale(new java.util.Locale("eo"));
            } else {
                cfg.setLocale(new java.util.Locale("zh"));
            }
            return base.createConfigurationContext(cfg);
        } catch (Throwable t) {
            return base;
        }
    }

    static SharedPreferences sp(Context c) {
        // 直接使用传入 Context（attachBaseContext 阶段 applicationContext 可能不可靠）
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- 超时/重试 ----
    public static int getTimeoutSec(Context c) {
        return sp(c).getInt(KEY_TIMEOUT, 15);
    }

    public static void setTimeoutSec(Context c, int sec) {
        sp(c).edit().putInt(KEY_TIMEOUT, sec).apply();
    }

    public static int getRetryCount(Context c) {
        return sp(c).getInt(KEY_RETRY, 0);
    }

    public static void setRetryCount(Context c, int n) {
        sp(c).edit().putInt(KEY_RETRY, n).apply();
    }

    // ---- 多币种显示 ----
    public static int getDisplayCurrency(Context c) {
        return sp(c).getInt(KEY_CURRENCY, 0);
    }

    public static void setDisplayCurrency(Context c, int mode) {
        sp(c).edit().putInt(KEY_CURRENCY, mode).apply();
    }

    // ---- 代理 ----
    public static boolean isProxyEnabled(Context c) {
        return sp(c).getBoolean(KEY_PROXY, false);
    }

    public static void setProxyEnabled(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_PROXY, b).apply();
    }

    public static String getProxyHost(Context c) {
        return sp(c).getString(KEY_PROXY_HOST, "127.0.0.1");
    }

    public static void setProxyHost(Context c, String h) {
        sp(c).edit().putString(KEY_PROXY_HOST, h).apply();
    }

    public static int getProxyPort(Context c) {
        return sp(c).getInt(KEY_PROXY_PORT, 7890);
    }

    public static void setProxyPort(Context c, int p) {
        sp(c).edit().putInt(KEY_PROXY_PORT, p).apply();
    }

    // ---- 隐私开关 ----
    public static boolean noScreenshot(Context c) {
        return sp(c).getBoolean(KEY_NO_SCREENSHOT, false);
    }

    public static void setNoScreenshot(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_NO_SCREENSHOT, b).apply();
    }

    public static boolean noNotify(Context c) {
        return sp(c).getBoolean(KEY_NO_NOTIFY, false);
    }

    public static void setNoNotify(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_NO_NOTIFY, b).apply();
    }

    public static boolean hideRecents(Context c) {
        return sp(c).getBoolean(KEY_HIDE_RECENTS, false);
    }

    public static void setHideRecents(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_HIDE_RECENTS, b).apply();
    }

    // ---- 余额提醒 ----
    public static boolean isAlertEnabled(Context c) {
        return sp(c).getBoolean(KEY_ALERT_ENABLED, false);
    }

    public static void setAlertEnabled(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_ALERT_ENABLED, b).apply();
    }

    public static double getAlertThreshold(Context c) {
        return sp(c).getFloat(KEY_ALERT_THRESHOLD, 10f);
    }

    public static void setAlertThreshold(Context c, double v) {
        sp(c).edit().putFloat(KEY_ALERT_THRESHOLD, (float) v).apply();
    }

    // ---- 定时刷新 ----
    public static boolean isAutoRefresh(Context c) {
        return sp(c).getBoolean(KEY_AUTO_REFRESH, false);
    }

    public static void setAutoRefresh(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_AUTO_REFRESH, b).apply();
    }

    // ---- 静音时段 ----
    public static boolean isQuietEnabled(Context c) {
        return sp(c).getBoolean(KEY_QUIET_ENABLED, false);
    }

    public static void setQuietEnabled(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_QUIET_ENABLED, b).apply();
    }

    public static int getQuietStart(Context c) {
        return sp(c).getInt(KEY_QUIET_START, 23); // 23:00
    }

    public static void setQuietStart(Context c, int h) {
        sp(c).edit().putInt(KEY_QUIET_START, h).apply();
    }

    public static int getQuietEnd(Context c) {
        return sp(c).getInt(KEY_QUIET_END, 7); // 07:00
    }

    public static void setQuietEnd(Context c, int h) {
        sp(c).edit().putInt(KEY_QUIET_END, h).apply();
    }

    /** 当前是否处于静音时段。 */
    public static boolean inQuietHours(Context c) {
        if (!isQuietEnabled(c)) return false;
        int now = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int start = getQuietStart(c);
        int end = getQuietEnd(c);
        if (start <= end) {
            return now >= start && now < end;
        } else {
            return now >= start || now < end; // 跨午夜
        }
    }

    // ---- 常驻通知 ----
    public static boolean isStickyNotify(Context c) {
        return sp(c).getBoolean(KEY_STICKY_NOTIFY, false);
    }

    public static void setStickyNotify(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_STICKY_NOTIFY, b).apply();
    }

    // ---- UA / 日志 ----
    // TLS 证书校验恒开启（安全要求），不再提供关闭开关

    /** 默认 UA：应用名/版本号，全部动态取，不写死任何版本。 */
    public static String defaultUserAgent(Context c) {
        String label = "APIBalance";
        try {
            android.content.pm.ApplicationInfo ai = c.getApplicationInfo();
            label = String.valueOf(ai.loadLabel(c.getPackageManager()));
        } catch (Throwable ignored) {
        }
        return label + "/" + BuildConfig.VERSION_NAME;
    }

    /** 用户自定义的趣味卡片文案；为空则用内置文案。 */
    public static String getCustomQuip(Context c) {
        return sp(c).getString("custom_quip", "");
    }

    public static void setCustomQuip(Context c, String q) {
        sp(c).edit().putString("custom_quip", q == null ? "" : q).apply();
    }

    /** 代理鉴权（部分企业代理需要账号密码）。 */
    public static String getProxyUser(Context c) {
        return sp(c).getString("proxy_user", "");
    }

    public static void setProxyUser(Context c, String u) {
        sp(c).edit().putString("proxy_user", u == null ? "" : u).apply();
    }

    public static String getProxyPass(Context c) {
        return sp(c).getString("proxy_pass", "");
    }

    public static void setProxyPass(Context c, String pw) {
        sp(c).edit().putString("proxy_pass", pw == null ? "" : pw).apply();
    }

    /** 用户昵称（仅本机保存）。未设置时返回空串，界面显示 NULL。 */
    /** 是否使用旧版设置界面（「我的」页可一键切回旧版）。 */
    /** 清空本模块记录的全部设置（恢复出厂用）。 */
    public static void clearAll(Context c) {
        sp(c).edit().clear().apply();
    }

    /** 引导页选了专业模式，进主界面后补答 5 道安全题。 */
    public static boolean isPendingProQuiz(Context c) {
        return sp(c).getBoolean("pending_pro_quiz", false);
    }

    public static void setPendingProQuiz(Context c, boolean pending) {
        sp(c).edit().putBoolean("pending_pro_quiz", pending).apply();
    }

    /** 是否已经提示过「长按可自定义峰谷文案」。 */
    public static boolean isQuipHintShown(Context c) {
        return sp(c).getBoolean("quip_hint_shown", false);
    }

    public static void setQuipHintShown(Context c, boolean shown) {
        sp(c).edit().putBoolean("quip_hint_shown", shown).apply();
    }

    public static boolean isLegacyUi(Context c) {
        return sp(c).getBoolean("legacy_ui", true);
    }

    public static void setLegacyUi(Context c, boolean legacy) {
        sp(c).edit().putBoolean("legacy_ui", legacy).apply();
    }

    public static String getNickname(Context c) {
        return sp(c).getString("nickname", "");
    }

    public static void setNickname(Context c, String n) {
        sp(c).edit().putString("nickname", n == null ? "" : n).apply();
    }

    public static String getUserAgent(Context c) {
        String v = sp(c).getString(KEY_USER_AGENT, null);
        // 旧版本存下来的 "APIBalance/1.x.x" 视为未自定义：跟随当前版本号重新生成
        if (v == null || v.trim().isEmpty() || v.matches("APIBalance/[0-9]+(\\.[0-9]+)*")) {
            return defaultUserAgent(c);
        }
        return v;
    }

    public static void setUserAgent(Context c, String ua) {
        sp(c).edit().putString(KEY_USER_AGENT, ua).apply();
    }

    public static boolean isReqLog(Context c) {
        return sp(c).getBoolean(KEY_REQ_LOG, false);
    }

    public static void setReqLog(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_REQ_LOG, b).apply();
    }

    // ---- 字体大小 ----
    public static int getFontSize(Context c) {
        return sp(c).getInt(KEY_FONT_SIZE, 1);
    }

    public static void setFontSize(Context c, int v) {
        sp(c).edit().putInt(KEY_FONT_SIZE, v).apply();
    }

    /** 字体缩放系数。 */
    public static float getFontScale(Context c) {
        int v = getFontSize(c);
        return v == 0 ? 0.9f : v == 2 ? 1.2f : 1.0f;
    }

    // ---- 自定义背景 ----
    public static String getBgPath(Context c) {
        return sp(c).getString(KEY_BG_PATH, null);
    }

    public static void setBgPath(Context c, String p) {
        sp(c).edit().putString(KEY_BG_PATH, p).apply();
    }

    // ---- 悬浮窗 ----
    public static boolean isFloatWin(Context c) {
        return sp(c).getBoolean(KEY_FLOAT, false);
    }

    public static void setFloatWin(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_FLOAT, b).apply();
    }

    // ---- 刷新间隔（秒）----
    public static int getRefreshIntervalSec(Context c) {
        return sp(c).getInt(KEY_REFRESH_INTERVAL, 3600); // 默认 1 小时
    }

    public static void setRefreshIntervalSec(Context c, int sec) {
        sp(c).edit().putInt(KEY_REFRESH_INTERVAL, sec).apply();
    }

    // ---- 语言：0=跟随系统 1=中文 2=English ----
    private static final String KEY_LANG = "language";
    private static final String KEY_LAST_UPDATE_CHECK = "last_update_check";

    public static int getLanguage(Context c) {
        return sp(c).getInt(KEY_LANG, 0);
    }

    public static void setLanguage(Context c, int lang) {
        sp(c).edit().putInt(KEY_LANG, lang).apply();
    }

    public static long getLastUpdateCheck(Context c) {
        return sp(c).getLong(KEY_LAST_UPDATE_CHECK, 0);
    }

    public static void setLastUpdateCheck(Context c, long t) {
        sp(c).edit().putLong(KEY_LAST_UPDATE_CHECK, t).apply();
    }

    // ---- 自定义汇率（URL + 约定 JSON，用户提供地址）----
    private static final String KEY_RATE_JSON = "rate_json";
    private static final String KEY_RATE_URL = "rate_url";
    private static final String KEY_RATE_HTTP = "rate_http";

    /** 用户自定义汇率 JSON（从用户提供的 URL 拉取缓存），如 {"usd_cny": 7.20}。 */
    public static String getRateJson(Context c) {
        return sp(c).getString(KEY_RATE_JSON, null);
    }

    public static void setRateJson(Context c, String json) {
        sp(c).edit().putString(KEY_RATE_JSON, json).apply();
    }

    /** 用户提供的汇率 JSON 地址（约定的 JSON 格式：{"usd_cny": 7.20}）。 */
    public static String getRateUrl(Context c) {
        return sp(c).getString(KEY_RATE_URL, null);
    }

    public static void setRateUrl(Context c, String url) {
        sp(c).edit().putString(KEY_RATE_URL, url).apply();
    }

    /** 调试开关：只允许汇率请求使用 HTTP，默认关闭。 */
    public static boolean isRateHttpAllowed(Context c) {
        return sp(c).getBoolean(KEY_RATE_HTTP, false);
    }

    public static void setRateHttpAllowed(Context c, boolean allowed) {
        sp(c).edit().putBoolean(KEY_RATE_HTTP, allowed).apply();
    }

    /** 解析用户汇率（USD→CNY）。JSON 无效或缺失时返回默认 7.2。 */
    public static double getUsdCnyRate(Context c) {
        String json = getRateJson(c);
        if (json != null) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(json);
                double rate = o.optDouble("usd_cny", 7.2);
                if (rate > 0.1 && rate < 100) return rate;
            } catch (Exception ignored) {
            }
        }
        return 7.2;
    }

    // ---- 定时自动深色 ----
    private static final String KEY_AUTO_DARK = "auto_dark";
    private static final String KEY_AUTO_DARK_START = "auto_dark_start";
    private static final String KEY_AUTO_DARK_END = "auto_dark_end";

    public static boolean isAutoDark(Context c) {
        return sp(c).getBoolean(KEY_AUTO_DARK, false);
    }

    public static void setAutoDark(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_AUTO_DARK, b).apply();
    }

    public static int getAutoDarkStart(Context c) {
        return sp(c).getInt(KEY_AUTO_DARK_START, 22);
    }

    public static int getAutoDarkEnd(Context c) {
        return sp(c).getInt(KEY_AUTO_DARK_END, 7);
    }

    public static void setAutoDarkStart(Context c, int h) {
        sp(c).edit().putInt(KEY_AUTO_DARK_START, h).apply();
    }

    public static void setAutoDarkEnd(Context c, int h) {
        sp(c).edit().putInt(KEY_AUTO_DARK_END, h).apply();
    }

    /** 当前是否处于定时深色时段。 */
    public static boolean inAutoDarkHours(Context c) {
        if (!isAutoDark(c)) return false;
        int now = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int start = getAutoDarkStart(c);
        int end = getAutoDarkEnd(c);
        if (start <= end) {
            return now >= start && now < end;
        }
        return now >= start || now < end; // 跨午夜
    }

    // ---- 定时自动锁（0=关闭 1/5/15 分钟）----
    private static final String KEY_AUTO_LOCK_MIN = "auto_lock_minutes";

    public static int getAutoLockMinutes(Context c) {
        return sp(c).getInt(KEY_AUTO_LOCK_MIN, 0);
    }

    public static void setAutoLockMinutes(Context c, int minutes) {
        sp(c).edit().putInt(KEY_AUTO_LOCK_MIN, minutes).apply();
    }

    // ---- 首次引导 / 专业模式 ----
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_PRO_MODE = "pro_mode";

    public static boolean isOnboardingDone(Context c) {
        return sp(c).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_ONBOARDING_DONE, b).apply();
    }

    public static boolean isProMode(Context c) {
        return sp(c).getBoolean(KEY_PRO_MODE, true); // 默认专业模式开（保持现状）
    }

    public static void setProMode(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_PRO_MODE, b).apply();
    }

    // ---- 自定义供应商配置 ----
    private static final String KEY_CUSTOM_NAME = "custom_provider_name";
    private static final String KEY_CUSTOM_URL = "custom_provider_url";
    private static final String KEY_CUSTOM_AUTH = "custom_provider_auth"; // 0=Bearer 1=x-api-key
    private static final String KEY_CUSTOM_CURRENCY = "custom_provider_currency";
    private static final String KEY_CUSTOM_PATH = "custom_provider_path"; // 余额字段 JSON 路径

    public static String getCustomProviderName(Context c) {
        return sp(c).getString(KEY_CUSTOM_NAME, "");
    }

    public static void setCustomProviderName(Context c, String v) {
        sp(c).edit().putString(KEY_CUSTOM_NAME, v).apply();
    }

    public static String getCustomProviderUrl(Context c) {
        return sp(c).getString(KEY_CUSTOM_URL, "");
    }

    public static void setCustomProviderUrl(Context c, String v) {
        sp(c).edit().putString(KEY_CUSTOM_URL, v).apply();
    }

    public static int getCustomProviderAuth(Context c) {
        return sp(c).getInt(KEY_CUSTOM_AUTH, ProviderConfig.AUTH_BEARER);
    }

    public static void setCustomProviderAuth(Context c, int v) {
        sp(c).edit().putInt(KEY_CUSTOM_AUTH, v).apply();
    }

    public static String getCustomProviderCurrency(Context c) {
        return sp(c).getString(KEY_CUSTOM_CURRENCY, "CNY");
    }

    public static void setCustomProviderCurrency(Context c, String v) {
        sp(c).edit().putString(KEY_CUSTOM_CURRENCY, v).apply();
    }

    public static String getCustomProviderPath(Context c) {
        return sp(c).getString(KEY_CUSTOM_PATH, "");
    }

    public static void setCustomProviderPath(Context c, String v) {
        sp(c).edit().putString(KEY_CUSTOM_PATH, v).apply();
    }

    /** 是否已配置自定义供应商（名称与 URL 均非空）。 */
    public static boolean hasCustomProvider(Context c) {
        return !getCustomProviderName(c).trim().isEmpty() && !getCustomProviderUrl(c).trim().isEmpty();
    }

    /** 开发人员页的图片（SAF 授权后的 URI，不需要任何权限）。 */
    public static String getDevImage(Context c, String which) {
        return sp(c).getString("dev_img_" + which, "");
    }

    public static void setDevImage(Context c, String which, String uri) {
        // apply()：避免在 onActivityResult/主线程做同步磁盘写入（原为 commit）
        sp(c).edit().putString("dev_img_" + which, uri == null ? "" : uri).apply();
    }

    /** 我的页自定义头像（SAF 授权后的 URI）。 */
    public static String getAvatarUri(Context c) {
        return sp(c).getString("avatar_uri", "");
    }

    public static void setAvatarUri(Context c, String uri) {
        sp(c).edit().putString("avatar_uri", uri == null ? "" : uri).apply();
    }

    /** 上次运行的版本号，用于识别强制降级。 */
    public static int getLastVersionCode(Context c) {
        return sp(c).getInt("last_version_code", 0);
    }

    public static void setLastVersionCode(Context c, int v) {
        sp(c).edit().putInt("last_version_code", v).apply();
    }

    // ===== 分享文案模板 =====
    public static int getShareTpl(Context c) {
        return sp(c).getInt("share_tpl", 0);
    }

    public static void setShareTpl(Context c, int v) {
        sp(c).edit().putInt("share_tpl", v).apply();
    }

    public static String getShareTplCustom(Context c) {
        return sp(c).getString("share_tpl_custom", "");
    }

    public static void setShareTplCustom(Context c, String v) {
        sp(c).edit().putString("share_tpl_custom", v == null ? "" : v).apply();
    }

    // ===== 隐藏主题 =====
    public static boolean isHorror(Context c) {
        return sp(c).getBoolean("horror", false);
    }

    public static void setHorror(Context c, boolean v) {
        sp(c).edit().putBoolean("horror", v).apply();
    }

    public static boolean isBrokenTapped(Context c) {
        return sp(c).getBoolean("broken_tap", false);
    }

    public static void setBrokenTapped(Context c, boolean v) {
        sp(c).edit().putBoolean("broken_tap", v).apply();
    }

    // ===== 隐藏粉色主题 =====
    public static boolean isPink(Context c) {
        return sp(c).getBoolean("pink", false);
    }

    public static void setPink(Context c, boolean v) {
        sp(c).edit().putBoolean("pink", v).apply();
    }

    // ===== 彩蛋聊天记录（换页/重开都还在）=====
    public static String getEggLog(Context c) {
        return sp(c).getString("egg_log", "");
    }

    public static void appendEggLog(Context c, boolean mine, String text) {
        String old = getEggLog(c);
        String line = (mine ? "1" : "0") + "\u0001" + (text == null ? "" : text.replace("\n", " "));
        if (old.length() > 20000) old = "";
        sp(c).edit().putString("egg_log", old.isEmpty() ? line : old + "\u0002" + line).apply();
    }

    /** 整体改写记录（用于清理重复）。 */
    public static void setEggLog(Context c, String v) {
        sp(c).edit().putString("egg_log", v == null ? "" : v).apply();
    }

    /** 去掉连续重复的聊天记录，返回清掉了几条。 */
    public static int dedupeEggLog(Context c) {
        String log = getEggLog(c);
        if (log == null || log.isEmpty()) return 0;
        String[] parts = log.split("");
        StringBuilder sb = new StringBuilder();
        String prev = null;
        int removed = 0;
        for (String line : parts) {
            if (line.trim().isEmpty()) continue;
            if (line.equals(prev)) {
                removed++;
                continue;
            }
            if (sb.length() > 0) sb.append('');
            sb.append(line);
            prev = line;
        }
        if (removed > 0) setEggLog(c, sb.toString());
        return removed;
    }

    public static void clearEggLog(Context c) {
        sp(c).edit().putString("egg_log", "").apply();
    }

    /** 实验性功能：MD3 组件开关（v1.5）。 */
    public static boolean isExperimentalMd3(Context c) {
        return sp(c).getBoolean("exp_md3", false);
    }

    public static void setExperimentalMd3(Context c, boolean on) {
        sp(c).edit().putBoolean("exp_md3", on).apply();
    }

    /** 分享卡片配色：0 深蓝 / 1 粉色 / 2 极简白 / 3 跟随主题强调色（v1.5）。 */
    public static int getShareCardStyle(Context c) {
        return sp(c).getInt("share_card_style", 0);
    }

    public static void setShareCardStyle(Context c, int v) {
        sp(c).edit().putInt("share_card_style", v).apply();
    }
}


