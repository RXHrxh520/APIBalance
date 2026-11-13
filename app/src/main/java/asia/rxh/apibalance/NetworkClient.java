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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 统一网络请求层：余额查询的所有请求（前台/后台定时刷新）都走这里，
 * 保证代理、超时、重试、UA、认证方式等设置行为一致。
 * TLS 证书校验恒开启，不允许关闭。
 */
public final class NetworkClient {

    private NetworkClient() {
    }

    public static final class HttpException extends Exception {
        public final int code;
        public final String body;

        HttpException(int code, String body) {
            super("HTTP " + code);
            this.code = code;
            this.body = body;
        }
    }

    public static final class ParseException extends Exception {
        public ParseException(String msg) {
            super(msg);
        }
    }

    public static final class UnsupportedException extends Exception {
        public final String providerName;

        UnsupportedException(Context ctx, String providerName) {
            super(providerName + ctx.getString(R.string.x_002));
            this.providerName = providerName;
        }
    }

    public static ProviderConfig.BalanceResult fetch(Context ctx, int providerId, String apiKey) throws Exception {
        ProviderConfig.Provider provider = ProviderConfig.get(ctx, providerId);
        if (!provider.isQuerySupported()) {
            throw new UnsupportedException(ctx, provider.name);
        }
        if (okHttpAvailable()) {
            try {
                return fetchViaOkHttp(ctx, provider, apiKey);
            } catch (HttpException | ParseException | UnsupportedException e) {
                throw e;
            } catch (java.io.IOException e) {
                throw e;
            } catch (Throwable ignored) {
                // OKHttp 运行时不可用时走内置实现。
            }
        }
        return fetchViaHttpUrlConnection(ctx, provider, apiKey);
    }

    private static boolean okHttpAvailable() {
        try {
            Class.forName("okhttp3.OkHttpClient");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static ProviderConfig.BalanceResult fetchViaOkHttp(Context ctx,
            ProviderConfig.Provider provider, String apiKey) throws Exception {
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
                if (!proxyFallbackDone && AppPrefs.isProxyEnabled(ctx)) {
                    proxyFallbackDone = true;
                    client = buildOkHttpClient(ctx, timeoutSec, false);
                    continue;
                }
                throw ioe;
            }
        }
    }

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

    private static volatile OkHttpClient FETCH_HTTP;
    private static volatile String FETCH_HTTP_KEY;

    private static OkHttpClient buildOkHttpClient(Context ctx, int timeoutSec) {
        return buildOkHttpClient(ctx, timeoutSec, true);
    }

