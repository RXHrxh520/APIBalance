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

package asia.rxh.apibalance;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

/**
 * 首次打开引导：合并「登录」与「初始化设置」，6 步快速走完（约半分钟）。
 * 欢迎 → 选服务商 → 填 Key → 测试链接 → 个性化（语言/主题/专业模式） → 完成。
 * 中途「跳过」不标记完成，下次冷启动会再次弹出；走完「完成」页才写入 onBoardingDone。
 */
public class OnboardingActivity extends Activity {

    private static final String PREFS_NAME = "ds_balance_prefs";
    private static final String KEY_PROVIDER = "provider_id";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_API_KEY = "api_key";

    private FrameLayout content;
    private TextView btnSkip, btnNext;

    private int step = 0;
    private int providerId = ProviderConfig.PROVIDER_DEEPSEEK;
    private String apiKey = "";

    // 个性化选择
    private int langChoice = 1;   // 0=跟随 1=中文 2=English 3=繁體 4=萌语
    private int themeChoice = AndroidThemeManager.THEME_MATERIAL_YOU;
    private boolean proMode = false;

    // 算术题
    private int captchaAnswer;
    private final Random random = new Random();

    // 测试连接状态：0 未测 1 测中 2 成功 3 失败
    private int testState = 0;
    private String testError = "";
    private TextView tvTestStatus;
    private View progTest;

    // 步骤控件引用
    // 下拉选择改为应用风格弹窗，不再使用原生 Spinner
    private EditText etKey, etCaptcha;
    private TextView tvCaptcha;
    private Switch swPro;
    private Switch swLegacy;
    private boolean legacyUi = true;

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
        applyThemeBeforeSuper();
        super.onCreate(savedInstanceState);

        // 已走过完整引导：秒跳主界面（避免老用户重启再看到引导）
        if (AppPrefs.isOnboardingDone(this)) {
            gotoMain();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        showAgreementOnce();
        content = findViewById(R.id.onboarding_content);
        btnSkip = findViewById(R.id.btn_onboard_skip);
        btnNext = findViewById(R.id.btn_onboard_next);

        btnSkip.setOnClickListener(v -> skip());
        btnNext.setOnClickListener(v -> next());
        // 从设置里点「重新引导」时要从头开始。
        // 之前这里无条件读 ds_onb_tmp 里的 step，所以永远接着上次那一步（比如个性化）继续。
        boolean fromStart = getIntent() != null && getIntent().getBooleanExtra("from_start", false);
        if (fromStart) {
            getSharedPreferences("ds_onb_tmp", MODE_PRIVATE).edit().clear().apply();
            step = 0;
            providerId = ProviderConfig.PROVIDER_DEEPSEEK;
        }
        int resumeStep = fromStart ? 0
                : getSharedPreferences("ds_onb_tmp", MODE_PRIVATE).getInt("step", 0);
        showStep(resumeStep);
    }

    private void applyThemeBeforeSuper() {
        int themeMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt("theme_mode", 0);
        int nightFlag;
        if (themeMode == 2) nightFlag = Configuration.UI_MODE_NIGHT_YES;
        else if (themeMode == 1) nightFlag = Configuration.UI_MODE_NIGHT_NO;
        else nightFlag = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        Configuration cfg = new Configuration(getResources().getConfiguration());
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightFlag;
        getResources().updateConfiguration(cfg, getResources().getDisplayMetrics());

        int androidTheme = AndroidThemeManager.get(this);
        if (androidTheme != AndroidThemeManager.THEME_MATERIAL_YOU) {
            setTheme(AndroidThemeManager.themeRes(androidTheme));
        } else {
            setTheme(nightFlag == Configuration.UI_MODE_NIGHT_YES
                    ? R.style.RxHTheme_Dark
                    : R.style.RxHTheme_FollowSystem);
        }
    }

    private void gotoMain() {
        // 关键：必须 CLEAR_TASK 重建 MainActivity。
        // 用 CLEAR_TOP|SINGLE_TOP 会复用旧实例，主题/语言/新旧版设置全部不会生效。
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // v1.5：从设置里重新运行引导时，按返回或侧滑不应回到设置页（那样等于白引导一次），
        // 统一回主界面并清掉旧任务栈。
        gotoMain();
    }

