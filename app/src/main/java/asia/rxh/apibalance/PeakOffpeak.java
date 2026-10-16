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

import java.util.Calendar;

/**
 * 峰谷价趣味卡片（仅 DeepSeek 服务商展示）。
 *
 * 梁文峰 / 梁文谷是 DeepSeek 专属的谐音梗：峰时 = 梁文峰，空闲时段 = 梁文谷。
 * 其它服务商没有这套峰谷定价，因此 {@link #isApplicable(int)} 只对 DeepSeek 返回 true。
 *
 * 2026 年 8 月起 DeepSeek 现行峰谷规则（北京时间）：
 *   - 高峰时段：周一至周五（不含法定节假日）09:00-12:00、14:00-18:00
 *   - 空闲时段：其余时间，含周末与法定节假日全天；空闲价 = 高峰价一半
 */
public final class PeakOffpeak {

    private PeakOffpeak() {
    }

    /** 峰谷卡片只适用于 DeepSeek。 */
    public static boolean isApplicable(int providerId) {
        return providerId == ProviderConfig.PROVIDER_DEEPSEEK;
    }

    /** 当前是否处于 DeepSeek 高峰计费时段。 */
    public static boolean isPeak() {
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;
        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        int day = c.get(Calendar.DAY_OF_WEEK);

        boolean weekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY;
        if (weekend) return false; // 周末全天空闲

        if (isPublicHoliday(month, dayOfMonth)) return false; // 法定节假日全天空闲

        int minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        boolean morning = minutes >= 540 && minutes < 720;   // 09:00-12:00
        boolean afternoon = minutes >= 840 && minutes < 1080; // 14:00-18:00
        return morning || afternoon;
    }

    /**
     * 公历固定的法定节假日近似判断（全天视为空闲时段）。
     * 注意：春节 / 清明 / 端午 / 中秋等农历节日日期逐年漂移，无法在未联网或未内置
     * 历表的情况下精确计算，这里只覆盖日期固定的公历节假日（含国庆/劳动节等黄金周）。
     */
    private static boolean isPublicHoliday(int month, int dayOfMonth) {
        if (month == 1 && dayOfMonth == 1) return true;          // 元旦
        if (month == 5 && dayOfMonth >= 1 && dayOfMonth <= 5) return true; // 劳动节
        if (month == 10 && dayOfMonth >= 1 && dayOfMonth <= 7) return true; // 国庆
        return false;
    }

    public static String name(android.content.Context c) {
        return isPeak() ? c.getString(R.string.g_015) : c.getString(R.string.g_016);
    }

    public static String iconText(android.content.Context c) {
        return isPeak() ? c.getString(R.string.g_017) : c.getString(R.string.g_018);
    }

    /** 全部内置俏皮话，供主题中心里挑选用。 */
    public static String[] allQuips(android.content.Context c) {
        return new String[]{
                c.getString(R.string.g_019), c.getString(R.string.g_020),
                c.getString(R.string.g_021), c.getString(R.string.g_022),
                c.getString(R.string.g_023), c.getString(R.string.g_024),
                c.getString(R.string.g_025), c.getString(R.string.g_026),
                c.getString(R.string.g_027), c.getString(R.string.g_028),
                c.getString(R.string.g_029), c.getString(R.string.g_030),
                c.getString(R.string.g_043), c.getString(R.string.g_044),
                c.getString(R.string.g_045), c.getString(R.string.g_046),
                c.getString(R.string.g_047), c.getString(R.string.g_048),
                c.getString(R.string.g_049), c.getString(R.string.g_050),
                c.getString(R.string.g_051), c.getString(R.string.g_052),
                c.getString(R.string.g_053), c.getString(R.string.g_054)};
    }

    /** 俏皮话（不用 emoji）。 */
    public static String quip(android.content.Context c) {
        if (isPeak()) {
            String[] q = {
                c.getString(R.string.g_019),
                c.getString(R.string.g_020),
                c.getString(R.string.g_021),
                c.getString(R.string.g_022),
                c.getString(R.string.g_023),
                c.getString(R.string.g_024),
                c.getString(R.string.g_043),
                c.getString(R.string.g_044),
                c.getString(R.string.g_045),
                c.getString(R.string.g_046),
                c.getString(R.string.g_047),
                c.getString(R.string.g_048)
            };
            return q[(int) (System.currentTimeMillis() / 1000 % q.length)];
        } else {
            String[] q = {
                c.getString(R.string.g_025),
                c.getString(R.string.g_026),
                c.getString(R.string.g_027),
                c.getString(R.string.g_028),
                c.getString(R.string.g_029),
                c.getString(R.string.g_030),
                c.getString(R.string.g_049),
                c.getString(R.string.g_050),
                c.getString(R.string.g_051),
                c.getString(R.string.g_052),
                c.getString(R.string.g_053),
                c.getString(R.string.g_054)
            };
            return q[(int) (System.currentTimeMillis() / 1000 % q.length)];
        }
    }
}
