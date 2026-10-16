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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_PREFS = "ds_crash_prefs";
    static final String KEY_HAS_CRASH = "has_crash";
    static final String KEY_CRASH_MSG = "crash_message";
    static final String KEY_CRASH_TRACE = "crash_trace";
    static final String KEY_CRASH_TIME = "crash_time";

    private final Context appContext;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (!(current instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context, current));
        }
    }

    private CrashHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.appContext = context.getApplicationContext();
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 1. 解析异常
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = throwable.getClass().getSimpleName();
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();

        // 截断过长堆栈（保留前2000字符）
        if (trace.length() > 2000) {
            trace = trace.substring(0, 2000) + appContext.getString(R.string.f_026);
        }

        // 2. 保存到 SharedPreferences（进程安全）
        SharedPreferences prefs = appContext.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_HAS_CRASH, true)
                .putString(KEY_CRASH_MSG, message)
                .putString(KEY_CRASH_TRACE, trace)
                .putLong(KEY_CRASH_TIME, System.currentTimeMillis())
                .commit(); // 同步写入，确保不丢失

        // 3. 尝试启动 CrashActivity
        try {
            Intent intent = new Intent(appContext, CrashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("crash_message", message);
            intent.putExtra("crash_trace", trace);
            appContext.startActivity(intent);
        } catch (Exception ignored) {
            // 如果弹出失败也没关系，下次启动会检测到
        }

        // 4. 等待一小会让 Activity 有机会启动，然后交给默认处理器（杀死进程）
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        if (defaultHandler != null && defaultHandler != this) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    /** 检查是否有待处理的崩溃 */
    public static boolean hasPendingCrash(Context context) {
        return context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_HAS_CRASH, false);
    }

    /** 获取保存的崩溃信息 */
    public static String[] getCrashInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE);
        return new String[]{
                prefs.getString(KEY_CRASH_MSG, context.getString(R.string.f_039)),
                prefs.getString(KEY_CRASH_TRACE, context.getString(R.string.f_040)),
                String.valueOf(prefs.getLong(KEY_CRASH_TIME, 0))
        };
    }

    /** 清除崩溃标记 */
    public static void clearCrash(Context context) {
        context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
}