    private void skip() {
        // 跳过也要把当前这一屏选好的东西存下来，否则用户会觉得跳过根本没用
        try {
            readPersonalize();
            // 跳过时专业模式已经在开关那一步答过题，别再丢「待答题」给主界面
            AppPrefs.setProMode(this, proMode);
            AppPrefs.setPendingProQuiz(this, false);
            AppPrefs.setLegacyUi(this, legacyUi);
        } catch (Throwable ignored) {
        }
        AppPrefs.setOnboardingDone(this, true);
        gotoMain();
    }

    private void next() {
        switch (step) {
            case 0:
                showStep(1);
                break;
            case 1:
                showStep(2);
                break;
            case 2:
                if (validateKeyAndCaptcha()) {
                    showStep(3);
                    startTest();
                }
                break;
            case 3:
                if (testState == 2) {
                    showStep(4);
                } else if (testState == 3) {
                    startTest(); // 失败重试
                }
                break;
            case 4:
                showStep(5);
                break;
            case 5:
                readPersonalize();
                showStep(6);
                break;
            case 6:
                finishOnboarding();
                break;
        }
    }

    private void showStep(int s) {
        step = s;
        content.removeAllViews();
        tvTestStatus = null;
        progTest = null;
        View v = buildStep(s);
        if (v != null) content.addView(v);
        animateIn(v);

        // 第 2 屏要填密钥、第 3 屏要跑测试，这两屏不能跳过；最后一屏也没有跳过
        boolean canSkip = (s == 0 || s == 1 || s == 4 || s == 5);
        if (s == 6) {
            btnSkip.setVisibility(View.GONE);
            btnNext.setText(getString(R.string.onb_done_btn));
        } else {
            btnSkip.setVisibility(canSkip ? View.VISIBLE : View.GONE);
            btnNext.setText(s == 3 && testState == 3 ? getString(R.string.onb_retry) : getString(R.string.onb_next));
        }
    }

    private void animateIn(View v) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(40f);
        v.animate().alpha(1f).translationY(0f)
                .setDuration(380)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animatePop(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(delay).setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private LinearLayout page(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(8);
        root.setPadding(pad, dp(20), pad, pad);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(26);
        tvTitle.setTextColor(ThemeUtils.textPrimary(this));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(subtitle);
            tvSub.setTextSize(14);
            tvSub.setTextColor(ThemeUtils.textSecondary(this));
            tvSub.setGravity(Gravity.CENTER);
            tvSub.setPadding(0, dp(10), 0, dp(18));
            root.addView(tvSub);
        }
        return root;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(ThemeUtils.textSecondary(this));
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ---- 6 个步骤 ----

    private View buildStep(int s) {
        switch (s) {
            case 0: return buildWelcome();
            case 1: return buildProvider();
            case 2: return buildKey();
            case 3: return buildTest();
            case 4: return buildPermission();
            case 5: return buildPersonalize();
            case 6: return buildDone();
            default: return null;
        }
    }

    /** 应用风格的下拉选择行（替代原生 Spinner 的古老外观）。 */
    private TextView pickRow(LinearLayout root, String[] options, int selected) {
        TextView tv = new TextView(this);
        tv.setText(options[selected]);
        tv.setTextSize(15);
        tv.setTextColor(ThemeUtils.textPrimary(this));
        tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tv.setPadding(dp(16), 0, dp(16), 0);
        tv.setBackgroundResource(R.drawable.bg_input_field);
        android.graphics.drawable.Drawable chev = getDrawable(R.drawable.ic_chevron_down);
        if (chev != null) chev.setTint(ThemeUtils.textSecondary(this));
        tv.setCompoundDrawablesWithIntrinsicBounds(null, null, chev, null);
        tv.setCompoundDrawablePadding(dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.topMargin = dp(12);
        root.addView(tv, lp);
        return tv;
    }

    private View buildWelcome() {
        LinearLayout root = page(getString(R.string.onb_welcome_title), getString(R.string.onb_welcome_sub));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_sparkle);
        logo.setColorFilter(ThemeUtils.accent(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(96));
        lp.bottomMargin = dp(24);
        root.addView(logo, lp);
        animatePop(logo, 60);
        return root;
    }

    private View buildProvider() {
        LinearLayout root = page(getString(R.string.onb_provider_title), getString(R.string.onb_provider_sub));

        String[] base = ProviderConfig.names(this);
        int[] ids = ProviderConfig.pickableIds();
        String[] names = new String[ids.length + 1];
        for (int i = 0; i < ids.length; i++) {
            names[i] = (ids[i] >= 0 && ids[i] < base.length) ? base[ids[i]] : "";
        }
        names[ids.length] = getString(R.string.onb_custom_entry);

        final String[] opts = names;
        final int finalCount = ids.length;
        int cur = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == providerId) cur = i;
        }
        final int initSel = cur;
        final TextView tvProvider = pickRow(root, opts, initSel);
        tvProvider.setOnClickListener(v -> UiDialog.showPicker(this,
                getString(R.string.onb_provider_title), opts, initSel, pos -> {
                    if (pos == finalCount) {
                        showCustomProviderEditor();
                        return;
                    }
                    providerId = ids[pos];
                    tvProvider.setText(opts[pos]);
                }));
        return root;
    }

