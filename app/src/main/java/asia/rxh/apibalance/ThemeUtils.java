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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 主题应用工具：把「当前主题」的完整调色板真正刷到整个视图树。
 *
 * 优先级：主题中心里的三套风格（复古2000 / Code CLI）优先；
 * 否则用「安卓主题设置」的四套真实系统配色（AndroidThemeManager）。
 * 无论深浅色主题都强制应用，保证切换后肉眼可见。
 */
public final class ThemeUtils {

    private ThemeUtils() {
    }

    /** 是否深色主题（供历史页等自行着色判断）。 */
    public static boolean isDarkTheme(Context c) {
        // 以真正刷到界面上的调色板为准：Y2K 是银灰亮色，不能在这里误判成深色。
        return isDark(c);
    }

    /** 当前强调色（忽略风格，统一取安卓主题或 CLI/Y2K 强调色）。 */
    /** 当前主题是不是深色底。 */
    public static boolean isDark(Context c) {
        try {
            int st = ThemePrefs.getStyle(c);
            if (st == ThemePrefs.STYLE_CODE) return true;
            if (st == ThemePrefs.STYLE_Y2K || st == ThemePrefs.STYLE_PINK) return false;
            // 默认主题跟随系统深浅（Material You 深色 palette）。
            return AndroidThemeManager.palette(c).dark;
        } catch (Throwable e) {
            return false;
        }
    }

    /** 卡片/弹窗底色，跟着主题走，别再到处写死白色。 */
    public static int cardBg(Context c) {
        try {
            int st = ThemePrefs.getStyle(c);
            if (st == ThemePrefs.STYLE_CODE) return 0xFF15171C;
            if (st == ThemePrefs.STYLE_PINK) return 0xFFFFF1F7;
            switch (AndroidThemeManager.get(c)) {
                case AndroidThemeManager.THEME_HORROR: return 0xFF13070A;
                case AndroidThemeManager.THEME_OREO: return 0xFF121212;
                case AndroidThemeManager.THEME_LOLLIPOP: return 0xFF212121;
                default: return 0xFFFFFFFF;
            }
        } catch (Throwable e) {
            return 0xFFFFFFFF;
        }
    }

    public static int accent(Context c) {
        // 主题中心手选过强调色就优先用它，否则才落到风格自带色。
        // 之前这里完全没读 colorIndex，所以选了颜色只有少数界面跟着变。
        if (ThemePrefs.getColorIndex(c) >= 0) {
            return ThemePrefs.getColor(c);
        }
        int style = ThemePrefs.getStyle(c);
        if (style == ThemePrefs.STYLE_CODE) return 0xFF00E676;
        if (style == ThemePrefs.STYLE_Y2K) return 0xFFFF2E9E;
        if (style == ThemePrefs.STYLE_PINK) return 0xFFFF4F97;
        return AndroidThemeManager.accent(c);
    }

    /** 当前副文字色。 */
    public static int textSecondary(Context c) {
        int style = ThemePrefs.getStyle(c);
        if (style == ThemePrefs.STYLE_CODE) return 0xFF66D9A0;
        if (style == ThemePrefs.STYLE_Y2K) return 0xFFC9A9C9;
        if (style == ThemePrefs.STYLE_PINK) return 0xFFB0628A;
        return AndroidThemeManager.palette(c).textSecondary;
    }

    /** 当前主文字色（正文/标题）。 */
    public static int textPrimary(Context c) {
        int style = ThemePrefs.getStyle(c);
        if (style == ThemePrefs.STYLE_PINK) return 0xFF4A1F33;
        return AndroidThemeManager.palette(c).textPrimary;
    }

    /** 当前底栏/顶部标题条背景色。 */
    public static int sectionBg(Context c) {
        int style = ThemePrefs.getStyle(c);
        if (style == ThemePrefs.STYLE_CODE) return 0xFF10151C;
        if (style == ThemePrefs.STYLE_Y2K) return 0xFF2A1B3D;
        if (style == ThemePrefs.STYLE_PINK) return 0xFFFFE3EF;
        return AndroidThemeManager.palette(c).sectionBg;
    }

