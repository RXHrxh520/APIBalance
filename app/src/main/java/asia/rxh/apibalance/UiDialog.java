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

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义 UI 弹窗：圆角卡片风格，替代系统 AlertDialog。
 * 支持：单选列表 / 双按钮确认 / 文本输入。
 */
public final class UiDialog {

    /** 全局唯一弹窗：新弹窗出现前先关掉旧的，避免层层堆叠导致界面点不动。 */
    private static android.app.Dialog activeDialog;

    private static void dismissActive() {
        try {
            if (activeDialog != null && activeDialog.isShowing()) activeDialog.dismiss();
        } catch (Throwable ignored) {
        }
        activeDialog = null;
    }


    private UiDialog() {
    }

    public interface OnPickListener {
        void onPick(int index);
    }

    public interface OnConfirmListener {
        void onConfirm();
    }

    /** 单选列表弹窗（无说明）。 */
    public static void showPicker(Context ctx, String title, String[] options, int selected, OnPickListener l) {
        showPickerWithDesc(ctx, title, null, options, selected, l);
    }

    /** 单选列表弹窗（带使用说明：在标题下显示一行灰字）。 */
    public static void showPickerWithDesc(Context ctx, String title, String description,
                                          String[] options, int selected, OnPickListener l) {
        Dialog d = baseDialog(ctx);
        TextView titleView = d.findViewById(R.id.dialog_title);
        TextView msg = d.findViewById(R.id.dialog_message);
        if (description != null && !description.isEmpty()) {
            msg.setText(description);
            msg.setVisibility(View.VISIBLE);
        }
        LinearLayout opts = d.findViewById(R.id.dialog_options);
        titleView.setText(title);

        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
            row.setBackgroundResource(R.drawable.bg_input_field);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(ctx, 8);
            row.setLayoutParams(lp);

            TextView tv = new TextView(ctx);
            tv.setText(options[i]);
            tv.setTextSize(15);
            tv.setTextColor(ctx.getResources().getColor(R.color.text_primary));
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(tvLp);
            row.addView(tv);

            // 选中态用整行高亮 + 主色文字表达，不再在行尾挂一个小勾号
            if (i == selected && selected >= 0) {
                tv.setTextColor(ctx.getResources().getColor(R.color.primary));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                android.graphics.drawable.GradientDrawable sel =
                        new android.graphics.drawable.GradientDrawable();
                sel.setColor(ctx.getResources().getColor(R.color.input_bg));
                sel.setCornerRadius(10 * ctx.getResources().getDisplayMetrics().density);
                row.setBackground(sel);
            }

            final Dialog fd = d;
            row.setOnClickListener(v -> {
                if (l != null) l.onPick(idx);
                fd.dismiss();
            });
            opts.addView(row);
        }

