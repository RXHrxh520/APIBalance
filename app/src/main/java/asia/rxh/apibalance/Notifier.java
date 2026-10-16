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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** 通知工具：余额提醒 / 定时刷新结果。 */
public final class Notifier {

    private static final String CHANNEL_ID = "balance_alerts";

    private Notifier() {
    }

    private static void ensureChannel(Context c) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, c.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
    }

    public static void notifyBalance(Context c, String title, String text) {
        notifyBalance(c, title, text, 1001);
    }

    public static void notifyBalance(Context c, String title, String text, int id) {
        if (AppPrefs.noNotify(c)) return; // 隐私：禁用通知
        boolean quiet = AppPrefs.inQuietHours(c);
        ensureChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 静音时段：用低重要度通道（不响铃）
        String channel = quiet ? CHANNEL_QUIET : CHANNEL_ID;
        ensureQuietChannel(c);
        Intent i = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, channel)
                : new Notification.Builder(c);
        Notification.Builder nb = b.setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOngoing(id == 1002);
        // 仅常驻通知(id==1002)带刷新按钮，普通提醒保持简洁
        if (id == 1002) {
            nb.addAction(R.drawable.ic_refresh, c.getString(R.string.j_131), refreshPendingIntent(c));
        }
        Notification n = nb.build();
        nm.notify(id, n);
    }

    private static final String CHANNEL_QUIET = "balance_quiet";
    private static void ensureQuietChannel(Context c) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_QUIET, c.getString(R.string.j_132), NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }

    /** 常驻余额通知（更新余额内容）。 */
    public static void showSticky(Context c, String provider, String balanceText) {
        if (!AppPrefs.isStickyNotify(c)) return;
        notifyBalance(c, "APIBalance", provider + c.getString(R.string.j_133) + PrivacyPrefs.maskBalance(c, balanceText), 1002);
    }

    public static void cancelSticky(Context c) {
        c.getSystemService(NotificationManager.class).cancel(1002);
    }

    private static PendingIntent refreshPendingIntent(Context c) {
        Intent i = new Intent(c, AutoRefreshReceiver.class);
        i.setAction("REFRESH");
        return PendingIntent.getBroadcast(c, 999, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}