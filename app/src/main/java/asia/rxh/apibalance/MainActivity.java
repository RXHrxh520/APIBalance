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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "ds_balance_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_PROVIDER = "provider_id";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_THEME = "theme_mode"; // 0=跟随系统 1=浅色 2=深色
    private static final String PREFS_DEBUG = "ds_debug_prefs";
    private static final String KEY_DEBUG = "debug_enabled";

    private int currentProviderId = ProviderConfig.PROVIDER_DEEPSEEK;
    private String currentApiKey;
    private Handler mainHandler;
    private long pausedAt = 0;

    /** 记录当前已生效的语言，用于检测设置变化后自动重建。 */
    private int appliedLang = -1;

    @Override
    protected void attachBaseContext(Context base) {
        // 应用用户选择的语言（0=跟随 1=中文 2=English 3=繁體 4=萌语）
        int lang = AppPrefs.getLanguage(base);
        appliedLang = lang;
        if (lang >= 1 && lang <= 4) {
            android.content.res.Configuration cfg = new android.content.res.Configuration(base.getResources().getConfiguration());
            if (lang == 2) {
                cfg.setLocale(new java.util.Locale("en"));
            } else if (lang == 3) {
                cfg.setLocale(java.util.Locale.TRADITIONAL_CHINESE); // zh-rTW
            } else if (lang == 4) {
                cfg.setLocale(new java.util.Locale("eo")); // 萌语（借用 eo 语言位，萌系简中）
            } else {
                cfg.setLocale(new java.util.Locale("zh"));
            }
            base = base.createConfigurationContext(cfg);
        }
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 主题：0=跟随系统 1=浅色 2=深色
        // 关键：Android 的 @color/* 资源（values-night）跟随"系统"uiMode，而非 App 主题。
        // 所以必须手动覆盖 Configuration.uiMode，强制资源按所选模式解析，浅色/深色才能真正生效。
        int themeMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_THEME, 0);
        int nightFlag;
        if (themeMode == 2) {
            nightFlag = Configuration.UI_MODE_NIGHT_YES;
        } else if (themeMode == 1) {
            nightFlag = Configuration.UI_MODE_NIGHT_NO;
        } else if (AppPrefs.inAutoDarkHours(this)) {
            // 定时自动深色：跟随系统模式下，到点强制深色
            nightFlag = Configuration.UI_MODE_NIGHT_YES;
        } else {
            nightFlag = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        }
        Configuration cfg = new Configuration(getResources().getConfiguration());
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightFlag;
        getResources().updateConfiguration(cfg, getResources().getDisplayMetrics());
        int androidTheme = AndroidThemeManager.get(this);
        if (androidTheme != AndroidThemeManager.THEME_MATERIAL_YOU) {
            // 选了经典安卓主题：用系统内建主题（Holo / Material 深 / Material 浅），控件呈现当年系统样式
            setTheme(AndroidThemeManager.themeRes(androidTheme));
        } else {
            // 默认 Material You：按明暗模式走
            setTheme(nightFlag == Configuration.UI_MODE_NIGHT_YES
                    ? R.style.RxHTheme_Dark
                    : R.style.RxHTheme_FollowSystem);
        }

        super.onCreate(savedInstanceState);
        unlocked = sessionUnlocked || !PrivacyPrefs.isLockEnabled(this);

        // 字体大小
        float scale = AppPrefs.getFontScale(this);
        if (scale != 1.0f) {
            Configuration cfg2 = new Configuration(getResources().getConfiguration());
            cfg2.fontScale = scale;
            getResources().updateConfiguration(cfg2, getResources().getDisplayMetrics());
        }

        setContentView(R.layout.activity_main);
        applyThemeBars();

        // 主题背景（Code CLI / 复古2000 深色），否则应用安卓主题窗口背景
        int themeBg = ThemePrefs.getBackground(this);
        if (themeBg != 0) {
            View rootView = findViewById(R.id.container);
            if (rootView != null) rootView.setBackgroundColor(themeBg);
        } else {
            View rootView = findViewById(R.id.container);
            if (rootView != null) rootView.setBackgroundColor(AndroidThemeManager.windowBg(this));
        }

        // 自定义背景（fragment 根背景已透明，背景在此层显示）
        applyCustomBg();
        applyBottomNavTheme();
        mainHandler = new Handler(Looper.getMainLooper());

        // 底部导航点击
        try {
            findViewById(R.id.nav_balance).setOnClickListener(v -> switchTab(0));
            findViewById(R.id.nav_trend).setOnClickListener(v -> switchTab(1));
            findViewById(R.id.nav_settings).setOnClickListener(v -> switchTab(2));
        } catch (Exception ignored) {
        }

        // 桌面快捷方式：一键查余额
        registerQuickShortcut();
        if (AppPrefs.isPendingProQuiz(this)) {
            AppPrefs.setPendingProQuiz(this, false);
            // 等界面真正布局完再弹，onCreate 里直接弹会被吞掉
            final View quizRoot = findViewById(android.R.id.content);
            if (quizRoot != null) {
                quizRoot.post(() -> ProQuiz.start(MainActivity.this,
                        () -> AppPrefs.setProMode(MainActivity.this, true)));
            }
        }

        // Install crash handler
        CrashHandler.install(getApplicationContext());

        // Check for pending crash
        if (CrashHandler.hasPendingCrash(getApplicationContext())) {
            String[] crashInfo = CrashHandler.getCrashInfo(getApplicationContext());
            Intent crashIntent = new Intent(this, CrashActivity.class);
            crashIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            crashIntent.putExtra("crash_message", crashInfo[0]);
            crashIntent.putExtra("crash_trace", crashInfo[1]);
            startActivity(crashIntent);
            finish();
            return;
        }

        // Immersive status bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.setStatusBarColor(0x00000000);
        }

        applyPrivacyFlags();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Startup open-source notice (show once)
        if (!prefs.getBoolean("oss_notice_shown", false)) {
            prefs.edit().putBoolean("oss_notice_shown", true).apply();
            UiDialog.showConfirm(this, getString(R.string.j_136),
                    getString(R.string.j_137),
                    getString(R.string.j_138), null, null, null);
        }

        // 启动静默检查更新（24 小时节流，有新版才提示）
        try {
            long lastCheck = AppPrefs.getLastUpdateCheck(this);
            if (System.currentTimeMillis() - lastCheck > 24L * 3600 * 1000) {
                AppPrefs.setLastUpdateCheck(this, System.currentTimeMillis());
                UpdateChecker.check(this, false);
            }
        } catch (Exception ignored) {
        }

        // 运行时完整性 + 环境风险检测（只在本地进行，不采集任何信息）
        try {
            // 不再每次启动都弹，检测结果统一放到「安全中心」里看
            EnvChecker.markChecked(this);
            checkDowngrade();
        } catch (Throwable ignored) {
        }

        // 夜深了提醒（每天最多一次）
        maybeNightGreeting();

        // 从主题中心跳回来时直接打开对应分类
        handleOpenGroup();

        // 长按桌面图标「卸载软件」
        String startAction = getIntent() != null ? getIntent().getAction() : null;
        if ("asia.rxh.apibalance.UNINSTALL".equals(startAction)) {
            // 处理完必须清掉，否则 recreate()/重启后会再跳一次卸载页
            getIntent().setAction(null);
            startActivity(new android.content.Intent(this, UninstallActivity.class));
        }

        // 桌面快捷方式"一键查余额"：直接进入余额页（有 key 时）
        if ("QUICK_QUERY".equals(getIntent() != null ? getIntent().getAction() : null)) {
            int quickPid = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getInt(KEY_PROVIDER, ProviderConfig.PROVIDER_DEEPSEEK);
            String qk = CryptoManager.read(this, getProviderKeyName(quickPid));
            if (qk == null || qk.isEmpty()) {
                quickPid = ProviderConfig.PROVIDER_DEEPSEEK;
                qk = CryptoManager.read(this, getProviderKeyName(quickPid));
            }
            if (qk != null && !qk.isEmpty()) {
                currentProviderId = quickPid;
                currentApiKey = qk;
                // 安全：启用密码/指纹锁时，快捷方式也必须先验证，不得直接进余额页
                if (PrivacyPrefs.isLockEnabled(this)) {
                    showLockFragment();
                } else {
                    showBalanceFragment(qk, currentProviderId, false);
                }
                return;
            }
        }

        currentProviderId = prefs.getInt(KEY_PROVIDER, ProviderConfig.PROVIDER_DEEPSEEK);

        boolean autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false);
        String savedKey = CryptoManager.read(this, getProviderKeyName(currentProviderId));

        // 首次引导：新装且无 Key 时进入引导（老用户有 Key 不强制）
        if (!AppPrefs.isOnboardingDone(this)
                && (savedKey == null || savedKey.isEmpty())) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        if (autoLogin && savedKey != null && !savedKey.isEmpty()) {
            currentApiKey = savedKey;
            if (PrivacyPrefs.isLockEnabled(this)) {
                // 有锁：先显示锁屏，解锁后进入余额页
                showLockFragment();
            } else {
                showBalanceFragment(savedKey, currentProviderId, false);
            }
        } else {
            showLoginFragment(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausedAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 语言在别处（引导页/设置页）被改过：重建自身让新语言立刻生效
        if (appliedLang >= 0 && AppPrefs.getLanguage(this) != appliedLang) {
            recreate();
            return;
        }
        try {
            highlightTab((android.widget.LinearLayout) findViewById(R.id.bottom_nav), currentTab);
        } catch (Throwable ignored) {
        }
        // 定时自动锁：离开超过设定分钟数后，回来需重新验证
        int minutes = AppPrefs.getAutoLockMinutes(this);
        if (PrivacyPrefs.isLockEnabled(this) && pausedAt > 0) {
            // minutes <= 0 表示「每次离开都重新验证」（0 为默认值，也是安全预期的行为）。
            // 留 2 秒容错，避免系统弹窗、文件选择这类瞬时暂停也触发锁屏。
            long elapsed = System.currentTimeMillis() - pausedAt;
            long limit = minutes <= 0 ? 2000L : minutes * 60_000L;
            if (elapsed > limit) {
                android.app.Fragment f = getFragmentManager().findFragmentById(R.id.container);
                if (!(f instanceof LockFragment)) {
                    showLockFragment();
                }
            }
        }
    }

    /** 注册动态快捷方式：长按图标 → 一键查余额。 */
    private void registerQuickShortcut() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 25) {
                android.content.pm.ShortcutManager sm = getSystemService(android.content.pm.ShortcutManager.class);
                if (sm == null) return;
                Intent quick = new Intent(this, MainActivity.class).setAction("QUICK_QUERY");
                android.content.pm.ShortcutInfo info = new android.content.pm.ShortcutInfo.Builder(this, "quick_query")
                        .setShortLabel(getString(R.string.h_051))
                        .setLongLabel(getString(R.string.h_052))
                        .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_wallet))
                        .setIntent(quick)
                        .build();
                sm.setDynamicShortcuts(java.util.Collections.singletonList(info));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && "QUICK_QUERY".equals(intent.getAction())) {
            int quickPid = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getInt(KEY_PROVIDER, ProviderConfig.PROVIDER_DEEPSEEK);
            String qk = CryptoManager.read(this, getProviderKeyName(quickPid));
            if (qk == null || qk.isEmpty()) {
                quickPid = ProviderConfig.PROVIDER_DEEPSEEK;
                qk = CryptoManager.read(this, getProviderKeyName(quickPid));
            }
            if (qk != null && !qk.isEmpty()) {
                currentProviderId = quickPid;
                currentApiKey = qk;
                if (PrivacyPrefs.isLockEnabled(this)) {
                    showLockFragment();
                } else {
                    showBalanceFragment(qk, currentProviderId, true);
                }
            }
        }
    }

    public void showLockFragment() {
        unlocked = false;
        // 安全：锁屏期间隐藏底部导航，避免未解锁就切到趋势/设置
        View nav = findViewById(R.id.bottom_nav);
        if (nav != null) nav.setVisibility(View.GONE);
        LockFragment frag = new LockFragment();
        NavAnim.tab(getFragmentManager().beginTransaction()
                .replace(R.id.container, frag))
                .commitAllowingStateLoss();
    }

    /** 锁屏验证通过：进入余额页。 */
    public void onLockPassed() {
        unlocked = true;
        sessionUnlocked = true;
        updateBottomNavVisibility();
        if (currentApiKey != null && !currentApiKey.isEmpty()) {
            showBalanceFragment(currentApiKey, currentProviderId, true);
        } else {
            showLoginFragment(true);
        }
    }

    public void showLoginFragment(boolean animate) {
        updateBottomNavVisibility();
        LoginFragment frag = new LoginFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // v1.5：动画统一走 NavAnim，避免有的入口有动画、有的没有
        if (animate) {
            NavAnim.back(ft);
        } else {
            NavAnim.tab(ft);
        }
        ft.replace(R.id.container, frag).commitAllowingStateLoss();
    }

    /** 底部导航可见性：未登录时隐藏（防止未授权进入趋势/设置）。 */
    public void updateBottomNavVisibility() {
        View nav = findViewById(R.id.bottom_nav);
        if (nav != null) {
            boolean loggedIn = currentApiKey != null && !currentApiKey.isEmpty();
            nav.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        }
    }

    public void showBalanceFragment(String apiKey, int providerId, boolean animate) {
        currentApiKey = apiKey;
        currentProviderId = providerId;
        switchTab(0);
    }

    /** 底部导航：0=余额 1=趋势 2=设置 */
    public void switchTab(int tab) {
        android.widget.LinearLayout nav = (android.widget.LinearLayout) findViewById(R.id.bottom_nav);
        if (nav == null) return;
        // 安全：没解锁就一律回锁屏，任何入口都不能绕
        if (!unlocked) {
            showLockFragment();
            return;
        }
        // 安全：未登录时只能待在余额/登录页，趋势和设置都不许直接进
        boolean loggedIn = currentApiKey != null && !currentApiKey.isEmpty();
        if (!loggedIn && tab != 0) {
            tab = 0;
        }
        updateBottomNavVisibility();
        currentTab = tab;
        highlightTab(nav, tab);
        android.app.Fragment f;
        if (tab == 0) {
            if (currentApiKey != null && !currentApiKey.isEmpty()) {
                f = BalanceFragment.newInstance(currentApiKey, currentProviderId);
            } else {
                f = new LoginFragment();
            }
        } else if (tab == 1) {
            f = new HistoryFragment();
        } else {
            f = AppPrefs.isLegacyUi(this) ? new SettingsFragment() : new MineFragment();
        }
        NavAnim.tab(getFragmentManager().beginTransaction().replace(R.id.container, f))
                .commitAllowingStateLoss();
    }

    /** 数据结构比当前版本新 —— 说明用户装了旧版，引导去恢复。 */
    private void checkDowngrade() {
        try {
            int cur = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int last = AppPrefs.getLastVersionCode(this);
            if (last > cur) {
                final android.app.Activity self = this;
                UiDialog.showConfirm(this, getString(R.string.backup_downgrade),
                        getString(R.string.backup_downgrade_desc),
                        getString(R.string.backup_restore), getString(R.string.h_004),
                        () -> {
                            try {
                                startActivity(new android.content.Intent(self, BackupActivity.class));
                            } catch (Throwable ignored) {
                            }
                        }, null);
            }
            AppPrefs.setLastVersionCode(this, cur);
        } catch (Throwable ignored) {
        }
    }

    private void highlightTab(android.widget.LinearLayout nav, int tab) {
        if (nav == null) return;
        TextView[] texts = new TextView[]{
                (TextView) findViewById(R.id.nav_balance_text),
                (TextView) findViewById(R.id.nav_trend_text),
                (TextView) findViewById(R.id.nav_settings_text)
        };
        android.widget.ImageView[] icons = new android.widget.ImageView[]{
                (android.widget.ImageView) findViewById(R.id.nav_balance_icon),
                (android.widget.ImageView) findViewById(R.id.nav_trend_icon),
                (android.widget.ImageView) findViewById(R.id.nav_settings_icon)
        };
        // 旧版底栏是「设置」，切到新版才变成「我的」+ 人像图标
        boolean legacy = AppPrefs.isLegacyUi(this);
        if (texts[2] != null) {
            texts[2].setText(legacy ? getString(R.string.nav_tab_settings) : getString(R.string.mine_title));
        }
        if (icons[2] != null) {
            icons[2].setImageResource(legacy ? R.drawable.ic_settings : R.drawable.ic_person);
        }
        int accent = ThemeUtils.accent(this);
        int secondary = ThemeUtils.textSecondary(this);
        for (int i = 0; i < texts.length; i++) {
            boolean active = i == tab;
            if (texts[i] != null) {
                texts[i].setTextColor(active ? accent : secondary);
                texts[i].setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
            if (icons[i] != null) {
                icons[i].setColorFilter(active ? accent : secondary);
            }
        }
    }

    /** 底栏背景跟随当前主题。 */
    private void applyBottomNavTheme() {
        View nav = findViewById(R.id.bottom_nav);
        if (nav != null) {
            nav.setBackgroundColor(ThemeUtils.sectionBg(this));
        }
    }

    private int currentTab = 0;
    /** 密码锁是否已通过；未通过时任何跳转都必须先回锁屏。 */
    private static boolean sessionUnlocked = false;
    private boolean unlocked = true;

    public void showSettingsFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        switchTab(2);
    }

    /** 设置首页（微信式大分类）。 */
    public void showSettingsHome() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        SettingsHomeFragment frag = new SettingsHomeFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** 打开某个大分类下的设置项。 */
    public void showSettingsGroup(String group) {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        SettingsFragment frag = SettingsFragment.newInstance(group);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** 开发人员页面。 */
    public void showDevelopersFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        DevelopersFragment frag = new DevelopersFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void showDependenciesFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        DependenciesFragment frag = new DependenciesFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** 贡献者页面（v1.5：骨架屏，数据后续接入）。 */
    public void showContributorsFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        ContributorsFragment frag = new ContributorsFragment();
        NavAnim.enter(getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, frag)
                .addToBackStack(null))
                .commitAllowingStateLoss();
    }

    /** 捐赠页面（v1.5：二维码占位，不接真实数据）。 */
    public void showDonateFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        DonateFragment frag = new DonateFragment();
        NavAnim.enter(getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, frag)
                .addToBackStack(null))
                .commitAllowingStateLoss();
    }

    /** 实验性功能页面（v1.5：MD3 开关，开启前有风险协议）。 */
    public void showExperimentalFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        ExperimentalFragment frag = new ExperimentalFragment();
        NavAnim.enter(getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, frag)
                .addToBackStack(null))
                .commitAllowingStateLoss();
    }

    public void showShareFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        ShareFragment frag = new ShareFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void showHistoryFragment() {
        switchTab(1);
    }

    public void showAboutFragment() {

        if (!unlocked) {
            showLockFragment();
            return;
        }
        AboutFragment frag = new AboutFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void showDebugFragment() {
        DebugFragment frag = new DebugFragment();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left)
                .replace(R.id.container, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    /** 快捷切换服务商：若该服务商已保存 Key 则直接切换，否则回登录页。 */
    public void switchProviderQuick(int newProviderId) {
        String savedKey = CryptoManager.read(this, getProviderKeyName(newProviderId));
        if (savedKey != null && !savedKey.isEmpty()) {
            currentApiKey = savedKey;
            currentProviderId = newProviderId;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putInt(KEY_PROVIDER, newProviderId).apply();
            showBalanceFragment(savedKey, newProviderId, true);
        } else {
            // 没有该服务商的 Key，回登录页选择
            currentProviderId = newProviderId;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putInt(KEY_PROVIDER, newProviderId).apply();
            showLoginFragment(true);
        }
    }

    /** 应用隐私标志：禁止截屏 / 隐藏最近任务。 */
    public void applyPrivacyFlags() {
        if (AppPrefs.noScreenshot(this) || PrivacyPrefs.isLockEnabled(this)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /** 开启/关闭定时刷新（每小时后台查询余额）。 */
    public void scheduleAutoRefresh(boolean enable) {
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, AutoRefreshReceiver.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(this, 0, i,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        if (enable) {
            long interval = Math.max(60, AppPrefs.getRefreshIntervalSec(this)) * 1000L;
            long first = System.currentTimeMillis() + 60 * 1000L; // 1 分钟后首次
            am.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, first, interval, pi);
        } else {
            am.cancel(pi);
        }
    }

    /** 用最近余额刷新常驻通知。 */
    public void showStickyNotify() {
        java.util.List<BalanceHistoryStore.HistoryItem> items =
                BalanceHistoryStore.getAll(this);
        BalanceHistoryStore.HistoryItem last = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).providerId == currentProviderId) {
                last = items.get(i);
                break;
            }
        }
        if (last == null && !items.isEmpty()) last = items.get(items.size() - 1);
        if (last != null) {
            String symbol = "CNY".equals(last.currency) ? "¥" : "$";
            Notifier.showSticky(this, ProviderConfig.get(this, last.providerId).name,
                    symbol + String.format("%.2f", last.total));
        } else {
            Notifier.showSticky(this, "APIBalance", getString(R.string.j_139));
        }
    }

    /** 按主题风格设置状态栏/导航栏颜色。 */
    public void applyThemeBars() {
        int accent = ThemePrefs.getColor(this);
        int style = ThemePrefs.getStyle(this);
        boolean dark = ThemePrefs.usesDarkBg(this);
        boolean atDark = AndroidThemeManager.isDark(this);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(AndroidThemeManager.accent(this));
            getWindow().setNavigationBarColor(dark || atDark ? 0xFF0D1117 : 0xFFFFFFFF);
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(dark || atDark
                    ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    : View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    /** 应用自定义背景图（无则恢复主题背景）。供选图后即时刷新调用。 */
    public void applyCustomBg() {
        View root = findViewById(R.id.container);
        if (root == null) return;
        String bgPath = AppPrefs.getBgPath(this);
        if (bgPath == null) {
            // 无自定义背景：恢复主题/安卓主题背景色
            root.setBackgroundColor(ThemePrefs.getBackground(this) != 0
                    ? ThemePrefs.getBackground(this) : AndroidThemeManager.windowBg(this));
            return;
        }
        try {
            android.net.Uri bgUri = android.net.Uri.parse(bgPath);
            java.io.InputStream is;
            if ("file".equals(bgUri.getScheme())) {
                is = new java.io.FileInputStream(bgUri.getPath());
            } else {
                is = getContentResolver().openInputStream(bgUri);
            }
            if (is != null) {
                root.setBackground(android.graphics.drawable.Drawable.createFromStream(is, null));
                is.close();
            }
        } catch (Exception ignored) {
        }
    }

    /** 长按桌面图标「卸载软件」：萌一点，但绝不阻止用户卸载。 */
    public void showUninstallPrompt() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            int rid = getResources().getIdentifier("uninstall_" + i, "string", getPackageName());
            if (rid != 0) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(getString(rid));
            }
        }
        UiDialog.showConfirm(this, getString(R.string.uninstall_title), sb.toString(),
                getString(R.string.uninstall_go), getString(R.string.uninstall_keep),
                this::doUninstall, null);
    }

    private void doUninstall() {
        try {
            startActivity(new Intent(Intent.ACTION_DELETE,
                    android.net.Uri.parse("package:" + getPackageName())));
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("package:" + getPackageName())));
            } catch (Throwable ignored) {
            }
        }
    }

    /** 夜深了：每晚最多提醒一次。 */
    private void maybeNightGreeting() {
        try {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 5 && hour < 23) return;
            String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                    .format(new java.util.Date());
            SharedPreferences p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (today.equals(p.getString("night_greet_day", ""))) return;
            p.edit().putString("night_greet_day", today).apply();
            int idx = (int) (System.currentTimeMillis() % 15) + 1;
            int rid = getResources().getIdentifier("night_" + idx, "string", getPackageName());
            String text = rid == 0 ? getString(R.string.night_title) : getString(rid);
            UiDialog.showConfirm(this, getString(R.string.night_title), text,
                    getString(R.string.h_004), null, null, null);
        } catch (Throwable ignored) {
        }
    }

    /** 恢复初始状态：清空本应用的全部数据后重启到首屏。 */
    public void wipeAndRestart() {
        try {
            // 1) 私有目录整包
            java.io.File dataDir = getFilesDir().getParentFile();
            String[] subs = {"shared_prefs", "databases", "files", "cache", "no_backup", "app_webview", "code_cache"};
            for (String name : subs) {
                deleteRecursive(new java.io.File(dataDir, name));
            }
            // 2) 数据库文件本身（有的还挂在 -journal/-wal 上）
            try {
                String[] dbs = databaseList();
                if (dbs != null) {
                    for (String db : dbs) deleteDatabase(db);
                }
            } catch (Throwable ignored) {
            }
            // 3) 外部私有目录
            try {
                deleteRecursive(getExternalFilesDir(null));
                deleteRecursive(getExternalCacheDir());
            } catch (Throwable ignored) {
            }
            // 4) 其它可能落盘的位置
            for (String name : new String[]{"ds_onb_tmp", "ds_secure_prefs", "ds_privacy", "ds_app_prefs",
                    "ds_theme", "rxh_env_baseline", "balance_history", "ds_history"}) {
                try {
                    getSharedPreferences(name, MODE_PRIVATE).edit().clear().commit();
                } catch (Throwable ignored) {
                }
                try {
                    deleteDatabase(name + ".db");
                } catch (Throwable ignored) {
                }
            }
            // 5) 快捷方式
            try {
                android.content.pm.ShortcutManager sm = getSystemService(android.content.pm.ShortcutManager.class);
                if (sm != null) sm.removeAllDynamicShortcuts();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        try {
            Intent it = new Intent(this, MainActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Throwable ignored) {
        }
    }

    private void deleteRecursive(java.io.File f) {
        try {
            if (f == null || !f.exists()) return;
            if (f.isDirectory()) {
                java.io.File[] kids = f.listFiles();
                if (kids != null) {
                    for (java.io.File k : kids) deleteRecursive(k);
                }
            }
            f.delete();
        } catch (Throwable ignored) {
        }
    }

    public boolean isDebugEnabled() {
        return getSharedPreferences(PREFS_DEBUG, MODE_PRIVATE).getBoolean(KEY_DEBUG, false);
    }

    /** 关闭调试模式（调试页的「关闭调试」按钮调用）。 */
    public void setDebugEnabled(boolean enabled) {
        getSharedPreferences(PREFS_DEBUG, MODE_PRIVATE).edit().putBoolean(KEY_DEBUG, enabled).apply();
    }

    /** 登录/选择服务商成功后调用：加密保存 Key，记录服务商。 */
    public void onLoginSuccess(String apiKey, int providerId, boolean autoLogin) {
        currentApiKey = apiKey;
        currentProviderId = providerId;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_LOGIN, autoLogin);
        editor.putInt(KEY_PROVIDER, providerId);
        if (autoLogin) {
            CryptoManager.save(this, getProviderKeyName(providerId), apiKey);
        } else {
            CryptoManager.remove(this, getProviderKeyName(providerId));
        }
        editor.apply();
        updateBottomNavVisibility();
        showBalanceFragment(apiKey, providerId, true);
    }

    public void onChangeKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(KEY_AUTO_LOGIN).apply();
        CryptoManager.remove(this, getProviderKeyName(currentProviderId));
        currentApiKey = null;
        // 退出登录：停止悬浮球、隐藏底部导航
        try {
            stopService(new Intent(this, FloatingWindowService.class));
            AppPrefs.setFloatWin(this, false);
        } catch (Exception ignored) {
        }
        updateBottomNavVisibility();
        showLoginFragment(true);
    }

    /** 设置主题模式：0=跟随系统 1=浅色 2=深色，设置后重建生效。 */
    public void setThemeMode(int mode) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(KEY_THEME, mode).apply();
        recreate();
    }

    public int getThemeMode() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_THEME, 0);
    }

    public String getCurrentApiKey() {
        return currentApiKey;
    }

    public int getCurrentProviderId() {
        return currentProviderId;
    }

    /** SharedPreferences 里按服务商区分 Key 的存储名。 */
    public String getProviderKeyName(int providerId) {
        return "provider_" + providerId + "_" + KEY_API_KEY;
    }

    /** 清除某服务商已保存的 Key（用于切换时避免残留）。 */
    public void clearProviderKey(int providerId) {
        CryptoManager.remove(this, getProviderKeyName(providerId));
    }

    public void animateCardEntrance(View view, int delay) {
        view.setAlpha(0f);
        view.setTranslationY(80f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(this, R.interpolator.anticipate_overshoot))
                .start();
    }

    public void animateNumberCount(TextView tv, double from, double to, String prefix, String suffix) {
        // 动画过程用 float（视觉平滑），结束帧写回 double 精确值，避免大余额精度失真
        ValueAnimator anim = ValueAnimator.ofFloat((float) from, (float) to);
        anim.setDuration(1000);
        anim.setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(this, R.interpolator.fast_out_slow_in));
        anim.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tv.setText(String.format("%s%.2f%s", prefix, val, suffix));
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                tv.setText(String.format("%s%.2f%s", prefix, to, suffix));
            }
        });
        anim.start();
    }

    /**
     * 按服务商请求余额并解析为统一模型。
     * 网络细节统一走 {@link NetworkClient}（代理/超时/重试/TLS 恒校验）。
     * 成功回调 {@link OnBalanceFetchListener#onSuccess(int, ProviderConfig.BalanceResult)}。
     */
    public void fetchBalance(final String apiKey, final int providerId, final OnBalanceFetchListener listener) {
        new Thread(() -> {
            try {
                ProviderConfig.BalanceResult result = NetworkClient.fetch(MainActivity.this, providerId, apiKey);
                mainHandler.post(() -> listener.onSuccess(providerId, result));
            } catch (NetworkClient.HttpException e) {
                final ProviderConfig.Provider provider = ProviderConfig.get(MainActivity.this, providerId);
                mainHandler.post(() -> {
                    if (e.code == 401) {
                        listener.onError(getString(R.string.err_key_invalid));
                        Notifier.notifyBalance(MainActivity.this, getString(R.string.j_140), provider.name + getString(R.string.j_141));
                    } else if (e.code == 429) {
                        listener.onError(getString(R.string.err_too_frequent));
                    } else {
                        // 错误响应体不展示在 UI（防敏感信息回显），仅记日志
                        if (AppPrefs.isReqLog(MainActivity.this)) {
                            android.util.Log.d("APIBalance", "HTTP " + e.code + " body=" + e.body);
                        }
                        listener.onError(getString(R.string.err_network) + " (" + e.code + ")");
                    }
                });
            } catch (NetworkClient.ParseException e) {
                final ProviderConfig.Provider provider = ProviderConfig.get(MainActivity.this, providerId);
                mainHandler.post(() -> {
                    listener.onError(getString(R.string.err_parse, e.getMessage()));
                    Notifier.notifyBalance(MainActivity.this, getString(R.string.j_142), provider.name + getString(R.string.j_143));
                });
            } catch (NetworkClient.UnsupportedException e) {
                mainHandler.post(() -> listener.onError(e.getMessage()));
            } catch (java.net.SocketTimeoutException e) {
                mainHandler.post(() -> listener.onError(getString(R.string.err_timeout)));
            } catch (java.net.UnknownHostException e) {
                mainHandler.post(() -> listener.onError(getString(R.string.err_no_network)));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(getString(R.string.err_network, e.getMessage())));
            }
        }).start();
    }

    public interface OnBalanceFetchListener {
        void onSuccess(int providerId, ProviderConfig.BalanceResult result);
        void onError(String errorMessage);
    }

    private void handleOpenGroup() {
        try {
            String g = getIntent() == null ? null : getIntent().getStringExtra("open_group");
            if (g != null && !g.isEmpty()) {
                getIntent().removeExtra("open_group");
                showSettingsGroup(g);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        android.app.FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return;
        }
        // 栈空了就正常退出。之前这里会强行 switchTab(0)，
        // 导致不管在哪个界面按返回，都被拽回余额页。
        super.onBackPressed();
    }
}