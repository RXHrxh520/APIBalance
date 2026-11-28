package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 指纹解锁的密码学绑定。
 *
 * 以前的做法是：BiometricPrompt 回调里拿到认证成功就直接放行，认证结果没有参与任何
 * 密码学运算（CodeQL: insecure local authentication）。现在改成：
 * Keystore 里存一把 AES-256-GCM 密钥，设置密码时用它加密一段固定标记（canary）；
 * 指纹通过后，必须用同一个 Cipher（通过 CryptoObject 传进去）真正把标记解出来并逐字节
 * 核对，核对通过才算解锁。指纹没通过，这个 Cipher 根本执行不了 doFinal。
 */
final class LockToken {

    private static final String ALIAS = "apibalance_lock_token";
    private static final String PREFS = "lock_token";
    private static final byte[] CANARY =
            "APIBALANCE-UNLOCK-OK".getBytes(StandardCharsets.UTF_8);

    private LockToken() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 指纹解锁是否可用：KeyStore 里有钥匙，且存过 nonce/密文。 */
    static boolean isAvailable(Context c) {
        if (c == null || Build.VERSION.SDK_INT < 28) return false;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            return ks.containsAlias(ALIAS) && sp(c).contains("nonce") && sp(c).contains("ct");
        } catch (Throwable t) {
            return false;
        }
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        Object existing = ks.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setInvalidatedByBiometricEnrollment(true)
                .build());
        return kg.generateKey();
    }

    /** 设置/修改密码成功后调用：换一把新钥匙，重新加密标记。失败就当作没有指纹解锁。 */
    static void provision(Context c) {
        if (c == null || Build.VERSION.SDK_INT < 28) return;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS); // 换密码 = 换钥匙
            Cipher ciph = Cipher.getInstance("AES/GCM/NoPadding");
            ciph.init(Cipher.ENCRYPT_MODE, key());
            byte[] ct = ciph.doFinal(CANARY);
            byte[] nonce = ciph.getIV();
            sp(c).edit()
                    .putString("nonce", Base64.encodeToString(nonce, Base64.NO_WRAP))
                    .putString("ct", Base64.encodeToString(ct, Base64.NO_WRAP))
                    .apply();
        } catch (Throwable ignored) {
            invalidate(c);
        }
    }

    /** 交给 BiometricPrompt 的 CryptoObject：必须带着它去认证，否则 doFinal 不成立。 */
    static Cipher decryptCipher(Context c) throws Exception {
        byte[] nonce = Base64.decode(sp(c).getString("nonce", ""), Base64.NO_WRAP);
        if (nonce.length != 12) throw new IllegalStateException("bad nonce");
        Cipher ciph = Cipher.getInstance("AES/GCM/NoPadding");
        ciph.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
        return ciph;
    }

    /** 认证成功后真正干活的地方：把标记解出来比对，成功才解锁。 */
    static boolean verify(Context c, Cipher ciph) {
        if (c == null || ciph == null) return false;
        try {
            byte[] ct = Base64.decode(sp(c).getString("ct", ""), Base64.NO_WRAP);
            return Arrays.equals(ciph.doFinal(ct), CANARY);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 关闭/重置密码、钥匙被系统作废（换指纹）时调用。 */
    static void invalidate(Context c) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        } catch (Throwable ignored) {
        }
        if (c != null) sp(c).edit().clear().apply();
    }
}
