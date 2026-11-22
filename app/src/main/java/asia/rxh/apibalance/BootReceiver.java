package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机后恢复定时刷新。AlarmManager 的重复闹钟在重启后会被系统清空，
 * 之前没这个接收器，用户开了自动刷新也会在重启后“永久失效”。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        if (!AppPrefs.isAutoRefresh(context)) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(context, AutoRefreshReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long interval = Math.max(60, AppPrefs.getRefreshIntervalSec(context)) * 1000L;
        long first = System.currentTimeMillis() + 60 * 1000L;
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi);
    }
}
