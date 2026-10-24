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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.content.Context;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多服务商余额/用量查询配置（端点与字段已按 2026 年各家官方文档核对）。
 *
 * 服务商与查询形态：
 *  - DeepSeek   预存余额   GET /user/balance（字段为字符串，货币 CNY/USD）
 *  - Kimi       预存余额   GET /v1/users/me/balance（币种为人民币元）
 *  - 智谱 GLM   预存余额   GET /api/paas/v4/balance（data.balance / data.grantBalance）
 *  - OpenAI     无公开余额 API（dashboard/billing 已废弃，需网页 session key），标记为不支持
 *  - Anthropic  后付费成本 GET /v1/organizations/cost_report（需管理员 key，x-api-key 认证）
 */
public final class ProviderConfig {

    public static final int PROVIDER_DEEPSEEK = 0;
    public static final int PROVIDER_KIMI = 1;
    public static final int PROVIDER_GLM = 2;
    public static final int PROVIDER_OPENAI = 3;
    public static final int PROVIDER_ANTHROPIC = 4;
    public static final int PROVIDER_CUSTOM = 5; // 用户自定义供应商

    /** 认证方式：Bearer 头 或 x-api-key 头。 */
    public static final int AUTH_BEARER = 0;
    public static final int AUTH_X_API_KEY = 1;

    /** 查询类型：预存余额 / 后付费成本 / 不支持。 */
    public static final int TYPE_BALANCE = 0;
    public static final int TYPE_COST = 1;
    public static final int TYPE_UNSUPPORTED = 2;

    /** Anthropic 要求的版本头。 */
    public static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final Provider[] PROVIDERS = {
        new Provider(
            PROVIDER_DEEPSEEK, "DeepSeek", new String[]{"sk-"},
            "https://api.deepseek.com/user/balance", AUTH_BEARER, "CNY", TYPE_BALANCE, null),
        new Provider(
            PROVIDER_KIMI, "Kimi", new String[]{"sk-"},
            "https://api.moonshot.cn/v1/users/me/balance", AUTH_BEARER, "CNY", TYPE_BALANCE, null),
        new Provider(
            PROVIDER_GLM, "智谱GLM", new String[]{"sk-", "id."},
            "https://open.bigmodel.cn/api/paas/v4/balance", AUTH_BEARER, "CNY", TYPE_BALANCE, null),
        new Provider(
            PROVIDER_OPENAI, "OpenAI", new String[]{"sk-"},
            "https://api.openai.com/v1/dashboard/billing/subscription", AUTH_BEARER, "USD", TYPE_UNSUPPORTED, null),
        new Provider(
            PROVIDER_ANTHROPIC, "Anthropic", new String[]{"sk-ant-"},
            "https://api.anthropic.com/v1/organizations/cost_report", AUTH_X_API_KEY, "USD", TYPE_COST, null),
    };

    private ProviderConfig() {
    }

    /** 按 id 取服务商；越界时回退 DeepSeek（防御历史脏数据）。 */
    public static Provider get(int providerId) {
        if (providerId == PROVIDER_CUSTOM) {
            // 无上下文的兜底：仅给个占位名，真实配置经 get(Context,id) 读取
            return new Provider(PROVIDER_CUSTOM, "自定义服务商", new String[]{},
                    null, AUTH_BEARER, "CNY", TYPE_BALANCE, null);
        }
        if (providerId < 0 || providerId >= PROVIDERS.length) {
            return PROVIDERS[PROVIDER_DEEPSEEK];
        }
        return PROVIDERS[providerId];
    }

    /** 带上下文的取法：自定义供应商动态读取用户本地配置。 */
    public static Provider get(Context ctx, int providerId) {
        if (providerId == PROVIDER_CUSTOM) {
            return getCustom(ctx);
        }
        return get(providerId);
    }

