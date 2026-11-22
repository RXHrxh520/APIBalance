package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 定时刷新：后台查询余额，低于阈值发通知。统一走 {@link NetworkClient}。 */
public class AutoRefreshReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 小组件「刷新」是手动触发：不看自动刷新总开关，否则按钮就是个死键
        boolean manual = intent != null && "REFRESH".equals(intent.getAction());
        if (!manual && !AppPrefs.isAutoRefresh(context)) return;
        final Context ctx = context.getApplicationContext();

        int providerId = ctx.getSharedPreferences("balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        String key = CryptoManager.read(ctx, "provider_" + providerId + "_api_key");
        if (key == null || key.isEmpty()) return;

        // goAsync() 让 onReceive 返回后线程仍可存活，避免后台刷新被中途终止（B7）
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                ProviderConfig.Provider p = ProviderConfig.get(ctx, providerId);
                ProviderConfig.BalanceResult result = NetworkClient.fetch(ctx, providerId, key);
                if (manual) {
                    // 手动刷新只更新界面，不发通知，避免每次点一下都响一声
                    BalanceWidgetProvider.updateAll(ctx);
                    return;
                }
                double thresholdCny = AppPrefs.getAlertThreshold(ctx);
                double threshold = "USD".equals(result.currency)
                        ? thresholdCny / AppPrefs.getUsdCnyRate(ctx) : thresholdCny;
                if (AppPrefs.isAlertEnabled(ctx) && result.totalBalance < threshold) {
                    Notifier.notifyBalance(ctx, ctx.getString(R.string.j_116),
                            p.name + ctx.getString(R.string.j_117) + PrivacyPrefs.maskBalance(ctx, String.format("%.2f %s", result.totalBalance, result.currency))
                                    + ctx.getString(R.string.j_118) + PrivacyPrefs.maskBalance(ctx, MoneyFmt.format(threshold) + " " + result.currency));
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
