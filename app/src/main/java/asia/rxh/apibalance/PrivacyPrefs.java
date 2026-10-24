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

import android.content.Context;
import android.content.SharedPreferences;

/** 锁屏密码、恢复代码与隐私开关。密码和恢复代码只保存加盐慢哈希。 */
public final class PrivacyPrefs {

    private static final String PREFS = "ds_privacy";
    private static final String KEY_LOCK = "lock_enabled";
    private static final String KEY_HIDE_BALANCE = "hide_balance";
    private static final String KEY_PASSWORD = "lock_password";
    private static final String KEY_SALT = "lock_salt";
    private static final String KEY_F2A = "f2a";
    private static final String KEY_F2A_SALT = "f2a_salt";

    private static final int PBKDF2_ITERATIONS = 210000;
    private static final int LEGACY_PBKDF2_ITERATIONS = 12000;

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

    public static boolean shouldMaskBalance(Context c) {
        return c != null && (isLockEnabled(c) || isHideBalance(c));
    }

    public static String maskBalance(Context c, String text) {
        return shouldMaskBalance(c) ? "••••" : text;
    }

    public static boolean hasPassword(Context c) {
        return sp(c).getString(KEY_PASSWORD, null) != null;
    }

    public static void setPassword(Context c, String pw) {
        if (pw == null || pw.length() < 4) return;
        byte[] salt = randomSalt();
        String hash = pbkdf2(salt, pw, PBKDF2_ITERATIONS);
        if (hash == null) return;
        sp(c).edit()
                .putString(KEY_SALT, toHex(salt))
                .putString(KEY_PASSWORD, "pbkdf2:" + PBKDF2_ITERATIONS + ":" + hash)
                .apply();
    }

    public static boolean checkPassword(Context c, String pw) {
        if (pw == null) return false;
        String stored = sp(c).getString(KEY_PASSWORD, null);
        String saltHex = sp(c).getString(KEY_SALT, null);
        if (stored == null || saltHex == null) return false;
        if (stored.startsWith("pbkdf2:")) {
            String rest = stored.substring(7);
            int iterations = LEGACY_PBKDF2_ITERATIONS;
            String hash = rest;
            int split = rest.indexOf(':');
            if (split > 0) {
                iterations = parseInt(rest.substring(0, split), LEGACY_PBKDF2_ITERATIONS);
                hash = rest.substring(split + 1);
            }
            String calc = pbkdf2(fromHex(saltHex), pw, iterations);
            return calc != null && constantTimeEquals(calc, hash);
        }
        String hash = sha256(saltHex + pw);
        return hash != null && constantTimeEquals(hash, stored);
    }

    public static void clearPassword(Context c) {
        sp(c).edit().remove(KEY_PASSWORD).remove(KEY_SALT).apply();
    }

    /** 恢复代码：设置时只保存哈希，不再落明文。 */
    public static void setF2a(Context c, String code) {
        if (code == null || code.trim().isEmpty()) {
            sp(c).edit().remove(KEY_F2A).remove(KEY_F2A_SALT).apply();
            return;
        }
        byte[] salt = randomSalt();
        String hash = pbkdf2(salt, code.trim(), PBKDF2_ITERATIONS);
        if (hash == null) return;
        sp(c).edit()
                .putString(KEY_F2A_SALT, toHex(salt))
                .putString(KEY_F2A, "pbkdf2:" + PBKDF2_ITERATIONS + ":" + hash)
                .apply();
    }

    /** 校验恢复代码；兼容旧版本遗留的明文。 */
    public static boolean checkF2a(Context c, String code) {
        String stored = sp(c).getString(KEY_F2A, "");
        if (stored == null || stored.isEmpty() || code == null) return false;
        String input = code.trim();
        if (stored.startsWith("pbkdf2:")) {
            String saltHex = sp(c).getString(KEY_F2A_SALT, null);
            if (saltHex == null) return false;
            String rest = stored.substring(7);
            int iterations = LEGACY_PBKDF2_ITERATIONS;
            String hash = rest;
            int split = rest.indexOf(':');
            if (split > 0) {
                iterations = parseInt(rest.substring(0, split), LEGACY_PBKDF2_ITERATIONS);
                hash = rest.substring(split + 1);
            }
            String calc = pbkdf2(fromHex(saltHex), input, iterations);
            return calc != null && constantTimeEquals(calc, hash);
        }
        boolean ok = constantTimeEquals(stored, input);
        if (ok) setF2a(c, input);
        return ok;
    }

    /** 仅供迁移/诊断使用；新代码应使用 checkF2a。 */
    public static String getF2a(Context c) {
        return sp(c).getString(KEY_F2A, "");
    }

    public static boolean hasF2a(Context c) {
        String v = getF2a(c);
        return v != null && !v.isEmpty();
    }

    public static void clearAll(Context c) {
        sp(c).edit().clear().apply();
    }

    public static int getLockDelay(Context c) {
        return sp(c).getInt("lock_delay", 0);
    }

    public static void setLockDelay(Context c, int minutes) {
        sp(c).edit().putInt("lock_delay", minutes).apply();
    }

    public static int getLockMethod(Context c) {
        return sp(c).getInt("lock_method", 0);
    }

    public static void setLockMethod(Context c, int method) {
        sp(c).edit().putInt("lock_method", method).apply();
    }

    private static byte[] randomSalt() {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        return salt;
    }

    private static String pbkdf2(byte[] salt, String pw, int iterations) {
        try {
            javax.crypto.SecretKeyFactory f =
                    javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec =
                    new javax.crypto.spec.PBEKeySpec(pw.toCharArray(), salt, iterations, 256);
            byte[] dk = f.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return toHex(dk);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
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
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}