    /** 从本地配置构造自定义供应商；未配置完整时回退 DeepSeek。 */
    public static Provider getCustom(Context ctx) {
        if (ctx == null || !AppPrefs.hasCustomProvider(ctx)) {
            return PROVIDERS[PROVIDER_DEEPSEEK];
        }
        String name = AppPrefs.getCustomProviderName(ctx).trim();
        String url = AppPrefs.getCustomProviderUrl(ctx).trim();
        int auth = AppPrefs.getCustomProviderAuth(ctx);
        String currency = AppPrefs.getCustomProviderCurrency(ctx).trim();
        if (currency.isEmpty()) currency = "CNY";
        String path = AppPrefs.getCustomProviderPath(ctx).trim();
        return new Provider(PROVIDER_CUSTOM, name, new String[]{}, url, auth, currency, TYPE_BALANCE, path);
    }

    /** 界面上可选的服务商（OpenAI 已全量下线，不再出现在任何列表里）。 */
    public static int[] pickableIds() {
        return new int[]{PROVIDER_DEEPSEEK, PROVIDER_KIMI, PROVIDER_GLM, PROVIDER_ANTHROPIC};
    }

    public static int count() {
        return PROVIDERS.length;
    }

    public static String[] names() {
        String[] n = new String[PROVIDERS.length];
        for (int i = 0; i < PROVIDERS.length; i++) n[i] = PROVIDERS[i].name;
        return n;
    }

    /** 含自定义供应商的总数。 */
    public static int count(Context ctx) {
        return (ctx != null && AppPrefs.hasCustomProvider(ctx)) ? PROVIDERS.length + 1 : PROVIDERS.length;
    }

    /** 含自定义供应商的名称列表（供下拉选择）。 */
    public static String[] names(Context ctx) {
        int n = count(ctx);
        String[] out = new String[n];
        for (int i = 0; i < PROVIDERS.length; i++) out[i] = PROVIDERS[i].name;
        if (n > PROVIDERS.length) {
            out[PROVIDERS.length] = getCustom(ctx).name;
        }
        return out;
    }

    /** 自定义供应商配置合法性校验；合法返回 null，否则返回错误文案。 */
    public static String validateCustom(android.content.Context ctx, String name, String url, int auth, String path, String currency) {
        if (name == null || name.trim().isEmpty()) return ctx.getString(R.string.g_003);
        if (name.trim().length() > 20) return ctx.getString(R.string.g_004);
        if (url == null || url.trim().isEmpty()) return ctx.getString(R.string.g_005);
        String u = url.trim();
        if (!u.startsWith("https://")) return ctx.getString(R.string.g_006);
        if (auth != AUTH_BEARER && auth != AUTH_X_API_KEY) return ctx.getString(R.string.g_007);
        if (path == null || path.trim().isEmpty()) return ctx.getString(R.string.g_008);
        String p = path.trim();
        if (!p.matches("[A-Za-z0-9_\\[\\].]+")) return ctx.getString(R.string.g_009);
        if (p.startsWith(".") || p.startsWith("[")) return ctx.getString(R.string.g_010);
        if (currency == null || currency.trim().isEmpty()) return ctx.getString(R.string.g_011);
        return null;
    }

    /** Key 弱校验：先 trim 再做前缀匹配，容忍用户粘贴时带入的空格。 */
    public static boolean isValidKey(int providerId, String key) {
        if (key == null || key.trim().isEmpty()) return false;
        Provider p = get(providerId);
        if (p.keyPrefixes.length == 0) return true;
        String k = key.trim();
        for (String prefix : p.keyPrefixes) {
            if (k.startsWith(prefix)) return true;
        }
        return false;
    }

    /** 服务商配置。 */
    public static final class Provider {
        public final int id;
        public final String name;
        public final String[] keyPrefixes;
        public final String balanceUrl;
        public final int authType;
        public final String defaultCurrency;
        public final int type;
        /** 自定义供应商的余额字段 JSON 路径（如 balance_infos[0].total_balance），内置服务商为 null。 */
        public final String balancePath;

