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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** 检查 GitHub Release 是否有新版本。 */
public final class UpdateChecker {

    private static final String REPO_API = "https://api.github.com/repos/RXHrxh520/APIBalance/releases/latest";
    private static final String REPO_URL = "https://github.com/RXHrxh520/APIBalance";

    private UpdateChecker() {
    }

    public static void check(final Context ctx, final boolean manual) {
        new Thread(() -> {
            int resultCode = 0; // 0=无网络/异常 1=暂无Release 2=有数据
            String version = null;
            String url = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(REPO_API).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) sb.append(line);
                    rd.close();
                    JSONObject obj = new JSONObject(sb.toString());
                    version = obj.optString("tag_name", null);
                    url = obj.optString("html_url", null);
                    resultCode = (version != null) ? 2 : 1;
                } else if (code == 404) {
                    resultCode = 1; // 暂无 Release
                } else {
                    resultCode = 0;
                }
                conn.disconnect();
            } catch (Exception ignored) {
                resultCode = 0;
            }

            final int rc = resultCode;
            final String fv = version;
            final String fu = url;
            final String current = "v" + BuildConfig.VERSION_NAME;
            if (ctx instanceof android.app.Activity) {
                android.app.Activity act = (android.app.Activity) ctx;
                act.runOnUiThread(() -> {
                    if (rc == 2) {
                        // 数字比较：只有 GitHub 版本号真正大于当前版本才提示（避免 1.2 > 1.3 反向升级）
                        boolean isNew = fv != null && compareVersions(fv, current) > 0;
                        if (isNew) {
                            // 有新版：手动和静默检查都提示（支持应用内下载安装）
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
                    // 静默检查（manual=false）：无新版/失败均不打扰用户
                });
            }
        }).start();
    }

    /** 下载 APK（Android 10+ 存 MediaStore）并触发安装；低版本提示保存位置。 */
    private static void downloadAndInstall(final android.app.Activity act, final String url) {
        if (url == null) return;
        final float density = act.getResources().getDisplayMetrics().density;
        final android.app.Dialog dlg = new android.app.Dialog(act);
        android.widget.LinearLayout col = new android.widget.LinearLayout(act);
        col.setOrientation(android.widget.LinearLayout.VERTICAL);
        col.setPadding((int) (22 * density), (int) (20 * density), (int) (22 * density), (int) (18 * density));
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
            java.io.InputStream in = null;
            java.io.OutputStream os = null;
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                final int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    act.runOnUiThread(() -> {
                        try { dlg.dismiss(); } catch (Throwable ignored) { }
                        android.widget.Toast.makeText(act,
                                act.getString(R.string.update_fail) + " (HTTP " + responseCode + ")",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                final int total = conn.getContentLength();
                in = conn.getInputStream();
                byte[] buf = new byte[8192];
                Uri uri = null;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "APIBalance.apk");
                    cv.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                    cv.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/APIBalance");
                    uri = act.getContentResolver().insert(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    os = act.getContentResolver().openOutputStream(uri);
                } else {
                    java.io.File f = new java.io.File(act.getExternalFilesDir(null), "APIBalance.apk");
                    os = new java.io.FileOutputStream(f);
                }
                int n;
                long done = 0;
                int lastPct = -1;
                while ((n = in.read(buf)) != -1) {
                    os.write(buf, 0, n);
                    done += n;
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
                os.flush();
                os.close();
                in.close();
                final Uri apkUri = uri;
                act.runOnUiThread(() -> {
                    try {
                        dlg.dismiss();
                    } catch (Throwable ignored) {
                    }
                    if (apkUri == null) {
                        android.widget.Toast.makeText(act, act.getString(R.string.update_manual_install),
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(install);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(act, act.getString(R.string.update_manual_install),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable t) {
                try {
                    if (os != null) os.close();
                    if (in != null) in.close();
                } catch (Throwable ignored) {
                }
                act.runOnUiThread(() -> {
                    try { dlg.dismiss(); } catch (Throwable ignored) { }
                    android.widget.Toast.makeText(act, act.getString(R.string.update_fail),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /** 语义化版本号数字比较：a > b 返回 1，a == b 返回 0，a < b 返回 -1。 */
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

    /** 解析版本号节段，超大/异常值按 0 处理，避免 Integer.parseInt 溢出。 */
    private static long parseSeg(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void openRepo(android.app.Activity act) {
        openRepo(act, null);
    }

    private static void openRepo(android.app.Activity act, String url) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url != null ? url : REPO_URL)));
        } catch (Exception ignored) {
        }
    }
}
