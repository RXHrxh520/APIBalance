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