        Provider(int id, String name, String[] keyPrefixes, String balanceUrl,
                 int authType, String defaultCurrency, int type, String balancePath) {
            this.id = id;
            this.name = name;
            this.keyPrefixes = keyPrefixes;
            this.balanceUrl = balanceUrl;
            this.authType = authType;
            this.defaultCurrency = defaultCurrency;
            this.type = type;
            this.balancePath = balancePath;
        }

        /** 该服务商是否存在可用的机器查询能力（OpenAI 不具备）。 */
        public boolean isQuerySupported() {
            return type != TYPE_UNSUPPORTED && balanceUrl != null;
        }

        /** 是否后付费成本模式（Anthropic）。 */
        public boolean isCostMode() {
            return type == TYPE_COST;
        }

        /** 把原始响应 JSON 解析为统一模型。解析失败抛异常由调用方兜底。 */
        public BalanceResult parse(android.content.Context ctx, String json) throws Exception {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (balancePath != null && !balancePath.isEmpty()) {
                double total = valueAtPath(root, balancePath);
                return new BalanceResult(defaultCurrency, total, 0, 0);
            }
            switch (id) {
                case PROVIDER_DEEPSEEK: return parseDeepSeek(ctx, root);
                case PROVIDER_KIMI:     return parseKimi(ctx, root);
                case PROVIDER_GLM:      return parseGlm(ctx, root);
                case PROVIDER_ANTHROPIC:return parseAnthropicCost(ctx, root);
                case PROVIDER_OPENAI:
                default:
                    throw new IllegalStateException(ctx.getString(R.string.g_012));
            }
        }

        /** DeepSeek：balance_infos[0]{currency, total_balance, granted_balance, topped_up_balance}（字符串数字）。 */
        private BalanceResult parseDeepSeek(android.content.Context ctx, JsonObject root) throws Exception {
            JsonElement infosEl = root.get("balance_infos");
            if (infosEl == null || !infosEl.isJsonArray() || infosEl.getAsJsonArray().size() == 0) {
                throw new IllegalStateException(ctx.getString(R.string.g_013));
            }
            JsonObject info = infosEl.getAsJsonArray().get(0).getAsJsonObject();
            String currency = normalizeCurrency(str(info, "currency", defaultCurrency));
            double total = num(info, "total_balance");
            double granted = num(info, "granted_balance");
            double topped = num(info, "topped_up_balance");
            return new BalanceResult(currency, total, granted, topped);
        }

        /** Kimi：data{available_balance, voucher_balance, cash_balance}（人民币元）。 */
        private BalanceResult parseKimi(android.content.Context ctx, JsonObject root) throws Exception {
            JsonElement dataEl = root.get("data");
            if (dataEl == null || !dataEl.isJsonObject()) throw new IllegalStateException(ctx.getString(R.string.g_014));
            JsonObject data = dataEl.getAsJsonObject();
            double available = dbl(data, "available_balance", 0);
            double voucher = dbl(data, "voucher_balance", 0);
            double cash = dbl(data, "cash_balance", 0);
            return new BalanceResult("CNY", available, voucher, cash);
        }

        /** 智谱：data{balance, grantBalance}（人民币元）。充值为 balance - grant。 */
        private BalanceResult parseGlm(android.content.Context ctx, JsonObject root) throws Exception {
            JsonElement dataEl = root.get("data");
            if (dataEl == null || !dataEl.isJsonObject()) throw new IllegalStateException(ctx.getString(R.string.g_014));
            JsonObject data = dataEl.getAsJsonObject();
            double balance = dbl(data, "balance", 0);
            double grant = dbl(data, "grantBalance", dbl(data, "grant_balance", 0));
            double topped = Math.max(0, balance - grant);
            return new BalanceResult("CNY", balance, grant, topped);
        }

