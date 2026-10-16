package asia.rxh.apibalance;/*
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 设置目录（v1.5 从 SettingsFragment 里抽出来的纯数据类）。
 *
 * 拆出来的目的：SettingsFragment 原本 1300+ 行，既管数据又管视图，属于典型上帝类。
 * 现在分类、行归属、Pro 标记、分类图标全部集中在这里，SettingsFragment 只负责绑定视图。
 */
public final class SettingsCatalog {

    /** 大分类标识。顺序即设置首页从上到下的展示顺序。 */
    public static final String[] GROUPS = {
            "general", "appearance", "network", "account", "notify", "privacy", "advanced", "about"};

    /** 分类标题。 */
    public static final int[] GROUP_TITLES = {
            R.string.cat_general, R.string.cat_appearance, R.string.cat_network,
            R.string.cat_account, R.string.cat_notify, R.string.cat_privacy,
            R.string.cat_advanced, R.string.cat_about};

    /** 分类副标题，说明该分类里到底有什么。 */
    public static final int[] GROUP_SUBTITLES = {
            R.string.cat_general_sub, R.string.cat_appearance_sub, R.string.cat_network_sub,
            R.string.cat_account_sub, R.string.cat_notify_sub, R.string.cat_privacy_sub,
            R.string.cat_advanced_sub, R.string.cat_about_sub};

    /** 分类图标：每个分类一个独立图标，避免重复。 */
    public static final int[] GROUP_ICONS = {
            R.drawable.ic_n_tune, R.drawable.ic_n_palette, R.drawable.ic_n_globe,
            R.drawable.ic_accounts, R.drawable.ic_n_bell, R.drawable.ic_n_shield,
            R.drawable.ic_n_build, R.drawable.ic_n_info};

    /** 顶层所有行。 */
    public static final String[] ALL_ROWS = {
            "btn_permission_center", "btn_theme", "btn_lang", "btn_onboarding", "btn_history",
            "btn_custom_bg", "btn_icon", "btn_android_theme", "btn_font_size", "row_auto_dark",
            "btn_currency", "btn_accounts", "btn_change_key", "btn_switch_provider", "btn_token_est",
            "row_alert", "row_auto_refresh", "row_quiet", "row_sticky", "row_float",
            "row_lock", "row_auto_lock", "row_hide_balance", "row_no_screenshot", "row_no_notify",
            "btn_timeout", "btn_proxy", "btn_advanced_net", "btn_update", "btn_deps",
            "btn_about", "btn_clear_data", "btn_debug", "row_pro_mode",
            "btn_lock_delay", "btn_lock_method", "btn_f2a", "btn_legacy_toggle",
            "btn_security_center", "btn_backup", "btn_experimental"};

    private static final Map<String, String[]> GROUP_MAP = new HashMap<String, String[]>();
    private static final Set<String> PRO_IDS = new HashSet<String>();

    static {
        // 通用：语言、字体、货币、超时、自动深色
        GROUP_MAP.put("general", new String[]{"btn_lang", "btn_font_size", "btn_currency",
                "btn_timeout", "row_auto_dark"});
        // 外观：主题中心、背景、图标、系统主题联动
        GROUP_MAP.put("appearance", new String[]{"btn_theme", "btn_custom_bg", "btn_icon",
                "btn_android_theme", "row_hide_balance"});
        // 网络：代理与高级网络
        GROUP_MAP.put("network", new String[]{"btn_proxy", "btn_advanced_net"});
        // 账号：多账号、切换密钥、服务商、Token 估算
        GROUP_MAP.put("account", new String[]{"btn_accounts", "btn_change_key",
                "btn_switch_provider", "btn_token_est"});
        // 通知：自动刷新、余额提醒、静音时段、常驻通知、悬浮球
        GROUP_MAP.put("notify", new String[]{"row_auto_refresh", "row_alert", "row_sticky",
                "row_quiet", "row_float"});
        // 隐私与安全：锁、截屏、通知隐藏、权限中心、安全中心、数据清理
        GROUP_MAP.put("privacy", new String[]{"row_lock", "row_auto_lock", "btn_lock_delay",
                "btn_lock_method", "btn_f2a", "row_no_screenshot", "row_no_notify",
                "btn_permission_center", "btn_security_center", "btn_clear_data"});
        // 进阶：Pro、调试、依赖声明、实验性功能、数据备份
        GROUP_MAP.put("advanced", new String[]{"row_pro_mode", "btn_experimental", "btn_deps",
                "btn_debug", "btn_backup"});
        // 关于：关于本应用、检查更新、历史记录、引导页、新旧版切换
        GROUP_MAP.put("about", new String[]{"btn_about", "btn_update", "btn_history",
                "btn_onboarding", "btn_legacy_toggle"});

        PRO_IDS.add("btn_advanced_net");
        PRO_IDS.add("btn_proxy");
        PRO_IDS.add("btn_debug");
        PRO_IDS.add("btn_deps");
        PRO_IDS.add("btn_android_theme");
        PRO_IDS.add("btn_custom_bg");
        PRO_IDS.add("btn_icon");
        PRO_IDS.add("btn_token_est");
        PRO_IDS.add("row_no_screenshot");
        PRO_IDS.add("row_no_notify");
        PRO_IDS.add("row_auto_dark");
        PRO_IDS.add("row_auto_lock");
        PRO_IDS.add("btn_experimental");
    }

    private SettingsCatalog() {
    }

    public static String[] rowsOf(String group) {
        String[] rows = GROUP_MAP.get(group);
        return rows == null ? ALL_ROWS : rows;
    }

    public static boolean isPro(String id) {
        return PRO_IDS.contains(id);
    }
}
