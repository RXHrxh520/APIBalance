package asia.rxh.apibalance;

/*
 * Copyright (C) 2026 软小寒
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 彩蛋：连续点击「我的」页面上的 DeepSeek 小图标 5 次后出现的对话。
 * 模拟真人打字节奏：先显示「正在输入…」，再逐条出现，顶部有在线 / 离线状态。
 */
public final class EggChat {

    private static final long TYPING_MS = 750L;
    private static final long GAP_MS = 380L;

    private final Activity act;
    private final Dialog dialog;
    private final LinearLayout transcript;
    private final LinearLayout options;
    private final TextView status;
    private final ScrollView scroll;
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** 同一时间只允许一个彩蛋窗口，反复点不会叠出一堆。 */
    private static boolean showing = false;
    /** 对话框是否还活着；关掉之后所有延时回调直接作废。 */
    private volatile boolean alive = true;

    /** 把彩蛋的内部状态打回初始（调试页用）。 */
    public static void resetState() {
        showing = false;
    }

    private EggChat(Activity act) {
        this.act = act;
        dialog = new Dialog(act);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeUtils.cardBg(act));

        LinearLayout header = new LinearLayout(act);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));
        header.setBackgroundColor(ThemeUtils.sectionBg(act));
        android.widget.ImageView icon = new android.widget.ImageView(act);
        icon.setImageResource(R.drawable.ic_deepseek);
        header.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout titles = new LinearLayout(act);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        TextView name = new TextView(act);
        name.setText(act.getString(R.string.egg_title));
        name.setTextSize(15f);
        name.setTextColor(ThemeUtils.textPrimary(act));
        status = new TextView(act);
        status.setTextSize(11f);
        status.setTextColor(0xFF10B981);
        status.setText(act.getString(R.string.egg_online));
        titles.addView(name);
        titles.addView(status);
        header.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = new TextView(act);
        close.setText(act.getString(R.string.dlg_cancel));
        close.setTextSize(14f);
        close.setTextColor(ThemeUtils.textSecondary(act));
        close.setPadding(dp(8), dp(8), dp(8), dp(8));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close);
        root.addView(header);

        scroll = new ScrollView(act);
        dialog.setCanceledOnTouchOutside(false);
        // 刚弹出来的 700ms 内不允许点外面关闭：
        // 用户连点三下时，第三下会顺手把刚出现的窗口点掉，看起来就是「刚打开就关」。
        handler.postDelayed(() -> {
            try {
                dialog.setCanceledOnTouchOutside(false);
            } catch (Throwable ignored) {
            }
        }, 700L);
        // 刚弹出来的 700ms 内不允许点外面关闭：
        // 用户连点三下时，第三下会顺手把刚出现的窗口点掉，看起来就是「刚打开就关」。
        handler.postDelayed(() -> {
            try {
                dialog.setCanceledOnTouchOutside(true);
            } catch (Throwable ignored) {
            }
        }, 700L);
        dialog.setOnDismissListener(d -> {
            showing = false;
            alive = false;
            // 关键：把排队里的延时任务全部撤掉。
            // 不清的话关掉再打开，旧任务会打到新对话框上 —— 看起来就是卡死 + 重复刷屏。
            handler.removeCallbacksAndMessages(null);
        });
        transcript = new LinearLayout(act);
        transcript.setOrientation(LinearLayout.VERTICAL);
        transcript.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(transcript);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        options = new LinearLayout(act);
        options.setOrientation(LinearLayout.HORIZONTAL);
        options.setPadding(dp(12), dp(8), dp(12), dp(12));
        options.setGravity(Gravity.END);
        root.addView(options);

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dp(460));
        }
    }

    /** 连续点击 5 次后调用。 */
    public static void show(Activity act) {
        if (act == null || showing) return;
        showing = true;
        try {
            new EggChat(act).start();
        } catch (Throwable ignored) {
            showing = false;
        }
    }

    /** 打开：有记录就先恢复，没有才从头演。 */
    private void start() {
        animateIn();
        String log = "";
        try {
            // 打开就把重复的记录清掉，别攒成一份像微信聊天记录一样的东西
            AppPrefs.dedupeEggLog(act);
            log = AppPrefs.getEggLog(act);
        } catch (Throwable ignored) {
        }
        if (log != null && !log.isEmpty()) {
            for (String line : log.split("\u0002")) {
                int i = line.indexOf('\u0001');
                if (i < 0) continue;
                bubble(line.substring(i + 1), "1".equals(line.substring(0, i)));
            }
            bot(getString(R.string.egg_resume), () -> { });
            return;
        }
        replay();
    }

    private void replay() {
        try {
            AppPrefs.clearEggLog(act);
        } catch (Throwable ignored) {
        }
        handler.postDelayed(() -> { if (!alive) return; bot(getString(R.string.egg_1), () ->
                bot(getString(R.string.egg_2), () ->
                        bot(getString(R.string.egg_3), () ->
                                bot(getString(R.string.egg_4), this::askBloat)))); }, 260L);
    }

    /** 弹出来的时候淡入 + 轻微放大，别那么生硬。 */
    private void animateIn() {
        try {
            dialog.show();
            android.view.Window w = dialog.getWindow();
            if (w != null) {
                w.setWindowAnimations(0);
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0x00000000));
            }
            View deco = w == null ? null : w.getDecorView();
            if (deco != null) {
                deco.setAlpha(0f);
                deco.setScaleX(0.94f);
                deco.setScaleY(0.94f);
                deco.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
            }
        } catch (Throwable ignored) {
        }
    }

    private String getString(int id) {
        return act.getString(id);
    }

    private void askBloat() {
        choose(new String[]{getString(R.string.egg_yes), getString(R.string.egg_no)}, which -> {
            if (which == 0) {
                bot(getString(R.string.egg_5), this::goOffline);
            } else {
                bot(getString(R.string.egg_6), () ->
                        bot(getString(R.string.egg_7), () ->
                                bot(getString(R.string.egg_8), () ->
                                        bot(getString(R.string.egg_9), this::askAlthough))));
            }
        });
    }

    private void askAlthough() {
        choose(new String[]{getString(R.string.egg_10)}, which ->
                bot(getString(R.string.egg_11), this::askFarewell));
    }

    private void askFarewell() {
        choose(new String[]{getString(R.string.egg_14), getString(R.string.egg_15),
                getString(R.string.egg_16)}, which -> {
            if (which == 2) {
                bot(getString(R.string.egg_12), this::grantTheme);
            } else {
                goOffline();
            }
        });
    }

    /** 最后一条线：偷偷送一个粉粉的隐藏主题。 */
    private void grantTheme() {
        handler.postDelayed(() -> {
            try {
                AppPrefs.setPink(act, true);
                ThemePrefs.setStyle(act, ThemePrefs.STYLE_PINK);
                android.widget.Toast.makeText(act, getString(R.string.egg_theme_added),
                        android.widget.Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
            goOffline();
        }, GAP_MS);
    }

    private void goOffline() {
        setStatus(getString(R.string.egg_offline), 0xFF9CA3AF);
    }

    private void setStatus(String text, int color) {
        status.setText(text);
        status.setTextColor(color);
    }

    private void bot(final String text, final Runnable next) {
        setStatus(getString(R.string.egg_typing), 0xFF6B7280);
        handler.postDelayed(() -> {
            if (!alive) return;
            bubble(text, false);
            setStatus(getString(R.string.egg_online), 0xFF10B981);
            if (next != null) handler.postDelayed(() -> {
                if (alive) next.run();
            }, GAP_MS);
        }, TYPING_MS);
    }

    private void me(String text) {
        bubble(text, true);
    }

    private void bubble(String text, boolean mine) {
        // QQ 味：头像 + 气泡，自己的靠右、对方的靠左
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(mine ? Gravity.END : Gravity.START);
        row.setPadding(0, dp(7), 0, 0);

        android.widget.ImageView avatar = new android.widget.ImageView(act);
        avatar.setImageResource(mine ? R.drawable.ic_dev_avatar_default : R.drawable.ic_deepseek_fish);
        avatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(32), dp(32));
        ap.leftMargin = dp(8);
        ap.rightMargin = dp(8);
        ap.topMargin = dp(2);

        TextView tv = new TextView(act);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(mine ? Color.WHITE : ThemeUtils.textPrimary(act));
        tv.setPadding(dp(12), dp(9), dp(12), dp(9));
        tv.setMaxWidth((int) (act.getResources().getDisplayMetrics().widthPixels * 0.62f));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(mine ? ThemeUtils.accent(act) : ThemeUtils.cardBg(act));
        // 靠谁那边，那个上角就尖一点，像 QQ 的对话尖角
        bg.setCornerRadii(mine
                ? new float[]{dp(14), dp(14), dp(3), dp(3), dp(14), dp(14), dp(14), dp(14)}
                : new float[]{dp(3), dp(3), dp(14), dp(14), dp(14), dp(14), dp(14), dp(14)});
        if (!mine) bg.setStroke(dp(1), 0xFFE5E5EA);
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(2);

        if (mine) {
            row.addView(tv, lp);
            row.addView(avatar, ap);
        } else {
            row.addView(avatar, ap);
            row.addView(tv, lp);
        }
        transcript.addView(row);
        row.setAlpha(0f);
        row.setTranslationY(dp(10));
        row.animate().alpha(1f).translationY(0f).setDuration(180).start();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        try {
            AppPrefs.appendEggLog(act, mine, text);
        } catch (Throwable ignored) {
        }
    }

    /** 用户可选项：点了以后隐藏按钮并把这句话当成用户消息发出去。 */
    private void choose(final String[] labels, final java.util.function.IntConsumer cb) {
        options.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView b = new TextView(act);
            b.setText(labels[i]);
            b.setTextSize(13f);
            b.setTextColor(ThemeUtils.accent(act));
            b.setPadding(dp(14), dp(8), dp(14), dp(8));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1), ThemeUtils.accent(act));
            b.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = dp(6);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                options.removeAllViews();
                me(labels[idx]);
                if (cb != null) handler.postDelayed(() -> cb.accept(idx), GAP_MS);
            });
            options.addView(b);
        }
    }

    private int dp(int v) {
        return (int) (v * act.getResources().getDisplayMetrics().density);
    }
}
