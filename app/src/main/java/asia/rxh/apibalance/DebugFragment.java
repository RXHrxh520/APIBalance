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
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * 调试工具页：
 *  - 分级崩溃测试（消息带 [测试崩溃] 标记，触发后由 CrashActivity 展示脱敏堆栈）
 *  - 网络 / API / Key / 加密存储 / 历史记录自检
 *  - 权限与系统信息检查
 *  - 最近崩溃报告查看（脱敏：只显示代码位置）
 */
public class DebugFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        TextView btnCrashTest = view.findViewById(R.id.btn_crash_test);
        btnCrashTest.setOnClickListener(v -> chooseCrashType());

        TextView btnCrashView = view.findViewById(R.id.btn_crash_view);
        if (btnCrashView != null) {
            btnCrashView.setOnClickListener(v -> showCrashReport());
        }

        TextView btnCrashClear = view.findViewById(R.id.btn_crash_clear);
        if (btnCrashClear != null) {
            btnCrashClear.setOnClickListener(v -> {
                CrashHandler.clearCrash(getActivity());
                showResult(getString(R.string.h_001));
            });
        }

        TextView btnNetwork = view.findViewById(R.id.btn_network_test);
        btnNetwork.setOnClickListener(v -> runNetworkTest());

        TextView btnApi = view.findViewById(R.id.btn_api_test);
        btnApi.setOnClickListener(v -> runApiProbe());

        TextView btnKeyTest = view.findViewById(R.id.btn_key_test);
        if (btnKeyTest != null) {
            btnKeyTest.setOnClickListener(v -> runKeyTest());
        }

        TextView btnCrypto = view.findViewById(R.id.btn_crypto_test);
        if (btnCrypto != null) {
            btnCrypto.setOnClickListener(v -> runCryptoTest());
        }

        TextView btnHistoryTest = view.findViewById(R.id.btn_history_test);
        if (btnHistoryTest != null) {
            btnHistoryTest.setOnClickListener(v -> runHistoryTest());
        }

        TextView btnNotify = view.findViewById(R.id.btn_notify_check);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v -> checkNotifyPermission());
        }

        TextView btnAlarm = view.findViewById(R.id.btn_alarm_check);
        if (btnAlarm != null) {
            btnAlarm.setOnClickListener(v -> checkAlarmState());
        }

        TextView btnOverlay = view.findViewById(R.id.btn_overlay_check);
        if (btnOverlay != null) {
            btnOverlay.setOnClickListener(v -> checkOverlayPermission());
        }

        TextView btnEnv = view.findViewById(R.id.btn_env_info);
        if (btnEnv != null) {
            btnEnv.setOnClickListener(v -> showEnvInfo());
        }

        TextView btnEggReset = view.findViewById(R.id.btn_egg_reset);
        if (btnEggReset != null) {
            btnEggReset.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                try {
                    // 强制把彩蛋打回出厂状态：记录、隐藏主题、粉色主题、崩坏入口标记全清
                    AppPrefs.clearEggLog(act);
                    AppPrefs.setHorror(act, false);
                    AppPrefs.setPink(act, false);
                    AppPrefs.setBrokenTapped(act, false);
                    EggChat.resetState();
                    android.widget.Toast.makeText(act, getString(R.string.egg_reset_done),
                            android.widget.Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    android.widget.Toast.makeText(act, getString(R.string.egg_reset_fail),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        TextView btnClearCache = view.findViewById(R.id.btn_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) {
                    try {
                        deleteRecursive(act.getCacheDir());
                    } catch (Exception ignored) {
                    }
                    try {
                        deleteRecursive(act.getExternalCacheDir());
                    } catch (Exception ignored) {
                    }
                    showResult(getString(R.string.h_002));
                }
            });
        }

        TextView btnShowSettings = view.findViewById(R.id.btn_show_settings);
        if (btnShowSettings != null) {
            btnShowSettings.setOnClickListener(v -> showSettingsInfo());
        }

        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.decelerate_cubic))
                .start();

        ThemeUtils.apply(view);
        View btnDeviceInfo = view.findViewById(R.id.btn_device_info);
        if (btnDeviceInfo != null) btnDeviceInfo.setOnClickListener(v -> showDeviceInfo());
        View btnMemInfo = view.findViewById(R.id.btn_mem_info);
        if (btnMemInfo != null) btnMemInfo.setOnClickListener(v -> showMemInfo());
        View btnTimeInfo = view.findViewById(R.id.btn_time_info);
        if (btnTimeInfo != null) btnTimeInfo.setOnClickListener(v -> showTimeInfo());
        View btnDnsCheck = view.findViewById(R.id.btn_dns_check);
        if (btnDnsCheck != null) btnDnsCheck.setOnClickListener(v -> runDnsCheck());
        View btnPrivacyInfo = view.findViewById(R.id.btn_privacy_info);
        if (btnPrivacyInfo != null) btnPrivacyInfo.setOnClickListener(v -> showPrivacyInfo());
        View btnProviderInfo = view.findViewById(R.id.btn_provider_info);
        if (btnProviderInfo != null) btnProviderInfo.setOnClickListener(v -> showProviderInfo());
        View btnExportDiag = view.findViewById(R.id.btn_export_diag);
        if (btnExportDiag != null) btnExportDiag.setOnClickListener(v -> exportDiag());
        bindCloseButton(view);
        return view;
    }

    /** 进度提示：只更新页面文字，绝不弹窗（弹窗会挡住整个界面导致按钮点不动）。 */
    /** 关闭调试模式：点一下就把调试功能整体关掉，不需要再去设置里找。 */
    private void bindCloseButton(View view) {
        View btn = view.findViewById(R.id.btn_debug_close);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            try {
                android.app.Activity act = getActivity();
                if (act instanceof MainActivity) {
                    ((MainActivity) act).setDebugEnabled(false);
                }
                android.widget.Toast.makeText(getActivity(), getString(R.string.debug_closed),
                        android.widget.Toast.LENGTH_SHORT).show();
                if (getActivity() != null) getActivity().getFragmentManager().popBackStack();
            } catch (Throwable ignored) {
            }
        });
    }

    private void showProgress(String text) {
        View v = getView();
        if (v == null) return;
        TextView tv = (TextView) v.findViewById(R.id.tv_test_result);
        if (tv != null) {
            tv.setText(text);
            tv.setVisibility(View.VISIBLE);
        }
    }

    private void showResult(String text) {
        View v = getView();
        if (v != null) {
            TextView tv = (TextView) v.findViewById(R.id.tv_test_result);
            if (tv != null) {
                tv.setText(text);
                tv.setVisibility(View.VISIBLE);
            }
        }
        // 结果同时用弹窗展示，避免内容堆在页面底部被忽略
        try {
            final android.app.Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(() -> {
                    try {
                        UiDialog.showConfirm(act, getString(R.string.h_003), text, getString(R.string.h_004), null, null, null);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    /** 分级崩溃测试：选择崩溃类型，异常消息统一带 [测试崩溃] 标记。 */
    private void chooseCrashType() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        String[] types = {
                getString(R.string.h_005),
                getString(R.string.h_006),
                getString(R.string.h_007),
                getString(R.string.h_008),
                getString(R.string.h_009),
                getString(R.string.h_010)
        };
        UiDialog.showPicker(act, getString(R.string.h_011), types, 0, w -> {
            throwForType(w);
        });
    }

    private void throwForType(int w) {
        switch (w) {
            case 0:
                throw new NullPointerException(getString(R.string.h_012));
            case 1:
                int[] arr = new int[2];
                arr[5] = 1;
                break;
            case 2:
                int z = 0;
                int x = 1 / z;
                break;
            case 3:
                recurse(1);
                break;
            case 4:
                Object o = new Object();
                String s = (String) o;
                break;
            default:
                throw new RuntimeException(getString(R.string.h_013));
        }
    }

    private void recurse(int n) {
        recurse(n + 1);
    }

    /** 查看最近崩溃报告：脱敏显示（只显示异常信息与代码位置，不含任何 Key）。 */
    private void showCrashReport() {
        Context ctx = getActivity();
        if (ctx == null) return;
        if (!CrashHandler.hasPendingCrash(ctx)) {
            showResult(getString(R.string.h_014));
            return;
        }
        String[] info = CrashHandler.getCrashInfo(ctx);
        String msg = info[0];
        String trace = info[1];
        if (trace != null && trace.length() > 1200) {
            trace = trace.substring(0, 1200) + getString(R.string.f_026);
        }
        showResult(getString(R.string.f_027)
                + getString(R.string.h_015) + msg + "\n\n"
                + getString(R.string.f_028) + trace);
    }

    private void showSettingsInfo() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Provider: ").append(ProviderConfig.get(act.getCurrentProviderId()).name);
        String[] st = ThemePrefs.styleNames(act);
        sb.append(" | Theme: ").append(st[ThemePrefs.getStyle(act) % st.length]);
        sb.append(" | Timeout: ").append(AppPrefs.getTimeoutSec(act)).append("s");
        sb.append(" | Retry: ").append(AppPrefs.getRetryCount(act));
        sb.append(" | Proxy: ").append(AppPrefs.isProxyEnabled(act) ? AppPrefs.getProxyHost(act) + ":" + AppPrefs.getProxyPort(act) : "off");
        sb.append(" | Refresh: ").append(AppPrefs.getRefreshIntervalSec(act)).append("s");
        sb.append(" | AutoRefresh: ").append(AppPrefs.isAutoRefresh(act));
        sb.append(" | Float: ").append(AppPrefs.isFloatWin(act));
        sb.append(" | Lock: ").append(PrivacyPrefs.isLockEnabled(act));
        sb.append(" | Alert: ").append(AppPrefs.isAlertEnabled(act));
        sb.append(" | Currency: ").append(AppPrefs.getDisplayCurrency(act));
        sb.append(" | UA: ").append(AppPrefs.getUserAgent(act));
        showResult(sb.toString());
    }

    private void runNetworkTest() {
        showProgress("testing network...");
        final String[] hosts = {
                "https://api.deepseek.com",
                "https://api.moonshot.ai",
                "https://open.bigmodel.cn"
        };
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            for (String host : hosts) {
                long start = System.currentTimeMillis();
                String status;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(host).openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    int code = conn.getResponseCode();
                    long ms = System.currentTimeMillis() - start;
                    status = "ok (" + code + ", " + ms + "ms)";
                    conn.disconnect();
                } catch (Exception e) {
                    status = "fail: " + e.getClass().getSimpleName();
                }
                sb.append(host.replace("https://", "")).append("  ").append(status).append("\n");
            }
            final String result = sb.toString();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showResult(result));
            }
        }).start();
    }

    private void runApiProbe() {
        showProgress("probing endpoints...");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ProviderConfig.count(); i++) {
                ProviderConfig.Provider p = ProviderConfig.get(i);
                String status;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(p.balanceUrl).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer test-invalid-key");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    int code = conn.getResponseCode();
                    status = (code == 401 || code == 403) ? "endpoint ok (" + code + ")" : "resp " + code;
                    conn.disconnect();
                } catch (Exception e) {
                    status = "unreachable: " + e.getClass().getSimpleName();
                }
                sb.append(p.name).append("  ").append(status).append("\n");
            }
            final String result = sb.toString();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showResult(result));
            }
        }).start();
    }

    private void runKeyTest() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        final int pid = act.getCurrentProviderId();
        final String key = act.getCurrentApiKey();
        if (key == null || key.isEmpty()) {
            showProgress("no API key to test");
            return;
        }
        showProgress("testing key for " + ProviderConfig.get(pid).name + "...");
        act.fetchBalance(key, pid, new MainActivity.OnBalanceFetchListener() {
            @Override
            public void onSuccess(int pid2, ProviderConfig.BalanceResult result) {
                showResult("key OK! " + ProviderConfig.get(pid2).name + " balance: "
                        + String.format("%.2f %s", result.totalBalance, result.currency));
            }

            @Override
            public void onError(String errorMessage) {
                showResult("key test failed: " + errorMessage);
            }
        });
    }

    /** 加密存储自检：CryptoManager 往返写入/读出，验证 Keystore 可用。 */
    private void runCryptoTest() {
        Context ctx = getActivity();
        if (ctx == null) return;
        try {
            String testKey = "crypto_selfcheck_" + System.currentTimeMillis();
            String secret = "sk-test-0123456789abcdef";
            boolean saved = CryptoManager.save(ctx, testKey, secret);
            String read = CryptoManager.read(ctx, testKey);
            CryptoManager.remove(ctx, testKey);
            if (saved && secret.equals(read)) {
                showResult(getString(R.string.f_029));
            } else {
                showResult(getString(R.string.f_030) + saved + " read=" + read);
            }
        } catch (Exception e) {
            showResult(getString(R.string.h_016) + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 余额历史记录自检。 */
    private void runHistoryTest() {
        Context ctx = getActivity();
        if (ctx == null) return;
        List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(ctx);
        if (items.isEmpty()) {
            showResult(getString(R.string.h_017));
            return;
        }
        BalanceHistoryStore.HistoryItem last = items.get(items.size() - 1);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
        showResult(getString(R.string.h_018) + items.size() + getString(R.string.f_031)
                + getString(R.string.h_019) + ProviderConfig.get(last.providerId).name + " "
                + String.format("%.2f %s", last.total, last.currency)
                + " @ " + sdf.format(new java.util.Date(last.ts)));
    }

    private void checkNotifyPermission() {
        Context ctx = getActivity();
        if (ctx == null) return;
        if (Build.VERSION.SDK_INT >= 33) {
            boolean granted = ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
            showResult(granted ? getString(R.string.h_020) : getString(R.string.h_021));
        } else {
            showResult(getString(R.string.h_022));
        }
    }

    private void checkAlarmState() {
        Context ctx = getActivity();
        if (ctx == null) return;
        boolean auto = AppPrefs.isAutoRefresh(ctx);
        int interval = AppPrefs.getRefreshIntervalSec(ctx);
        boolean quiet = AppPrefs.inQuietHours(ctx);
        showResult(getString(R.string.h_023) + (auto ? getString(R.string.h_024) + interval + getString(R.string.h_025) : getString(R.string.h_026)) + "\n"
                + getString(R.string.h_027) + (quiet ? getString(R.string.h_028) : getString(R.string.h_029)) + "\n"
                + getString(R.string.h_030) + (ctx.checkSelfPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
                == android.content.pm.PackageManager.PERMISSION_GRANTED ? getString(R.string.h_031) : getString(R.string.h_032)));
    }

    private void checkOverlayPermission() {
        Context ctx = getActivity();
        if (ctx == null) return;
        if (Build.VERSION.SDK_INT >= 23) {
            boolean can = android.provider.Settings.canDrawOverlays(ctx);
            showResult(can ? getString(R.string.h_033) : getString(R.string.h_034));
        } else {
            showResult(getString(R.string.h_035));
        }
    }

    private void showEnvInfo() {
        Context ctx = getActivity();
        if (ctx == null) return;
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.0");
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.h_036)).append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append(getString(R.string.h_037)).append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append(getString(R.string.h_038)).append(BuildConfig.VERSION_NAME).append(" (code ").append(BuildConfig.VERSION_CODE).append(")\n");
        java.io.File data = ctx.getFilesDir();
        java.io.File cache = ctx.getCacheDir();
        sb.append(getString(R.string.h_039)).append(data.getPath()).append("\n");
        sb.append(getString(R.string.h_040)).append(df.format(cacheDirSize(cache) / 1048576.0)).append(" MB\n");
        Runtime rt = Runtime.getRuntime();
        sb.append(getString(R.string.h_041)).append(df.format(rt.totalMemory() / 1048576.0)).append(getString(R.string.h_042))
                .append(df.format(rt.maxMemory() / 1048576.0)).append("MB\n");
        sb.append(getString(R.string.h_043)).append(cryptoSelfCheck(ctx) ? getString(R.string.h_044) : getString(R.string.h_045));
        showResult(sb.toString());
    }

    /** 递归删除目录内容（原 File.delete() 无法删除非空目录）。 */
    private static void deleteRecursive(java.io.File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            java.io.File[] children = f.listFiles();
            if (children != null) {
                for (java.io.File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    /** 真实加密往返自检：写入→读回→清理。 */
    private static boolean cryptoSelfCheck(Context ctx) {
        try {
            CryptoManager.save(ctx, "selfcheck", "ok");
            boolean ok = "ok".equals(CryptoManager.read(ctx, "selfcheck"));
            CryptoManager.remove(ctx, "selfcheck");
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private long cacheDirSize(java.io.File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isDirectory()) size += cacheDirSize(f);
                else size += f.length();
            }
        }
        return size;
    }

    private void showDeviceInfo() {
        try {
            StringBuilder sb = new StringBuilder(getString(R.string.f_032));
            sb.append(getString(R.string.e_001)).append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
            sb.append("Android: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")").append("\n");
            sb.append("ABI: ").append(android.os.Build.SUPPORTED_ABIS.length > 0 ? android.os.Build.SUPPORTED_ABIS[0] : "?").append("\n");
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            sb.append(getString(R.string.e_002)).append(dm.widthPixels).append("x").append(dm.heightPixels).append(" @").append(dm.density)
              .append(" (").append(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? getString(R.string.e_003) : getString(R.string.e_004)).append(")").append("\n");
            sb.append(getString(R.string.e_005)).append(java.util.Locale.getDefault().toString()).append("\n");
            sb.append(getString(R.string.e_006)).append(AndroidThemeManager.palette(getActivity()).dark ? getString(R.string.e_007) : getString(R.string.e_008)).append("\n");
            showResult(sb.toString());
        } catch (Exception e) {
            showResult(getString(R.string.e_009) + e);
        }
    }

    private long dirSize(java.io.File f) {
        long n = 0;
        java.io.File[] fs = f == null ? null : f.listFiles();
        if (fs == null) return 0;
        for (java.io.File c : fs) n += c.isDirectory() ? dirSize(c) : c.length();
        return n;
    }

    private void showMemInfo() {
        try {
            Runtime rt = Runtime.getRuntime();
            StringBuilder sb = new StringBuilder(getString(R.string.f_033));
            sb.append(getString(R.string.e_010)).append(rt.maxMemory() / 1048576).append(" MB").append("\n");
            sb.append(getString(R.string.e_011)).append((rt.totalMemory() - rt.freeMemory()) / 1048576).append(" MB").append("\n");
            sb.append(getString(R.string.e_012)).append("\n");
            sb.append("files: ").append(dirSize(getActivity().getFilesDir()) / 1024).append(" KB").append("\n");
            sb.append("cache: ").append(dirSize(getActivity().getCacheDir()) / 1024).append(" KB").append("\n");
            java.io.File sp = new java.io.File(getActivity().getFilesDir().getParentFile(), "shared_prefs");
            sb.append("shared_prefs: ").append(dirSize(sp) / 1024).append(" KB").append("\n");
            showResult(sb.toString());
        } catch (Exception e) {
            showResult(getString(R.string.e_013) + e);
        }
    }

    private void showTimeInfo() {
        try {
            StringBuilder sb = new StringBuilder(getString(R.string.f_034));
            sb.append(getString(R.string.e_014)).append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date())).append("\n");
            sb.append(getString(R.string.e_015)).append(java.util.TimeZone.getDefault().getID()).append("\n");
            sb.append(getString(R.string.e_016)).append(java.util.TimeZone.getDefault().getRawOffset() / 3600000).append(getString(R.string.e_017)).append("\n");
            sb.append(getString(R.string.e_018)).append(android.os.SystemClock.elapsedRealtime() / 1000).append(getString(R.string.e_019)).append("\n");
            showResult(sb.toString());
        } catch (Exception e) {
            showResult(getString(R.string.e_020) + e);
        }
    }

    private void runDnsCheck() {
        showResult(getString(R.string.e_021));
        new Thread(() -> {
            StringBuilder sb = new StringBuilder(getString(R.string.f_035));
            String[] hosts = {"api.deepseek.com", "api.moonshot.cn", "open.bigmodel.cn"};
            for (String h : hosts) {
                long t0 = System.currentTimeMillis();
                try {
                    java.net.InetAddress a = java.net.InetAddress.getByName(h);
                    sb.append(h).append(" -> ").append(a.getHostAddress()).append(" (").append(System.currentTimeMillis() - t0).append("ms)").append("\n");
                } catch (Exception e) {
                    sb.append(h).append(getString(R.string.e_022)).append(e.getClass().getSimpleName()).append("\n");
                }
            }
            final String out = sb.toString();
            if (getActivity() != null) getActivity().runOnUiThread(() -> showResult(out));
        }).start();
    }

    private void showPrivacyInfo() {
        try {
            android.content.Context c = getActivity();
            StringBuilder sb = new StringBuilder(getString(R.string.f_036));
            sb.append(getString(R.string.e_023)).append(PrivacyPrefs.isLockEnabled(c) ? getString(R.string.e_024) : getString(R.string.e_025)).append("\n");
            sb.append(getString(R.string.e_026)).append(AppPrefs.getAutoLockMinutes(c)).append(getString(R.string.e_027)).append("\n");
            sb.append(getString(R.string.e_028)).append(AppPrefs.noScreenshot(c) ? getString(R.string.e_024) : getString(R.string.e_025)).append("\n");
            sb.append(getString(R.string.e_029)).append(AppPrefs.isProMode(c) ? getString(R.string.e_024) : getString(R.string.e_025)).append("\n");
            sb.append(getString(R.string.e_030)).append(AppPrefs.isOnboardingDone(c) ? getString(R.string.e_007) : getString(R.string.e_008)).append("\n");
            sb.append(getString(R.string.e_031)).append(AppPrefs.hasCustomProvider(c) ? getString(R.string.e_032) : getString(R.string.e_033)).append("\n");
            showResult(sb.toString());
        } catch (Exception e) {
            showResult(getString(R.string.e_034) + e);
        }
    }

    private void showProviderInfo() {
        try {
            android.content.Context c = getActivity();
            StringBuilder sb = new StringBuilder(getString(R.string.f_037));
            String[] names = ProviderConfig.names(c);
            for (int i = 0; i < names.length; i++) {
                sb.append(i).append(". ").append(names[i]).append("\n");
            }
            sb.append(getString(R.string.e_035)).append(ProviderConfig.count(c)).append("\n");
            if (AppPrefs.hasCustomProvider(c)) {
                sb.append(getString(R.string.e_036)).append(AppPrefs.getCustomProviderName(c)).append("\n");
                sb.append(getString(R.string.e_037)).append(AppPrefs.getCustomProviderUrl(c)).append("\n");
                sb.append(getString(R.string.e_038)).append(AppPrefs.getCustomProviderPath(c)).append("\n");
            }
            showResult(sb.toString());
        } catch (Exception e) {
            showResult(getString(R.string.e_039) + e);
        }
    }

    private void exportDiag() {
        try {
            StringBuilder sb = new StringBuilder(getString(R.string.f_038));
            sb.append(getString(R.string.e_040)).append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date())).append("\n");
            sb.append("Android: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")").append("\n");
            sb.append(getString(R.string.e_001)).append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
            sb.append(getString(R.string.e_041)).append(PrivacyPrefs.isLockEnabled(getActivity()) ? getString(R.string.e_024) : getString(R.string.e_025)).append("\n");
            sb.append(getString(R.string.e_029)).append(AppPrefs.isProMode(getActivity()) ? getString(R.string.e_024) : getString(R.string.e_025)).append("\n");
            sb.append(getString(R.string.e_042)).append(ProviderConfig.count(getActivity())).append("\n");
            java.io.File f = new java.io.File(getActivity().getExternalFilesDir(null), "diag_report.txt");
            java.io.FileWriter w = new java.io.FileWriter(f);
            w.write(sb.toString());
            w.close();
            showResult(getString(R.string.e_043) + "\n" + f.getAbsolutePath());
        } catch (Exception e) {
            showResult(getString(R.string.e_044) + e);
        }
    }
}
