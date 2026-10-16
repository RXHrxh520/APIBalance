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
        if (floatBall == null) {
            createFloatBall();
        }
        return START_STICKY;
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
                        if (!isDragging && System.currentTimeMillis() - downTime < 300) {
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
        List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(this);
        if (items.isEmpty()) return "¥?";
        BalanceHistoryStore.HistoryItem last = items.get(items.size() - 1);
        if (PrivacyPrefs.shouldMaskBalance(this)) return "••••";
        String symbol = "CNY".equals(last.currency) ? "¥" : "$";
        return symbol + String.format("%.2f", last.total);
    }

    /** 点击悬浮球：后台刷新余额并更新悬浮球文本（无需打开 App）。 */
    private void refreshAndUpdate() {
        final android.widget.TextView ball = (android.widget.TextView) floatBall;
        if (ball != null) ball.setText("…");
        final int pid = getSharedPreferences("ds_balance_prefs", 0)
                .getInt("provider_id", ProviderConfig.PROVIDER_DEEPSEEK);
        final String key = CryptoManager.read(this, "provider_" + pid + "_api_key");
        if (key == null || key.isEmpty()) {
            if (ball != null) ball.setText(compactBalance());
            return;
        }
        new Thread(() -> {
            try {
                ProviderConfig.BalanceResult result = NetworkClient.fetch(this, pid, key);
                BalanceHistoryStore.add(this, pid, result);
                BalanceWidgetProvider.updateAll(this);
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
                tvBalance.setText(PrivacyPrefs.maskBalance(this, symbol + String.format("%.2f", last.total)));
                tvProvider.setText(ProviderConfig.get(last.providerId).name);
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
