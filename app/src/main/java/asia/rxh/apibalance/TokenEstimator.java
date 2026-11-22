package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

/** Token 估算器：按中英混合文本估算 token 数（近似：1 英文≈1 token，1 中文≈1.5 token）。 */
public final class TokenEstimator {

    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cjk = 0;
        int ascii = 0;
        for (char c : text.toCharArray()) {
            if (c > 0x2E80) { // CJK 及扩展
                cjk++;
            } else if (c > 0x1F || c == '\n' || c == '\t') {
                ascii++;
            }
        }
        return (int) Math.ceil(cjk * 1.5) + ascii;
    }
}
