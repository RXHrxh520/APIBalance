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

/**
 * 隐私/锁屏设置：指纹锁、隐藏余额、密码等。
 * 密码以「随机盐 + SHA-256 哈希」存储，不落明文。
 */
public final class PrivacyPrefs {

    private static final String PREFS = "ds_privacy";
    private static final String KEY_LOCK = "lock_enabled";
    private static final String KEY_HIDE_BALANCE = "hide_balance";
    private static final String KEY_PASSWORD = "lock_password"; // 存 salt:hash
    private static final String KEY_SALT = "lock_salt";

    private PrivacyPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLockEnabled(Context c) {
        return sp(c).getBoolean(KEY_LOCK, false);
    }

    public static void setLockEnabled(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_LOCK, b).apply();
    }

    public static boolean isHideBalance(Context c) {
        return sp(c).getBoolean(KEY_HIDE_BALANCE, false);
    }

    public static void setHideBalance(Context c, boolean b) {
        sp(c).edit().putBoolean(KEY_HIDE_BALANCE, b).apply();
    }

    /** 锁屏或「隐藏余额」开启时，任何地方都不应直接展示余额数字。 */
    public static boolean shouldMaskBalance(Context c) {
        return isLockEnabled(c) || isHideBalance(c);
    }

    /** 按隐私设置打码余额文本。 */
    public static String maskBalance(Context c, String text) {
        return shouldMaskBalance(c) ? "••••" : text;
    }

    /** 是否已设置锁屏密码。 */
    public static boolean hasPassword(Context c) {
        return sp(c).getString(KEY_PASSWORD, null) != null;
    }

    /** 设置锁屏密码：生成随机盐，存 salt:sha256(salt+pw)。少于 4 位拒绝。 */
    public static void setPassword(Context c, String pw) {
        if (pw == null || pw.length() < 4) return;
        try {
            byte[] salt = new byte[16];
            new java.security.SecureRandom().nextBytes(salt);
            String hash = pbkdf2(salt, pw);
            if (hash == null) return;
            sp(c).edit()
                    .putString(KEY_SALT, toHex(salt))
                    .putString(KEY_PASSWORD, "pbkdf2:" + hash)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    /** 校验密码；不存在或错误返回 false。 */
    public static boolean checkPassword(Context c, String pw) {
        if (pw == null) return false;
        String stored = sp(c).getString(KEY_PASSWORD, null);
        String saltHex = sp(c).getString(KEY_SALT, null);
        if (stored == null || saltHex == null) return false;
        if (stored.startsWith("pbkdf2:")) {
            String hash = pbkdf2(fromHex(saltHex), pw);
            return hash != null && constantTimeEquals(hash, stored.substring("pbkdf2:".length()));
        }
        // 兼容旧版本 sha256(salt+pw) 格式
        String hash = sha256(saltHex + pw);
        return hash != null && constantTimeEquals(hash, stored);
    }

    /** 清除密码（与锁开关一起由清除数据/设置处理）。 */
    public static void clearPassword(Context c) {
        sp(c).edit().remove(KEY_PASSWORD).remove(KEY_SALT).apply();
    }

    /** PBKDF2 迭代次数。 */
    private static final int PBKDF2_ITERATIONS = 12000;

    /** PBKDF2-HMAC-SHA256 派生 256 位密钥（慢哈希，抗暴力破解）。 */
    private static String pbkdf2(byte[] salt, String pw) {
        try {
            javax.crypto.SecretKeyFactory f =
                    javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec =
                    new javax.crypto.spec.PBEKeySpec(pw.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            byte[] dk = f.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return toHex(dk);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return toHex(md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /** 自动锁定延迟（分钟，0 = 每次回到应用都验证）。 */
    public static int getLockDelay(Context c) {
        return sp(c).getInt("lock_delay", 0);
    }

    public static void setLockDelay(Context c, int minutes) {
        sp(c).edit().putInt("lock_delay", minutes).apply();
    }

    /** 验证方式：0 = 软件密码，1 = 系统原生验证。 */
    public static int getLockMethod(Context c) {
        return sp(c).getInt("lock_method", 0);
    }

    public static void setLockMethod(Context c, int method) {
        sp(c).edit().putInt("lock_method", method).apply();
    }

    /** F2a 验证代码：忘记密码时用它重置，由用户提前设置。 */
    public static String getF2a(Context c) {
        return sp(c).getString("f2a", "");
    }

    public static void setF2a(Context c, String code) {
        sp(c).edit().putString("f2a", code == null ? "" : code).apply();
    }

    public static boolean hasF2a(Context c) {
        return !getF2a(c).isEmpty();
    }

    /** 清空本模块记录的全部隐私设置。 */
    public static void clearAll(Context c) {
        sp(c).edit().clear().apply();
    }
}
