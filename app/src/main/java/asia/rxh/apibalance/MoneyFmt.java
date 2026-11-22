package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
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
        // 接口会把欠费返回成字符串 "-0.00"；double 的 -0.0 与 0 相等，普通比较会丢掉负号
        if (isNegativeZero(value)) return "-0.00";
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

    /** 是否是「负零」：值等于 0，但符号位为负。 */
    public static boolean isNegativeZero(double value) {
        return value == 0.0 && Double.doubleToRawLongBits(value) != 0L;
    }

    /** 带符号前缀，例如 ¥-0.0031；负号由数字本身携带，避免出现 ¥--0.01。 */
    public static String withSymbol(String symbol, double value) {
        return symbol + format(value);
    }
}
