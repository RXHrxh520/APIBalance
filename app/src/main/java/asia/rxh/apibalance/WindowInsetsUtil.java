package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** 给弹窗根视图补系统栏内边距，避免底部内容被导航栏/手势条盖住。 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
    }

    /**
     * 只处理左右与底部，不处理顶部：弹窗通常居中，顶部状态栏不会压住内容。
     * 对已有 padding 的根视图采用“原 padding + 系统栏 inset”的方式，不覆盖主题间距。
     */
    public static void applyBottomInset(View root) {
        if (root == null) return;
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int insetLeft = 0;
            int insetRight = 0;
            int insetBottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                insetLeft = bars.left;
                insetRight = bars.right;
                insetBottom = bars.bottom;
            } else {
                insetLeft = insets.getSystemWindowInsetLeft();
                insetRight = insets.getSystemWindowInsetRight();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left + insetLeft, top, right + insetRight, bottom + insetBottom);
            return insets;
        });
        root.requestApplyInsets();
    }
}
