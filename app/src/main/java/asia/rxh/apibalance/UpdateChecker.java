package asia.rxh.apibalance;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** 检查 GitHub Release 并下载安装包。下载后必须校验签名，签名不符拒绝安装。 */
public final class UpdateChecker {

    private static final String REPO_API = "https://api.github.com/repos/RXHrxh520/APIBalance/releases/latest";
    private static final String REPO_URL = "https://github.com/RXHrxh520/APIBalance";
    /** 国内加速镜像；只做传输，下载后仍然强制校验签名。 */
    private static final String MIRROR_PRIMARY = "https://gh-proxy.com/";
    private static final long MAX_APK_BYTES = 200L * 1024 * 1024;

    private UpdateChecker() {
    }

    public static void check(final Context ctx, final boolean manual) {
        new Thread(() -> {
            int resultCode = 0;
            String version = null;
            String apkUrl = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(REPO_API).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME);
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) sb.append(line);
                    rd.close();
                    JSONObject obj = new JSONObject(sb.toString());
                    version = obj.optString("tag_name", null);
                    JSONArray assets = obj.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.optJSONObject(i);
                            if (a == null) continue;
                            String name = a.optString("name", "");
                            if (name.endsWith(".apk")) {
                                apkUrl = a.optString("browser_download_url", null);
                                break;
                            }
                        }
                    }
                    resultCode = version != null ? 2 : 1;
                } else if (code == 404) {
                    resultCode = 1;
                }
                conn.disconnect();
            } catch (Exception ignored) {
                resultCode = 0;
            }

            final int rc = resultCode;
            final String fv = version;
            final String fu = apkUrl;
            final String current = "v" + BuildConfig.VERSION_NAME;
            if (!(ctx instanceof android.app.Activity)) return;
            android.app.Activity act = (android.app.Activity) ctx;
            act.runOnUiThread(() -> {
                if (rc == 2) {
                    boolean isNew = fv != null && compareVersions(fv, current) > 0;
                    if (isNew) {
                        if (fu == null || fu.isEmpty()) {
                            UiDialog.showConfirm(act, act.getString(R.string.j_125),
                                    act.getString(R.string.j_127),
                                    act.getString(R.string.j_128), act.getString(R.string.j_016),
                                    () -> openRepo(act), null);
                            return;
                        }
                        UiDialog.showConfirm(act, act.getString(R.string.j_120),
                                act.getString(R.string.j_121) + fv + act.getString(R.string.j_122) + current
                                        + "\n\n" + act.getString(R.string.update_install_hint),
                                act.getString(R.string.update_download_install), act.getString(R.string.j_124),
                                () -> downloadAndInstall(act, fu), null);
                    } else if (manual) {
                        UiDialog.showConfirm(act, act.getString(R.string.j_125),
                                act.getString(R.string.j_126) + current + ")",
                                act.getString(R.string.j_075), null, null, null);
                    }
                } else if (manual) {
                    if (rc == 1) {
                        UiDialog.showConfirm(act, act.getString(R.string.j_125),
                                act.getString(R.string.j_127),
                                act.getString(R.string.j_128), act.getString(R.string.j_016),
                                () -> openRepo(act), null);
                    } else {
                        UiDialog.showConfirm(act, act.getString(R.string.j_125),
                                act.getString(R.string.j_129),
                                act.getString(R.string.j_130), act.getString(R.string.j_016),
                                () -> check(ctx, true), null);
                    }
                }
            });
        }).start();
    }

    private static void downloadAndInstall(final android.app.Activity act, final String url) {
        if (url == null || !isAllowedHost(url)) {
            android.widget.Toast.makeText(act, act.getString(R.string.update_bad_url),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        // 国内直连 GitHub 经常很慢；优先走镜像，失败后仍可回退原始地址。
        final String downloadUrl = url.startsWith("https://github.com/")
                ? MIRROR_PRIMARY + url : url;
        final float density = act.getResources().getDisplayMetrics().density;
        final android.app.Dialog dlg = new android.app.Dialog(act);
        android.widget.LinearLayout col = new android.widget.LinearLayout(act);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);
        col.setPadding((int) (22 * density), (int) (20 * density),
                (int) (22 * density), (int) (18 * density));
        col.setBackgroundColor(ThemeUtils.cardBg(act));

        android.widget.TextView title = new android.widget.TextView(act);
        title.setText(act.getString(R.string.update_downloading));
        title.setTextColor(ThemeUtils.textPrimary(act));
        title.setTextSize(16);
        col.addView(title);

        final android.widget.ProgressBar bar = new android.widget.ProgressBar(
                act, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        android.widget.LinearLayout.LayoutParams blp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (int) (7 * density));
        blp.topMargin = (int) (16 * density);
        col.addView(bar, blp);

        final android.widget.TextView tvPct = new android.widget.TextView(act);
        tvPct.setText("0%");
        tvPct.setTextColor(ThemeUtils.textSecondary(act));
        tvPct.setTextSize(12);
        android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = (int) (8 * density);
        col.addView(tvPct, tlp);

        dlg.setContentView(col);
        dlg.setCancelable(false);
        try {
            dlg.show();
        } catch (Throwable ignored) {
        }

        new Thread(() -> {
            File apk = new File(act.getCacheDir(), "update.apk");
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME);
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new java.io.IOException("HTTP " + responseCode);
                }
                long total = conn.getContentLengthLong();
                if (total > MAX_APK_BYTES) throw new java.io.IOException("apk too large");
                try (InputStream in = conn.getInputStream();
                     FileOutputStream os = new FileOutputStream(apk)) {
                    byte[] buf = new byte[8192];
                    long done = 0;
                    int lastPct = -1;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        done += n;
                        if (done > MAX_APK_BYTES) throw new java.io.IOException("apk too large");
                        os.write(buf, 0, n);
                        if (total > 0) {
                            final int pct = (int) (done * 100 / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                act.runOnUiThread(() -> {
                                    bar.setProgress(pct);
                                    tvPct.setText(pct + "%");
                                });
                            }
                        }
                    }
                }
                if (!verifySignature(apk, act.getApplicationContext())) {
                    apk.delete();
                    act.runOnUiThread(() -> {
                        try { dlg.dismiss(); } catch (Throwable ignored) { }
                        android.widget.Toast.makeText(act, act.getString(R.string.update_sig_mismatch),
                                android.widget.Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                act.runOnUiThread(() -> {
                    try { dlg.dismiss(); } catch (Throwable ignored) { }
                    try {
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setDataAndType(UpdateFileProvider.uriFor(apk),
                                "application/vnd.android.package-archive");
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(install);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(act,
                                act.getString(R.string.update_manual_install),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable t) {
                apk.delete();
                act.runOnUiThread(() -> {
                    try { dlg.dismiss(); } catch (Throwable ignored) { }
                    android.widget.Toast.makeText(act, act.getString(R.string.update_fail),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /** 只允许 GitHub 域名，防止 API 被篡改后把 APK 指向任意主机。 */
    private static boolean isAllowedHost(String url) {
        try {
            String host = Uri.parse(url).getHost();
            if (host == null) return false;
            host = host.toLowerCase(java.util.Locale.US);
            return host.equals("github.com")
                    || host.equals("objects.githubusercontent.com")
                    || host.endsWith(".githubusercontent.com")
                    || host.equals("gh-proxy.com")
                    || host.equals("ghfast.top");
        } catch (Throwable t) {
            return false;
        }
    }

    /** 校验下载包的签名证书 SHA-256 与官方指纹一致。 */
    private static boolean verifySignature(File apk, Context ctx) {
        try {
            if (apk == null || !apk.isFile() || ctx == null) return false;
            PackageManager pm = ctx.getPackageManager();
            int flags = Build.VERSION.SDK_INT >= 28
                    ? PackageManager.GET_SIGNING_CERTIFICATES
                    : PackageManager.GET_SIGNATURES;
            PackageInfo pi = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
            if (pi == null) return false;
            Signature[] sigs;
            if (Build.VERSION.SDK_INT >= 28) {
                SigningInfo info = pi.signingInfo;
                sigs = info == null ? null : info.getApkContentsSigners();
            } else {
                sigs = pi.signatures;
            }
            if (sigs == null || sigs.length == 0) return false;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(sigs[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", d[i]));
            }
            return sb.toString().equalsIgnoreCase(EnvChecker.OFFICIAL_CERT_SHA256);
        } catch (Throwable t) {
            return false;
        }
    }

    private static int compareVersions(String a, String b) {
        if (a == null || b == null) return 0;
        String va = a.replaceAll("[^0-9.]", "");
        String vb = b.replaceAll("[^0-9.]", "");
        String[] pa = va.split("\\.");
        String[] pb = vb.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            long na = parseSeg(i < pa.length ? pa[i] : "");
            long nb = parseSeg(i < pb.length ? pb[i] : "");
            if (na != nb) return na > nb ? 1 : -1;
        }
        return 0;
    }

    private static long parseSeg(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void openRepo(android.app.Activity act) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)));
        } catch (Exception ignored) {
        }
    }
}

