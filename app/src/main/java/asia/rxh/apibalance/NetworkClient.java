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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 统一网络请求层：余额查询的所有请求（前台/后台定时刷新）都走这里，
 * 保证代理、超时、重试、UA、认证方式等设置行为一致。
 * TLS 证书校验恒开启，不允许关闭。
 *
 * 认证按服务商区分：
 *  - Bearer：Authorization: Bearer <key>（DeepSeek / Kimi / 智谱）
 *  - x-api-key：x-api-key: <key> + anthropic-version（Anthropic）
 *
 * 网络实现策略：优先走 OKHttp（现代依赖）；若 OKHttp 依赖缺失或运行时崩溃，
 * 自动回退到内置 HttpURLConnection 路线，保证核心功能永不挂。
 */
public final class NetworkClient {

    private NetworkClient() {
    }

    /** 服务端返回非 200 时抛出，code 为 HTTP 状态码，body 为响应体（已截断）。 */
    public static final class HttpException extends Exception {
        public final int code;
        public final String body;

        HttpException(int code, String body) {
            super("HTTP " + code);
            this.code = code;
            this.body = body;
        }
    }

    /** 响应 JSON 无法解析时抛出。 */
    public static final class ParseException extends Exception {
        public ParseException(String msg) {
            super(msg);
        }
    }

    /** 该服务商没有可用的机器查询能力（如 OpenAI）。 */
    public static final class UnsupportedException extends Exception {
        public final String providerName;

        UnsupportedException(android.content.Context ctx, String providerName) {
            super(providerName + ctx.getString(R.string.x_002));
            this.providerName = providerName;
        }
    }

    /** 同步查询余额/成本（OKHttp 优先，HttpURLConnection 回退）。 */
    public static ProviderConfig.BalanceResult fetch(Context ctx, int providerId, String apiKey) throws Exception {
        ProviderConfig.Provider provider = ProviderConfig.get(ctx, providerId);

        // OpenAI 等无公开余额 API 的服务商：直接给友好异常，不发请求
        if (!provider.isQuerySupported()) {
            throw new UnsupportedException(ctx, provider.name);
        }

        if (okHttpAvailable()) {
            try {
                return fetchViaOkHttp(ctx, provider, apiKey);
            } catch (HttpException e) {
                throw e;
            } catch (ParseException e) {
                throw e;
            } catch (UnsupportedException e) {
                throw e;
            } catch (java.io.IOException e) {
                // 网络/超时/DNS 错误：按原语义上抛，不回退（避免重复请求）
                throw e;
            } catch (Throwable t) {
                // OKHttp 依赖运行时崩溃（类/接口不兼容）：自动回退到 HttpURLConnection
            }
        }
        return fetchViaHttpUrlConnection(ctx, provider, apiKey);
    }

