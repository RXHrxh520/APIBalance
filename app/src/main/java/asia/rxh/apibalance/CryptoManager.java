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
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * API Key 安全存储：Android Keystore 生成 AES 密钥，对内容做 AES/GCM 加密后再落盘。
 * 密钥不出硬件 Keystore，明文不会直接写入 SharedPreferences。
 */
public final class CryptoManager {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "rxh_ds_apikey";
    private static final String PREFS = "ds_secure_prefs";
    private static final String KEY_CIPHER = "enc_apikey";
    private static final String KEY_IV = "enc_apikey_iv";

    private static final int GCM_TAG_BITS = 128;

    private CryptoManager() {
    }

    /** 加密后写入 SharedPreferences。 */
    public static boolean save(Context ctx, String providerKey, String value) {
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] enc = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();

            // 用 commit() 同步落盘，避免 apply() 异步窗口导致保存后立刻读取失败
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(providerKey + "_" + KEY_CIPHER, Base64.encodeToString(enc, Base64.NO_WRAP))
                .putString(providerKey + "_" + KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit();
        } catch (Exception e) {
            return false;
        }
    }

    /** 解密读取；不存在或解密失败返回 null。 */
    public static String read(Context ctx, String providerKey) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String encB64 = sp.getString(providerKey + "_" + KEY_CIPHER, null);
            String ivB64 = sp.getString(providerKey + "_" + KEY_IV, null);
            if (encB64 == null || ivB64 == null) return null;

            byte[] enc = Base64.decode(encB64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivB64, Base64.NO_WRAP);

            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** 清除某个服务商保存的密钥。 */
    public static void remove(Context ctx, String providerKey) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(providerKey + "_" + KEY_CIPHER)
            .remove(providerKey + "_" + KEY_IV)
            .commit();
    }

    /** 清空全部加密数据，并删除 Keystore 密钥（下次保存时自动重建）。 */
    public static void clearAll(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) {
                ks.deleteEntry(KEY_ALIAS);
            }
        } catch (Exception ignored) {
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        // 首次：生成 AES-256 密钥（API 26+ 支持）
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        kg.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return kg.generateKey();
    }
}
