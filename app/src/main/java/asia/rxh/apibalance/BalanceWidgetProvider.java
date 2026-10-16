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
        String providerName = "DeepSeek";
        String balance = "¥ 0.00";
        if (!items.isEmpty()) {
            BalanceHistoryStore.HistoryItem last = items.get(items.size() - 1);
            providerName = ProviderConfig.get(last.providerId).name;
            String symbol = "CNY".equals(last.currency) ? "¥" : "$";
            balance = symbol + String.format("%.2f", last.total);
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
        if (!items.isEmpty()) {
            BalanceHistoryStore.HistoryItem last = items.get(items.size() - 1);
            String sym = "CNY".equals(last.currency) ? "¥" : "$";
            if (masked) {
                grantedText = context.getString(R.string.e_045);
                toppedText = context.getString(R.string.e_046);
            } else {
                grantedText = context.getString(R.string.e_047) + sym + String.format("%.2f", last.granted);
                toppedText = context.getString(R.string.e_048) + sym + String.format("%.2f", last.topped);
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
