package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.content.Context;

/** 把 HTTP、网络、解析异常翻译成用户能理解的短句。 */
public final class ErrorText {

    private ErrorText() {
    }

    public static String message(Context ctx, int providerId, Throwable error) {
        if (ctx == null) return "";
        ProviderConfig.Provider provider = ProviderConfig.get(ctx, providerId);
        String hint = keyHint(ctx, provider);

        if (error instanceof NetworkClient.HttpException) {
            NetworkClient.HttpException http = (NetworkClient.HttpException) error;
            if (http.code == 401) {
                return hint + " " + ctx.getString(R.string.err_key_invalid);
            }
            if (http.code == 403) {
                String b = http.body == null ? "" : http.body.trim();
                return hint + " " + ctx.getString(R.string.err_forbidden) + (b.isEmpty() ? "" : " · " + b);
            }
            if (http.code == 404) {
                return ctx.getString(R.string.err_endpoint_missing, provider.name);
            }
            if (http.code == 429) {
                return ctx.getString(R.string.err_too_frequent);
            }
            // 400 / 403 / 其它状态：把服务商返回的原文带出来，否则用户只能看到“请求失败”
            String body = http.body == null ? "" : http.body.trim();
            if (!body.isEmpty()) {
                return hint + " HTTP " + http.code + " · " + body;
            }
            if (http.code >= 500) {
                return ctx.getString(R.string.err_server_busy);
            }
            return ctx.getString(R.string.err_http_code, http.code);
        }
        if (error instanceof NetworkClient.ParseException) {
            return ctx.getString(R.string.err_parse_simple, provider.name);
        }
        if (error instanceof NetworkClient.UnsupportedException) {
            return ctx.getString(R.string.err_unsupported, provider.name);
        }
        if (error instanceof java.net.SocketTimeoutException) {
            return ctx.getString(R.string.err_timeout);
        }
        if (error instanceof java.net.UnknownHostException
                || error instanceof java.net.ConnectException) {
            return ctx.getString(R.string.err_no_network);
        }
        String raw = error == null ? "" : String.valueOf(error.getMessage());
        if (raw.contains("VOLC_CREDENTIAL_FORMAT")) {
            return ctx.getString(R.string.err_volc_credential);
        }
        if (raw.contains("VOLC_SIGN_FAILED")) {
            return ctx.getString(R.string.err_volc_sign);
        }
        return ctx.getString(R.string.err_network_short);
    }

    private static String keyHint(Context ctx, ProviderConfig.Provider provider) {
        if (provider == null) return "";
        switch (provider.id) {
            case ProviderConfig.PROVIDER_ANTHROPIC:
                return ctx.getString(R.string.err_hint_anthropic);
            case ProviderConfig.PROVIDER_OPENAI:
                return ctx.getString(R.string.err_hint_openai);
            case ProviderConfig.PROVIDER_CODEX:
                return ctx.getString(R.string.err_hint_codex);
            case ProviderConfig.PROVIDER_VOLCENGINE:
                return ctx.getString(R.string.err_hint_volc);
            case ProviderConfig.PROVIDER_GLM_CODE:
                return ctx.getString(R.string.err_hint_glm_code);
            case ProviderConfig.PROVIDER_KIMI_CODE:
                return ctx.getString(R.string.err_hint_kimi_code);
            default:
                return "";
        }
    }
}
