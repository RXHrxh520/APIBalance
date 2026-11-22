package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/** 桌面小组件：显示最近一次余额，点击打开 App。 */
public class BalanceWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager am = AppWidgetManager.getInstance(context);
        int[] ids = am.getAppWidgetIds(
                new android.content.ComponentName(context, BalanceWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, am, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager am, int widgetId) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_balance);
        // 从历史取最近一条
        List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(context);
        int currentPid = context.getSharedPreferences("balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        String providerName = ProviderConfig.get(context, currentPid).name;
        String balance = "¥ 0.00";
        BalanceHistoryStore.HistoryItem last = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).providerId == currentPid) {
                last = items.get(i);
                break;
            }
        }
        if (last != null) {
            providerName = ProviderConfig.get(last.providerId).name;
            String symbol = "CNY".equals(last.currency) ? "¥" : "$";
            balance = MoneyFmt.withSymbol(symbol, last.total);
        }
        rv.setTextViewText(R.id.widget_title, "APIBalance");
        rv.setTextViewText(R.id.widget_provider, providerName);
        boolean masked = PrivacyPrefs.shouldMaskBalance(context);
        if (masked) {
            balance = "••••";
            providerName = "APIBalance";
        }
        rv.setTextViewText(R.id.widget_balance, balance);
        // 明细：赠金 / 充值
        String grantedText = "";
        String toppedText = "";
        if (last != null) {
            String sym = "CNY".equals(last.currency) ? "¥" : "$";
            if (masked) {
                grantedText = context.getString(R.string.e_045);
                toppedText = context.getString(R.string.e_046);
            } else {
                grantedText = context.getString(R.string.e_047) + MoneyFmt.withSymbol(sym, last.granted);
                toppedText = context.getString(R.string.e_048) + MoneyFmt.withSymbol(sym, last.topped);
            }
        }
        rv.setTextViewText(R.id.widget_granted, grantedText);
        rv.setTextViewText(R.id.widget_topped, toppedText);

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_balance, pi);

        // 刷新按钮：触发 AutoRefreshReceiver（action REFRESH）
        Intent refresh = new Intent(context, AutoRefreshReceiver.class);
        refresh.setAction("REFRESH");
        PendingIntent rpi = PendingIntent.getBroadcast(context, 1, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_refresh, rpi);

        am.updateAppWidget(widgetId, rv);
    }
}