    /** 是否加载得到 OKHttp（依赖缺失时返回 false）。 */
    private static boolean okHttpAvailable() {
        try {
            Class.forName("okhttp3.OkHttpClient");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** OKHttp 路线：现代化 HTTP/2 + 连接池，逻辑与原路线一致。 */
    private static ProviderConfig.BalanceResult fetchViaOkHttp(Context ctx, ProviderConfig.Provider provider, String apiKey) throws Exception {
        int timeoutSec = AppPrefs.getTimeoutSec(ctx);
        int retryCount = AppPrefs.getRetryCount(ctx);
        OkHttpClient client = buildOkHttpClient(ctx, timeoutSec);
        Request request = buildRequest(ctx, provider, apiKey);
        int attempt = 0;
        boolean proxyFallbackDone = false;

        while (true) {
            try (Response resp = client.newCall(request).execute()) {
                int code = resp.code();
                if (code == 200) {
                    String json = resp.body() == null ? "" : resp.body().string();
                    try {
                        return provider.parse(ctx, json);
                    } catch (Exception parseEx) {
                        throw new ParseException(parseEx.getMessage() == null
                                ? "unknown" : parseEx.getMessage());
                    }
                } else if (code == 401) {
                    throw new HttpException(401, null);
                } else if (code == 429) {
                    if (attempt < retryCount) {
                        attempt++;
                        sleepQuiet(1500);
                        continue;
                    }
                    throw new HttpException(429, null);
                } else {
                    String body = resp.body() == null ? null : resp.body().string();
                    if (attempt < retryCount) {
                        attempt++;
                        sleepQuiet(1000);
                        continue;
                    }
                    throw new HttpException(code, truncate(body, 200));
                }
            } catch (SocketTimeoutException e) {
                if (attempt < retryCount) {
                    attempt++;
                    sleepQuiet(1000);
                    continue;
                }
                throw e;
            } catch (java.io.IOException ioe) {
                // 代理异常时自动回退直连：开着代理反而完全查不了余额是最差的结果
                if (!proxyFallbackDone && AppPrefs.isProxyEnabled(ctx)) {
                    proxyFallbackDone = true;
                    client = buildOkHttpClient(ctx, timeoutSec, false);
                    continue;
                }
                throw ioe;
            }
        }
    }

    /** 通用共享客户端（开发者头像等非余额请求），只创建一次。 */
    private static volatile OkHttpClient GENERIC_HTTP;

    public static OkHttpClient http() {
        OkHttpClient c = GENERIC_HTTP;
        if (c == null) {
            synchronized (NetworkClient.class) {
                c = GENERIC_HTTP;
                if (c == null) {
                    c = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(15, TimeUnit.SECONDS)
                            .build();
                    GENERIC_HTTP = c;
                }
            }
        }
        return c;
    }

    /** 余额查询客户端缓存：只有超时/代理等设置变化时才重建，保住连接池与 HTTP/2。 */
    private static volatile OkHttpClient FETCH_HTTP;
    private static volatile String FETCH_HTTP_KEY;

    private static OkHttpClient buildOkHttpClient(Context ctx, int timeoutSec) {
        return buildOkHttpClient(ctx, timeoutSec, true);
    }

    private static synchronized OkHttpClient buildOkHttpClient(Context ctx, int timeoutSec, boolean useProxy) {
        boolean proxyOn = useProxy && AppPrefs.isProxyEnabled(ctx);
        String key = timeoutSec + "|" + proxyOn + "|" + (proxyOn
                ? AppPrefs.getProxyHost(ctx) + ":" + AppPrefs.getProxyPort(ctx) + ":" + AppPrefs.getProxyUser(ctx)
                : "");
        OkHttpClient cached = FETCH_HTTP;
        if (cached != null && key.equals(FETCH_HTTP_KEY)) return cached;

        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS);
        if (useProxy && AppPrefs.isProxyEnabled(ctx)) {
            b.proxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(AppPrefs.getProxyHost(ctx), AppPrefs.getProxyPort(ctx))));
        if (!AppPrefs.getProxyUser(ctx).isEmpty()) {
            final String pu = AppPrefs.getProxyUser(ctx);
            final String pp = AppPrefs.getProxyPass(ctx);
            b.proxyAuthenticator((route, response) -> response.request().newBuilder()
                    .header("Proxy-Authorization", okhttp3.Credentials.basic(pu, pp)).build());
        }
        }
        OkHttpClient built = b.build();
        FETCH_HTTP = built;
        FETCH_HTTP_KEY = key;
        return built;
    }

    private static Request buildRequest(Context ctx, ProviderConfig.Provider provider, String apiKey) {
        Request.Builder rb = new Request.Builder()
                .url(provider.balanceUrl)
                .header("Accept", "application/json")
                .header("User-Agent", AppPrefs.getUserAgent(ctx));
        if (provider.authType == ProviderConfig.AUTH_X_API_KEY) {
            rb.header("x-api-key", apiKey);
            rb.header("anthropic-version", ProviderConfig.ANTHROPIC_VERSION);
        } else {
            rb.header("Authorization", "Bearer " + apiKey);
        }
        return rb.build();
    }

    /** HttpURLConnection 路线（内置回退，逻辑为原实现）。 */
    private static ProviderConfig.BalanceResult fetchViaHttpUrlConnection(Context ctx, ProviderConfig.Provider provider, String apiKey) throws Exception {
        int timeoutSec = AppPrefs.getTimeoutSec(ctx);
        int retryCount = AppPrefs.getRetryCount(ctx);
        int attempt = 0;

        while (true) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(provider.balanceUrl);
                if (AppPrefs.isProxyEnabled(ctx)) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(AppPrefs.getProxyHost(ctx), AppPrefs.getProxyPort(ctx)));
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", AppPrefs.getUserAgent(ctx));
                applyAuth(conn, provider, apiKey);
                conn.setConnectTimeout(timeoutSec * 1000);
                conn.setReadTimeout(timeoutSec * 1000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    String json = readStream(conn.getInputStream());
                    try {
                        return provider.parse(ctx, json);
                    } catch (Exception parseEx) {
                        throw new ParseException(parseEx.getMessage() == null
                                ? "unknown" : parseEx.getMessage());
                    }
                } else if (responseCode == 401) {
                    throw new HttpException(401, null);
                } else if (responseCode == 429) {
                    if (attempt < retryCount) {
                        attempt++;
                        sleepQuiet(1500);
                        continue;
                    }
                    throw new HttpException(429, null);
                } else {
                    InputStream es = conn.getErrorStream();
                    String body = es == null ? null : readStream(es);
                    if (attempt < retryCount) {
                        attempt++;
                        sleepQuiet(1000);
                        continue;
                    }
                    throw new HttpException(responseCode, truncate(body, 200));
                }
            } catch (SocketTimeoutException e) {
                if (attempt < retryCount) {
                    attempt++;
                    sleepQuiet(1000);
                    continue;
                }
                throw e;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    /** 按服务商认证方式设置请求头。 */
    private static void applyAuth(HttpURLConnection conn, ProviderConfig.Provider provider, String apiKey) {
        if (provider.authType == ProviderConfig.AUTH_X_API_KEY) {
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", ProviderConfig.ANTHROPIC_VERSION);
        } else {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
    }

    private static String readStream(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
