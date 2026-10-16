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

import android.app.FragmentTransaction;

/**
 * 统一页面转场（v1.5）：所有 Fragment 切换都从这里取动画，
 * 避免「有的按钮会动、有的按钮不会动」。
 */
public final class NavAnim {

    private NavAnim() {
    }

    /** 进入下一级：新页从右往左进，旧页往左淡出。 */
    public static FragmentTransaction enter(FragmentTransaction ft) {
        return ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left,
                R.animator.slide_in_left, R.animator.slide_out_right);
    }

    /** 返回上一级：新页从左往右进，旧页往右淡出。 */
    public static FragmentTransaction back(FragmentTransaction ft) {
        return ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right,
                R.animator.slide_in_right, R.animator.slide_out_left);
    }

    /**
     * 页面进场动画：直接动 View，不依赖 Fragment 转场是否生效。
     * 之前贡献者 / 捐赠页点进去「直接跳转、没有动画」，用这个兜底保证每个入口都有动效。
     */
    public static void animateIn(android.view.View v) {
        if (v == null) return;
        final float dx = v.getResources().getDisplayMetrics().widthPixels * 0.28f;
        v.setTranslationX(dx);
        v.setAlpha(0f);
        v.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    /** 首屏/切 Tab：不加位移，只做淡入淡出，避免底部导航切换时晃动。 */
    public static FragmentTransaction tab(FragmentTransaction ft) {
        return ft.setCustomAnimations(R.animator.fade_in_content, R.animator.fade_out);
    }
}