    /** 把当前主题应用到整个视图树。 */
    /** 崩坏主题：给根视图压一层暗红，别让它只剩配色没有风格。 */
    private static void horrorTint(View root) {
        if (root == null || root.getContext() == null) return;
        try {
            if (AndroidThemeManager.get(root.getContext()) != AndroidThemeManager.THEME_HORROR) return;
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x33C62828, 0x00000000, 0x22C62828});
            root.setBackground(g);
            if (root instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) root;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View c = vg.getChildAt(i);
                    if (c instanceof android.widget.TextView) {
                        ((android.widget.TextView) c).setShadowLayer(6f, 0f, 0f, 0x66C62828);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void apply(View root) {
        if (root == null) return;
        Context c = root.getContext();
        int style = ThemePrefs.getStyle(c);

        int bg, card, input, textP, textS, accent, section;
        float corner;
        final boolean dark, cli;

        if (style == ThemePrefs.STYLE_CODE) {
            // Code CLI：终端深色 + 荧光绿 + 等宽字体
            bg = 0xFF0D1117; card = 0xFF10151C; input = 0xFF1B242E;
            textP = 0xFFC9FFE0; textS = 0xFF66D9A0; accent = 0xFF00E676;
            section = 0xFF10151C; corner = 2f; dark = true; cli = true;
        } else if (style == ThemePrefs.STYLE_Y2K) {
            // 复古2000（v1.5 重做）：还原千禧年前后个人主页的样子 ——
            // 银灰底、宝蓝超链接、直角面板、白底输入框，不再用「深紫+霓虹粉」充数
            bg = 0xFFB9C2D0; card = 0xFFE4E9F2; input = 0xFFFFFFFF;
            textP = 0xFF000033; textS = 0xFF3B4A6B; accent = 0xFF0033CC;
            section = 0xFFCBD4E2; corner = 0f; dark = false; cli = false;
        } else if (style == ThemePrefs.STYLE_PINK) {
            // 隐藏粉色主题：整个界面都变得粉粉的
            bg = 0xFFFFF0F6; card = 0xFFFFE3EF; input = 0xFFFFD6E7;
            textP = 0xFF4A1F33; textS = 0xFFB0628A; accent = 0xFFFF4F97;
            section = 0xFFFFE3EF; corner = 18f; dark = false; cli = false;
        } else {
            AndroidThemeManager.Palette p = AndroidThemeManager.palette(c);
            bg = p.windowBg; card = p.cardBg; input = p.inputBg;
            textP = p.textPrimary; textS = p.textSecondary; accent = p.accent;
            section = p.sectionBg; corner = p.cardCorner; dark = p.dark; cli = false;
        }

        // v1.5 实验性功能：MD3 组件开关。
        // 之前只把圆角设成 28f、色差混 4%~6%，而默认主题本来就是 28f 圆角，
        // 所以开了跟没开一样。现在改为整体色调化：背景/卡片/分节/输入框都向强调色靠，
        // 圆角拉到 32f，肉眼一眼能看出区别。
        if (AppPrefs.isExperimentalMd3(c)) {
            corner = 32f;
            bg = blendTone(bg, accent, 0.10f);
            card = blendTone(card, accent, 0.16f);
            section = blendTone(section, accent, 0.24f);
            input = blendTone(input, accent, 0.08f);
        }

        float density = c.getResources().getDisplayMetrics().density;
        applyRecursive(root, bg, card, input, textP, textS, accent, section, corner, dark, cli, density);
        horrorTint(root);
    }

    private static void applyRecursive(View v, int bg, int card, int input, int textP, int textS,
                                       int accent, int section, float corner, boolean dark,
                                       boolean cli, float density) {
        // 背景一：纯色背景（页面/顶栏）
        if (v.getBackground() instanceof ColorDrawable) {
            try {
                int col = ((ColorDrawable) v.getBackground()).getColor();
                if (col == 0) {
                    // 透明背景：保持不动，让主界面背景层透出
                } else if (isLight(col)) {
                    v.setBackgroundColor(bg);
                } else {
                    v.setBackgroundColor(section);
                }
            } catch (Exception ignored) {
            }
        }
        // 背景二：shape 背景（卡片 / 输入框 / 按钮 / 强调点）
        else if (v.getBackground() instanceof GradientDrawable) {
            try {
                GradientDrawable gd = (GradientDrawable) v.getBackground();
                int[] grad = null;
                try { grad = gd.getColors(); } catch (Exception ignored) { }
                boolean isOval = gd.getShape() == GradientDrawable.OVAL;
                if (grad != null && grad.length >= 2) {
                    // 渐变按钮：换成当前主题强调色渐变
                    GradientDrawable nd = new GradientDrawable(
                            GradientDrawable.Orientation.TL_BR,
                            new int[]{accent, darker(accent)});
                    nd.setCornerRadius(corner * density);
                    v.setBackground(nd);
                } else {
                    int target = card;
                    if (v instanceof EditText) target = input;
                    if (isOval) target = accent;
                    GradientDrawable nd = new GradientDrawable();
                    if (isOval) nd.setShape(GradientDrawable.OVAL);
                    else nd.setCornerRadius(corner * density);
                    nd.setColor(target);
                    if (cli) nd.setStroke((int) (1 * density + 0.5f), accent);
                    v.setBackground(nd);
                }
            } catch (Exception ignored) {
            }
        }

        // 文字颜色
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            int cur = tv.getCurrentTextColor();
            int lum = luminance(cur);
            if (dark) {
                // 深色主题：深/中色文字一律改为主题浅色文字；白色等浅色保留
                if (lum < 170) tv.setTextColor(textP);
                else if (lum < 210) tv.setTextColor(textS);
            } else {
                // 浅色主题：深色文字统一为主题主文字
                if (lum < 120) tv.setTextColor(textP);
                else if (lum < 170) tv.setTextColor(textS);
            }
            if (cli) tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        }

        // 输入框
        if (v instanceof EditText) {
            EditText et = (EditText) v;
            et.setTextColor(textP);
            et.setHintTextColor(textS);
            if (cli) et.setTypeface(android.graphics.Typeface.MONOSPACE);
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyRecursive(vg.getChildAt(i), bg, card, input, textP, textS,
                        accent, section, corner, dark, cli, density);
            }
        }
    }

    private static boolean isLight(int col) {
        return luminance(col) > 180;
    }

    private static int luminance(int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
        return (r * 29 + g * 59 + b * 12) / 100;
    }

    private static int darker(int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
        r = (int) (r * 0.78); g = (int) (g * 0.78); b = (int) (b * 0.78);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 按比例把 accent 混进底色，用于 MD3 色调表面。 */
    private static int blendTone(int base, int tone, float ratio) {
        int br = (base >> 16) & 0xFF, bgc = (base >> 8) & 0xFF, bb = base & 0xFF;
        int tr = (tone >> 16) & 0xFF, tg = (tone >> 8) & 0xFF, tb = tone & 0xFF;
        int r = (int) (br + (tr - br) * ratio);
        int g = (int) (bgc + (tg - bgc) * ratio);
        int b = (int) (bb + (tb - bb) * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
