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
 * 安卓主题设置：四套「当年真实系统」的完整调色板，色值取自 AOSP framework-res
 * （themes_holo.xml / themes_material.xml / colors_holo.xml / colors_material.xml，
 *  Android 12+ 的 Material You 基线色取自 system_background / surface 系列）。
 *
 *  - 0 = Android 4.4 KitKat（Holo 深黑 + 全息蓝）
 *  - 1 = Android 5.1.1 Lollipop（Material 深灰 + Teal）
 *  - 2 = Android 8.0 Oreo（Material 浅灰白 + Blue Grey / Teal）
 *  - 3 = Android 12 Material You（近白 + Material You 紫，默认）
 */
public final class AndroidThemeManager {

    public static final int THEME_KITKAT = 0;
    public static final int THEME_LOLLIPOP = 1;
    public static final int THEME_OREO = 2;
    public static final int THEME_MATERIAL_YOU = 3;
    /** 隐藏主题：崩坏（崩一次才能解锁）。 */
    public static final int THEME_HORROR = 4;

    private static final String PREFS = "ds_android_theme";
    private static final String KEY = "theme";

    private AndroidThemeManager() {
    }

    public static int get(Context c) {
        return clamp(sp(c).getInt(KEY, THEME_MATERIAL_YOU));
    }

    public static void set(Context c, int t) {
        sp(c).edit().putInt(KEY, clamp(t)).apply();
    }

    private static int clamp(int t) {
        if (t < THEME_KITKAT) return THEME_KITKAT;
        if (t > THEME_HORROR) return THEME_HORROR;
        return t;
    }

    /** 主题显示名。 */
    public static String[] names(android.content.Context c) {
        return new String[]{
                c.getString(R.string.g_031),
                c.getString(R.string.g_032),
                c.getString(R.string.g_033),
                c.getString(R.string.g_034)
        };
    }

    /** 主题名列表：崩坏主题解锁后才多出第 5 项。 */
    public static String[] namesUnlocked(android.content.Context c) {
        String[] base = names(c);
        if (!AppPrefs.isHorror(c)) return base;
        String[] more = new String[base.length + 1];
        System.arraycopy(base, 0, more, 0, base.length);
        more[base.length] = c.getString(R.string.g_035);
        return more;
    }

    /** 对应系统内建主题资源 id（用于 Activity.setTheme 换内建控件样式）。 */
    public static int themeRes(int t) {
        switch (clamp(t)) {
            case THEME_KITKAT: return R.style.RxHTheme_KitKat;
            case THEME_LOLLIPOP: return R.style.RxHTheme_Lollipop;
            case THEME_OREO: return R.style.RxHTheme_Oreo;
            case THEME_HORROR: return R.style.RxHTheme_Oreo;
            default: return R.style.RxHTheme_MaterialYou;
        }
    }

    /** 一套主题的完整颜色定义（均为 AOSP 真实色值）。 */
    public static final class Palette {
        public final int accent;        // 强调色
        public final int statusBar;     // 状态栏
        public final int navBar;        // 导航栏
        public final int windowBg;      // 窗口背景
        public final int cardBg;        // 卡片背景
        public final float cardCorner;  // 卡片圆角(dp)
        public final int textPrimary;   // 主文字
        public final int textSecondary; // 副文字
        public final int inputBg;       // 输入框背景
        public final int divider;       // 分割线
        public final boolean dark;      // 是否深色风格
        public final int sectionBg;     // 顶部标题条背景

        Palette(int a, int s, int n, int w, int c, float cr, int tp, int ts,
                int ib, int dv, boolean dk, int sb) {
            accent = a; statusBar = s; navBar = n; windowBg = w; cardBg = c;
            cardCorner = cr; textPrimary = tp; textSecondary = ts; inputBg = ib;
            divider = dv; dark = dk; sectionBg = sb;
        }
    }

    public static Palette palette(int t) {
        switch (clamp(t)) {
            case THEME_KITKAT:
                // Holo 深色：黑底、全息蓝、1.5dp 近似方角
                return new Palette(
                        0xFF33B5E5, 0xFF000000, 0xFF000000,
                        0xFF000000, 0xFF1C1C1C, 1.5f,
                        0xFFFFFFFF, 0xFFB3B3B3,
                        0xFF262626, 0x33FFFFFF, true, 0xFF1A1A1A);
            case THEME_LOLLIPOP:
                // Material 深色：深灰 850/800、Teal 强调、2dp 圆角
                return new Palette(
                        0xFF008577, 0xFF21272B, 0xFF000000,
                        0xFF303030, 0xFF424242, 2f,
                        0xFFFFFFFF, 0xFFB3B3B3,
                        0xFF424242, 0x33FFFFFF, true, 0xFF37474F);
            case THEME_OREO:
                // Material 浅色：灰白 50、Blue Grey 900 主色、Teal 强调、4dp 圆角
                return new Palette(
                        0xFF008577, 0xFF21272B, 0xFF21272B,
                        0xFFFAFAFA, 0xFFFFFFFF, 4f,
                        0xFF212121, 0xFF757575,
                        0xFFFFFFFF, 0x1F000000, false, 0xFF263238);
            case THEME_HORROR:
                // 崩坏：近黑的暗红，血一样的强调色
                return new Palette(
                        0xFFC62828, 0xFF0A0203, 0xFF0A0203,
                        0xFF13070A, 0xFF1E0A0E, 2f,
                        0xFFE6D2D4, 0xFF9A7B80,
                        0xFF2A0E12, 0x44FF5252, true, 0xFF1A0809);
            default:
                // Material You 浅色：近白、Material You 紫、28dp 大圆角
                return new Palette(
                        0xFF6750A4, 0xFF6750A4, 0xFFFDF7FF,
                        0xFFFAF8FF, 0xFFFFFBFE, 28f,
                        0xFF1C1B1F, 0xFF49454F,
                        0xFFFFFFFF, 0xFFCAC4D0, false, 0xFFFFFBFE);
        }
    }

    public static Palette palette(Context c) {
        return palette(get(c));
    }

    /** 强调色。 */
    public static int accent(int t) {
        return palette(t).accent;
    }

    public static int accent(Context c) {
        return palette(c).accent;
    }

    /** 窗口背景色。 */
    public static int windowBg(int t) {
        return palette(t).windowBg;
    }

    public static int windowBg(Context c) {
        return palette(c).windowBg;
    }

    /** 是否深色风格。 */
    public static boolean isDark(int t) {
        return palette(t).dark;
    }

    public static boolean isDark(Context c) {
        return palette(c).dark;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
