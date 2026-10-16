package asia.rxh.apibalance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 独立主题中心：把原来散在设置里的主题相关项全部收进来，分好类。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public class ThemeCenterActivity extends Activity {

    private LinearLayout body;
    private int order = 0;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppPrefs.wrapLocale(base));
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_center);
        ThemeUtils.apply(findViewById(android.R.id.content));
        View back = findViewById(R.id.tc_back);
        if (back != null) back.setOnClickListener(v -> finish());
        body = findViewById(R.id.tc_body);
        build();
    }

    private void build() {
        body.removeAllViews();
        order = 0;

        header(R.string.tc_style, R.string.tc_style_desc);
        // 用真正的风格名。之前这里误用了 g_021~g_023，
        // 那三个键在加峰时文案时被覆盖，于是风格选项全变成了俏皮话。
        String[] styles = ThemePrefs.styleNames(this);
        int curStyle = ThemePrefs.getStyle(this);
        for (int i = 0; i <= 2; i++) {
            final int idx = i;
            row(styles[i], curStyle == i, v -> {
                ThemePrefs.setStyle(this, idx);
                restart();
            });
        }
        if (AppPrefs.isPink(this)) {
            row(getString(R.string.tc_pink), curStyle == ThemePrefs.STYLE_PINK, v -> {
                ThemePrefs.setStyle(this, ThemePrefs.STYLE_PINK);
                restart();
            });
        }

        header(R.string.tc_android, R.string.tc_android_desc);
        String[] names = AndroidThemeManager.namesUnlocked(this);
        int curTheme = AndroidThemeManager.get(this);
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            row(names[i], curTheme == i, v -> {
                AndroidThemeManager.set(this, idx);
                restart();
            });
        }

        header(R.string.tc_hidden, R.string.tc_hidden_desc);
        if (AppPrefs.isHorror(this)) {
            row(AndroidThemeManager.namesUnlocked(this)[4], curTheme == AndroidThemeManager.THEME_HORROR, v -> {
                AndroidThemeManager.set(this, AndroidThemeManager.THEME_HORROR);
                restart();
            });
        } else {
            row(getString(R.string.tc_locked), false, null);
        }

        header(R.string.tc_color, R.string.tc_color_desc);
        String[] cnames = ThemePrefs.colorNames(this);
        int curColor = ThemePrefs.getColorIndex(this);
        for (int i = 0; i < cnames.length; i++) {
            final int idx = i;
            row(cnames[i], curColor == i, v -> {
                ThemePrefs.setColorIndex(this, idx);
                restart();
            });
        }

        header(R.string.tc_personal, R.string.tc_personal_desc);
        row(getString(R.string.j_050), false, v -> pickBackground());
        row(getString(R.string.h_070), false, v -> pickIcon());

        header(R.string.tc_text, R.string.tc_text_desc);
        row(getString(R.string.quip_edit_title), false, v -> editQuip());
        row(getString(R.string.tc_quip_pick), false, v -> pickBuiltinQuip());
        row(getString(R.string.share_tpl_title), false, v -> pickShareTpl());
    }

    private void header(int title, int desc) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), order == 0 ? dp(8) : dp(20), dp(20), dp(8));
        TextView t = new TextView(this);
        t.setText(getString(title));
        t.setTextColor(ThemeUtils.accent(this));
        t.setTextSize(12);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setLetterSpacing(0.1f);
        col.addView(t);
        TextView d = new TextView(this);
        d.setText(getString(desc));
        d.setTextColor(ThemeUtils.textSecondary(this));
        d.setTextSize(11);
        d.setPadding(0, dp(2), 0, 0);
        col.addView(d);
        body.addView(col);
        order++;
    }

    private void row(String title, final boolean selected, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card_balance);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.leftMargin = dp(12);
        lp.rightMargin = dp(12);
        lp.topMargin = dp(6);
        card.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(ThemeUtils.textPrimary(this));
        t.setTextSize(15);
        card.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mk = new TextView(this);
        mk.setText(selected ? "\\u2713" : "");
        mk.setTextColor(ThemeUtils.accent(this));
        mk.setTextSize(17);
        card.addView(mk);

        if (click != null) {
            card.setClickable(true);
            card.setFocusable(false);
            card.setForeground(getDrawable(R.drawable.bg_ripple_24));
            card.setOnClickListener(click);
        } else {
            card.setAlpha(0.55f);
        }

        card.setAlpha(card.getAlpha() * 0f + 0f);
        body.addView(card);
        final float target = click != null ? 1f : 0.55f;
        card.setTranslationY(dp(14));
        card.animate().alpha(target).translationY(0f)
                .setStartDelay(order * 45L).setDuration(230).start();
        order++;
    }

    private static final int REQ_BG = 1001;
    private static final int REQ_ICON = 1002;

    /** 自定义背景：选图 / 恢复默认。 */
    /** 自定义卡片文案。 */
    private void editQuip() {
        UiDialog.showInputWithDesc(this, getString(R.string.quip_edit_title),
                getString(R.string.quip_edit_hint), getString(R.string.quip_edit_field),
                AppPrefs.getCustomQuip(this), t -> {
                    AppPrefs.setCustomQuip(this, t == null ? "" : t.trim());
                    android.widget.Toast.makeText(this, getString(R.string.tc_saved),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    /** 从内置文案里挑一句。 */
    private void pickBuiltinQuip() {
        final String[] q = PeakOffpeak.allQuips(this);
        UiDialog.showPickerWithDesc(this, getString(R.string.quip_edit_title),
                getString(R.string.tc_quip_pick), q, -1, i -> {
                    AppPrefs.setCustomQuip(this, q[i]);
                    android.widget.Toast.makeText(this, getString(R.string.tc_saved),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    /** 挑一套分享文案。 */
    private void pickShareTpl() {
        final String[] t = ShareFragment.TEMPLATES;
        UiDialog.showPickerWithDesc(this, getString(R.string.share_tpl_title),
                getString(R.string.tc_tpl_pick), t, AppPrefs.getShareTpl(this), i -> {
                    AppPrefs.setShareTpl(this, i);
                    android.widget.Toast.makeText(this, getString(R.string.tc_saved),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void pickBackground() {
        String[] opts = {getString(R.string.j_048), getString(R.string.j_049)};
        UiDialog.showPickerWithDesc(this, getString(R.string.j_050), getString(R.string.j_051),
                opts, 0, w -> {
                    if (w == 0) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        try {
                            startActivityForResult(pick, REQ_BG);
                        } catch (Throwable ignored) {
                        }
                    } else {
                        AppPrefs.setBgPath(this, null);
                        restart();
                    }
                });
    }

    /** 更换应用图标：选图 / 恢复默认。 */
    private void pickIcon() {
        String[] opts = {getString(R.string.h_068), getString(R.string.h_069)};
        UiDialog.showPickerWithDesc(this, getString(R.string.h_070), getString(R.string.h_071),
                opts, -1, w -> {
                    if (w == 0) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        try {
                            startActivityForResult(pick, REQ_ICON);
                        } catch (Throwable ignored) {
                        }
                    } else {
                        AppIconManager.clear(this);
                        android.widget.Toast.makeText(this, getString(R.string.h_072),
                                android.widget.Toast.LENGTH_SHORT).show();
                        restart();
                    }
                });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (req == REQ_ICON) {
                java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                if (in != null && AppIconManager.save(this, in)) {
                    restart();
                } else {
                    android.widget.Toast.makeText(this, getString(R.string.j_097),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (req == REQ_BG) {
                java.io.InputStream in = getContentResolver().openInputStream(data.getData());
                if (in == null) {
                    android.widget.Toast.makeText(this, getString(R.string.j_097),
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                java.io.File out = new java.io.File(getFilesDir(), "custom_bg.png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                fos.flush();
                fos.close();
                in.close();
                AppPrefs.setBgPath(this, out.getAbsolutePath());
                restart();
            }
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, getString(R.string.j_097),
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void restart() {
        Intent it = new Intent(this, MainActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(it);
        finish();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}

