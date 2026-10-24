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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsFragment extends Fragment {

    /** 大分类标识。数据统一放在 SettingsCatalog，避免和设置首页各维护一份。 */
    public static final String[] GROUPS = SettingsCatalog.GROUPS;

    private String groupFilter;
    private boolean proEnabled;

    public static SettingsFragment newInstance(String group) {
        SettingsFragment f = new SettingsFragment();
        Bundle b = new Bundle();
        b.putString("group", group);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) groupFilter = args.getString("group");
    }


    /** 绑定新增的隐私 / 外观行：锁定时间、验证方式、F2a、旧版开关。 */
    private void bindExtraRows(View view) {
        View btnDelay = view.findViewById(R.id.btn_lock_delay);
        final TextView tvDelay = view.findViewById(R.id.tv_lock_delay_desc);
        if (btnDelay != null) {
            showDelay(tvDelay);
            btnDelay.setOnClickListener(v -> {
                final int[] mins = {0, 1, 5, 15, 30, 60};
                String[] opts = new String[mins.length];
                for (int i = 0; i < mins.length; i++) {
                    opts[i] = mins[i] == 0 ? getString(R.string.mins_always) : (mins[i] + " " + getString(R.string.mins_unit));
                }
                int cur = PrivacyPrefs.getLockDelay(getActivity());
                int sel = 0;
                for (int i = 0; i < mins.length; i++) if (mins[i] == cur) sel = i;
                UiDialog.showPicker(getActivity(), getString(R.string.lock_delay_title), opts, sel, which -> {
                    PrivacyPrefs.setLockDelay(getActivity(), mins[which]);
                    showDelay(tvDelay);
                });
            });
        }
        View btnMethod = view.findViewById(R.id.btn_lock_method);
        final TextView tvMethod = view.findViewById(R.id.tv_lock_method_desc);
        if (btnMethod != null) {
            showMethod(tvMethod);
            btnMethod.setOnClickListener(v -> {
                // 系统原生验证已下线，验证方式里只保留软件密码
                String[] opts = {getString(R.string.lock_method_app)};
                UiDialog.showPicker(getActivity(), getString(R.string.lock_method_title), opts,
                        0, which -> {
                            PrivacyPrefs.setLockMethod(getActivity(), 0);
                            showMethod(tvMethod);
                        });
            });
        }
        View btnF2a = view.findViewById(R.id.btn_f2a);
        final TextView tvF2a = view.findViewById(R.id.tv_f2a_desc);
        if (btnF2a != null) {
            showF2a(tvF2a);
            btnF2a.setOnClickListener(v -> {
                final Runnable doSet = () -> UiDialog.showInputWithDesc(getActivity(),
                        getString(R.string.f2a_set_title), getString(R.string.f2a_set_hint),
                        getString(R.string.f2a_set_hint), "", code -> {
                            PrivacyPrefs.setF2a(getActivity(), code == null ? "" : code.trim());
                            showF2a(tvF2a);
                        });
                // 恢复代码等于一把备用钥匙，改它之前先过一遍锁屏密码
                if (PrivacyPrefs.hasPassword(getActivity())) {
                    UiDialog.showInputWithDesc(getActivity(),
                            getString(R.string.verify_pwd_title),
                            getString(R.string.verify_pwd_desc),
                            getString(R.string.f2a_row_hint), "", pw -> {
                                if (PrivacyPrefs.checkPassword(getActivity(), pw)) {
                                    doSet.run();
                                } else {
                                    android.widget.Toast.makeText(getActivity(),
                                            getString(R.string.verify_pwd_wrong),
                                            android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    doSet.run();
                }
            });
        }
        View btnLegacy = view.findViewById(R.id.btn_legacy_toggle);
        final TextView tvLegacy = view.findViewById(R.id.tv_legacy_desc);
        if (btnLegacy != null) {
            showLegacy(btnLegacy, tvLegacy);
            btnLegacy.setOnClickListener(v -> {
                boolean toNew = AppPrefs.isLegacyUi(getActivity());
                AppPrefs.setLegacyUi(getActivity(), !toNew);
                android.widget.Toast.makeText(getActivity(),
                        getString(toNew ? R.string.legacy_switched_new : R.string.legacy_switched_old),
                        android.widget.Toast.LENGTH_SHORT).show();
                // 整套界面（含底栏文案图标）要重建才生效
                try {
                    Intent it = new Intent(getActivity(), MainActivity.class);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    getActivity().finish();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private void showDelay(TextView tv) {
        if (tv == null) return;
        int m = PrivacyPrefs.getLockDelay(getActivity());
        tv.setText(m == 0 ? getString(R.string.mins_always) : (m + " " + getString(R.string.mins_unit)));
    }

    private void showMethod(TextView tv) {
        if (tv == null) return;
        tv.setText(PrivacyPrefs.getLockMethod(getActivity()) == 1
                ? getString(R.string.lock_method_app) : getString(R.string.lock_method_app));
    }

    private void showF2a(TextView tv) {
        if (tv == null) return;
        tv.setText(PrivacyPrefs.hasF2a(getActivity())
                ? getString(R.string.f2a_row_set) : getString(R.string.f2a_row_unset));
    }

    /** 旧版开关：这一行在旧版页面显示「返回新版」，在新版页面显示「返回旧版」。 */
    private void showLegacy(View row, TextView tv) {
        if (tv != null) tv.setText(getString(R.string.legacy_desc));
        try {
            View parent = (View) row;
            android.view.ViewGroup g = (android.view.ViewGroup) parent;
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child instanceof android.view.ViewGroup) {
                    android.view.ViewGroup inner = (android.view.ViewGroup) child;
                    for (int k = 0; k < inner.getChildCount(); k++) {
                        View t = inner.getChildAt(k);
                        if (t instanceof TextView && t.getId() != R.id.tv_legacy_desc) {
                            ((TextView) t).setText(AppPrefs.isLegacyUi(getActivity())
                                    ? getString(R.string.legacy_to_new) : getString(R.string.legacy_to_old));
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 按大分类过滤：只显示本分类的行，同时尊重专业模式的开关状态。
     * 用 getIdentifier 取 id，避免因为个别 id 名称变化导致编译失败。
     */
    private void applyGroupFilter(View root) {
        if (root == null) return;
        // 崩坏入口必须强制只出现在「关于」里。
        // 旧版会直接打开设置明细页，那时 groupFilter 是 null，原来这里直接 return，
        // 于是崩坏那一行在每个设置页都露出来了。
        String bpkg = getActivity() == null ? "asia.rxh.apibalance" : getActivity().getPackageName();
        int brid = getResources().getIdentifier("row_broken", "id", bpkg);
        if (brid != 0) {
            View bv = root.findViewById(brid);
            if (bv != null) {
                bv.setVisibility("about".equals(groupFilter) ? View.VISIBLE : View.GONE);
            }
        }
        if (groupFilter == null) return;
        String[] keep = SettingsCatalog.rowsOf(groupFilter);
        java.util.Set<String> keepSet = keep == null
                ? java.util.Collections.<String>emptySet()
                : new java.util.HashSet<String>(java.util.Arrays.asList(keep));
        String pkg = getActivity() == null ? "asia.rxh.apibalance" : getActivity().getPackageName();
        java.util.Set<String> always = java.util.Collections.emptySet();
        for (String name : SettingsCatalog.ALL_ROWS) {
            int rid = getResources().getIdentifier(name, "id", pkg);
            if (rid == 0) continue;
            View v = root.findViewById(rid);
            if (v == null) continue;
            boolean visible = keepSet.contains(name) || always.contains(name);
            if ("row_broken".equals(name)) visible = "about".equals(groupFilter);
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        // 分节标题和页脚不属于任何分类，进分类页时一起藏掉
        for (int i = 1; i <= 7; i++) {
            int sid = getResources().getIdentifier("sec_hdr_" + i, "id", pkg);
            if (sid == 0) continue;
            View sv = root.findViewById(sid);
            if (sv != null) sv.setVisibility(View.GONE);
        }
        int fid = getResources().getIdentifier("tv_oss_footer", "id", pkg);
        if (fid != 0) {
            View fv = root.findViewById(fid);
            if (fv != null) fv.setVisibility(View.GONE);
        }
        // 新版分类页里同样用五颜六色的图标
        final int[] PALETTE_CAT = {0xFF6750A4, 0xFF0EA5E9, 0xFF10B981, 0xFFF59E0B,
                0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899, 0xFF14B8A6, 0xFFF97316, 0xFF6366F1};
        String[] tint = SettingsCatalog.rowsOf(groupFilter);
        if (tint != null) {
            for (int i = 0; i < tint.length; i++) {
                int rid2 = getResources().getIdentifier(tint[i], "id", pkg);
                if (rid2 == 0) continue;
                View row = root.findViewById(rid2);
                if (!(row instanceof android.view.ViewGroup)) continue;
                android.view.ViewGroup g = (android.view.ViewGroup) row;
                for (int k = 0; k < g.getChildCount(); k++) {
                    View ch = g.getChildAt(k);
                    if (ch instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) ch).setColorFilter(PALETTE_CAT[i % PALETTE_CAT.length]);
                        break;
                    }
                }
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        View btnChangeKey = view.findViewById(R.id.btn_change_key);
        View btnSwitchProvider = view.findViewById(R.id.btn_switch_provider);
        View btnAccounts = view.findViewById(R.id.btn_accounts);
        View btnHistory = view.findViewById(R.id.btn_history);
        View btnTheme = view.findViewById(R.id.btn_theme);
        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> startActivity(
                    new android.content.Intent(getActivity(), ThemeCenterActivity.class)));
        }
        View btnTimeout = view.findViewById(R.id.btn_timeout);
        View btnCurrency = view.findViewById(R.id.btn_currency);
        View btnClearData = view.findViewById(R.id.btn_clear_data);
        View btnTokenEst = view.findViewById(R.id.btn_token_est);
        View btnAdvancedNet = view.findViewById(R.id.btn_advanced_net);
        View btnFontSize = view.findViewById(R.id.btn_font_size);
        // v1.5：进阶分类下的实验性功能入口
        View btnExperimental = view.findViewById(R.id.btn_experimental);
        if (btnExperimental != null) {
            btnExperimental.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showExperimentalFragment();
            });
        }
        View btnCustomBg = view.findViewById(R.id.btn_custom_bg);
        if (btnCustomBg != null) {
            btnCustomBg.setOnClickListener(v -> startActivity(
                    new android.content.Intent(getActivity(), ThemeCenterActivity.class)));
        }
        View btnIcon = view.findViewById(R.id.btn_icon);
        View btnAndroidTheme = view.findViewById(R.id.btn_android_theme);
        if (btnAndroidTheme != null) {
            btnAndroidTheme.setOnClickListener(v -> startActivity(
                    new android.content.Intent(getActivity(), ThemeCenterActivity.class)));
        }
        TextView tvFontSize = view.findViewById(R.id.tv_font_size);
        TextView tvBgDesc = view.findViewById(R.id.tv_bg_desc);
        View btnProxy = view.findViewById(R.id.btn_proxy);
        Switch swProxy = view.findViewById(R.id.sw_proxy);
        TextView tvProxyDesc = view.findViewById(R.id.tv_proxy_desc);
        TextView tvTimeoutDesc = view.findViewById(R.id.tv_timeout_desc);
        TextView tvCurrencyDesc = view.findViewById(R.id.tv_currency_desc);
        Switch swLock = view.findViewById(R.id.sw_lock);
        Switch swHideBalance = view.findViewById(R.id.sw_hide_balance);
        Switch swNoScreenshot = view.findViewById(R.id.sw_no_screenshot);
        Switch swNoNotify = view.findViewById(R.id.sw_no_notify);
        Switch swFloat = view.findViewById(R.id.sw_float);
        View rowFloat = view.findViewById(R.id.row_float);
        Switch swAlert = view.findViewById(R.id.sw_alert);
        Switch swAutoRefresh = view.findViewById(R.id.sw_auto_refresh);
        Switch swQuiet = view.findViewById(R.id.sw_quiet);
        Switch swSticky = view.findViewById(R.id.sw_sticky);
        TextView tvQuietDesc = view.findViewById(R.id.tv_quiet_desc);
        View rowQuiet = view.findViewById(R.id.row_quiet);
        View rowSticky = view.findViewById(R.id.row_sticky);
        TextView tvAlertDesc = view.findViewById(R.id.tv_alert_desc);
        View rowAlert = view.findViewById(R.id.row_alert);
        View rowAutoRefresh = view.findViewById(R.id.row_auto_refresh);
        View rowLock = view.findViewById(R.id.row_lock);
        View rowHideBalance = view.findViewById(R.id.row_hide_balance);
        View rowNoScreenshot = view.findViewById(R.id.row_no_screenshot);
        View rowNoNotify = view.findViewById(R.id.row_no_notify);
        TextView tvThemeMode = view.findViewById(R.id.tv_theme_mode);
        View btnLang = view.findViewById(R.id.btn_lang);
        TextView tvLangDesc = view.findViewById(R.id.tv_lang_desc);
        View btnOnboarding = view.findViewById(R.id.btn_onboarding);
        View rowProMode = view.findViewById(R.id.row_pro_mode);
        Switch swProMode = view.findViewById(R.id.sw_pro_mode);
        View btnProHelp = view.findViewById(R.id.btn_pro_help);
        View btnPermissionCenter = view.findViewById(R.id.btn_permission_center);
        View rowAutoDark = view.findViewById(R.id.row_auto_dark);
        Switch swAutoDark = view.findViewById(R.id.sw_auto_dark);
        TextView tvAutoDarkDesc = view.findViewById(R.id.tv_auto_dark_desc);
        View rowAutoLock = view.findViewById(R.id.row_auto_lock);
        TextView tvAutoLockDesc = view.findViewById(R.id.tv_auto_lock_desc);
        View btnAbout = view.findViewById(R.id.btn_about);
        View btnDeps = view.findViewById(R.id.btn_deps);
        View btnUpdate = view.findViewById(R.id.btn_update);
        View btnDebug = view.findViewById(R.id.btn_debug);
        TextView tvCurrentProvider = view.findViewById(R.id.tv_current_provider);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            if (activity.isDebugEnabled()) {
                btnDebug.setVisibility(View.VISIBLE);
            }
            if (tvCurrentProvider != null) {
                tvCurrentProvider.setText(getString(R.string.j_001) + ProviderConfig.get(activity.getCurrentProviderId()).name);
            }
            if (tvTimeoutDesc != null) {
                tvTimeoutDesc.setText(getString(R.string.j_002) + AppPrefs.getTimeoutSec(getActivity()) + getString(R.string.j_003) + AppPrefs.getRetryCount(getActivity()) + getString(R.string.j_004));
            }
            if (tvCurrencyDesc != null) {
                int cm = AppPrefs.getDisplayCurrency(getActivity());
                tvCurrencyDesc.setText(cm == 1 ? "CNY" : cm == 2 ? "USD" : getString(R.string.j_005));
            }
        }

        btnBack.setOnClickListener(v -> {
            // 先回上一级；已经是最外层才回余额页
            if (getActivity() != null && getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
                return;
            }
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.switchTab(0);
        });

        // 提醒/刷新
        if (swAlert != null) {
            swAlert.setChecked(AppPrefs.isAlertEnabled(getActivity()));
            swAlert.setOnCheckedChangeListener((b, isChecked) -> AppPrefs.setAlertEnabled(getActivity(), isChecked));
            if (rowAlert != null) {
                rowAlert.setOnClickListener(v -> {
                    MainActivity act = (MainActivity) getActivity();
                    if (act == null) return;
                    final String[] opts = {"5", "10", "20", "50", "100"};
                    int sel = 1;
                    double cur = AppPrefs.getAlertThreshold(act);
                    for (int i = 0; i < opts.length; i++) {
                        if ((int) cur == Integer.parseInt(opts[i])) { sel = i; break; }
                    }
                    UiDialog.showPickerWithDesc(act, getString(R.string.j_006),
                            getString(R.string.j_007), opts, sel, w -> {
                        AppPrefs.setAlertThreshold(act, Integer.parseInt(opts[w]));
                        if (tvAlertDesc != null) tvAlertDesc.setText(getString(R.string.j_008) + opts[w]);
                    });
                });
            }
        }
        if (swAutoRefresh != null) {
            swAutoRefresh.setChecked(AppPrefs.isAutoRefresh(getActivity()));
            swAutoRefresh.setOnCheckedChangeListener((b, isChecked) -> {
                AppPrefs.setAutoRefresh(getActivity(), isChecked);
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.scheduleAutoRefresh(isChecked);
            });
        }

        // 通知增强
        if (swQuiet != null) {
            swQuiet.setChecked(AppPrefs.isQuietEnabled(getActivity()));
            if (tvQuietDesc != null) {
                tvQuietDesc.setText(AppPrefs.getQuietStart(getActivity()) + ":00 - "
                        + AppPrefs.getQuietEnd(getActivity()) + getString(R.string.j_009));
            }
            swQuiet.setOnCheckedChangeListener((b, isChecked) -> AppPrefs.setQuietEnabled(getActivity(), isChecked));
            if (rowQuiet != null) {
                rowQuiet.setOnClickListener(v -> {
                    MainActivity act = (MainActivity) getActivity();
                    if (act == null) return;
                    String[] hours = new String[24];
                    for (int i = 0; i < 24; i++) hours[i] = i + ":00";
                    UiDialog.showPickerWithDesc(act, getString(R.string.j_010),
                            getString(R.string.j_011), hours, AppPrefs.getQuietStart(act), s -> {
                        AppPrefs.setQuietStart(act, s);
                        UiDialog.showPickerWithDesc(act, getString(R.string.j_012),
                                getString(R.string.j_011), hours, AppPrefs.getQuietEnd(act), e -> {
                            AppPrefs.setQuietEnd(act, e);
                            if (tvQuietDesc != null) {
                                tvQuietDesc.setText(s + ":00 - " + e + getString(R.string.j_009));
                            }
                        });
                    });
                });
            }
        }
        if (swSticky != null) {
            swSticky.setChecked(AppPrefs.isStickyNotify(getActivity()));
            swSticky.setOnCheckedChangeListener((b, isChecked) -> {
                AppPrefs.setStickyNotify(getActivity(), isChecked);
                MainActivity act = (MainActivity) getActivity();
                if (act != null) {
                    if (isChecked) act.showStickyNotify();
                    else Notifier.cancelSticky(act);
                }
            });
        }

        // 桌面悬浮球
        if (swFloat != null) {
            swFloat.setChecked(AppPrefs.isFloatWin(getActivity()));
            swFloat.setOnCheckedChangeListener((b, isChecked) -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(act)) {
                        UiDialog.showConfirm(act, getString(R.string.j_013),
                                getString(R.string.j_014),
                                getString(R.string.j_015), getString(R.string.j_016),
                                () -> openOverlaySettings(act), () -> swFloat.setChecked(false));
                    } else {
                        act.startService(new Intent(act, FloatingWindowService.class));
                    }
                } else {
                    act.stopService(new Intent(act, FloatingWindowService.class));
                }
                AppPrefs.setFloatWin(act, isChecked);
            });
        }

        // 隐私开关
        if (swLock != null) {
            swLock.setChecked(PrivacyPrefs.isLockEnabled(getActivity()));
            swLock.setOnCheckedChangeListener((b, isChecked) -> {
                if (isChecked) {
                    // 打开开关立刻生效：没设过密码的先引导设置，然后马上弹锁界面，不必等重启
                    if (!PrivacyPrefs.hasPassword(getActivity())) {
                        UiDialog.showInput(getActivity(), getString(R.string.j_lock_set_title),
                                getString(R.string.j_lock_set_hint), "", pw -> {
                                    if (pw == null || pw.length() < 4) {
                                        swLock.setChecked(false);
                                        return;
                                    }
                                    PrivacyPrefs.setPassword(getActivity(), pw);
                                    PrivacyPrefs.setLockEnabled(getActivity(), true);
                                    MainActivity a2 = (MainActivity) getActivity();
                                    if (a2 != null) a2.showLockFragment();
                                });
                    } else {
                        PrivacyPrefs.setLockEnabled(getActivity(), true);
                        MainActivity a2 = (MainActivity) getActivity();
                        if (a2 != null) a2.showLockFragment();
                    }
                } else {
                    PrivacyPrefs.setLockEnabled(getActivity(), false);
                }
            });
        }
        if (swHideBalance != null) {
            swHideBalance.setChecked(PrivacyPrefs.isHideBalance(getActivity()));
            swHideBalance.setOnCheckedChangeListener((b, isChecked) -> PrivacyPrefs.setHideBalance(getActivity(), isChecked));
        }
        if (swNoScreenshot != null) {
            swNoScreenshot.setChecked(AppPrefs.noScreenshot(getActivity()));
            swNoScreenshot.setOnCheckedChangeListener((b, isChecked) -> {
                AppPrefs.setNoScreenshot(getActivity(), isChecked);
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.applyPrivacyFlags();
            });
        }
        if (swNoNotify != null) {
            swNoNotify.setChecked(AppPrefs.noNotify(getActivity()));
            swNoNotify.setOnCheckedChangeListener((b, isChecked) -> AppPrefs.setNoNotify(getActivity(), isChecked));
        }

        // 语言切换
        if (btnLang != null && tvLangDesc != null) {
            int lang0 = AppPrefs.getLanguage(getActivity());
            tvLangDesc.setText(lang0 == 1 ? getString(R.string.j_017) : lang0 == 2 ? "English"
                    : lang0 == 3 ? getString(R.string.h_060) : lang0 == 4 ? getString(R.string.lang_meow) : lang0 == 3 ? getString(R.string.lang_trad) : getString(R.string.str_026));
            btnLang.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {getString(R.string.j_018), getString(R.string.j_017), "English", getString(R.string.lang_trad), getString(R.string.lang_meow)};
                int sel = AppPrefs.getLanguage(act);
                UiDialog.showPickerWithDesc(act, getString(R.string.j_019),
                        getString(R.string.j_020), opts, sel == 0 ? 0 : sel, w -> {
                    AppPrefs.setLanguage(act, w);
                    act.recreate();
                });
            });
        }

        // 重新运行首次引导
        if (btnOnboarding != null) {
            btnOnboarding.setOnClickListener(v -> {
                AppPrefs.setOnboardingDone(getActivity(), false);
                android.content.Intent oi = new android.content.Intent(getActivity(), OnboardingActivity.class);
                // 从头开始，别接着上次那一步
                oi.putExtra("from_start", true);
                oi.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(oi);
            });
        }

        // 专业模式：关闭后只保留账户与基础个性化，隐藏高级项
        if (rowProMode != null && swProMode != null) {
            final View[] proViews = { btnAdvancedNet, btnProxy, btnDebug, btnDeps, btnAndroidTheme,
                    btnCustomBg, btnIcon, btnTokenEst, rowNoScreenshot, rowNoNotify, rowAutoDark, rowAutoLock };
            final View rootView = view;
            final java.util.function.Consumer<Boolean> applyPro = on -> {
                proEnabled = on;
                for (View pv : proViews) {
                    if (pv == null) continue;
                    if (on) {
                        pv.setVisibility(View.VISIBLE);
                        pv.setAlpha(0f);
                        pv.animate().alpha(1f).setStartDelay(40L * java.util.Arrays.asList(proViews).indexOf(pv))
                                .setDuration(240).start();
                    } else {
                        pv.animate().alpha(0f).setDuration(150)
                                .withEndAction(() -> pv.setVisibility(View.GONE)).start();
                    }
                }
                if (groupFilter != null) applyGroupFilter(rootView);
            };
            boolean proOn = AppPrefs.isProMode(getActivity());
            swProMode.setChecked(proOn);
            applyPro.accept(proOn);
            rowProMode.setOnClickListener(v -> swProMode.setChecked(!swProMode.isChecked()));
            swProMode.setOnCheckedChangeListener((b, isChecked) -> {
                if (isChecked && !AppPrefs.isProMode(getActivity())) {
                    // 开启专业模式前先答 5 道 API 安全小测验
                    swProMode.setChecked(false);
                    ProQuiz.start(getActivity(), () -> {
                        AppPrefs.setProMode(getActivity(), true);
                        swProMode.setChecked(true);
                        applyPro.accept(true);
                    });
                    return;
                }
                AppPrefs.setProMode(getActivity(), isChecked);
                applyPro.accept(isChecked);
            });
            if (btnProHelp != null) {
                btnProHelp.setOnClickListener(v -> UiDialog.showConfirm(getActivity(),
                        getString(R.string.pro_mode_title), getString(R.string.pro_mode_help),
                        getString(R.string.j_016), null, null, null));
            }
        }

        // 权限中心：既能看状态，也能一键去申请
        if (btnPermissionCenter != null) {
            // 安全中心：详细展示环境检测结果
        View btnSecurity = view.findViewById(R.id.btn_security_center);
        if (btnSecurity != null) {
            btnSecurity.setOnClickListener(v -> {
                if (getActivity() == null) return;
                startActivity(new android.content.Intent(getActivity(), SecurityCenterActivity.class));
            });
        }

        // 备份与恢复：独立界面
        View btnBackup = view.findViewById(R.id.btn_backup);
        // 只在新版设置里露出的那一条：点下去会崩
        View rowBroken = view.findViewById(R.id.row_broken);
        if (rowBroken != null) {
            if (AppPrefs.isLegacyUi(getActivity())) {
                rowBroken.setVisibility(View.GONE);
            } else {
                rowBroken.setOnClickListener(v -> {
                    AppPrefs.setBrokenTapped(getActivity(), true);
                    throw new RuntimeException("broken");
                });
            }
        }
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(getActivity(), BackupActivity.class));
                } catch (Throwable ignored) {
                }
            });
        }

        btnPermissionCenter.setOnClickListener(v -> {
                final android.content.Context ctx = getActivity();
                if (ctx == null) return;
                boolean notifOk = true;
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    notifOk = ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED;
                }
                boolean overlayOk = android.provider.Settings.canDrawOverlays(ctx);
                boolean installOk = true;
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    installOk = ctx.getPackageManager().canRequestPackageInstalls();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(getString(R.string.h_065)).append(notifOk ? getString(R.string.h_031) : getString(R.string.h_063)).append("\n");
                sb.append(getString(R.string.h_066)).append(overlayOk ? getString(R.string.h_031) : getString(R.string.h_063)).append("\n\n");
                sb.append(getString(R.string.h_067));

                final java.util.List<String> labels = new java.util.ArrayList<String>();
                final java.util.List<Integer> kinds = new java.util.ArrayList<Integer>();
                if (!notifOk) { labels.add(getString(R.string.perm_notif)); kinds.add(0); }
                if (!overlayOk) { labels.add(getString(R.string.perm_overlay)); kinds.add(1); }
                if (!installOk) { labels.add(getString(R.string.perm_install)); kinds.add(3); }
                if (labels.isEmpty()) {
                    UiDialog.showConfirm(ctx, getString(R.string.perm_center_title), sb.toString(),
                            getString(R.string.h_004), null, null, null);
                    return;
                }
                UiDialog.showPicker(ctx, getString(R.string.perm_center_title) + "\n" + sb.toString(),
                        labels.toArray(new String[0]), -1, pos -> {
                            if (pos < 0 || pos >= kinds.size()) return;
                            try {
                                switch (kinds.get(pos)) {
                                    case 0:
                                        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 6100);
                                        break;
                                    case 1:
                                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:" + ctx.getPackageName())));
                                        break;
                                    case 2:
                                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                                            startActivity(new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                    android.net.Uri.parse("package:" + ctx.getPackageName())));
                                        }
                                        break;
                                    case 3:
                                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                                            startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                    android.net.Uri.parse("package:" + ctx.getPackageName())));
                                        }
                                        break;
                                }
                            } catch (Throwable ignored) {
                            }
                        });
            });
        }

        // 定时自动深色
        if (rowAutoDark != null && swAutoDark != null) {
            swAutoDark.setChecked(AppPrefs.isAutoDark(getActivity()));
            if (tvAutoDarkDesc != null) {
                tvAutoDarkDesc.setText(getString(R.string.auto_dark_desc));
            }
            swAutoDark.setOnCheckedChangeListener((b, isChecked) -> AppPrefs.setAutoDark(getActivity(), isChecked));
            rowAutoDark.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] hours = new String[24];
                for (int i = 0; i < 24; i++) hours[i] = i + ":00";
                UiDialog.showPickerWithDesc(act, getString(R.string.auto_dark_start),
                        getString(R.string.auto_dark_desc), hours, AppPrefs.getAutoDarkStart(act), s -> {
                    AppPrefs.setAutoDarkStart(act, s);
                    UiDialog.showPickerWithDesc(act, getString(R.string.auto_dark_end),
                            getString(R.string.auto_dark_desc), hours, AppPrefs.getAutoDarkEnd(act), e -> {
                        AppPrefs.setAutoDarkEnd(act, e);
                    });
                });
            });
        }

        // 定时自动锁
        if (rowAutoLock != null && tvAutoLockDesc != null) {
            int cur = AppPrefs.getAutoLockMinutes(getActivity());
            tvAutoLockDesc.setText(cur == 0 ? getString(R.string.auto_lock_off)
                    : cur + getString(R.string.auto_lock_min));
            rowAutoLock.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {getString(R.string.auto_lock_off), "1" + getString(R.string.auto_lock_min),
                        "5" + getString(R.string.auto_lock_min), "15" + getString(R.string.auto_lock_min)};
                int sel = cur == 1 ? 1 : cur == 5 ? 2 : cur == 15 ? 3 : 0;
                UiDialog.showPickerWithDesc(act, getString(R.string.auto_lock),
                        getString(R.string.auto_lock_desc), opts, sel, w -> {
                    int min = w == 1 ? 1 : w == 2 ? 5 : w == 3 ? 15 : 0;
                    AppPrefs.setAutoLockMinutes(act, min);
                    tvAutoLockDesc.setText(min == 0 ? getString(R.string.auto_lock_off)
                            : min + getString(R.string.auto_lock_min));
                });
            });
        }

        // 主题中心：明暗模式 → 风格 → 强调色 三步合一
        if (btnTheme != null && tvThemeMode != null) {
            final MainActivity act0 = (MainActivity) getActivity();
            if (act0 != null) {
                int tm0 = act0.getThemeMode();
                int style0 = ThemePrefs.getStyle(act0);
                int col0 = ThemePrefs.getColorIndex(act0);
                String[] stn = ThemePrefs.styleNames(act0);
                String[] cln = ThemePrefs.colorNames(act0);
                int colIdx = (col0 >= 0 && col0 < cln.length) ? col0 : 0;
                tvThemeMode.setText(stn[style0 % stn.length]
                        + " · " + cln[colIdx]
                        + " · " + (tm0 == 2 ? getString(R.string.j_021) : tm0 == 1 ? getString(R.string.j_022) : getString(R.string.str_026)));
            }
            if (false) btnTheme.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] modes = {getString(R.string.j_018), getString(R.string.j_022), getString(R.string.j_021)};
                UiDialog.showPickerWithDesc(act, getString(R.string.j_023),
                        getString(R.string.j_024), modes, act.getThemeMode(), w1 -> {
                    UiDialog.showPickerWithDesc(act, getString(R.string.j_025),
                            getString(R.string.j_026),
                            ThemePrefs.styleNames(act), ThemePrefs.getStyle(act), w2 -> {
                        UiDialog.showPickerWithDesc(act, getString(R.string.j_027),
                                getString(R.string.j_028),
                                ThemePrefs.colorNames(act), ThemePrefs.getColorIndex(act), w3 -> {
                            ThemePrefs.setStyle(act, w2);
                            ThemePrefs.setColorIndex(act, w3);
                            act.setThemeMode(w1); // 最后统一应用并重建界面
                        });
                    });
                });
            });
        }

        // 超时/重试
        if (btnTimeout != null) {
            btnTimeout.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                final int cur = AppPrefs.getTimeoutSec(act);
                final int curRetry = AppPrefs.getRetryCount(act);
                String[] timeouts = {getString(R.string.j_029), getString(R.string.j_030), getString(R.string.j_031), getString(R.string.j_032)};
                int sel = cur == 10 ? 0 : cur == 30 ? 2 : cur == 60 ? 3 : 1;
                UiDialog.showPickerWithDesc(act, getString(R.string.j_033),
                        getString(R.string.j_034), timeouts, sel, w -> {
                    int sec = w == 0 ? 10 : w == 2 ? 30 : w == 3 ? 60 : 15;
                    AppPrefs.setTimeoutSec(act, sec);
                    if (tvTimeoutDesc != null)
                        tvTimeoutDesc.setText(getString(R.string.j_002) + sec + getString(R.string.j_003) + curRetry + getString(R.string.j_004));
                });
            });
        }

        // 多币种
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] modes = {getString(R.string.j_005), "CNY", "USD", getString(R.string.rate_custom), getString(R.string.rate_refresh)};
                UiDialog.showPickerWithDesc(act, getString(R.string.j_035),
                        getString(R.string.rate_desc), modes, AppPrefs.getDisplayCurrency(act), w -> {
                    if (w == 3) {
                        // 自定义汇率：用户填写「汇率 JSON 地址」，App 拉取该地址并解析 {"usd_cny": 7.2}
                        UiDialog.showInputWithDesc(act, getString(R.string.rate_title),
                                getString(R.string.rate_url_desc), getString(R.string.rate_url_hint), AppPrefs.getRateUrl(act), url -> {
                            if (url == null || url.trim().isEmpty()) {
                                AppPrefs.setRateUrl(act, null);
                                android.widget.Toast.makeText(act, getString(R.string.rate_cleared), android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }
                            AppPrefs.setRateUrl(act, url.trim());
                            android.widget.Toast.makeText(act, getString(R.string.rate_fetching), android.widget.Toast.LENGTH_SHORT).show();
                            new Thread(() -> {
                                final double rate = FxRateFetcher.fetch(act);
                                act.runOnUiThread(() -> {
                                    if (rate > 0) {
                                        android.widget.Toast.makeText(act,
                                                getString(R.string.rate_saved) + rate, android.widget.Toast.LENGTH_SHORT).show();
                                    } else {
                                        android.widget.Toast.makeText(act,
                                                getString(R.string.rate_invalid), android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).start();
                        });
                        return;
                    }
                    if (w == 4) {
                        // 重新拉取已保存的汇率地址
                        if (AppPrefs.getRateUrl(act) == null) {
                            android.widget.Toast.makeText(act, getString(R.string.rate_no_url), android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        android.widget.Toast.makeText(act, getString(R.string.rate_fetching), android.widget.Toast.LENGTH_SHORT).show();
                        new Thread(() -> {
                            final double rate = FxRateFetcher.fetch(act);
                            act.runOnUiThread(() -> {
                                if (rate > 0) {
                                    android.widget.Toast.makeText(act,
                                            getString(R.string.rate_saved) + rate, android.widget.Toast.LENGTH_SHORT).show();
                                } else {
                                    android.widget.Toast.makeText(act,
                                            getString(R.string.rate_invalid), android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                        return;
                    }
                    AppPrefs.setDisplayCurrency(act, w);
                    if (tvCurrencyDesc != null)
                        tvCurrencyDesc.setText(w == 1 ? "CNY" : w == 2 ? "USD" : getString(R.string.j_005));
                });
            });
        }

        // 网络代理
        if (btnProxy != null && swProxy != null) {
            swProxy.setChecked(AppPrefs.isProxyEnabled(getActivity()));
            if (tvProxyDesc != null) {
                tvProxyDesc.setText((AppPrefs.isProxyEnabled(getActivity()) ? getString(R.string.j_037) : getString(R.string.j_038))
                        + " · " + AppPrefs.getProxyHost(getActivity()) + ":" + AppPrefs.getProxyPort(getActivity()));
            }
            swProxy.setOnCheckedChangeListener((b, isChecked) -> {
                AppPrefs.setProxyEnabled(getActivity(), isChecked);
                if (tvProxyDesc != null) {
                    tvProxyDesc.setText((isChecked ? getString(R.string.j_037) : getString(R.string.j_038))
                            + " · " + AppPrefs.getProxyHost(getActivity()) + ":" + AppPrefs.getProxyPort(getActivity()));
                }
            });
            btnProxy.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                UiDialog.showInputWithDesc(act, getString(R.string.j_039),
                        getString(R.string.j_040), getString(R.string.j_041), AppPrefs.getProxyHost(act), host -> {
                    AppPrefs.setProxyHost(act, host);
                    UiDialog.showInputWithDesc(act, getString(R.string.j_042),
                            getString(R.string.j_040), getString(R.string.j_043), String.valueOf(AppPrefs.getProxyPort(act)), portStr -> {
                        try { AppPrefs.setProxyPort(act, Integer.parseInt(portStr)); } catch (Exception ignored) {}
                        if (tvProxyDesc != null) {
                            tvProxyDesc.setText((AppPrefs.isProxyEnabled(act) ? getString(R.string.j_037) : getString(R.string.j_038))
                                    + " · " + AppPrefs.getProxyHost(act) + ":" + AppPrefs.getProxyPort(act));
                        }
                    });
                });
            });
        }

        // 字体大小
        if (btnFontSize != null && tvFontSize != null) {
            tvFontSize.setText(AppPrefs.getFontSize(getActivity()) == 0 ? getString(R.string.j_044)
                    : AppPrefs.getFontSize(getActivity()) == 2 ? getString(R.string.j_045) : getString(R.string.j_046));
            btnFontSize.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {getString(R.string.j_044), getString(R.string.j_046), getString(R.string.j_045)};
                UiDialog.showPicker(act, getString(R.string.j_047), opts, AppPrefs.getFontSize(act), w -> {
                    AppPrefs.setFontSize(act, w);
                    act.recreate();
                });
            });
        }

        // 自定义背景
        if (false) if (btnCustomBg != null) {
            btnCustomBg.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {getString(R.string.j_048), getString(R.string.j_049)};
                UiDialog.showPickerWithDesc(act, getString(R.string.j_050),
                        getString(R.string.j_051), opts, 0, w -> {
                    if (w == 0) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        try {
                            startActivityForResult(pick, 1001);
                        } catch (Exception ignored) {
                        }
                    } else {
                        AppPrefs.setBgPath(act, null);
                        act.recreate();
                    }
                });
            });
        }

        // 更换应用图标：上传自定义 / 清除回退默认
        if (btnIcon != null) {
            btnIcon.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {getString(R.string.h_068), getString(R.string.h_069)};
                UiDialog.showPickerWithDesc(act, getString(R.string.h_070),
                        getString(R.string.h_071),
                        opts, -1, w -> {
                    if (w == 0) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        try {
                            startActivityForResult(pick, 1002);
                        } catch (Exception ignored) { }
                    } else if (w == 1) {
                        AppIconManager.clear(act);
                        android.widget.Toast.makeText(act, getString(R.string.h_072), android.widget.Toast.LENGTH_SHORT).show();
                        restartApp(act);
                    } else {
                        AppIconManager.clear(act);
                        android.widget.Toast.makeText(act, getString(R.string.h_072), android.widget.Toast.LENGTH_SHORT).show();
                        restartApp(act);
                    }
                });
            });
        }

        // 安卓主题设置
        if (btnAndroidTheme != null) {
            if (false) btnAndroidTheme.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                UiDialog.showPickerWithDesc(act, getString(R.string.h_073),
                        getString(R.string.h_074),
                        AndroidThemeManager.namesUnlocked(act), AndroidThemeManager.get(act), w -> {
                    AndroidThemeManager.set(act, w);
                    act.recreate();
                });
            });
        }

        // 高级网络设置
        if (btnAdvancedNet != null) {
            btnAdvancedNet.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                String[] opts = {
                    getString(R.string.j_052),
                    getString(R.string.j_053) + (AppPrefs.isReqLog(act) ? getString(R.string.j_037) : getString(R.string.j_038)),
                    getString(R.string.j_054)
                };
                UiDialog.showPicker(act, getString(R.string.j_055), opts, 0, w -> {
                    switch (w) {
                        case 0:
                            UiDialog.showInput(act, "User-Agent", getString(R.string.j_056), AppPrefs.getUserAgent(act),
                                    ua -> AppPrefs.setUserAgent(act, ua));
                            break;
                        case 1:
                            AppPrefs.setReqLog(act, !AppPrefs.isReqLog(act));
                            break;
                        case 2:
                            String[] intervals = {getString(R.string.j_057), getString(R.string.j_058), getString(R.string.j_059), getString(R.string.j_060), getString(R.string.j_061), getString(R.string.j_062), getString(R.string.j_063)};
                            UiDialog.showPickerWithDesc(act, getString(R.string.j_064),
                                    getString(R.string.j_065), intervals, 0, i2 -> {
                                MainActivity ma = (MainActivity) getActivity();
                                if (i2 == 6) {
                                    // 自定义秒数
                                    UiDialog.showInput(act, getString(R.string.j_066), getString(R.string.j_067),
                                            String.valueOf(AppPrefs.getRefreshIntervalSec(act)), s -> {
                                        try {
                                            int sec = Math.max(60, Integer.parseInt(s));
                                            if (sec > 0) {
                                                AppPrefs.setRefreshIntervalSec(act, sec);
                                                AppPrefs.setAutoRefresh(act, true);
                                                if (ma != null) ma.scheduleAutoRefresh(true);
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    });
                                } else if (i2 == 0) {
                                    // “手动”模式：关闭自动刷新并取消已注册的定时任务
                                    AppPrefs.setAutoRefresh(act, false);
                                    if (ma != null) ma.scheduleAutoRefresh(false);
                                } else {
                                    int sec = i2 == 1 ? 60 : i2 == 2 ? 300 : i2 == 3 ? 900 : i2 == 4 ? 1800 : i2 == 5 ? 3600 : 0;
                                    AppPrefs.setAutoRefresh(act, true);
                                    AppPrefs.setRefreshIntervalSec(act, sec);
                                    if (ma != null) ma.scheduleAutoRefresh(true);
                                }
                            });
                            break;
                    }
                });
            });
        }

        // Token 估算器
        if (btnTokenEst != null) {
            btnTokenEst.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                UiDialog.showInputWithDesc(act, getString(R.string.j_068),
                        getString(R.string.j_069), getString(R.string.j_070), "", input -> {
                    int tokens = TokenEstimator.estimate(input);
                    TokenUsageStore.add(act, tokens); // 记录用量（按天累计）
                    UiDialog.showConfirm(act, getString(R.string.j_071),
                            getString(R.string.j_072) + input.length() + getString(R.string.j_073) + tokens + getString(R.string.j_074)
                                    + "\n" + getString(R.string.usage_today) + TokenUsageStore.today(act),
                            getString(R.string.j_075), null, null, null);
                });
            });
        }

        // 清除数据
        if (btnClearData != null) {
            btnClearData.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                UiDialog.showConfirm(act, getString(R.string.j_076),
                        getString(R.string.j_077),
                        getString(R.string.j_078), getString(R.string.j_016),
                        () -> act.wipeAndRestart(), null);
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showHistoryFragment();
            });
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) UpdateChecker.check(act, true);
            });
        }

        if (btnDeps != null) {
            btnDeps.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showDependenciesFragment();
            });
        }

        btnAbout.setOnClickListener(v -> {
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.showAboutFragment();
        });

        btnChangeKey.setOnClickListener(v -> {
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.onChangeKey();
        });

        // 账户管理：选择账户后可切换 / 重命名 / 删除
        if (btnAccounts != null) {
            btnAccounts.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                final int pid = act.getCurrentProviderId();
                final java.util.List<AccountManager.Account> accounts =
                        AccountManager.getAccounts(act, pid);
                if (accounts.isEmpty()) {
                    UiDialog.showConfirm(act, getString(R.string.j_079),
                            getString(R.string.j_080),
                            getString(R.string.j_075), null, null, null);
                    return;
                }
                String[] names = new String[accounts.size()];
                for (int i = 0; i < accounts.size(); i++) names[i] = accounts.get(i).name;
                UiDialog.showPicker(act, getString(R.string.j_081) + ProviderConfig.get(pid).name + "）",
                        names, -1, w -> {
                            final AccountManager.Account acc = accounts.get(w);
                            String[] ops = {getString(R.string.j_082), getString(R.string.j_083), getString(R.string.j_084)};
                            UiDialog.showPickerWithDesc(act, getString(R.string.j_085) + acc.name + getString(R.string.j_086),
                                    getString(R.string.j_087),
                                    ops, -1, op -> {
                                        if (op == 0) {
                                            act.onLoginSuccess(acc.key, pid, true);
                                        } else if (op == 1) {
                                            UiDialog.showInput(act, getString(R.string.j_088), getString(R.string.j_089), acc.name, newName -> {
                                                if (newName == null || newName.trim().isEmpty()) {
                                                    android.widget.Toast.makeText(act, getString(R.string.j_090),
                                                            android.widget.Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                AccountManager.removeAccount(act, pid, acc.name);
                                                if (AccountManager.saveAccount(act, pid, newName.trim(), acc.key)) {
                                                    android.widget.Toast.makeText(act, getString(R.string.j_091) + newName.trim(),
                                                            android.widget.Toast.LENGTH_SHORT).show();
                                                } else {
                                                    // 失败回滚，恢复原账户
                                                    AccountManager.saveAccount(act, pid, acc.name, acc.key);
                                                    android.widget.Toast.makeText(act, getString(R.string.j_092),
                                                            android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else if (op == 2) {
                                            UiDialog.showConfirm(act, getString(R.string.j_093),
                                                    getString(R.string.j_094) + acc.name + getString(R.string.j_095),
                                                    getString(R.string.j_084), getString(R.string.j_016),
                                                    () -> {
                                                        AccountManager.removeAccount(act, pid, acc.name);
                                                        android.widget.Toast.makeText(act, getString(R.string.j_096) + acc.name,
                                                                android.widget.Toast.LENGTH_SHORT).show();
                                                    }, null);
                                        }
                                    });
                        });
            });
        }

        // 切换服务商：清掉当前 key，回登录页重新选择
        if (btnSwitchProvider != null) {
            btnSwitchProvider.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.onChangeKey();
            });
        }

        btnDebug.setOnClickListener(v -> {
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.showDebugFragment();
        });

        // 入场动画：仅在首次进入时播放，避免每次打开设置重复弹跳
        if (savedInstanceState == null) {
            btnChangeKey.setAlpha(0f);
            btnChangeKey.setTranslationY(60f);
            btnChangeKey.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(150)
                    .setInterpolator(new OvershootInterpolator(0.6f))
                    .start();

            if (btnSwitchProvider != null) {
                btnSwitchProvider.setAlpha(0f);
                btnSwitchProvider.setTranslationY(60f);
                btnSwitchProvider.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setStartDelay(220)
                        .setInterpolator(new OvershootInterpolator(0.6f))
                        .start();
            }
        }


        // 内容入场动画：淡入 + 上滑
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.decelerate_cubic))
                .start();

        bindExtraRows(view);
        applyGroupFilter(view);
        ThemeUtils.apply(view);
        // q9: 设置项图标统一跟随主题强调色
        applyIconAccent(view, ThemePrefs.getColor(getActivity()));
        return view;
    }

    /** 递归给设置项图标着色（跟随主题强调色）。 */
    private void applyIconAccent(View root, int accent) {
        if (root instanceof android.widget.ImageView) {
            ((android.widget.ImageView) root).setColorFilter(accent);
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyIconAccent(vg.getChildAt(i), accent);
            }
        }
    }

    /** 自定义背景选图结果：复制到应用私有目录后应用（B2 修复）。 */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 更换应用图标选图结果
        if (requestCode == 1002) {
            handleIconResult(resultCode, data);
            return;
        }
        if (requestCode != 1001 || resultCode != android.app.Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        try {
            java.io.InputStream in = act.getContentResolver().openInputStream(data.getData());
            if (in == null) {
                android.widget.Toast.makeText(act, getString(R.string.j_097), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.File outFile = new java.io.File(act.getFilesDir(), "custom_bg.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
            fos.close();
            in.close();
            // 存私有文件 uri，App 重启后仍可读（GET_CONTENT 的临时授权可能失效）
            AppPrefs.setBgPath(act, android.net.Uri.fromFile(outFile).toString());
            // 即时应用背景，停留在当前页面（不再 recreate 返回主界面）
            act.applyCustomBg();
            android.widget.Toast.makeText(act, getString(R.string.j_098), android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(act, getString(R.string.j_097), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /** 处理更换应用图标选图结果。 */
    private void handleIconResult(int resultCode, Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        try {
            java.io.InputStream in = act.getContentResolver().openInputStream(data.getData());
            if (in == null) {
                android.widget.Toast.makeText(act, getString(R.string.h_075), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (AppIconManager.save(act, in)) {
                android.widget.Toast.makeText(act, getString(R.string.h_076), android.widget.Toast.LENGTH_SHORT).show();
                restartApp(act);
            } else {
                android.widget.Toast.makeText(act, getString(R.string.h_077), android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(act, getString(R.string.h_077), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /** 强制重启应用，使新图标/头像立即生效。 */
    /**
     * 打开悬浮窗授权页。不同 ROM 对 ACTION_MANAGE_OVERLAY_PERMISSION 支持不一，
     * 因此做三级回退：悬浮窗页 → 应用详情页 → 手动指引，保证任何设备都有出路。
     */
    private void openOverlaySettings(MainActivity act) {
        android.net.Uri pkg = android.net.Uri.parse("package:" + act.getPackageName());
        try {
            act.startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg));
            return;
        } catch (Throwable ignored) {
        }
        try {
            act.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg));
            return;
        } catch (Throwable ignored) {
        }
        UiDialog.showConfirm(act, getString(R.string.perm_manual_title), getString(R.string.perm_manual_hint),
                getString(R.string.j_016), null, null, null);
    }

    private void restartApp(android.app.Activity act) {
        try {
            android.content.Intent i = act.getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(act.getBaseContext().getPackageName());
            if (i != null) {
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                act.getBaseContext().startActivity(i);
            }
        } catch (Exception ignored) {
        }
        act.finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
