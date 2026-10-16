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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 定时刷新：后台查询余额，低于阈值发通知。统一走 {@link NetworkClient}。 */
public class AutoRefreshReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AppPrefs.isAutoRefresh(context)) return;
        final Context ctx = context.getApplicationContext();

        int providerId = ctx.getSharedPreferences("ds_balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        String key = CryptoManager.read(ctx, "provider_" + providerId + "_api_key");
        if (key == null || key.isEmpty()) return;

        // goAsync() 让 onReceive 返回后线程仍可存活，避免后台刷新被中途终止（B7）
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                ProviderConfig.Provider p = ProviderConfig.get(ctx, providerId);
                ProviderConfig.BalanceResult result = NetworkClient.fetch(ctx, providerId, key);
                double threshold = AppPrefs.getAlertThreshold(ctx);
                if (AppPrefs.isAlertEnabled(ctx) && result.totalBalance < threshold) {
                    Notifier.notifyBalance(ctx, ctx.getString(R.string.j_116),
                            p.name + ctx.getString(R.string.j_117) + PrivacyPrefs.maskBalance(ctx, String.format("%.2f %s", result.totalBalance, result.currency))
                                    + ctx.getString(R.string.j_118) + PrivacyPrefs.maskBalance(ctx, String.format("%.2f", threshold)));
                } else {
                    Notifier.notifyBalance(ctx, ctx.getString(R.string.j_134),
                            p.name + ctx.getString(R.string.j_135) + PrivacyPrefs.maskBalance(ctx, String.format("%.2f %s", result.totalBalance, result.currency)));
                }
            } catch (Exception ignored) {
            } finally {
                if (pendingResult != null) pendingResult.finish();
            }
        }).start();
    }
}
