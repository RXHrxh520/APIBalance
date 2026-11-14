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

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额显示：欠费只有几分钱时，不能再四舍五入成 -0.00。
 *
 * 以前各页面各自用 String.format("%.2f") 拼字符串，负数小额会显示成 "-0.00"，
 * 部分机型上甚至只剩 "-0"。这里统一处理：
 * · 绝对值 ≥ 0.01：两位小数；
 * · 绝对值 < 0.01 且不为 0：最多四位小数并去掉末尾零，真实反映欠了几分；
 * · 正好为 0：显示 0.00。
 */
public final class MoneyFmt {

    private MoneyFmt() {
    }

    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0.00";
        try {
            BigDecimal exact = BigDecimal.valueOf(value);
            BigDecimal two = exact.setScale(2, RoundingMode.HALF_UP);
            if (two.signum() == 0 && value != 0) {
                // 不足一分钱（正负都算）：保留四位并去掉末尾零，别显示成 0.00
                BigDecimal fine = exact.setScale(4, RoundingMode.HALF_UP);
                if (fine.signum() == 0) {
                    return exact.stripTrailingZeros().toPlainString();
                }
                return fine.stripTrailingZeros().toPlainString();
            }
            return two.toPlainString();
        } catch (Throwable t) {
            return String.format(java.util.Locale.US, "%.2f", value);
        }
    }

    /** 带符号前缀，例如 ¥-0.0031；负号由数字本身携带，避免出现 ¥--0.01。 */
    public static String withSymbol(String symbol, double value) {
        return symbol + format(value);
    }
}
