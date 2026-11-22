package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** 桌面悬浮球：点击弹出最近余额。 */
public class FloatingWindowService extends Service {

    private WindowManager wm;
    private View floatBall;
    private View floatPanel;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams panelParams;
    private int touchStartX, touchStartY;
    private int startX, startY;
    private boolean isDragging = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            // 拿不到悬浮窗权限：明确告知用户，而不是静默退出
            android.widget.Toast.makeText(this, getString(R.string.j_014),
                    android.widget.Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundCompat();
        if (intent != null && "float_bg".equals(intent.getAction())) {
            applyPanelBackground();
            return START_STICKY;
        }
        if (floatBall == null) {
            createFloatBall();
        }
        return START_STICKY;
    }


    /** Android 8+ 后台服务需要前台通知；Android 14+ 由清单声明 specialUse 类型。 */
    private void startForegroundCompat() {
        try {
            final String channelId = "floating_balance";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationManager nm =
                        getSystemService(android.app.NotificationManager.class);
                if (nm != null) {
                    android.app.NotificationChannel ch = new android.app.NotificationChannel(
                            channelId, getString(R.string.float_channel),
                            android.app.NotificationManager.IMPORTANCE_LOW);
                    nm.createNotificationChannel(ch);
                }
            }
            android.content.Intent open = new android.content.Intent(this, MainActivity.class);
            open.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                    this, 0, open,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                            | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(this, channelId)
                    : new android.app.Notification.Builder(this);
            android.app.Notification n = b.setSmallIcon(R.drawable.ic_wallet)
                    .setContentTitle(getString(R.string.float_running))
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
            startForeground(1003, n);
        } catch (Throwable t) {
            stopSelf();
        }
    }

    private void createFloatBall() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int size = dp(48);
        ballParams = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        if (PrivacyPrefs.shouldMaskBalance(this) || AppPrefs.noScreenshot(this)) {
            ballParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        }
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = dp(20);
        ballParams.y = dp(200);

        TextView ball = new TextView(this);
        // 悬浮球直接显示余额数字（紧凑），点击即刷新
        ball.setText(compactBalance());
        ball.setTextColor(0xFFFFFFFF);
        ball.setTextSize(13);
        ball.setGravity(Gravity.CENTER);
        ball.setPadding(dp(8), 0, dp(8), 0);
        ball.setBackgroundResource(R.drawable.bg_gradient_button);
        ball.setAlpha(0.9f);
        floatBall = ball;
        try {
            wm.addView(floatBall, ballParams);
        } catch (Exception ignored) {
            stopSelf();
            return;
        }
        // 点击/拖动统一走 onTouch：未拖动且 <300ms 视为点击，触发刷新并展开面板；拖动只移动位置。
        // （原 onClick 会被 onTouch 消费事件导致永不触发，已移除。）
        floatBall.setOnTouchListener(new View.OnTouchListener() {
            private int downX, downY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = (int) e.getRawX();
                        downY = (int) e.getRawY();
                        startX = ballParams.x;
                        startY = ballParams.y;
                        downTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawX() - downX) > 10 || Math.abs(e.getRawY() - downY) > 10) {
                            isDragging = true;
                            ballParams.x = startX + (int) (e.getRawX() - downX);
                            ballParams.y = startY + (int) (e.getRawY() - downY);
                            wm.updateViewLayout(floatBall, ballParams);
                            // 展开的面板跟随小球一起移动
                            if (floatPanel != null && panelParams != null) {
                                panelParams.x = Math.max(8, ballParams.x - dp(30));
                                panelParams.y = ballParams.y + dp(56);
                                wm.updateViewLayout(floatPanel, panelParams);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        long held = System.currentTimeMillis() - downTime;
                        if (!isDragging && held >= 700) {
                            openFloatBgPicker();
                            return true;
                        }
                        if (!isDragging && held < 300) {
                            refreshAndUpdate();
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    /** 从最近历史取余额，紧凑显示（如 ¥12.34）。 */
    private String compactBalance() {
        int currentPid = getSharedPreferences("balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        ProviderConfig.Provider cp = ProviderConfig.get(this, currentPid);
        if (cp.isQuotaMode()) return getString(R.string.float_mode_quota);
        if (cp.isCostMode()) return getString(R.string.float_mode_cost);
        List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(this);
        // 只认当前服务商的记录，切服务商后不能还挂着上一家的金额
        BalanceHistoryStore.HistoryItem last = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).providerId == currentPid) {
                last = items.get(i);
                break;
            }
        }
        if (last == null) return "¥?";
        if (PrivacyPrefs.shouldMaskBalance(this)) return "••••";
        String symbol = "CNY".equals(last.currency) ? "¥" : "$";
        return MoneyFmt.withSymbol(symbol, last.total);
    }

    /** 点击悬浮球：后台刷新余额并更新悬浮球文本（无需打开 App）。 */
    private void refreshAndUpdate() {
        final android.widget.TextView ball = (android.widget.TextView) floatBall;
        if (ball != null) ball.setText("…");
        final int pid = getSharedPreferences("balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        final String key = CryptoManager.read(this, "provider_" + pid + "_api_key");
        if (key == null || key.isEmpty()) {
            if (ball != null) ball.setText(compactBalance());
            return;
        }
        new Thread(() -> {
            try {
                ProviderConfig.BalanceResult result = NetworkClient.fetch(this, pid, key);
                if (!result.isQuota()) {
                    BalanceHistoryStore.add(this, pid, result);
                    BalanceWidgetProvider.updateAll(this);
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (floatBall != null) {
                        ((TextView) floatBall).setText(compactBalance());
                    }
                });
            } catch (Exception ignored) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (floatBall != null) {
                        ((TextView) floatBall).setText(compactBalance());
                    }
                });
            }
        }).start();
    }

    /** 长按悬浮球打开背景选择器。 */
    private void openFloatBgPicker() {
        try {
            android.content.Intent i = new android.content.Intent(this, FloatBgPickerActivity.class);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    /** 面板背景优先使用用户选的图片，数字和控件仍由原布局渲染。 */
    private void applyPanelBackground() {
        if (floatPanel == null) return;
        String uri = AppPrefs.getFloatBgUri(this);
        if (uri == null || uri.trim().isEmpty()) {
            floatPanel.setBackgroundResource(R.drawable.bg_dialog);
            return;
        }
        // 先挂默认背景，解码放到后台线程：大图在主线程解码会直接把悬浮窗卡住
        floatPanel.setBackgroundResource(R.drawable.bg_dialog);
        final View panel = floatPanel;
        new Thread(() -> {
            android.graphics.Bitmap bmp = decodePanelBackground(uri);
            if (bmp == null) return;
            new android.os.Handler(getMainLooper()).post(() -> {
                if (panel == floatPanel && floatPanel != null) {
                    floatPanel.setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), bmp));
                } else {
                    bmp.recycle();
                }
            });
        }, "float-bg-decode").start();
    }

    /** 背景图降采样解码；优先读应用私有文件，兼容旧的 content:// 记录。 */
    private android.graphics.Bitmap decodePanelBackground(String stored) {
        try {
            String path = ImageStore.localPathOf(stored);
            java.io.InputStream in = path != null
                    ? new java.io.FileInputStream(path)
                    : getContentResolver().openInputStream(android.net.Uri.parse(stored));
            if (in == null) return null;
            android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) > Math.max(720, screenW)) {
                sample *= 2;
            }
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = sample;
            in = path != null
                    ? new java.io.FileInputStream(path)
                    : getContentResolver().openInputStream(android.net.Uri.parse(stored));
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in, null, opts);
            if (in != null) in.close();
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    private void togglePanel() {
        if (floatPanel != null) {
            hidePanel();
            return;
        }
        showPanel();
    }

    private void showPanel() {
        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = Math.max(8, ballParams.x - dp(30));
        panelParams.y = ballParams.y + dp(56);

        floatPanel = LayoutInflater.from(this).inflate(R.layout.float_panel, null);
        applyPanelBackground();
        fillPanel();

        View btnRefresh = floatPanel.findViewById(R.id.btn_float_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> fillPanel());
        }
        View btnClose = floatPanel.findViewById(R.id.btn_float_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                hidePanel();
                stopSelf();
            });
        }
        floatPanel.setOnClickListener(v -> {
            hidePanel();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        try {
            ThemeUtils.apply(floatPanel);
        } catch (Throwable ignored) {
        }

        try {
            wm.addView(floatPanel, panelParams);
        } catch (Exception ignored) {
        }
    }

    /** 把最近一次余额填进面板；刷新按钮与首次显示都走这里。 */
    private void fillPanel() {
        if (floatPanel == null) return;
        try {
            TextView tvBalance = floatPanel.findViewById(R.id.float_balance);
            TextView tvProvider = floatPanel.findViewById(R.id.float_provider);
            java.util.List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(this);
            if (!items.isEmpty()) {
                BalanceHistoryStore.HistoryItem last = items.get(items.size() - 1);
                String symbol = "CNY".equals(last.currency) ? "¥" : "$";
                tvBalance.setText(PrivacyPrefs.maskBalance(this, MoneyFmt.withSymbol(symbol, last.total)));
                tvProvider.setText(ProviderConfig.get(this, last.providerId).name);
            } else {
                tvBalance.setText(PrivacyPrefs.maskBalance(this, getString(R.string.j_139)));
                tvProvider.setText(getString(R.string.j_184));
            }
        } catch (Throwable ignored) {
        }
    }

    private void hidePanel() {
        if (floatPanel != null) {
            try {
                wm.removeView(floatPanel);
            } catch (Exception ignored) {
            }
            floatPanel = null;
        }
    }

    @Override
    public void onDestroy() {
        if (floatBall != null) {
            try {
                wm.removeView(floatBall);
            } catch (Exception ignored) {
            }
            floatBall = null;
        }
        hidePanel();
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
