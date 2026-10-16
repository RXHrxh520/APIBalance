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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashActivity extends Activity {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        int lang = AppPrefs.getLanguage(base);
        if (lang >= 1 && lang <= 4) {
            android.content.res.Configuration cfg =
                    new android.content.res.Configuration(base.getResources().getConfiguration());
            if (lang == 2) cfg.setLocale(new java.util.Locale("en"));
            else if (lang == 3) cfg.setLocale(java.util.Locale.TRADITIONAL_CHINESE);
            else if (lang == 4) cfg.setLocale(new java.util.Locale("eo"));
            else cfg.setLocale(new java.util.Locale("zh"));
            base = base.createConfigurationContext(cfg);
        }
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏彩蛋：上次是故意崩的，就给一个不一样的画面
        if (AppPrefs.isBrokenTapped(this)) {
            buildHorror();
            return;
        }

        // 使用代码构建界面，避免资源加载二次崩溃
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFFF6F3FA);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        // 红色警告图标（代码画圆）
        android.widget.LinearLayout iconCircle = new android.widget.LinearLayout(this);
        iconCircle.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(72), dp(72)));
        android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
        circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circle.setColor(0xFFB3261E);
        iconCircle.setBackground(circle);
        iconCircle.setGravity(android.view.Gravity.CENTER);

        TextView iconText = new TextView(this);
        iconText.setText("!");
        iconText.setTextColor(0xFFFFFFFF);
        iconText.setTextSize(36);
        iconText.setTypeface(null, android.graphics.Typeface.BOLD);
        iconCircle.addView(iconText);
        root.addView(iconCircle);

        // 标题
        addSpace(root, dp(20));
        TextView title = new TextView(this);
        title.setText(getString(R.string.j_176));
        title.setTextColor(0xFF1C1B1F);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        addSpace(root, dp(8));
        TextView subtitle = new TextView(this);
        subtitle.setText(getString(R.string.j_177));
        subtitle.setTextColor(0xFF49454F);
        subtitle.setTextSize(14);
        subtitle.setGravity(android.view.Gravity.CENTER);
        root.addView(subtitle);

        // 错误卡片
        addSpace(root, dp(24));
        android.widget.LinearLayout errorCard = new android.widget.LinearLayout(this);
        errorCard.setOrientation(android.widget.LinearLayout.VERTICAL);
        errorCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        errorCard.setBackgroundColor(0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(0xFFFFFFFF);
        cardBg.setStroke(dp(1), 0xFFE5E7EB);
        errorCard.setBackground(cardBg);
        errorCard.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        // 错误信息
        TextView labelMsg = new TextView(this);
        labelMsg.setText(getString(R.string.j_178));
        labelMsg.setTextColor(0xFF6750A4);
        labelMsg.setTextSize(12);
        labelMsg.setTypeface(null, android.graphics.Typeface.BOLD);
        errorCard.addView(labelMsg);

        addSpace(errorCard, dp(6));

        String crashMsg = getIntent().getStringExtra("crash_message");
        String crashTrace = getIntent().getStringExtra("crash_trace");

        if (crashMsg == null) {
            String[] info = CrashHandler.getCrashInfo(this);
            crashMsg = info[0];
            crashTrace = info[1];
        }

        TextView tvErrorMsg = new TextView(this);
        tvErrorMsg.setText(crashMsg != null ? crashMsg : getString(R.string.j_179));
        tvErrorMsg.setTextColor(0xFFB3261E);
        tvErrorMsg.setTextSize(15);
        tvErrorMsg.setTypeface(null, android.graphics.Typeface.BOLD);
        tvErrorMsg.setPadding(dp(12), dp(10), dp(12), dp(10));
        android.graphics.drawable.GradientDrawable errBg = new android.graphics.drawable.GradientDrawable();
        errBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        errBg.setCornerRadius(dp(8));
        errBg.setColor(0xFFF9DEDC);
        tvErrorMsg.setBackground(errBg);
        tvErrorMsg.setTextIsSelectable(true);
        errorCard.addView(tvErrorMsg);

        addSpace(errorCard, dp(16));

        // 堆栈追踪
        TextView labelStack = new TextView(this);
        labelStack.setText(getString(R.string.j_180));
        labelStack.setTextColor(0xFF6750A4);
        labelStack.setTextSize(12);
        labelStack.setTypeface(null, android.graphics.Typeface.BOLD);
        errorCard.addView(labelStack);

        addSpace(errorCard, dp(6));

        TextView tvStackTrace = new TextView(this);
        tvStackTrace.setText(crashTrace != null ? crashTrace : getString(R.string.j_181));
        tvStackTrace.setTextColor(0xFF49454F);
        tvStackTrace.setTextSize(11);
        tvStackTrace.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvStackTrace.setLineSpacing(dp(2), 1f);
        tvStackTrace.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvStackTrace.setMaxHeight(dp(300));
        tvStackTrace.setMovementMethod(new ScrollingMovementMethod());
        android.graphics.drawable.GradientDrawable stackBg = new android.graphics.drawable.GradientDrawable();
        stackBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        stackBg.setCornerRadius(dp(8));
        stackBg.setColor(0xFFF5F5F5);
        tvStackTrace.setBackground(stackBg);
        tvStackTrace.setTextIsSelectable(true);
        errorCard.addView(tvStackTrace);

        root.addView(errorCard);

        // 重启按钮
        addSpace(root, dp(28));
        TextView btnRestart = new TextView(this);
        btnRestart.setText(getString(R.string.j_182));
        btnRestart.setTextColor(0xFFFFFFFF);
        btnRestart.setTextSize(16);
        btnRestart.setTypeface(null, android.graphics.Typeface.BOLD);
        btnRestart.setGravity(android.view.Gravity.CENTER);
        btnRestart.setPadding(dp(16), dp(14), dp(16), dp(14));
        btnRestart.setMinHeight(dp(50));
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(dp(16));
        btnBg.setColors(new int[]{0xFF6750A4, 0xFF4F378B});
        btnBg.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
        btnRestart.setBackground(btnBg);
        btnRestart.setOnClickListener(v -> {
            CrashHandler.clearCrash(CrashActivity.this);
            Intent intent = new Intent(CrashActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        root.addView(btnRestart);

        // 版本信息
        addSpace(root, dp(16));
        TextView tvVersion = new TextView(this);
        tvVersion.setText("v" + BuildConfig.VERSION_NAME + getString(R.string.j_183));
        tvVersion.setTextColor(0xFF79747E);
        tvVersion.setTextSize(11);
        tvVersion.setGravity(android.view.Gravity.CENTER);
        root.addView(tvVersion);

        scrollView.addView(root);
        setContentView(scrollView);

        // 入场动画
        iconCircle.setAlpha(0f);
        iconCircle.setScaleX(0.5f);
        iconCircle.setScaleY(0.5f);
        iconCircle.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(500).setStartDelay(100)
                .setInterpolator(new OvershootInterpolator(0.8f)).start();

        errorCard.setAlpha(0f);
        errorCard.setTranslationY(60f);
        errorCard.animate().alpha(1f).translationY(0f)
                .setDuration(450).setStartDelay(250)
                .setInterpolator(new OvershootInterpolator(0.5f)).start();
    }

    private static final String GARBLE = "̸̛̪̮̤̯̭̖̠̮̯̪̲̤̊̈́͋͌̊";

    /** 崩坏模式：近黑的暗红，全是乱码，最后给一个隐藏主题。 */
    private void buildHorror() {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(0xFF0A0203);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(72), dp(24), dp(24));

        android.widget.TextView big = new android.widget.TextView(this);
        big.setText(GARBLE + GARBLE + GARBLE);
        big.setTextSize(30);
        big.setTextColor(0xFFC62828);
        big.setGravity(android.view.Gravity.CENTER);
        big.setLetterSpacing(0.2f);
        root.addView(big);
        // 抖 + 闪，让它真的像坏掉了
        android.animation.ObjectAnimator flick = android.animation.ObjectAnimator.ofFloat(
                big, "alpha", 1f, 0.25f, 0.9f, 0.4f, 1f);
        flick.setDuration(900);
        flick.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        flick.start();
        android.animation.ObjectAnimator jit = android.animation.ObjectAnimator.ofFloat(
                big, "translationX", -dp(3), dp(3), -dp(2), dp(2), 0f);
        jit.setDuration(320);
        jit.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        jit.start();

        android.widget.TextView t1 = new android.widget.TextView(this);
        t1.setText(GARBLE + " " + getString(R.string.horror_title) + GARBLE);
        t1.setTextSize(20);
        t1.setTextColor(0xFFE6D2D4);
        t1.setGravity(android.view.Gravity.CENTER);
        t1.setPadding(0, dp(20), 0, 0);
        t1.setLetterSpacing(0.35f);
        root.addView(t1);
        android.animation.ObjectAnimator glow = android.animation.ObjectAnimator.ofFloat(
                t1, "alpha", 1f, 0.45f, 1f);
        glow.setDuration(2000);
        glow.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        glow.start();
        root.addView(bloodLine());

        android.widget.TextView t2 = new android.widget.TextView(this);
        t2.setText(getString(R.string.horror_desc));
        t2.setTextSize(13);
        t2.setTextColor(0xFF9A7B80);
        t2.setGravity(android.view.Gravity.CENTER);
        t2.setPadding(0, dp(12), 0, 0);
        root.addView(t2);

        android.widget.TextView unlock = new android.widget.TextView(this);
        unlock.setText(getString(R.string.horror_unlock));
        unlock.setTextSize(15);
        unlock.setTextColor(0xFFFFFFFF);
        unlock.setGravity(android.view.Gravity.CENTER);
        unlock.setPadding(dp(24), dp(14), dp(24), dp(14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(0xFFC62828);
        unlock.setBackground(bg);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(28);
        unlock.setClickable(true);
        unlock.setOnClickListener(v -> {
            AppPrefs.setHorror(this, true);
            AppPrefs.setBrokenTapped(this, false);
            try {
                AndroidThemeManager.set(this, AndroidThemeManager.THEME_HORROR);
            } catch (Throwable ignored) {
            }
            new android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.horror_congrats)
                    .setMessage(R.string.horror_congrats_desc)
                    .setCancelable(false)
                    .setPositiveButton(R.string.horror_ok, (d, w) -> restartApp())
                    .show();
        });
        root.addView(unlock, lp);
        root.addView(bloodLine());

        android.widget.TextView cont = new android.widget.TextView(this);
        cont.setText(getString(R.string.err_continue));
        cont.setTextSize(14);
        cont.setTextColor(0xFF9A7B80);
        cont.setGravity(android.view.Gravity.CENTER);
        cont.setPadding(dp(16), dp(12), dp(16), dp(12));
        cont.setClickable(true);
        cont.setOnClickListener(v -> {
            AppPrefs.setBrokenTapped(this, false);
            restartApp();
        });
        android.widget.LinearLayout.LayoutParams lp2 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = dp(14);
        root.addView(cont, lp2);

        android.widget.TextView backup = new android.widget.TextView(this);
        backup.setText(getString(R.string.err_emergency_backup));
        backup.setTextSize(14);
        backup.setTextColor(0xFF9A7B80);
        backup.setGravity(android.view.Gravity.CENTER);
        backup.setPadding(dp(16), dp(12), dp(16), dp(12));
        backup.setClickable(true);
        backup.setOnClickListener(v -> {
            try {
                AppPrefs.setBrokenTapped(this, false);
                startActivity(new android.content.Intent(this, BackupActivity.class));
            } catch (Throwable ignored) {
            }
        });
        android.widget.LinearLayout.LayoutParams lp3 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.topMargin = dp(8);
        root.addView(backup, lp3);

        sv.addView(root);
        setContentView(sv);
    }

    private void restartApp() {
        CrashHandler.clearCrash(this);
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** 一条会呼吸的血线。 */
    private View bloodLine() {
        View v = new View(this);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(0xFFC62828);
        v.setBackground(g);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(18);
        v.setLayoutParams(lp);
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f, 1f);
        a.setDuration(1500);
        a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        a.start();
        return v;
    }

    private void addSpace(android.widget.LinearLayout parent, int dp) {
        android.view.View space = new android.view.View(this);
        space.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp));
        parent.addView(space);
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density + 0.5f);
    }
}