    private View buildKey() {
        LinearLayout root = page(getString(R.string.onb_key_title), getString(R.string.onb_key_sub));

        etKey = new EditText(this);
        etKey.setHint("sk-…");
        etKey.setSingleLine(true);
        etKey.setTextSize(15);
        etKey.setBackgroundResource(R.drawable.bg_input_field);
        etKey.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        root.addView(etKey, klp);

        root.addView(label(getString(R.string.onb_captcha_label)));

        LinearLayout capRow = new LinearLayout(this);
        capRow.setOrientation(LinearLayout.HORIZONTAL);
        capRow.setGravity(Gravity.CENTER_VERTICAL);

        tvCaptcha = new TextView(this);
        tvCaptcha.setTextSize(18);
        tvCaptcha.setTextColor(ThemeUtils.textPrimary(this));
        tvCaptcha.setTypeface(null, android.graphics.Typeface.BOLD);
        capRow.addView(tvCaptcha);

        etCaptcha = new EditText(this);
        etCaptcha.setHint(getString(R.string.h_053));
        etCaptcha.setSingleLine(true);
        etCaptcha.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etCaptcha.setBackgroundResource(R.drawable.bg_input_field);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(100), dp(52));
        clp.leftMargin = dp(12);
        capRow.addView(etCaptcha, clp);
        root.addView(capRow);