        TextView neg = d.findViewById(R.id.dialog_negative);
        neg.setVisibility(View.GONE);
        d.findViewById(R.id.dialog_positive).setVisibility(View.GONE);
        showSafely(d);
    }

    /** 双按钮确认弹窗（可带 message）。 */
    public static void showConfirm(Context ctx, String title, String message,
                                   String posText, String negText,
                                   OnConfirmListener pos, OnConfirmListener neg) {
        // 没传取消文案的，一律补一个「取消」，避免弹窗只能点确定
        if (negText == null || negText.trim().isEmpty()) {
            negText = ctx.getString(R.string.dlg_cancel);
        }
        Dialog d = baseDialog(ctx);
        TextView titleView = d.findViewById(R.id.dialog_title);
        TextView msg = d.findViewById(R.id.dialog_message);
        TextView posBtn = d.findViewById(R.id.dialog_positive);
        TextView negBtn = d.findViewById(R.id.dialog_negative);
        titleView.setText(title);
        if (message != null) {
            msg.setText(message);
            msg.setVisibility(View.VISIBLE);
        }
        if (posText != null) posBtn.setText(posText);
        if (negText != null) negBtn.setText(negText);
        posBtn.setOnClickListener(v -> {
            if (pos != null) pos.onConfirm();
            d.dismiss();
        });
        negBtn.setOnClickListener(v -> {
            if (neg != null) neg.onConfirm();
            d.dismiss();
        });
        showSafely(d);
    }

    /** 文本输入弹窗（单输入框）。 */
    public static void showInput(Context ctx, String title, String hint, String initial,
                                 final OnInputListener l) {
        showInputWithDesc(ctx, title, null, hint, initial, l);
    }

    /** 文本输入弹窗（带使用说明）。 */
    public static void showInputWithDesc(Context ctx, String title, String description,
                                         String hint, String initial, final OnInputListener l) {
        Dialog d = baseDialog(ctx);
        TextView titleView = d.findViewById(R.id.dialog_title);
        TextView msg = d.findViewById(R.id.dialog_message);
        if (description != null && !description.isEmpty()) {
            msg.setText(description);
            msg.setVisibility(View.VISIBLE);
        }
        LinearLayout inputArea = d.findViewById(R.id.dialog_input_area);
        titleView.setText(title);

        final EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setText(initial == null ? "" : initial);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setTextColor(ctx.getResources().getColor(R.color.text_primary));
        et.setHintTextColor(ctx.getResources().getColor(R.color.text_hint));
        et.setTextSize(15);
        et.setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10));
        // 固定输入区高度：大量文本时内部滚动，对话框永不超过屏幕
        et.setMaxLines(6);
        et.setMaxHeight(dp(ctx, 180));
        et.setInputType(et.getInputType() | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setVerticalScrollBarEnabled(true);
        inputArea.addView(et);
        inputArea.setVisibility(View.VISIBLE);

        TextView posBtn = d.findViewById(R.id.dialog_positive);
        posBtn.setOnClickListener(v -> {
            if (l != null) l.onInput(et.getText().toString().trim());
            d.dismiss();
        });
        showSafely(d);
    }

    public interface OnInputListener {
        void onInput(String text);
    }

    /** 弹窗安全显示：异步回调时 Activity 可能已销毁，捕获 BadTokenException。 */
    private static void remember(android.app.Dialog d) {
        activeDialog = d;
    }

    private static void showSafely(Dialog d) {
        dismissActive();
        if (d == null) return;
        try {
            remember(d);
            fitLongMessage(d);
            d.show();
            centerWindow(d);
        } catch (android.view.WindowManager.BadTokenException ignored) {
        } catch (Exception ignored) {
        }
    }

    /**
     * 长文案（用户协议 / 隐私政策）必须限制正文高度。
     * 否则正文会把弹窗撑到超出屏幕，确定键被顶到屏幕外，怎么点都点不到。
     */
    private static void fitLongMessage(Dialog d) {
        try {
            TextView msg = d.findViewById(R.id.dialog_message);
            View scroll = d.findViewById(R.id.dialog_message_scroll);
            if (msg == null || scroll == null) return;
            CharSequence cs = msg.getText();
            if (cs == null || cs.length() <= 500) return;
            android.util.DisplayMetrics dm = d.getContext().getResources().getDisplayMetrics();
            ViewGroup.LayoutParams lp = scroll.getLayoutParams();
            lp.height = (int) (dm.heightPixels * 0.40f);
            scroll.setLayoutParams(lp);
        } catch (Throwable ignored) {
        }
    }

    /** 部分系统会在 show() 时把过高的弹窗挤偏，这里再固定一次宽度与居中位置。 */
    private static void centerWindow(Dialog d) {
        try {
            Window w = d.getWindow();
            if (w == null) return;
            w.setGravity(Gravity.CENTER);
            w.setLayout(dp(d.getContext(), 320), ViewGroup.LayoutParams.WRAP_CONTENT);
        } catch (Throwable ignored) {
        }
    }

    private static Dialog baseDialog(Context ctx) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_custom);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new GradientDrawable());
            w.setLayout(dp(ctx, 320), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
        }
        // 深色主题（复古2000/CLI）下，弹窗也跟随主题配色
        ThemeUtils.apply(d.getWindow() != null ? d.getWindow().getDecorView() : d.findViewById(R.id.dialog_root));
        return d;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
