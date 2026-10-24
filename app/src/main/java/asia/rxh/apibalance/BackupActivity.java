package asia.rxh.apibalance;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 备份与恢复：把 shared_prefs、databases 和 API Key 打包后用口令加密。
 * 新格式为 PBKDF2-SHA256 + AES-GCM；旧版 CBC 格式仍可读取。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public class BackupActivity extends Activity {

    private static final int REQ_RESTORE = 8101;
    private static final byte[] MAGIC = {'R', 'X', 'H', 'B', 'K', '2'};
    private static final int PBKDF2_ITERATIONS = 210000;
    private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 256 * 1024 * 1024;

    private int format = 0;
    private Uri pendingRestore;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppPrefs.wrapLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        ThemeUtils.apply(findViewById(android.R.id.content));

        View back = findViewById(R.id.bk_back);
        if (back != null) back.setOnClickListener(v -> finish());
        View doBackup = findViewById(R.id.bk_do_backup);
        if (doBackup != null) doBackup.setOnClickListener(v -> pickFormat());
        View doRestore = findViewById(R.id.bk_do_restore);
        if (doRestore != null) doRestore.setOnClickListener(v -> pickBackupFile());
    }

    private void pickFormat() {
        UiDialog.showPicker(this, getString(R.string.backup_pick_format),
                new String[]{getString(R.string.backup_export_bin), getString(R.string.backup_export_zip)},
                format, i -> {
                    format = i;
                    askPasswordThenBackup();
                });
    }

    /** 口令不落盘，每次备份都重新输入。 */
    private void askPasswordThenBackup() {
        UiDialog.showInputWithDesc(this, getString(R.string.backup_need_pwd),
                getString(R.string.backup_need_pwd_desc), getString(R.string.f2a_row_hint), "", text -> {
                    if (text == null || text.trim().length() < 4) {
                        toast(getString(R.string.backup_pwd_short));
                        return;
                    }
                    doBackup(text.trim());
                });
    }

    private void doBackup(final String pwd) {
        toast(getString(R.string.backup_starting));
        AppExecutors.io().execute(() -> {
            try {
                byte[] zip = collectIntoZip();
                byte[] blob = encrypt(zip, pwd);
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
                String name = "APIBalance_backup_" + System.currentTimeMillis() + ext;
                String where = saveToDownloads(payload, name);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this,
                            getString(R.string.backup_saved) + "\n" + where,
                            android.widget.Toast.LENGTH_LONG).show();
                });
            } catch (Throwable t) {
                runOnUiThread(() -> toast(getString(R.string.backup_failed)));
            }
        });
    }

    private String saveToDownloads(byte[] data, String name) throws Throwable {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver()
                    .insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri != null) {
                java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(data);
                    os.flush();
                    os.close();
                }
                return "Download/" + name;
            }
        }
        File dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        if (dir != null && (dir.exists() || dir.mkdirs())) {
            File f = new File(dir, name);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(data);
            }
            return f.getAbsolutePath();
        }
        File f2 = new File(getExternalFilesDir(null), name);
        try (FileOutputStream fos = new FileOutputStream(f2)) {
            fos.write(data);
        }
        return f2.getAbsolutePath();
    }

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

    /** 主 Key 和多账户 Key 都写入 keys.txt；账户名用 Base64，避免分隔符冲突。 */
    private void addKeys(ZipOutputStream zos) throws Throwable {
        StringBuilder sb = new StringBuilder();
        for (int pid = 0; pid <= 5; pid++) {
            String v = safeRead("provider_" + pid + "_api_key");
            if (v == null || v.isEmpty()) continue;
            sb.append("P\t").append(pid).append('\t').append(b64(v)).append('\n');
        }
        for (int pid = 0; pid <= 5; pid++) {
            List<AccountManager.Account> accounts = AccountManager.getAccounts(this, pid);
            for (AccountManager.Account a : accounts) {
                if (a == null || a.key == null || a.key.isEmpty()) continue;
                sb.append("A\t").append(pid).append('\t')
                        .append(b64(a.name == null ? "" : a.name)).append('\t')
                        .append(b64(a.key)).append('\n');
            }
        }
        if (sb.length() == 0) return;
        zos.putNextEntry(new ZipEntry("keys.txt"));
        zos.write(sb.toString().getBytes("UTF-8"));
        zos.closeEntry();
    }

    private String safeRead(String key) {
        try {
            return CryptoManager.read(this, key);
        } catch (Throwable t) {
            return null;
        }
    }

    private void addDir(ZipOutputStream zos, File dir, String prefix) throws Throwable {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f == null || !f.isFile()) continue;
            String n = f.getName();
            if (n.endsWith("-journal") || n.endsWith("-wal")) continue;
            // 旧备份口令和本机 Keystore 密文没有恢复价值，不带走
            if ("ds_backup_prefs.xml".equals(n) || "ds_secure_prefs.xml".equals(n)) continue;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (InputStream in = new java.io.FileInputStream(f)) {
                byte[] buf = new byte[8192];
                int c;
                while ((c = in.read(buf)) > 0) {
                    b.write(buf, 0, c);
                    if (b.size() > MAX_ENTRY_BYTES) throw new java.io.IOException("entry too large");
                }
            }
            zos.putNextEntry(new ZipEntry(prefix + n));
            zos.write(b.toByteArray());
            zos.closeEntry();
        }
    }

    private void pickBackupFile() {
        try {
            Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            it.addCategory(Intent.CATEGORY_OPENABLE);
            it.setType("*/*");
            startActivityForResult(it, REQ_RESTORE);
        } catch (Throwable t) {
            toast(getString(R.string.backup_restore_failed));
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_RESTORE || res != RESULT_OK || data == null || data.getData() == null) return;
        pendingRestore = data.getData();
        UiDialog.showInputWithDesc(this, getString(R.string.backup_need_pwd),
                getString(R.string.backup_need_pwd_desc), getString(R.string.f2a_row_hint), "", text -> {
                    if (text == null || text.trim().length() < 4) {
                        toast(getString(R.string.backup_pwd_short));
                        return;
                    }
                    doRestore(pendingRestore, text.trim());
                });
    }

    private void doRestore(final Uri uri, final String pwd) {
        toast(getString(R.string.backup_restoring));
        AppExecutors.io().execute(() -> {
            try {
                byte[] raw = readAll(uri);
                if (raw.length > 4 && raw[0] == 0x50 && raw[1] == 0x4B) {
                    ZipInputStream z0 = new ZipInputStream(new ByteArrayInputStream(raw));
                    ZipEntry e0 = z0.getNextEntry();
                    if (e0 != null) raw = readEntry(z0, MAX_ENTRY_BYTES);
                    z0.close();
                }
                byte[] zip = decrypt(raw, pwd);
                unpackZip(zip);
                runOnUiThread(() -> UiDialog.showConfirm(this, getString(R.string.backup_row_title),
                        getString(R.string.backup_restored), getString(R.string.h_004), null,
                        () -> {
                            Intent it = new Intent(BackupActivity.this, MainActivity.class);
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(it);
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }, null));
            } catch (Throwable t) {
                runOnUiThread(() -> toast(getString(R.string.backup_restore_failed)));
            }
        });
    }

    private byte[] readAll(Uri uri) throws Throwable {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new java.io.IOException("no stream");
        try (InputStream is = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                b.write(buf, 0, n);
                if (b.size() > MAX_TOTAL_BYTES) throw new java.io.IOException("too large");
            }
            return b.toByteArray();
        }
    }

    private static byte[] readEntry(ZipInputStream zis, int limit) throws Throwable {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = zis.read(buf)) > 0) {
            b.write(buf, 0, n);
            if (b.size() > limit) throw new java.io.IOException("entry too large");
        }
        return b.toByteArray();
    }

    private void unpackZip(byte[] zip) throws Throwable {
        File dataDir = getFilesDir().getParentFile();
        String base = dataDir.getCanonicalPath();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry e;
        long total = 0;
        while ((e = zis.getNextEntry()) != null) {
            String name = e.getName().replace('\\', '/');
            if ("keys.txt".equals(name)) {
                readKeysInto(zis);
                continue;
            }
            if (name.contains("..")) continue;
            if (!name.startsWith("shared_prefs/") && !name.startsWith("databases/")) continue;
            if (name.endsWith("ds_backup_prefs.xml") || name.endsWith("ds_secure_prefs.xml")) continue;

            File out = new File(dataDir, name);
            String canonical = out.getCanonicalPath();
            if (!canonical.startsWith(base + File.separator)) continue;
            File parent = out.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) continue;

            byte[] content = readEntry(zis, MAX_ENTRY_BYTES);
            total += content.length;
            if (total > MAX_TOTAL_BYTES) throw new java.io.IOException("backup too large");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(content);
            }
        }
        zis.close();
    }

    /** keys.txt：P=主 Key，A=多账户；同时兼容旧版“pid\tBase64”格式。 */
    private void readKeysInto(ZipInputStream zis) throws Throwable {
        String txt = new String(readEntry(zis, 4 * 1024 * 1024), "UTF-8");
        for (String line : txt.split("\n")) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\t");
            try {
                if ("P".equals(parts[0]) && parts.length >= 3) {
                    int pid = Integer.parseInt(parts[1]);
                    if (pid < 0 || pid > 5) continue;
                    CryptoManager.save(this, "provider_" + pid + "_api_key", unb64(parts[2]));
                } else if ("A".equals(parts[0]) && parts.length >= 4) {
                    int pid = Integer.parseInt(parts[1]);
                    if (pid < 0 || pid > 5) continue;
                    AccountManager.saveAccount(this, pid, unb64(parts[2]), unb64(parts[3]));
                } else if (parts.length >= 2) {
                    int pid = Integer.parseInt(parts[0]);
                    if (pid < 0 || pid > 5) continue;
                    CryptoManager.save(this, "provider_" + pid + "_api_key", unb64(parts[1]));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------- 加密 ----------

    private static byte[] encrypt(byte[] data, String pwd) throws Throwable {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key(pwd, salt), new GCMParameterSpec(128, iv));
        byte[] enc = c.doFinal(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC);
        out.write(salt);
        out.write(iv);
        out.write(enc);
        return out.toByteArray();
    }

    private static byte[] decrypt(byte[] blob, String pwd) throws Throwable {
        if (blob != null && startsWith(blob, MAGIC)) {
            int off = MAGIC.length;
            if (blob.length < off + 16 + 12 + 16) throw new java.io.IOException("bad");
            byte[] salt = slice(blob, off, 16);
            byte[] iv = slice(blob, off + 16, 12);
            byte[] enc = slice(blob, off + 28, blob.length - off - 28);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(pwd, salt), new GCMParameterSpec(128, iv));
            return c.doFinal(enc);
        }
        return decryptLegacy(blob, pwd);
    }

    private static byte[] decryptLegacy(byte[] blob, String pwd) throws Throwable {
        if (blob == null || blob.length < 33) throw new java.io.IOException("bad");
        byte[] salt = slice(blob, 0, 16);
        byte[] iv = slice(blob, 16, 16);
        byte[] enc = slice(blob, 32, blob.length - 32);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] k = f.generateSecret(new PBEKeySpec(pwd.toCharArray(), salt, 2000, 256)).getEncoded();
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k, "AES"), new IvParameterSpec(iv));
        return c.doFinal(enc);
    }

    private static SecretKeySpec key(String pwd, byte[] salt) throws Throwable {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] k = f.generateSecret(new PBEKeySpec(pwd.toCharArray(), salt,
                PBKDF2_ITERATIONS, 256)).getEncoded();
        return new SecretKeySpec(k, "AES");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (data[i] != prefix[i]) return false;
        return true;
    }

    private static byte[] slice(byte[] data, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(data, off, out, 0, len);
        return out;
    }

    private static String b64(String s) {
        return android.util.Base64.encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.NO_WRAP);
    }

    private static String unb64(String s) {
        return new String(android.util.Base64.decode(s, android.util.Base64.NO_WRAP),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}

