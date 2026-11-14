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
 */

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * 老版本用的是 ds_ 前缀的 SharedPreferences 文件，1.5.4 起统一换成不带前缀的名字。
 *
 * 迁移原则：**只补不覆盖**。
 * · 老文件里有、新文件没有的键，原样搬过去；
 * · 新文件里已有的键一律不动，避免把用户后来改过的设置冲掉；
 * · 老文件保留不删，万一新版本出问题还能回退；
 * · 加密值依赖 Keystore 别名，不依赖文件名，搬过去照样能解密。
 *
 * Application 启动时调用一次，所有进程都会走，重复执行也是安全的。
 */
public final class PrefsMigrator {

    private static final String MARKER = "migrated_v2";
    private static final String MARKER_FILE = "migration_meta";

    /** 老文件名 → 新文件名。 */
    private static final String[][] MAP = {
            {"ds_settings", "settings"},
            {"ds_privacy", "privacy"},
            {"ds_accounts", "accounts"},
            {"ds_secure_prefs", "secure_prefs"},
            {"ds_balance_prefs", "balance_prefs"},
            {"ds_env", "env"},
            {"ds_onb_tmp", "onboarding_tmp"},
            {"ds_app_prefs", "app_prefs"},
            {"ds_backup_prefs", "backup_prefs"},
            {"ds_theme", "theme"},
            {"ds_debug_prefs", "debug_prefs"},
            {"ds_android_theme", "android_theme"},
            {"ds_balance_history", "balance_history"},
            {"ds_crash_prefs", "crash_prefs"},
            {"ds_token_usage", "token_usage"},
    };

    private PrefsMigrator() {
    }

    public static void migrate(Context ctx) {
        if (ctx == null) return;
        try {
            Context app = ctx.getApplicationContext();
            SharedPreferences meta = app.getSharedPreferences(MARKER_FILE, Context.MODE_PRIVATE);
            if (meta.getBoolean(MARKER, false)) return;
            for (String[] pair : MAP) {
                copy(app, pair[0], pair[1]);
            }
            // 标记写在最后：中途被杀掉下次会重跑，仍然是“只补不覆盖”，不会损坏数据
            meta.edit().putBoolean(MARKER, true).commit();
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void copy(Context ctx, String from, String to) {
        try {
            SharedPreferences oldSp = ctx.getSharedPreferences(from, Context.MODE_PRIVATE);
            Map<String, ?> all = oldSp.getAll();
            if (all.isEmpty()) return;
            SharedPreferences newSp = ctx.getSharedPreferences(to, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = newSp.edit();
            for (Map.Entry<String, ?> e : all.entrySet()) {
                String key = e.getKey();
                if (newSp.contains(key)) continue;   // 已有的不动
                Object v = e.getValue();
                if (v instanceof String) editor.putString(key, (String) v);
                else if (v instanceof Integer) editor.putInt(key, (Integer) v);
                else if (v instanceof Long) editor.putLong(key, (Long) v);
                else if (v instanceof Float) editor.putFloat(key, (Float) v);
                else if (v instanceof Boolean) editor.putBoolean(key, (Boolean) v);
                else if (v instanceof java.util.Set) {
                    //noinspection unchecked
                    editor.putStringSet(key, (java.util.Set<String>) v);
                }
            }
            editor.commit();
        } catch (Throwable ignored) {
        }
    }
}