        /** Anthropic cost_report：递归汇总所有 amount 字段（USD）。 */
        private BalanceResult parseAnthropicCost(android.content.Context ctx, JsonObject root) throws Exception {
            double total = sumAmounts(root);
            return new BalanceResult("USD", total, 0, 0);
        }
    }

    /** 统一余额/成本模型。 */
    public static final class BalanceResult {
        public final String currency;
        public final double totalBalance;
        public final double grantedBalance;
        public final double toppedUpBalance;

        BalanceResult(String currency, double total, double granted, double topped) {
            this.currency = currency;
            this.totalBalance = total;
            this.grantedBalance = granted;
            this.toppedUpBalance = topped;
        }
    }

    // ---- 解析辅助 ----

    /** 兼容字符串数字与数值数字。 */
    private static double num(JsonObject o, String key) {
        JsonElement v = o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return 0;
        if (v.getAsJsonPrimitive().isNumber()) return v.getAsDouble();
        if (v.getAsJsonPrimitive().isString()) {
            try { return Double.parseDouble(v.getAsString().trim()); } catch (Exception ignored) { }
        }
        return 0;
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement v = o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return def;
        return v.getAsJsonPrimitive().isString() ? v.getAsString() : def;
    }

    private static double dbl(JsonObject o, String key, double def) {
        JsonElement v = o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return def;
        return v.getAsJsonPrimitive().isNumber() ? v.getAsDouble() : def;
    }

    /** 递归汇总 JSON 树中所有 amount 字段的数值。 */
    private static double sumAmounts(JsonElement node) {
        double sum = 0;
        if (node == null || node.isJsonNull()) return sum;
        if (node.isJsonObject()) {
            JsonObject o = node.getAsJsonObject();
            JsonElement amt = o.get("amount");
            if (amt != null && amt.isJsonPrimitive()) {
                if (amt.getAsJsonPrimitive().isNumber()) sum += amt.getAsDouble();
                else if (amt.getAsJsonPrimitive().isString()) {
                    try { sum += Double.parseDouble(amt.getAsString().trim()); } catch (Exception ignored) { }
                }
            }
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                if (!"amount".equals(e.getKey())) sum += sumAmounts(e.getValue());
            }
        } else if (node.isJsonArray()) {
            JsonArray a = node.getAsJsonArray();
            for (JsonElement e : a) sum += sumAmounts(e);
        }
        return sum;
    }

    /** 归一化币种，避免 "usd"/"USD " 之类导致换算失效。 */
    private static String normalizeCurrency(String c) {
        if (c == null) return "CNY";
        String u = c.trim().toUpperCase();
        return u.contains("USD") ? "USD" : "CNY";
    }

    /** 按路径从 JSON 树取值：a.b[0].c → 递归取数值（字符串数字兼容）。 */
    private static double valueAtPath(JsonObject root, String path) throws Exception {
        JsonElement cur = root;
        Matcher m = Pattern.compile("([^.\\[\\]]+)|\\[(\\d+)\\]").matcher(path);
        while (m.find()) {
            if (cur == null) return 0;
            if (m.group(1) != null) {
                String key = m.group(1);
                if (!cur.isJsonObject()) return 0;
                cur = cur.getAsJsonObject().get(key);
            } else {
                int idx = Integer.parseInt(m.group(2));
                if (!cur.isJsonArray()) return 0;
                JsonArray arr = cur.getAsJsonArray();
                cur = (idx >= 0 && idx < arr.size()) ? arr.get(idx) : null;
            }
        }
        if (cur == null || !cur.isJsonPrimitive()) return 0;
        if (cur.getAsJsonPrimitive().isNumber()) return cur.getAsDouble();
        if (cur.getAsJsonPrimitive().isString()) {
            try { return Double.parseDouble(cur.getAsString().trim()); } catch (Exception e) { return 0; }
        }
        return 0;
    }
}
