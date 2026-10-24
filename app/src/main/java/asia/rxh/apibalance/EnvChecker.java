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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 运行环境与完整性检测。多方位覆盖：Root、TEE 可信执行环境、安装包签名、应用文件、
 * 调试器、模拟器与注入框架。所有检测全部在本地完成，不采集、不上传任何信息。
 * 检测结果只用于提醒用户，绝不强制退出应用。
 */
public final class EnvChecker {

    /** 正式发布证书的 SHA-256 指纹。只要装到的不是这个证书签的包，就是被人重新打包过。 */
    static final String OFFICIAL_CERT_SHA256 =
            "08:8E:E5:FA:C5:2E:7F:9A:8B:89:7D:6E:D9:E9:9E:E2:22:B1:F8:46:18:4D:74:19:22:DE:A6:6D:DA:BB:92:9A";

    /** 当前安装包实际的签名指纹（SHA-256）。 */
    public static String currentCertSha256(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            if (pi.signatures == null || pi.signatures.length == 0) return null;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(pi.signatures[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", d[i]));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 是不是官方证书签的。 */
    public static boolean signatureIsOfficial(Context c) {
        String cur = currentCertSha256(c);
        return cur != null && cur.equalsIgnoreCase(OFFICIAL_CERT_SHA256);
    }


    /** 检测结果。 */
    public static final class Report {
        public final List<String> risks = new ArrayList<String>();
        public boolean tampered;

        public boolean hasRisk() {
            return !risks.isEmpty();
        }
    }

    private static final String PREF = "rxh_env_baseline";

    private EnvChecker() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** 执行全部检测，任何单项异常都不会影响其它项。 */
    public static Report check(Context c) {
        Report r = new Report();
        if (c == null) return r;
        try { checkRoot(r); } catch (Throwable ignored) { }
        try { checkTee(r); } catch (Throwable ignored) { }
        try { checkSignature(c, r); } catch (Throwable ignored) { }
        try { checkFiles(c, r); } catch (Throwable ignored) { }
        try { checkDebugger(c, r); } catch (Throwable ignored) { }
        try { checkEmulator(r); } catch (Throwable ignored) { }
        try { checkHook(r); } catch (Throwable ignored) { }
        return r;
    }

    /** 运行中的代码路径是否就是安装包自身（防外挂式补丁）。 */
    private static boolean apkIntact(Context c) {
        try {
            String src = c.getApplicationInfo().sourceDir;
            String code = c.getPackageCodePath();
            if (src == null || code == null) return false;
            if (!src.equals(code)) return false;
            java.io.File f = new java.io.File(code);
            return f.exists() && f.length() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 安装来源的真实包名，读不到就说明系统没给。 */
    private static String installerName(Context c) {
        try {
            String p = c.getPackageManager().getInstallerPackageName(c.getPackageName());
            if (p == null || p.trim().isEmpty()) return "系统没有提供安装来源（多见于侧载安装）";
            return "安装来源：" + p;
        } catch (Throwable t) {
            return "系统没有提供安装来源（多见于侧载安装）";
        }
    }

    /** 把风险标识翻译成用户看得懂的文案。 */
    public static String label(Context c, String key) {
        int id;
        if ("root".equals(key)) id = R.string.env_root;
        else if ("tee".equals(key)) id = R.string.env_tee;
        else if ("sig".equals(key)) id = R.string.env_sig;
        else if ("file".equals(key)) id = R.string.env_file;
        else if ("debug".equals(key)) id = R.string.env_debug;
        else if ("emu".equals(key)) id = R.string.env_emu;
        else if ("hook".equals(key)) id = R.string.env_hook;
        else id = R.string.env_ok;
        return c.getString(id);
    }

    /** 启动时提醒：只在发现风险时弹一次，不打断使用。 */
    /** 记录一次检测时间（供安全中心显示）。 */
    public static void markChecked(Context c) {
        try {
            c.getSharedPreferences("ds_env", Context.MODE_PRIVATE).edit()
                    .putLong("last_check", System.currentTimeMillis()).commit();
        } catch (Throwable ignored) {
        }
    }

    /** 安全中心用的详细报告：逐项列出检测结果。 */

    // ================= 隐私优先的全面体检（不申请任何权限）=================

    /** 每条检测：{名称, 状态 ok/warn/unknown, 实测到什么, 为什么重要}。 */
    private static void add(java.util.List<String[]> out, String label, String state,
                            String found, String why) {
        out.add(new String[]{label, state, found, why});
    }

    private static boolean risksHas(Report r, String k) {
        return r != null && r.risks != null && r.risks.contains(k);
    }

    public static String mark(String state, Context c) {
        if ("ok".equals(state)) return "\u2713";
        if ("warn".equals(state)) return "\u2717";
        return "?";
    }

    public static String stateText(Context c, String state) {
        if ("ok".equals(state)) return c.getString(R.string.sec_ok);
        if ("warn".equals(state)) return c.getString(R.string.sec_warn);
        return c.getString(R.string.sec_unknown);
    }

    public static int colorOf(String state) {
        if ("ok".equals(state)) return 0xFF10B981;
        if ("warn".equals(state)) return 0xFFEF4444;
        return 0xFFF59E0B;
    }

    /** 全部检测；测不出来的返回 unknown，绝不为了检测去要权限。 */
    public static java.util.List<String[]> scanAll(Context c) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        if (c == null) return out;
        Report r;
        try {
            r = check(c);
        } catch (Throwable t) {
            r = null;
        }
        // 签名必须和正式发布证书比对。之前是跟「首次运行记录的签名」比，
        // 结果是：一个被重新签名的仿冒包，第一次运行时照样把自己记成基准，永远显示正常。
        final boolean sigOk = signatureIsOfficial(c);
        final String curSig = currentCertSha256(c);
        add(out, "应用签名", sigOk ? "ok" : "warn",
                sigOk ? "与官方发布证书一致" :
                        ("不是官方证书签名（实际：" + (curSig == null ? "读取失败" :
                                curSig.substring(0, Math.min(17, curSig.length())) + "…") + "）"),
                "只要不是官方证书签出来的包，就说明这个安装包被人重新打包过，可能被塞了东西。");
        add(out, "硬件密钥库 TEE", risksHas(r, "tee") ? "warn" : "ok",
                risksHas(r, "tee") ? "TEE 不可用或被绕过" : "TEE 可用",
                "TEE 是芯片里的安全区，密钥在里面别人拿不到。");
        add(out, "Root 痕迹", risksHas(r, "root") ? "warn" : "ok",
                risksHas(r, "root") ? "检测到 su 或 root 管理应用" : "没发现 su 与常见 root 应用",
                "设备有 Root 时，别的应用理论上能读到本应用的私有数据。");
        add(out, "Hook / 注入框架", risksHas(r, "hook") ? "warn" : "ok",
                risksHas(r, "hook") ? "发现 Xposed / Frida 之类框架" : "没发现常见 Hook 框架",
                "Hook 框架能改运行中的代码，包括改掉余额显示。");
        add(out, "模拟器特征", risksHas(r, "emulator") ? "warn" : "ok",
                risksHas(r, "emulator") ? "硬件特征像模拟器" : "硬件特征像真机",
                "模拟器上更容易被批量脚本抓包和改数据。");
        final boolean dbgConn = android.os.Debug.isDebuggerConnected();
        final boolean dbgRisk = risksHas(r, "debug");
        add(out, "调试器连接", (dbgConn || dbgRisk) ? "warn" : "ok",
                dbgConn ? "当前有调试器挂着"
                        : (dbgRisk ? "检测到调试相关痕迹（未在连接状态）" : "没有调试器连接"),
                "挂着调试器就能随时暂停程序、查看和修改内存。");
        add(out, "APK 完整性", apkIntact(c) ? "ok" : "warn",
                apkIntact(c) ? "运行中的代码来自本安装包" : "代码来源与安装包不一致",
                "有人会用外挂式补丁把代码从别的地方加载进来，这项就是查这个。");
        add(out, "可调试标记", debuggableState(c),
                "FLAG_DEBUGGABLE = " + ("warn".equals(debuggableState(c)) ? "已打开" : "未打开"),
                "正式包打开这个标记，任何应用都能附加调试。");
        add(out, "安装来源", installerState(c), installerName(c),
                "从浏览器、聊天软件装来的包，来源不可信时风险更高。");
        add(out, "开发者选项", globalFlag(c, "development_settings_enabled"),
                flagFound(c, "development_settings_enabled"),
                "开了开发者选项才能用 ADB 之类工具直接操作这台机器。");
        add(out, "USB 调试", globalFlag(c, "adb_enabled"),
                flagFound(c, "adb_enabled"),
                "USB 调试开着，插上电脑就能读写这台手机。");
        add(out, "未知来源安装", unknownSourcesState(c),
                "warn".equals(unknownSourcesState(c)) ? "允许安装未知来源应用" : "不允许安装未知来源应用",
                "关掉它，能减少误装流氓应用的机会。");
        add(out, "无障碍服务", accessibilityState(c),
                "warn".equals(accessibilityState(c)) ? "有应用开启了无障碍" : "没有应用开启无障碍",
                "无障碍能读取屏幕上的一切文字，包括你的余额和 Key。");
        add(out, "SELinux", selinuxState(),
                "warn".equals(selinuxState()) ? "处于 Permissive" : ("unknown".equals(selinuxState()) ? "读不到状态文件" : "处于 Enforcing"),
                "SELinux 是系统的第二道门，Permissive 等于没关门。");
        add(out, "屏幕锁", lockState(c),
                "warn".equals(lockState(c)) ? "设备没有设置锁屏密码" : "设备已设置锁屏",
                "没锁屏的话，别人拿到手机就能直接打开本应用。");
        add(out, "系统时间自动同步", autoTimeState(c),
                flagFound(c, "auto_time"),
                "时间被改过会让证书校验和记录时间对不上。");
        add(out, "网络代理", proxyState(),
                "warn".equals(proxyState()) ? "系统设置了 HTTP 代理" : "没有设置 HTTP 代理",
                "代理能把你的请求转到别人机器上，余额和 Key 都可能被看到。");
        // 悬浮窗是本应用自己要用的（悬浮球），不能再把它当风险项报出来
        boolean ovl = false;
        try {
            ovl = android.provider.Settings.canDrawOverlays(c);
        } catch (Throwable ignored) {
        }
        add(out, "悬浮窗权限", "ok",
                ovl ? "本应用自己的悬浮球需要它，属于正常" : "未授予，悬浮球功能不可用",
                "悬浮窗权限本身可以被恶意应用拿来伪造输入框，本应用只用于显示自己的悬浮球。");
        add(out, "设备型号", deviceInfo(),
                android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                "只是记一下当前是什么机器，方便对照问题。");
        return out;
    }

    private static String flagFound(Context c, String key) {
        try {
            int v = android.provider.Settings.Global.getInt(c.getContentResolver(), key, -1);
            if (v < 0) return "系统不允许读取这项";
            return v == 0 ? "已关闭" : "已打开";
        } catch (Throwable t) {
            return "系统不允许读取这项";
        }
    }

    private static String debuggableState(Context c) {
        try {
            android.content.pm.ApplicationInfo ai = c.getApplicationInfo();
            return (ai.flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? "warn" : "ok";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String installerState(Context c) {
        try {
            String pkg;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.content.pm.InstallSourceInfo i = c.getPackageManager().getInstallSourceInfo(c.getPackageName());
                pkg = i == null ? null : i.getInstallingPackageName();
            } else {
                pkg = c.getPackageManager().getInstallerPackageName(c.getPackageName());
            }
            if (pkg == null || pkg.isEmpty()) return "unknown";
            return "ok";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String globalFlag(Context c, String key) {
        try {
            int v = android.provider.Settings.Global.getInt(c.getContentResolver(), key, -1);
            if (v < 0) return "unknown";
            return v == 0 ? "ok" : "warn";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String unknownSourcesState(Context c) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                return c.getPackageManager().canRequestPackageInstalls() ? "warn" : "ok";
            }
            return "unknown";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String accessibilityState(Context c) {
        try {
            String v = android.provider.Settings.Secure.getString(c.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (v == null) return "unknown";
            return v.trim().isEmpty() ? "ok" : "warn";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String selinuxState() {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/sys/fs/selinux/enforce"));
            String line = br.readLine();
            br.close();
            if (line == null) return "unknown";
            return "1".equals(line.trim()) ? "ok" : "warn";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String lockState(Context c) {
        try {
            android.app.KeyguardManager km = (android.app.KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
            if (km == null) return "unknown";
            return km.isDeviceSecure() ? "ok" : "warn";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String autoTimeState(Context c) {
        return globalFlag(c, "auto_time");
    }

    private static String proxyState() {
        try {
            String h = System.getProperty("http.proxyHost");
            return (h == null || h.isEmpty()) ? "ok" : "warn";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String overlayState(Context c) {
        try {
            return android.provider.Settings.canDrawOverlays(c) ? "warn" : "ok";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String deviceInfo() {
        try {
            String d = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
            return d.trim().isEmpty() ? "unknown" : "ok";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /** 渲染成带对勾、叉、问号的文本（每项都带说明）。 */
    public static String render(java.util.List<String[]> rows, Context c) {
        StringBuilder sb = new StringBuilder();
        int okN = 0, warnN = 0, unkN = 0;
        for (String[] p : rows) {
            String st = p.length > 1 ? p[1] : "unknown";
            if ("ok".equals(st)) okN++;
            else if ("warn".equals(st)) warnN++;
            else unkN++;
            sb.append(mark(st, c)).append("  ").append(p[0]).append("\n");
            if (p.length > 2 && p[2] != null) sb.append("     实测：").append(p[2]).append("\n");
            if (p.length > 3 && p[3] != null) sb.append("     说明：").append(p[3]).append("\n");
        }
        sb.append("\n").append("\u2713 ").append(okN).append("   \u2717 ").append(warnN)
                .append("   ? ").append(unkN);
        sb.append("\n\n");
        if (c != null) sb.append(c.getString(R.string.env_privacy));
        return sb.toString();
    }

    public static void warnIfNeeded(final Activity act) {
        if (act == null) return;
        final Report r = check(act);
        StringBuilder sb = new StringBuilder();
        if (!r.hasRisk()) sb.append(act.getString(R.string.env_ok));
        for (String k : r.risks) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("· ").append(label(act, k));
        }
        sb.append("\n\n").append(act.getString(R.string.env_privacy));
        final String body = sb.toString();
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!r.hasRisk()) {
                        UiDialog.showConfirm(act, act.getString(R.string.env_title),
                                act.getString(R.string.env_ok) + "\n\n" + act.getString(R.string.env_privacy),
                                act.getString(R.string.h_004), null, null, null);
                        return;
                    }
                    if (r.tampered) {
                        UiDialog.showConfirm(act, act.getString(R.string.env_warn_title),
                                act.getString(R.string.env_warn_body) + "\n\n" + body,
                                act.getString(R.string.h_004), null, null, null);
                    } else {
                        UiDialog.showConfirm(act, act.getString(R.string.env_title), body,
                                act.getString(R.string.h_004), null, null, null);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void add(Report r, String key) {
        if (!r.risks.contains(key)) r.risks.add(key);
    }

    private static void checkRoot(Report r) {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
                "/su/bin/su", "/sbin/.magisk", "/data/adb/magisk"};
        for (String p : paths) {
            try {
                if (new File(p).exists()) {
                    add(r, "root");
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        String tags = Build.TAGS;
        if (tags != null && tags.contains("test-keys")) add(r, "root");
    }

    /** TEE：能生成硬件内保护的密钥说明可信执行环境正常。 */
    private static void checkTee(Report r) {
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance(
                    "HmacSHA256", "AndroidKeyStore");
            kg.init(new android.security.keystore.KeyGenParameterSpec.Builder("rxh_tee_probe",
                    android.security.keystore.KeyProperties.PURPOSE_SIGN).build());
            javax.crypto.SecretKey key = kg.generateKey();
            javax.crypto.SecretKeyFactory f = javax.crypto.SecretKeyFactory.getInstance(
                    key.getAlgorithm(), "AndroidKeyStore");
            android.security.keystore.KeyInfo info = (android.security.keystore.KeyInfo)
                    f.getKeySpec(key, android.security.keystore.KeyInfo.class);
            ks.deleteEntry("rxh_tee_probe");
            if (!info.isInsideSecureHardware()) add(r, "tee");
        } catch (Throwable t) {
            add(r, "tee");
        }
    }

    /** 签名：首次运行记录基线，之后若发生变化即认为安装包被篡改。 */
    private static void checkSignature(Context c, Report r) throws Exception {
        Signature[] sigs = null;
        PackageManager pm = c.getPackageManager();
        if (Build.VERSION.SDK_INT >= 28) {
            PackageInfo pi = pm.getPackageInfo(c.getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
            if (pi.signingInfo != null) sigs = pi.signingInfo.getApkContentsSigners();
        } else {
            PackageInfo pi = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES);
            sigs = pi.signatures;
        }
        if (sigs == null || sigs.length == 0) return;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        StringBuilder sb = new StringBuilder();
        for (Signature s : sigs) sb.append(hex(md.digest(s.toByteArray())));
        String cur = sb.toString();
        String known = sp(c).getString("sig", null);
        if (known == null) {
            sp(c).edit().putString("sig", cur).apply();
        } else if (!known.equals(cur)) {
            add(r, "sig");
            r.tampered = true;
        }
    }

    /**
     * 文件：记录基线体积，版本变化或安装包被系统重新写入（正常升级 / 重装）时
     * 重新建立基线，避免把正常升级误判成篡改。
     */
    private static void checkFiles(Context c, Report r) throws Exception {
        ApplicationInfo ai = c.getApplicationInfo();
        File apk = new File(ai.sourceDir);
        int ver = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        long baseTs = sp(c).getLong("ts", 0L);
        String cur = apk.length() + ":" + apk.lastModified();
        String knownVer = sp(c).getString("ver", "");
        if (baseTs == 0L || apk.lastModified() > baseTs || !String.valueOf(ver).equals(knownVer)) {
            sp(c).edit().putString("ver", String.valueOf(ver)).putString("apk", cur)
                    .putLong("ts", System.currentTimeMillis()).apply();
            return;
        }
        String known = sp(c).getString("apk", null);
        if (known != null && !known.equals(cur)) {
            add(r, "file");
            r.tampered = true;
        }
    }

    private static void checkDebugger(Context c, Report r) {
        if ((c.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) add(r, "debug");
        try {
            if (android.os.Debug.isDebuggerConnected()) add(r, "debug");
        } catch (Throwable ignored) {
        }
    }

    private static void checkEmulator(Report r) {
        String fp = Build.FINGERPRINT == null ? "" : Build.FINGERPRINT.toLowerCase();
        String model = Build.MODEL == null ? "" : Build.MODEL.toLowerCase();
        String hw = Build.HARDWARE == null ? "" : Build.HARDWARE.toLowerCase();
        if (fp.startsWith("generic") || fp.contains("vbox") || fp.contains("emulator")
                || model.contains("sdk") || model.contains("emulator")
                || hw.contains("goldfish") || hw.contains("ranchu")) {
            add(r, "emu");
        }
    }

    private static void checkHook(Report r) {
        String[] classes = {"de.robv.android.xposed.XposedBridge",
                "de.robv.android.xposed.XC_MethodHook", "org.lsposed.lspd.core.Main",
                "com.saurik.substrate.MS", "eu.chainfire.supersu.MainActivity"};
        for (String n : classes) {
            try {
                Class.forName(n);
                add(r, "hook");
                return;
            } catch (Throwable ignored) {
            }
        }
        try {
            throw new Exception("probe");
        } catch (Exception e) {
            for (StackTraceElement el : e.getStackTrace()) {
                String cn = el.getClassName().toLowerCase();
                if (cn.contains("xposed") || cn.contains("substrate")) {
                    add(r, "hook");
                    return;
                }
            }
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
