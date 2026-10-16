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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 多账户管理：每个服务商下可保存多个命名 Key。
 * 数据结构：{provider: [{name, ...}, ...]}，key 本身经 {@link CryptoManager}
 * 加密后存入 ds_secure_prefs（键 "account_<providerId>_<name>"），此处不落明文。
 */
public final class AccountManager {

    private static final String PREFS = "ds_accounts";
    private static final String KEY_DATA = "accounts";

    private AccountManager() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static class Account {
        public String name;
        public String key;
    }

    /** 某服务商的账户列表。 */
    public static List<Account> getAccounts(Context c, int providerId) {
        List<Account> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optInt("provider", -1) == providerId) {
                    Account a = new Account();
                    a.name = o.optString("name", c.getString(R.string.x_003));
                    // key 加密存储于 ds_secure_prefs，读回时解密；解密失败视为空
                    a.key = CryptoManager.read(c, keyId(providerId, a.name));
                    if (a.key == null) a.key = "";
                    list.add(a);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /** 添加/更新账户（同名覆盖）。key 加密存储，不落明文。成功返回 true。 */
    public static boolean saveAccount(Context c, int providerId, String name, String key) {
        if (name == null || name.trim().isEmpty()) return false;
        if (key == null || key.trim().isEmpty()) return false;
        name = name.trim();
        try {
            // 先加密 key，失败则不落任何数据
            if (!CryptoManager.save(c, keyId(providerId, name), key)) return false;

            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            // 删除同名旧账户
            JSONArray cleaned = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optInt("provider", -1) == providerId && name.equals(o.optString("name"))) continue;
                cleaned.put(o);
            }
            JSONObject n = new JSONObject();
            n.put("provider", providerId);
            n.put("name", name);
            n.put("key", ""); // 占位：真实 key 在 CryptoManager 的 ds_secure_prefs
            cleaned.put(n);
            sp(c).edit().putString(KEY_DATA, cleaned.toString()).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void removeAccount(Context c, int providerId, String name) {
        if (name == null) return;
        try {
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            JSONArray cleaned = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optInt("provider", -1) == providerId && name.equals(o.optString("name"))) {
                    CryptoManager.remove(c, keyId(providerId, name));
                    continue;
                }
                cleaned.put(o);
            }
            sp(c).edit().putString(KEY_DATA, cleaned.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static void clearAll(Context c) {
        try {
            // 逐个删除加密 key，避免残留
            JSONArray arr = new JSONArray(sp(c).getString(KEY_DATA, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                CryptoManager.remove(c, keyId(o.optInt("provider", -1), o.optString("name", "")));
            }
        } catch (Exception ignored) {
        }
        sp(c).edit().remove(KEY_DATA).apply();
    }

    /** CryptoManager 中某账户 key 的存储前缀。 */
    private static String keyId(int providerId, String name) {
        return "account_" + providerId + "_" + name;
    }
}