        refreshCaptcha();
        return root;
    }

    private void refreshCaptcha() {
        int a = random.nextInt(10) + 1; // 1-10，无零
        int b = random.nextInt(10) + 1;
        captchaAnswer = a + b;
        if (tvCaptcha != null) tvCaptcha.setText(a + " + " + b + " = ?");
    }

    private boolean validateKeyAndCaptcha() {
        String key = etKey.getText().toString().trim();
        if (key.isEmpty()) {
            toast(getString(R.string.h_054));
            return false;
        }
        String ans = etCaptcha.getText().toString().trim();
        try {
            if (Integer.parseInt(ans) != captchaAnswer) {
                toast(getString(R.string.h_055));
                refreshCaptcha();
                etCaptcha.setText("");
                return false;
            }
        } catch (Exception e) {
            toast(getString(R.string.h_056));
            return false;
        }
        apiKey = key;
        return true;
    }

    private View buildTest() {
        LinearLayout root = page(getString(R.string.onb_test_title), getString(R.string.onb_test_sub));

        progTest = new android.widget.ProgressBar(this);
        progTest.setVisibility(View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(44), dp(44));
        plp.topMargin = dp(18);
        root.addView(progTest, plp);

        tvTestStatus = new TextView(this);
        tvTestStatus.setTextSize(15);
        tvTestStatus.setTextColor(ThemeUtils.textSecondary(this));
        tvTestStatus.setGravity(Gravity.CENTER);
        tvTestStatus.setPadding(dp(12), dp(18), dp(12), 0);
        root.addView(tvTestStatus);

        updateTestUi();
        return root;
    }

    private void startTest() {
        testState = 1;
        testError = "";
        updateTestUi();
        new Thread(() -> {
            try {
                ProviderConfig.BalanceResult r = NetworkClient.fetch(OnboardingActivity.this, providerId, apiKey);
                runOnUiThread(() -> {
                    testState = 2;
                    testError = String.format("%.2f %s", r.totalBalance, r.currency);
                    updateTestUi();
                    btnNext.setText(getString(R.string.onb_next));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    testState = 3;
                    testError = e.getMessage();
                    updateTestUi();
                    btnNext.setText(getString(R.string.onb_retry));
                });
            }
        }).start();
    }

    private void updateTestUi() {
        if (tvTestStatus == null) return;
        if (progTest != null) progTest.setVisibility(View.GONE);
        switch (testState) {
            case 1:
                if (progTest != null) progTest.setVisibility(View.VISIBLE);
                tvTestStatus.setText(getString(R.string.onb_connecting));
                tvTestStatus.setTextColor(ThemeUtils.textSecondary(this));
                break;
            case 2:
                tvTestStatus.setText(getString(R.string.onb_test_ok, testError));
                tvTestStatus.setTextColor(ThemeUtils.accent(this));
                break;
            case 3:
                tvTestStatus.setText(getString(R.string.onb_test_fail, testError));
                tvTestStatus.setTextColor(ThemeUtils.textPrimary(this));
                break;
        }
    }

    private View buildPermission() {
        LinearLayout root = page(getString(R.string.onb_perm_title), getString(R.string.onb_perm_sub));
        boolean notif = true;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notif = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        final boolean overlay = android.provider.Settings.canDrawOverlays(this);
        TextView tvN = new TextView(this);
        tvN.setTextSize(15);
        tvN.setTextColor(ThemeUtils.textPrimary(this));
        tvN.setPadding(0, dp(10), 0, dp(10));
        tvN.setText(getString(R.string.onb_perm_notif) + "：" + (notif ? getString(R.string.onb_perm_granted) : getString(R.string.onb_perm_denied)));
        root.addView(tvN);
        TextView tvO = new TextView(this);
        tvO.setTextSize(15);
        tvO.setTextColor(ThemeUtils.textPrimary(this));
        tvO.setPadding(0, dp(10), 0, dp(10));
        tvO.setText(getString(R.string.onb_perm_overlay) + "：" + (overlay ? getString(R.string.onb_perm_granted) : getString(R.string.onb_perm_denied)));
        root.addView(tvO);
        TextView tvTip = new TextView(this);
        tvTip.setTextSize(13);
        tvTip.setTextColor(ThemeUtils.textSecondary(this));
        tvTip.setPadding(0, dp(6), 0, dp(6));
        tvTip.setText(getString(R.string.onb_perm_tip));
        root.addView(tvTip);
        TextView btn = new TextView(this);
        btn.setTextSize(15);
        btn.setTextColor(ThemeUtils.accent(this));
        btn.setPadding(0, dp(16), 0, 0);
        btn.setText(getString(R.string.onb_perm_open));
        btn.setOnClickListener(v -> {
            try {
                android.content.Intent i = new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(i);
            } catch (Exception ex) {
                try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (Exception ignored) { }
            }
        });
        root.addView(btn);
        return root;
    }

    private View buildPersonalize() {
        LinearLayout root = page(getString(R.string.onb_pers_title), getString(R.string.onb_pers_sub));

        root.addView(label(getString(R.string.h_057)));
        final String[] langs = new String[]{getString(R.string.h_058), getString(R.string.h_059),
                "English", getString(R.string.h_060), getString(R.string.h_061)};
        final TextView tvLang = pickRow(root, langs, langChoice);
        tvLang.setOnClickListener(v -> UiDialog.showPicker(this, getString(R.string.h_057),
                langs, langChoice, pos -> {
                    langChoice = pos;
                    tvLang.setText(langs[pos]);
                    AppPrefs.setLanguage(OnboardingActivity.this, pos);
                    // 重建自己让新语言立刻生效；记下当前步，重建后回到原处
                    getSharedPreferences("ds_onb_tmp", MODE_PRIVATE).edit()
                            .putInt("step", step).apply();
                    recreate();
                }));

        root.addView(label(getString(R.string.h_062)));
        final String[] themes = AndroidThemeManager.names(this);
        final TextView tvTheme = pickRow(root, themes, themeChoice);
        tvTheme.setOnClickListener(v -> UiDialog.showPicker(this, getString(R.string.h_062),
                themes, themeChoice, pos -> {
                    themeChoice = pos;
                    tvTheme.setText(themes[pos]);
                }));

        LinearLayout proRow = new LinearLayout(this);
        proRow.setOrientation(LinearLayout.HORIZONTAL);
        proRow.setGravity(Gravity.CENTER_VERTICAL);
        proRow.setPadding(0, dp(16), 0, 0);

        LinearLayout proText = new LinearLayout(this);
        proText.setOrientation(LinearLayout.VERTICAL);
        TextView proTitle = new TextView(this);
        proTitle.setText(getString(R.string.onb_pro_title));
        proTitle.setTextSize(15);
        proTitle.setTextColor(ThemeUtils.textPrimary(this));
        proTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        proText.addView(proTitle);
        TextView proDesc = new TextView(this);
        proDesc.setText(getString(R.string.onb_pro_desc));
        proDesc.setTextSize(12);
        proDesc.setTextColor(ThemeUtils.textSecondary(this));
        proText.addView(proDesc);
        proRow.addView(proText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        swPro = new Switch(this);
        swPro.setChecked(proMode);
        // 一打开就当场答题。之前把答题拖到「完成」那一步，
        // 结果引导页看着是开的、最后一刻才弹题，答完还卡住出不去。
        swPro.setOnCheckedChangeListener((b, checked) -> {
            if (checked && !proMode) {
                // 先退回关闭，答对了再打开
                swPro.setChecked(false);
                ProQuiz.start(OnboardingActivity.this,
                        () -> {
                            proMode = true;
                            swPro.setChecked(true);
                        }, null);
                return;
            }
            proMode = checked;
        });
        proRow.addView(swPro);
        root.addView(proRow);

        // 界面版本：想进新版还是旧版，让用户自己定
        LinearLayout uiRow = new LinearLayout(this);
        uiRow.setOrientation(LinearLayout.HORIZONTAL);
        uiRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        uiRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout uiText = new LinearLayout(this);
        uiText.setOrientation(LinearLayout.VERTICAL);
        TextView uiTitle = new TextView(this);
        uiTitle.setText(getString(R.string.onb_ui_title));
        uiTitle.setTextSize(15);
        uiTitle.setTextColor(ThemeUtils.textPrimary(this));
        uiTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        uiText.addView(uiTitle);
        TextView uiDesc = new TextView(this);
        uiDesc.setText(getString(R.string.onb_ui_desc));
        uiDesc.setTextSize(12);
        uiDesc.setTextColor(ThemeUtils.textSecondary(this));
        uiText.addView(uiDesc);
        uiRow.addView(uiText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        swLegacy = new Switch(this);
        swLegacy.setChecked(legacyUi);
        swLegacy.setText(getString(R.string.onb_ui_legacy));
        uiRow.addView(swLegacy);
        root.addView(uiRow);
        return root;
    }

    private void readPersonalize() {
        // langChoice / themeChoice 已由下拉弹窗回调直接更新
        if (swPro != null) proMode = swPro.isChecked();
        if (swLegacy != null) legacyUi = swLegacy.isChecked();
        if (themeChoice < AndroidThemeManager.THEME_KITKAT) themeChoice = AndroidThemeManager.THEME_MATERIAL_YOU;
        if (themeChoice > AndroidThemeManager.THEME_MATERIAL_YOU) themeChoice = AndroidThemeManager.THEME_MATERIAL_YOU;
    }

    private View buildDone() {
        LinearLayout root = page(getString(R.string.onb_done_title), getString(R.string.onb_done_sub));

        ImageView check = new ImageView(this);
        check.setImageResource(R.drawable.ic_check_circle);
        check.setColorFilter(ThemeUtils.accent(this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(88), dp(88));
        lp.bottomMargin = dp(20);
        root.addView(check, lp);
        animatePop(check, 80);
        return root;
    }

    private void finishOnboarding() {
        // 保存 Key 与所选服务商（与 MainActivity 同一份存储）
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_PROVIDER, providerId)
                .putBoolean(KEY_AUTO_LOGIN, true)
                .apply();
        CryptoManager.save(this, "provider_" + providerId + "_" + KEY_API_KEY, apiKey);

        // 保存个性化
        AppPrefs.setLanguage(this, langChoice);
        AndroidThemeManager.set(this, themeChoice);
        // 引导页开启专业模式同样必须先答对 5 道 API 安全题
        // 专业模式当场答题，答对才开。
        // 之前是把「待答题」标记丢给 MainActivity，结果引导页看着是开的、
        // 进软件却是关的，切到新版设置还会再弹一次，前后矛盾。
        // 专业模式在开关那一步就答过题了，这里只落状态
        AppPrefs.setProMode(this, proMode);
        AppPrefs.setPendingProQuiz(this, false);
        AppPrefs.setLegacyUi(this, legacyUi);

        // 标记完成：走完此页才不再弹
        AppPrefs.setOnboardingDone(this, true);
        gotoMain();
    }

    private void showCustomProviderEditor() {
        UiDialog.showInput(this, getString(R.string.custom_title), getString(R.string.custom_name_hint),
                AppPrefs.getCustomProviderName(this), name -> {
            UiDialog.showInput(this, getString(R.string.custom_title), getString(R.string.custom_url_hint),
                    AppPrefs.getCustomProviderUrl(this), url -> {
                UiDialog.showPicker(this, getString(R.string.custom_auth),
                        new String[]{getString(R.string.custom_auth_bearer), getString(R.string.custom_auth_xkey)},
                        AppPrefs.getCustomProviderAuth(this), auth -> {
                    UiDialog.showInput(this, getString(R.string.h_050),
                            getString(R.string.custom_path_hint),
                            AppPrefs.getCustomProviderPath(this), path -> {
                        String err = ProviderConfig.validateCustom(this,name, url, auth, path, "CNY");
                        if (err != null) {
                            toast(err);
                            return;
                        }
                        AppPrefs.setCustomProviderName(this, name.trim());
                        AppPrefs.setCustomProviderUrl(this, url.trim());
                        AppPrefs.setCustomProviderAuth(this, auth);
                        AppPrefs.setCustomProviderCurrency(this, "CNY");
                        AppPrefs.setCustomProviderPath(this, path.trim());
                        providerId = ProviderConfig.PROVIDER_CUSTOM;
                        toast(getString(R.string.custom_saved) + name.trim());
                        showStep(1); // 刷新下拉
                    });
                });
            });
        });
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private static final String AGREEMENT_KEY = "onb_agreement_ok";

    /** v1.5：首次进入引导页前先展示用户协议与隐私政策，不同意则直接退出。 */
    private void showAgreementOnce() {
        try {
            if (getSharedPreferences("ds_onb_tmp", MODE_PRIVATE)
                    .getBoolean(AGREEMENT_KEY, false)) {
                return;
            }
            String body = getString(R.string.agreement_user) + "\n\n"
                    + getString(R.string.agreement_privacy);
            UiDialog.showConfirm(this,
                    getString(R.string.agreement_title),
                    body,
                    getString(R.string.agreement_agree),
                    getString(R.string.agreement_reject),
                    () -> getSharedPreferences("ds_onb_tmp", MODE_PRIVATE)
                            .edit().putBoolean(AGREEMENT_KEY, true).apply(),
                    this::finish);
        } catch (Throwable ignored) {
        }
    }
}
