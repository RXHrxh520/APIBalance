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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务商查询定义。每个服务商只保留公开的查询地址、认证方式和响应解析，
 * 不依赖网页登录态，也不把网页接口当成稳定 API。
 */
public final class ProviderConfig {

    public static final int PROVIDER_DEEPSEEK = 0;
    public static final int PROVIDER_KIMI = 1;
    public static final int PROVIDER_GLM = 2;
    public static final int PROVIDER_OPENAI = 3;
    public static final int PROVIDER_ANTHROPIC = 4;
    public static final int PROVIDER_CUSTOM = 5;
    public static final int PROVIDER_VOLCENGINE = 6;
    public static final int PROVIDER_KIMI_CODE = 7;
    public static final int PROVIDER_GLM_CODE = 8;
    public static final int PROVIDER_CODEX = 9;

    public static final int AUTH_BEARER = 0;
    public static final int AUTH_X_API_KEY = 1;
    /** Authorization 直接放密钥，不加 Bearer。 */
    public static final int AUTH_RAW = 2;
    /** ChatGPT/Codex 的 Bearer Token，可选 accountId 用 | 分隔。 */
    public static final int AUTH_CODEX = 3;
    /** 火山引擎 AK|SK，请求使用 HMAC-SHA256 签名。 */
    public static final int AUTH_VOLC = 4;

    public static final int TYPE_BALANCE = 0;
    public static final int TYPE_COST = 1;
    public static final int TYPE_UNSUPPORTED = 2;
    public static final int TYPE_QUOTA = 3;

    public static final String ANTHROPIC_VERSION = "2023-06-01";
    public static final String VOLC_BILLING_HOST = "billing.volcengineapi.com";
    public static final String VOLC_BILLING_URL =
            "https://billing.volcengineapi.com/?Action=QueryBalanceAcct&Version=2022-01-01";

    private static final Provider[] PROVIDERS = {
        new Provider(PROVIDER_DEEPSEEK, "DeepSeek", new String[]{"sk-"},
                "https://api.deepseek.com/user/balance", AUTH_BEARER, "CNY", TYPE_BALANCE,
                null, "预存余额"),
        new Provider(PROVIDER_KIMI, "Kimi", new String[]{"sk-"},
                "https://api.moonshot.cn/v1/users/me/balance", AUTH_BEARER, "CNY", TYPE_BALANCE,
                null, "预存余额"),
        new Provider(PROVIDER_GLM, "智谱GLM", new String[]{},
                "https://open.bigmodel.cn/api/biz/account/query-customer-account-report",
                AUTH_BEARER, "CNY", TYPE_BALANCE, null, "预存余额"),
        new Provider(PROVIDER_OPENAI, "OpenAI", new String[]{"sk-"},
                "https://api.openai.com/v1/organization/costs", AUTH_BEARER, "USD", TYPE_COST,
                null, "组织成本"),
        new Provider(PROVIDER_ANTHROPIC, "Anthropic", new String[]{"sk-ant-"},
                "https://api.anthropic.com/v1/organizations/cost_report", AUTH_X_API_KEY, "USD",
                TYPE_COST, null, "累计成本"),
        new Provider(PROVIDER_VOLCENGINE, "火山方舟", new String[]{},
                VOLC_BILLING_URL, AUTH_VOLC, "CNY", TYPE_BALANCE, null, "账户余额"),
        new Provider(PROVIDER_KIMI_CODE, "Kimi Code", new String[]{},
                "https://api.kimi.com/coding/v1/usages", AUTH_BEARER, "QUOTA", TYPE_QUOTA,
                null, "套餐配额"),
        new Provider(PROVIDER_GLM_CODE, "GLM Coding", new String[]{},
                "https://open.bigmodel.cn/api/monitor/usage/quota/limit", AUTH_RAW, "QUOTA",
                TYPE_QUOTA, null, "套餐配额"),
        new Provider(PROVIDER_CODEX, "Codex", new String[]{},
                "https://chatgpt.com/backend-api/wham/usage", AUTH_CODEX, "QUOTA", TYPE_QUOTA,
                null, "套餐配额"),
    };

