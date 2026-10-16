package asia.rxh.apibalance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 备份与恢复。独立界面，不用弹窗。
 * 备份文件用用户设置的加密密码做 AES 加密。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public class BackupActivity extends Activity {

    private static final String BK_PREFS = "ds_backup_prefs";
    private static final int REQ_RESTORE = 8101;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppPrefs.wrapLocale(base));
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_backup);
        } catch (Throwable t) {
            finish();
            return;
        }
        try {
            ThemeUtils.apply(findViewById(android.R.id.content));
        } catch (Throwable ignored) {
        }
        View back = findViewById(R.id.bk_back);
        if (back != null) back.setOnClickListener(v -> finish());
        View doBackup = findViewById(R.id.bk_do_backup);
        if (doBackup != null) {
            doBackup.setOnClickListener(v -> {
                // 先给个动静，避免点了没反应让人以为坏了
                android.widget.Toast.makeText(this, getString(R.string.backup_starting),
                        android.widget.Toast.LENGTH_SHORT).show();
                pickFormat();
            });
        }
        View doRestore = findViewById(R.id.bk_do_restore);
        if (doRestore != null) doRestore.setOnClickListener(v -> pickBackupFile());
    }

    // ---------- 备份 ----------

    private String getPwd() {
        return getSharedPreferences(BK_PREFS, MODE_PRIVATE).getString("pwd", "");
    }

    private int format = 0;   // 0=BIN  1=ZIP

    /** 点备份先问导出成什么格式。 */
    private void pickFormat() {
        UiDialog.showPicker(this, getString(R.string.backup_pick_format),
                new String[]{getString(R.string.backup_export_bin), getString(R.string.backup_export_zip)},
                format, i -> {
                    format = i;
                    askPasswordThenBackup();
                });
    }

    private void askPasswordThenBackup() {
        String pwd = getPwd();
        if (pwd == null || pwd.isEmpty()) {
            UiDialog.showInputWithDesc(this, getString(R.string.backup_need_pwd),
                    getString(R.string.backup_need_pwd_desc), getString(R.string.f2a_row_hint), "", text -> {
                        if (text == null || text.trim().length() < 4) {
                            android.widget.Toast.makeText(this, getString(R.string.backup_pwd_short),
                                    android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getSharedPreferences(BK_PREFS, MODE_PRIVATE).edit().putString("pwd", text.trim()).commit();
                        askPasswordThenBackup();
                    });
            return;
        }
        doBackup(pwd);
    }

    private void doBackup(final String pwd) {
        try {
            final byte[] zip = collectIntoZip();
            final byte[] blob = encrypt(zip, pwd);
            byte[] payload = blob;
            String ext = ".bin";
            if (format == 1) {
                ByteArrayOutputStream zb = new ByteArrayOutputStream();
                ZipOutputStream z = new ZipOutputStream(zb);
                z.putNextEntry(new ZipEntry("backup.enc"));
                z.write(blob);
                z.closeEntry();
                z.finish();
                z.close();
                payload = zb.toByteArray();
                ext = ".zip";
            }
            final String name = "APIBalance_backup_" + System.currentTimeMillis() + ext;
            String where = Exporter.saveToDownloads(this, payload, name,
                    format == 1 ? "application/zip" : "application/octet-stream");
            android.widget.Toast.makeText(this, getString(R.string.backup_saved) + "\n" + where,
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, getString(R.string.backup_failed), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** 存到用户看得见的「下载」目录，这样恢复时系统选文件器才挑得到。 */
    private String saveToDownloads(byte[] data, String name) throws Throwable {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            android.net.Uri uri = getContentResolver()
                    .insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri != null) {
                java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(data);
                os.flush();
                os.close();
                return "Download/" + name;
            }
        }
        File dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        if (dir != null && (dir.exists() || dir.mkdirs())) {
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.flush();
            fos.close();
            return f.getAbsolutePath();
        }
        File f2 = new File(getExternalFilesDir(null), name);
        FileOutputStream fos2 = new FileOutputStream(f2);
        fos2.write(data);
        fos2.flush();
        fos2.close();
        return f2.getAbsolutePath();
    }

    /** 把 shared_prefs 和 databases 打进一个 zip。 */
    private byte[] collectIntoZip() throws Throwable {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        File dataDir = getFilesDir().getParentFile();
        addDir(zos, new File(dataDir, "shared_prefs"), "shared_prefs/");
        addDir(zos, new File(dataDir, "databases"), "databases/");
        addKeys(zos);
        zos.finish();
        zos.close();
        return bos.toByteArray();
    }

    /**
     * 把 API Key 也打进备份。
     * Key 平时是用系统 Keystore 加密存的，Keystore 的密钥导不出来，
     * 所以换设备/重装后只还原密文是解不开的 —— 必须把明文一起放进这个
     * 已经用密码加密过的包里。
     */
    private void addKeys(ZipOutputStream zos) throws Throwable {
        StringBuilder sb = new StringBuilder();
        for (int pid = 0; pid <= 5; pid++) {
            try {
                String v = CryptoManager.read(this, "provider_" + pid + "_api_key");
                if (v == null || v.isEmpty()) continue;
                sb.append(pid).append('\t')
                        .append(android.util.Base64.encodeToString(v.getBytes("UTF-8"),
                                android.util.Base64.NO_WRAP))
                        .append('\n');
            } catch (Throwable ignored) {
            }
        }
        if (sb.length() == 0) return;
        zos.putNextEntry(new ZipEntry("keys.txt"));
        zos.write(sb.toString().getBytes("UTF-8"));
        zos.closeEntry();
    }

    /** 还原时把 Key 重新用本机 Keystore 加密存回去。 */
    private void restoreKeys() {
        StringBuilder sb = new StringBuilder();
        for (int pid = 0; pid <= 5; pid++) {
            try {
                String v = CryptoManager.read(this, "provider_" + pid + "_api_key");
                if (v != null && !v.isEmpty()) sb.append(pid).append('\t')
                        .append(android.util.Base64.encodeToString(v.getBytes("UTF-8"),
                                android.util.Base64.NO_WRAP)).append('\n');
            } catch (Throwable ignored) {
            }
        }
    }

    private void readKeysInto(ZipInputStream zis) throws Throwable {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] t = new byte[8192];
        int m;
        while ((m = zis.read(t)) > 0) b.write(t, 0, m);
        String txt = new String(b.toByteArray(), "UTF-8");
        for (String line : txt.split("\n")) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\t");
            if (parts.length < 2) continue;
            try {
                byte[] raw = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP);
                CryptoManager.save(this, "provider_" + parts[0] + "_api_key", new String(raw, "UTF-8"));
            } catch (Throwable ignored) {
            }
        }
    }

    private void addDir(ZipOutputStream zos, File dir, String prefix) throws Throwable {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f == null || !f.isFile()) continue;
            if (f.getName().endsWith("-journal") || f.getName().endsWith("-wal")) continue;
            InputStream in = new java.io.FileInputStream(f);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            in.close();
            zos.putNextEntry(new ZipEntry(prefix + f.getName()));
            zos.write(b.toByteArray());
            zos.closeEntry();
        }
    }

    // ---------- 恢复 ----------

    private void pickBackupFile() {
        try {
            Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            it.addCategory(Intent.CATEGORY_OPENABLE);
            it.setType("*/*");
            it.putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[]{"*/*"});
            startActivityForResult(it, REQ_RESTORE);
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, getString(R.string.backup_restore_failed), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_RESTORE || res != RESULT_OK || data == null || data.getData() == null) return;
        final String pwd = getPwd();
        if (pwd == null || pwd.isEmpty()) {
            android.widget.Toast.makeText(this, getString(R.string.backup_need_pwd), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            in.close();
            byte[] raw = b.toByteArray();
            if (raw.length > 4 && raw[0] == 0x50 && raw[1] == 0x4B) {
                ZipInputStream z0 = new ZipInputStream(new ByteArrayInputStream(raw));
                ZipEntry e0 = z0.getNextEntry();
                if (e0 != null) {
                    ByteArrayOutputStream bb = new ByteArrayOutputStream();
                    byte[] t = new byte[8192];
                    int m;
                    while ((m = z0.read(t)) > 0) bb.write(t, 0, m);
                    raw = bb.toByteArray();
                }
                z0.close();
            }
            byte[] zip = decrypt(raw, pwd);
            unpackZip(zip);
            android.widget.Toast.makeText(this, getString(R.string.backup_restored), android.widget.Toast.LENGTH_LONG).show();
            UiDialog.showConfirm(this, getString(R.string.backup_row_title),
                    getString(R.string.backup_restored), getString(R.string.h_004), null,
                    () -> {
                        Intent it = new Intent(BackupActivity.this, MainActivity.class);
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(it);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }, null);
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, getString(R.string.backup_restore_failed), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void unpackZip(byte[] zip) throws Throwable {
        File dataDir = getFilesDir().getParentFile();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry e;
        byte[] buf = new byte[8192];
        while ((e = zis.getNextEntry()) != null) {
            String name = e.getName();
            if ("keys.txt".equals(name)) {
                readKeysInto(zis);
                continue;
            }
            if (name.contains("..")) continue;
            File out = new File(dataDir, name);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(out);
            int n;
            while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
            fos.flush();
            fos.close();
            zis.closeEntry();
        }
        zis.close();
    }

    // ---------- 加解密 ----------

    private static byte[] encrypt(byte[] data, String pwd) throws Throwable {
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        SecretKeySpec key = key(pwd, salt);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] enc = c.doFinal(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(salt);
        out.write(iv);
        out.write(enc);
        return out.toByteArray();
    }

    private static byte[] decrypt(byte[] blob, String pwd) throws Throwable {
        if (blob == null || blob.length < 33) throw new java.io.IOException("bad");
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        System.arraycopy(blob, 0, salt, 0, 16);
        System.arraycopy(blob, 16, iv, 0, 16);
        byte[] enc = new byte[blob.length - 32];
        System.arraycopy(blob, 32, enc, 0, enc.length);
        SecretKeySpec key = key(pwd, salt);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return c.doFinal(enc);
    }

    private static SecretKeySpec key(String pwd, byte[] salt) throws Throwable {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] k = f.generateSecret(new PBEKeySpec(pwd.toCharArray(), salt, 2000, 256)).getEncoded();
        return new SecretKeySpec(k, "AES");
    }
}
