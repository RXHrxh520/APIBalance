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

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 智谱（GLM）鉴权，新旧两套密钥都要能用。
 *
 * · 新版 API Key 是一整串（形如 32 位十六进制），直接放进 Authorization 头；
 * · 旧版密钥是「{id}.{secret}」两段式，必须现场用 HS256 签一个 JWT，
 *   再把 JWT 放进 Authorization 头（不带 Bearer）。
 *
 * 以前只按 Bearer 处理，旧密钥一律 401；这里按密钥形态自动分流，不做任何裁剪。
 */
public final class GlmAuth {

    private GlmAuth() {
    }

    /** 返回应写入 Authorization 头的值。 */
    public static String authorization(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isEmpty()) return "";
        if (!key.contains(".")) {
            // 新版密钥：整串直接用，不加 Bearer
            return key;
        }
        String[] parts = key.split("\\.", 2);
        String id = parts[0].trim();
        String secret = parts.length > 1 ? parts[1].trim() : "";
        if (id.isEmpty() || secret.isEmpty()) return key;
        long now = System.currentTimeMillis() / 1000L;
        String header = "{\"alg\":\"HS256\",\"sign_type\":\"SIGN\"}";
        String payload = "{\"api_key\":\"" + id + "\",\"exp\":" + (now + 3600L)
                + ",\"timestamp\":" + now + "}";
        String h = b64(header);
        String p = b64(payload);
        String signingInput = h + "." + p;
        return signingInput + "." + hmacSha256(signingInput, secret);
    }

    private static String b64(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(sig,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception e) {
            throw new IllegalStateException("GLM_SIGN_FAILED", e);
        }
    }
}