    private ProviderConfig() {
    }

    public static Provider get(int providerId) {
        if (providerId == PROVIDER_CUSTOM) {
            return new Provider(PROVIDER_CUSTOM, "自定义服务商", new String[]{},
                    null, AUTH_BEARER, "CNY", TYPE_BALANCE, null, "自定义余额");
        }
        if (providerId < 0 || providerId >= PROVIDERS.length) {
            return PROVIDERS[PROVIDER_DEEPSEEK];
        }
        return PROVIDERS[providerId];
    }

    public static Provider get(Context ctx, int providerId) {
        if (providerId == PROVIDER_CUSTOM) return getCustom(ctx);
        return get(providerId);
    }

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
        return new Provider(PROVIDER_CUSTOM, name, new String[]{}, url, auth, currency,
                TYPE_BALANCE, path, "自定义余额");
    }

    /** 界面上可选的服务商。OpenAI 使用组织成本接口，需要 Admin Key，默认不放入列表。 */
    public static int[] pickableIds() {
        return new int[]{PROVIDER_DEEPSEEK, PROVIDER_KIMI, PROVIDER_GLM,
                PROVIDER_ANTHROPIC, PROVIDER_VOLCENGINE, PROVIDER_KIMI_CODE,
                PROVIDER_GLM_CODE, PROVIDER_CODEX};
    }

    public static int count() {
        return PROVIDERS.length;
    }

    public static String[] names() {
        String[] n = new String[PROVIDERS.length];
        for (int i = 0; i < PROVIDERS.length; i++) n[i] = PROVIDERS[i].name;
        return n;
    }

    public static int count(Context ctx) {
        return (ctx != null && AppPrefs.hasCustomProvider(ctx)) ? PROVIDERS.length + 1 : PROVIDERS.length;
    }

    public static String[] names(Context ctx) {
        int n = count(ctx);
        String[] out = new String[n];
        for (int i = 0; i < PROVIDERS.length; i++) out[i] = PROVIDERS[i].name;
        if (n > PROVIDERS.length) out[PROVIDERS.length] = getCustom(ctx).name;
        return out;
    }

    public static String validateCustom(Context ctx, String name, String url, int auth,
                                        String path, String currency) {
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

    /**
     * 只做输入形态检查，不拿固定前缀限制真实密钥。
     * GLM 旧版密钥可能是数字或字母，Kimi Code/Codex 可能是 OAuth Token。
     */
    public static boolean isValidKey(int providerId, String key) {
        if (key == null || key.trim().isEmpty()) return false;
        if (providerId == PROVIDER_VOLCENGINE) {
            String[] parts = key.trim().split("\\|", 2);
            return parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty();
        }
        Provider p = get(providerId);
        if (p.type == TYPE_QUOTA) return true;
        if (p.keyPrefixes.length == 0) return true;
        String k = key.trim();
        for (String prefix : p.keyPrefixes) {
            if (k.startsWith(prefix)) return true;
        }
        return false;
    }

    public static final class Provider {
        public final int id;
        public final String name;
        public final String[] keyPrefixes;
        public final String balanceUrl;
        public final int authType;
        public final String defaultCurrency;
        public final int type;
        public final String balancePath;
        public final String modeLabel;

        Provider(int id, String name, String[] keyPrefixes, String balanceUrl,
                 int authType, String defaultCurrency, int type, String balancePath,
                 String modeLabel) {
            this.id = id;
            this.name = name;
            this.keyPrefixes = keyPrefixes;
            this.balanceUrl = balanceUrl;
            this.authType = authType;
            this.defaultCurrency = defaultCurrency;
            this.type = type;
            this.balancePath = balancePath;
            this.modeLabel = modeLabel;
        }

        public boolean isQuerySupported() {
            return type != TYPE_UNSUPPORTED && balanceUrl != null;
        }

        public boolean isCostMode() {
            return type == TYPE_COST;
        }

        public boolean isQuotaMode() {
            return type == TYPE_QUOTA;
        }

        public BalanceResult parse(Context ctx, String json) throws Exception {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (balancePath != null && !balancePath.isEmpty()) {
                double total = valueAtPath(root, balancePath);
                return BalanceResult.balance(defaultCurrency, total, 0, 0);
            }
            switch (id) {
                case PROVIDER_DEEPSEEK: return parseDeepSeek(ctx, root);
                case PROVIDER_KIMI: return parseKimi(ctx, root);
                case PROVIDER_GLM: return parseGlm(ctx, root);
                case PROVIDER_ANTHROPIC: return parseAnthropicCost(ctx, root);
                case PROVIDER_OPENAI: return parseOpenAiCost(ctx, root);
                case PROVIDER_VOLCENGINE: return parseVolcengine(ctx, root);
                case PROVIDER_KIMI_CODE: return parseKimiCode(ctx, root);
                case PROVIDER_GLM_CODE: return parseGlmCode(ctx, root);
                case PROVIDER_CODEX: return parseCodex(ctx, root);
                default:
                    throw new IllegalStateException(ctx.getString(R.string.g_012));
            }
        }

        private BalanceResult parseDeepSeek(Context ctx, JsonObject root) throws Exception {
            JsonElement infosEl = root.get("balance_infos");
            if (infosEl == null || !infosEl.isJsonArray() || infosEl.getAsJsonArray().size() == 0) {
                throw new IllegalStateException(ctx.getString(R.string.g_013));
            }
            JsonObject info = infosEl.getAsJsonArray().get(0).getAsJsonObject();
            String currency = normalizeCurrency(str(info, "currency", defaultCurrency));
            double total = num(info, "total_balance");
            double granted = num(info, "granted_balance");
            double topped = num(info, "topped_up_balance");
            return BalanceResult.balance(currency, total, granted, topped);
        }

        private BalanceResult parseKimi(Context ctx, JsonObject root) throws Exception {
            JsonElement dataEl = root.get("data");
            if (dataEl == null || !dataEl.isJsonObject()) {
                throw new IllegalStateException(ctx.getString(R.string.g_014));
            }
            JsonObject data = dataEl.getAsJsonObject();
            double available = dbl(data, "available_balance", 0);
            double voucher = dbl(data, "voucher_balance", 0);
            double cash = dbl(data, "cash_balance", 0);
            return BalanceResult.balance("CNY", available, voucher, cash);
        }

        private BalanceResult parseGlm(Context ctx, JsonObject root) throws Exception {
            JsonElement dataEl = root.get("data");
            if (dataEl == null || !dataEl.isJsonObject()) {
                throw new IllegalStateException(ctx.getString(R.string.g_014));
            }
            JsonObject data = dataEl.getAsJsonObject();
            double available = dbl(data, "availableBalance", dbl(data, "balance", 0));
            double grant = dbl(data, "giveAmount", 0);
            double recharge = dbl(data, "rechargeAmount", 0);
            double topped = recharge > 0 ? recharge : Math.max(0, available - grant);
            return BalanceResult.balance("CNY", available, grant, topped);
        }

        /** Anthropic Cost Report 的 amount 单位是美分。 */
        private BalanceResult parseAnthropicCost(Context ctx, JsonObject root) throws Exception {
            double cents = 0;
            JsonElement data = root.get("data");
            if (data != null && data.isJsonArray()) {
                for (JsonElement bucket : data.getAsJsonArray()) {
                    if (!bucket.isJsonObject()) continue;
                    JsonElement results = bucket.getAsJsonObject().get("results");
                    if (results == null || !results.isJsonArray()) continue;
                    for (JsonElement row : results.getAsJsonArray()) {
                        if (!row.isJsonObject()) continue;
                        JsonElement amount = row.getAsJsonObject().get("amount");
                        cents += primitiveNumber(amount, 0);
                    }
                }
            } else {
                cents = sumAmounts(root);
            }
            return BalanceResult.balance("USD", cents / 100.0, 0, 0);
        }

        /** OpenAI Organization Costs：amount.value 单位是美元。 */
        private BalanceResult parseOpenAiCost(Context ctx, JsonObject root) throws Exception {
            double total = 0;
            JsonElement data = root.get("data");
            if (data != null && data.isJsonArray()) {
                for (JsonElement bucket : data.getAsJsonArray()) {
                    if (!bucket.isJsonObject()) continue;
                    JsonElement results = bucket.getAsJsonObject().get("results");
                    if (results == null || !results.isJsonArray()) continue;
                    for (JsonElement row : results.getAsJsonArray()) {
                        if (!row.isJsonObject()) continue;
                        JsonElement amount = row.getAsJsonObject().get("amount");
                        if (amount != null && amount.isJsonObject()) {
                            total += primitiveNumber(amount.getAsJsonObject().get("value"), 0);
                        }
                    }
                }
            }
            return BalanceResult.balance("USD", total, 0, 0);
        }

        /** 火山引擎 QueryBalanceAcct 的 Result 字段。 */
        private BalanceResult parseVolcengine(Context ctx, JsonObject root) throws Exception {
            JsonObject meta = root.getAsJsonObject("ResponseMetadata");
            if (meta != null && meta.has("Error")) {
                JsonElement err = meta.get("Error");
                if (err != null && err.isJsonObject()) {
                    throw new IllegalStateException(str(err.getAsJsonObject(), "Message",
                            ctx.getString(R.string.err_provider_response)));
                }
            }
            JsonObject result = root.getAsJsonObject("Result");
            if (result == null) {
                throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            }
            double available = num(result, "AvailableBalance");
            double cash = num(result, "CashBalance");
            double credit = num(result, "CreditLimit");
            return BalanceResult.balance("CNY", available, cash, credit);
        }

        /** Kimi Code：limits[0].detail 是 5 小时窗口，usage 是每周窗口。 */
        private BalanceResult parseKimiCode(Context ctx, JsonObject root) throws Exception {
            if (root.has("error")) {
                JsonElement error = root.get("error");
                String msg = error != null && error.isJsonObject()
                        ? str(error.getAsJsonObject(), "message", ctx.getString(R.string.err_provider_response))
                        : ctx.getString(R.string.err_provider_response);
                throw new IllegalStateException(msg);
            }
            JsonObject five = null;
            JsonElement limits = root.get("limits");
            if (limits != null && limits.isJsonArray() && limits.getAsJsonArray().size() > 0) {
                JsonElement first = limits.getAsJsonArray().get(0);
                if (first != null && first.isJsonObject()) {
                    JsonElement detail = first.getAsJsonObject().get("detail");
                    if (detail != null && detail.isJsonObject()) five = detail.getAsJsonObject();
                }
            }
            JsonObject weekly = object(root, "usage");
            if (five == null && weekly == null) {
                throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            }
            String main = quotaText(five, false);
            String sub = quotaText(weekly, false);
            String reset = resetText(five != null ? str(five, "resetTime", "") : "");
            String weeklyReset = resetText(weekly != null ? str(weekly, "resetTime", "") : "");
            String membership = "";
            JsonObject user = object(root, "user");
            JsonObject membershipObj = user == null ? null : object(user, "membership");
            if (membershipObj != null) membership = str(membershipObj, "level", "");
            String footer = joinFooter(reset, weeklyReset, membership);
            return BalanceResult.quota("5小时剩余", main, "每周剩余", sub, footer);
        }

        /**
         * GLM Coding：data.limits[] 的 type 可能是 TOKENS_LIMIT，也可能已经变成
         * CREDIT_LIMIT；unit=3 表示 5 小时，unit=6 表示每周。
         */
        private BalanceResult parseGlmCode(Context ctx, JsonObject root) throws Exception {
            if (root.has("success") && root.get("success").isJsonPrimitive()
                    && !root.get("success").getAsBoolean()) {
                throw new IllegalStateException(str(root, "msg", ctx.getString(R.string.err_provider_response)));
            }
            JsonObject data = object(root, "data");
            if (data == null) throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            JsonArray limits = data.has("limits") && data.get("limits").isJsonArray()
                    ? data.getAsJsonArray("limits") : new JsonArray();
            JsonObject five = null;
            JsonObject weekly = null;
            for (JsonElement el : limits) {
                if (el == null || !el.isJsonObject()) continue;
                JsonObject item = el.getAsJsonObject();
                String type = str(item, "type", "").toLowerCase(Locale.US);
                if (!type.equals("tokens_limit") && !type.equals("credit_limit")) continue;
                int unit = (int) num(item, "unit");
                if (unit == 3 && five == null) five = item;
                else if (unit == 6 && weekly == null) weekly = item;
            }
            if (five == null && weekly == null) {
                for (JsonElement el : limits) {
                    if (el == null || !el.isJsonObject()) continue;
                    JsonObject item = el.getAsJsonObject();
                    String type = str(item, "type", "").toLowerCase(Locale.US);
                    if (!type.contains("limit")) continue;
                    if (five == null) five = item;
                    else if (weekly == null) weekly = item;
                }
            }
            if (five == null && weekly == null) {
                throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            }
            String main = quotaTextGlm(five);
            String sub = quotaTextGlm(weekly);
            String reset = resetText(five != null ? str(five, "nextResetTime", "") : "");
            String weeklyReset = resetText(weekly != null ? str(weekly, "nextResetTime", "") : "");
            return BalanceResult.quota("5小时剩余", main, "每周剩余", sub,
                    joinFooter(reset, weeklyReset, ""));
        }

        /** Codex：rate_limit.primary_window / secondary_window。 */
        private BalanceResult parseCodex(Context ctx, JsonObject root) throws Exception {
            JsonObject rate = object(root, "rate_limit");
            if (rate == null) throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            JsonObject primary = object(rate, "primary_window");
            JsonObject secondary = object(rate, "secondary_window");
            if (primary == null && secondary == null) {
                throw new IllegalStateException(ctx.getString(R.string.err_provider_response));
            }
            String mainLabel = codexWindowLabel(primary);
            String subLabel = codexWindowLabel(secondary);
            String main = quotaTextCodex(primary);
            String sub = quotaTextCodex(secondary);
            String reset = resetText(primary != null ? str(primary, "reset_at", "") : "");
            String weeklyReset = resetText(secondary != null ? str(secondary, "reset_at", "") : "");
            return BalanceResult.quota(mainLabel, main, subLabel, sub,
                    joinFooter(reset, weeklyReset, ""));
        }
    }

    /** 统一余额/成本/套餐模型。 */
    public static final class BalanceResult {
        public final String currency;
        public final double totalBalance;
        public final double grantedBalance;
        public final double toppedUpBalance;
        public final boolean quota;
        public final String mainLabel;
        public final String mainValue;
        public final String subLabel;
        public final String subValue;
        public final String footer;

        private BalanceResult(String currency, double total, double granted, double topped,
                              boolean quota, String mainLabel, String mainValue,
                              String subLabel, String subValue, String footer) {
            this.currency = currency;
            this.totalBalance = total;
            this.grantedBalance = granted;
            this.toppedUpBalance = topped;
            this.quota = quota;
            this.mainLabel = mainLabel;
            this.mainValue = mainValue;
            this.subLabel = subLabel;
            this.subValue = subValue;
            this.footer = footer;
        }

        static BalanceResult balance(String currency, double total, double granted, double topped) {
            return new BalanceResult(currency, total, granted, topped, false,
                    null, null, null, null, null);
        }

        static BalanceResult quota(String mainLabel, String mainValue, String subLabel,
                                   String subValue, String footer) {
            return new BalanceResult("QUOTA", 0, 0, 0, true,
                    mainLabel, mainValue, subLabel, subValue, footer);
        }

        public boolean isQuota() {
            return quota;
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null) return null;
        JsonElement el = parent.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static double num(JsonObject o, String key) {
        return primitiveNumber(o == null ? null : o.get(key), 0);
    }

    private static double dbl(JsonObject o, String key, double def) {
        JsonElement v = o == null ? null : o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return def;
        if (v.getAsJsonPrimitive().isNumber()) return v.getAsDouble();
        if (v.getAsJsonPrimitive().isString()) {
            try { return Double.parseDouble(v.getAsString().trim()); } catch (Exception ignored) { }
        }
        return def;
    }

    private static double primitiveNumber(JsonElement v, double def) {
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return def;
        if (v.getAsJsonPrimitive().isNumber()) return v.getAsDouble();
        if (v.getAsJsonPrimitive().isString()) {
            try { return Double.parseDouble(v.getAsString().trim()); } catch (Exception ignored) { }
        }
        return def;
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement v = o == null ? null : o.get(key);
        if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) return def;
        return v.getAsJsonPrimitive().isString() ? v.getAsString() : def;
    }

    /** 兼容旧响应：递归汇总 amount 字段。 */
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
            for (JsonElement e : node.getAsJsonArray()) sum += sumAmounts(e);
        }
        return sum;
    }

    private static String quotaText(JsonObject o, boolean usedPercent) {
        if (o == null) return "—";
        double limit = num(o, "limit");
        double remaining = num(o, "remaining");
        if (limit > 0) {
            double pct = Math.max(0, Math.min(100, (remaining / limit) * 100));
            return Math.round(pct) + "%";
        }
        double pct = num(o, "percentage");
        if (pct > 0 || o.has("percentage")) return Math.round((usedPercent ? 100 - pct : pct)) + "%";
        return "—";
    }

    private static String quotaTextGlm(JsonObject o) {
        if (o == null) return "—";
        double used = num(o, "percentage");
        double remaining = Math.max(0, Math.min(100, 100 - used));
        return Math.round(remaining) + "%";
    }

    private static String quotaTextCodex(JsonObject o) {
        if (o == null) return "—";
        double used = num(o, "used_percent");
        return Math.round(Math.max(0, Math.min(100, 100 - used))) + "%";
    }

    private static String codexWindowLabel(JsonObject o) {
        if (o == null) return "窗口剩余";
        long secs = (long) num(o, "limit_window_seconds");
        if (secs == 18000) return "5小时剩余";
        if (secs == 604800) return "7天剩余";
        if (secs == 2592000) return "30天剩余";
        long hours = secs / 3600;
        if (hours >= 24) return (hours / 24) + "天剩余";
        return Math.max(1, hours) + "小时剩余";
    }

    private static String resetText(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String s = raw.trim();
        try {
            long ms;
            if (s.matches("\\d+")) {
                ms = Long.parseLong(s);
                if (ms < 100000000000L) ms *= 1000;
            } else {
                String normalized = s.replace("Z", "+00:00");
                ms = java.time.OffsetDateTime.parse(normalized).toInstant().toEpochMilli();
            }
            SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            return f.format(new Date(ms));
        } catch (Throwable ignored) {
            return s;
        }
    }

    private static String joinFooter(String a, String b, String membership) {
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.isEmpty()) sb.append("5小时重置 ").append(a);
        if (b != null && !b.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("每周重置 ").append(b);
        }
        if (membership != null && !membership.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(membership);
        }
        return sb.toString();
    }

    private static String normalizeCurrency(String c) {
        if (c == null) return "CNY";
        String u = c.trim().toUpperCase(Locale.US);
        return u.contains("USD") ? "USD" : "CNY";
    }

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
        return primitiveNumber(cur, 0);
    }
}