    private static synchronized OkHttpClient buildOkHttpClient(Context ctx, int timeoutSec,
            boolean useProxy) {
        boolean proxyOn = useProxy && AppPrefs.isProxyEnabled(ctx);
        String key = timeoutSec + "|" + proxyOn + "|" + (proxyOn
                ? AppPrefs.getProxyHost(ctx) + ":" + AppPrefs.getProxyPort(ctx) + ":"
                + AppPrefs.getProxyUser(ctx)
                : "");
        OkHttpClient cached = FETCH_HTTP;
        if (cached != null && key.equals(FETCH_HTTP_KEY)) return cached;

        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(timeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS);
        if (proxyOn) {
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
        String clean = cleanKey(apiKey);
        Request.Builder rb = new Request.Builder()
                .url(buildUrl(provider))
                .header("Accept", "application/json")
                .header("User-Agent", AppPrefs.getUserAgent(ctx));
        switch (provider.authType) {
            case ProviderConfig.AUTH_X_API_KEY:
                rb.header("x-api-key", clean);
                rb.header("anthropic-version", ProviderConfig.ANTHROPIC_VERSION);
                break;
            case ProviderConfig.AUTH_RAW:
                rb.header("Authorization", clean);
                break;
            case ProviderConfig.AUTH_CODEX: {
                String[] parts = clean.split("\\|", 2);
                rb.header("Authorization", "Bearer " + parts[0].trim());
                rb.header("User-Agent", "codex-cli");
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    rb.header("ChatGPT-Account-Id", parts[1].trim());
                }
                break;
            }
            case ProviderConfig.AUTH_VOLC: {
                String[] parts = clean.split("\\|", 2);
                if (parts.length < 2) {
                    throw new IllegalArgumentException("VOLC_CREDENTIAL_FORMAT");
                }
                Map<String, String> headers = VolcSigner.headers(parts[0].trim(), parts[1].trim());
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    rb.header(e.getKey(), e.getValue());
                }
                break;
            }
            default:
                rb.header("Authorization", "Bearer " + clean);
                break;
        }
        return rb.build();
    }

    private static String buildUrl(ProviderConfig.Provider provider) {
        HttpUrl base = HttpUrl.parse(provider.balanceUrl);
        if (base == null) return provider.balanceUrl;
        HttpUrl.Builder b = base.newBuilder();
        long now = System.currentTimeMillis();
        if (provider.id == ProviderConfig.PROVIDER_ANTHROPIC) {
            Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            b.addQueryParameter("starting_at", iso(start.getTime()));
            b.addQueryParameter("ending_at", iso(new Date(now + 24L * 60 * 60 * 1000)));
            b.addQueryParameter("bucket_width", "1d");
            b.addQueryParameter("limit", "31");
        } else if (provider.id == ProviderConfig.PROVIDER_OPENAI) {
            long start = (now - 30L * 24 * 60 * 60 * 1000) / 1000L;
            long end = now / 1000L;
            b.addQueryParameter("start_time", String.valueOf(start));
            b.addQueryParameter("end_time", String.valueOf(end));
            b.addQueryParameter("bucket_width", "1d");
            b.addQueryParameter("limit", "31");
        }
        return b.build().toString();
    }

    private static String iso(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(date);
    }

    private static String cleanKey(String key) {
        if (key == null) return "";
        String k = key.trim();
        if (k.regionMatches(true, 0, "Bearer ", 0, 7)) k = k.substring(7).trim();
        if (k.regionMatches(true, 0, "Authorization:", 0, 14)) {
            k = k.substring(14).trim();
        }
        return k;
    }

    private static ProviderConfig.BalanceResult fetchViaHttpUrlConnection(Context ctx,
            ProviderConfig.Provider provider, String apiKey) throws Exception {
        int timeoutSec = AppPrefs.getTimeoutSec(ctx);
        int retryCount = AppPrefs.getRetryCount(ctx);
        int attempt = 0;
        String clean = cleanKey(apiKey);

        while (true) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(buildUrl(provider));
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
                applyAuth(conn, provider, clean);
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

    private static void applyAuth(HttpURLConnection conn, ProviderConfig.Provider provider, String key) {
        switch (provider.authType) {
            case ProviderConfig.AUTH_X_API_KEY:
                conn.setRequestProperty("x-api-key", key);
                conn.setRequestProperty("anthropic-version", ProviderConfig.ANTHROPIC_VERSION);
                break;
            case ProviderConfig.AUTH_RAW:
                conn.setRequestProperty("Authorization", key);
                break;
            case ProviderConfig.AUTH_CODEX: {
                String[] parts = key.split("\\|", 2);
                conn.setRequestProperty("Authorization", "Bearer " + parts[0].trim());
                conn.setRequestProperty("User-Agent", "codex-cli");
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    conn.setRequestProperty("ChatGPT-Account-Id", parts[1].trim());
                }
                break;
            }
            case ProviderConfig.AUTH_VOLC: {
                String[] parts = key.split("\\|", 2);
                if (parts.length < 2) throw new IllegalArgumentException("VOLC_CREDENTIAL_FORMAT");
                for (Map.Entry<String, String> e
                        : VolcSigner.headers(parts[0].trim(), parts[1].trim()).entrySet()) {
                    conn.setRequestProperty(e.getKey(), e.getValue());
                }
                break;
            }
            default:
                conn.setRequestProperty("Authorization", "Bearer " + key);
                break;
        }
    }

    /** 火山引擎签名：HMAC-SHA256，固定 billing 服务的 QueryBalanceAcct。 */
    static final class VolcSigner {
        private static final String HOST = ProviderConfig.VOLC_BILLING_HOST;
        private static final String QUERY = "Action=QueryBalanceAcct&Version=2022-01-01";
        private static final String REGION = "cn-north-1";
        private static final String SERVICE = "billing";

        static Map<String, String> headers(String ak, String sk) {
            try {
                String xDate = utcDate();
                String shortDate = xDate.substring(0, 8);
                String payloadHash = sha256Hex("");
                String canonicalHeaders = "host:" + HOST + "\n"
                        + "x-content-sha256:" + payloadHash + "\n"
                        + "x-date:" + xDate + "\n";
                String signedHeaders = "host;x-content-sha256;x-date";
                String canonicalRequest = "GET\n/\n" + QUERY + "\n" + canonicalHeaders
                        + "\n" + signedHeaders + "\n" + payloadHash;
                String scope = shortDate + "/" + REGION + "/" + SERVICE + "/request";
                String stringToSign = "HMAC-SHA256\n" + xDate + "\n" + scope + "\n"
                        + sha256Hex(canonicalRequest);
                byte[] kDate = hmac(sk.getBytes(StandardCharsets.UTF_8),
                        shortDate.getBytes(StandardCharsets.UTF_8));
                byte[] kRegion = hmac(kDate, REGION.getBytes(StandardCharsets.UTF_8));
                byte[] kService = hmac(kRegion, SERVICE.getBytes(StandardCharsets.UTF_8));
                byte[] kSigning = hmac(kService, "request".getBytes(StandardCharsets.UTF_8));
                String signature = hex(hmac(kSigning, stringToSign.getBytes(StandardCharsets.UTF_8)));
                Map<String, String> out = new HashMap<>();
                out.put("X-Date", xDate);
                out.put("X-Content-Sha256", payloadHash);
                out.put("Authorization", "HMAC-SHA256 Credential=" + ak + "/" + scope
                        + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
                return out;
            } catch (Throwable t) {
                throw new IllegalStateException("VOLC_SIGN_FAILED");
            }
        }

        private static String utcDate() {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.format(new Date());
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
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
