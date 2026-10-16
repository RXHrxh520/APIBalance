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
 * 主题中心：
 *  - 主题风格：0=默认 1=复古2000 2=Code CLI
 *  - 每套风格下可选 accent 色（随心选色）
 */
public final class ThemePrefs {

    private static final String PREFS = "ds_theme";
    private static final String KEY_STYLE = "style_index";
    private static final String KEY_COLOR = "color_index";

    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_Y2K = 1;
    public static final int STYLE_CODE = 2;
    /** 隐藏主题：只能通过彩蛋获得，不出现在主题中心列表里。 */
    public static final int STYLE_PINK = 3;

    public static final String[] STYLE_NAMES = {"默认", "复古2000", "Code CLI"};

    /** 每套主题的默认 accent 色。 */
    public static final int[] STYLE_ACCENT = {
        0xFF6750A4, // 默认紫
        0xFFFF2E9E, // 复古2000 霓虹粉
        0xFF00E676, // Code CLI 荧光绿
        0xFFFF4F97  // 隐藏：粉粉的
    };

    /** 随心选色：8 种 accent。 */
    public static final int[] COLORS = {
        0xFF6750A4, // 紫
        0xFF2E7D32, // 绿
        0xFF1565C0, // 蓝
        0xFFE65100, // 橙
        0xFFD81B60, // 粉
        0xFF00838F, // 青
        0xFFF9A825, // 黄
        0xFFC62828  // 红
    };

    public static final String[] COLOR_NAMES = {"紫色", "绿色", "蓝色", "橙色", "粉色", "青色", "黄色", "红色"};

    private ThemePrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getStyle(Context c) {
        return sp(c).getInt(KEY_STYLE, STYLE_DEFAULT);
    }

    public static void setStyle(Context c, int s) {
        sp(c).edit().putInt(KEY_STYLE, s).apply();
    }

    public static int getColorIndex(Context c) {
        // -1 表示「未手动选色」，走风格默认色；用户选中后为 0..7
        return sp(c).getInt(KEY_COLOR, -1);
    }

    public static void setColorIndex(Context c, int i) {
        sp(c).edit().putInt(KEY_COLOR, i).apply();
    }

    /** 主题风格显示名（跟随语言）。 */
    public static String[] styleNames(Context c) {
        return c.getResources().getStringArray(R.array.theme_styles);
    }

    /** 强调色显示名（跟随语言）。 */
    public static String[] colorNames(Context c) {
        return c.getResources().getStringArray(R.array.theme_colors);
    }

    /** 当前 accent 色：优先用自定义选色，否则用风格默认色。 */
    public static int getColor(Context c) {
        int style = getStyle(c);
        int ci = getColorIndex(c);
        if (ci >= 0 && ci < COLORS.length) {
            return COLORS[ci];
        }
        return STYLE_ACCENT[style % STYLE_ACCENT.length];
    }

    /** 主题背景色（Code CLI 用深色背景）。 */
    public static int getBackground(Context c) {
        int style = getStyle(c);
        if (style == STYLE_CODE) return 0xFF0D1117; // 终端深色
        if (style == STYLE_Y2K) return 0xFFB9C2D0;  // 复古2000：银灰桌面底色
        if (style == STYLE_PINK) return 0xFFFFF0F6; // 隐藏粉色主题
        return 0; // 0 表示用默认 @color/background
    }

    /** 是否启用深色背景（用于 Code CLI / Y2K）。 */
    public static boolean usesDarkBg(Context c) {
        int st = getStyle(c);
        // v1.5：复古2000 改回银灰亮色，只有 Code CLI 还是深色终端
        return st == STYLE_CODE;
    }
}
