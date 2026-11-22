package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 悬浮窗背景选择。
 *
 * 这里刻意做成一个有实体界面的普通 Activity，而不是“透明 Activity + 弹窗”：
 * 部分国产 ROM 会在 DocumentsUI 返回前把透明的中间 Activity 回收，结果就是选完图
 * 什么也没发生。实体界面能稳定活到 onActivityResult。
 */
public class FloatBgPickerActivity extends Activity {

    private static final int REQ_PICK = 41;
    private android.widget.ProgressBar progress;
    private Button pickButton, resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(getString(R.string.float_bg_title));
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(ThemeUtils.textPrimary(this));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView desc = new TextView(this);
        desc.setText(getString(R.string.float_bg_processing));
        desc.setTextSize(13);
        desc.setGravity(Gravity.CENTER);
        desc.setTextColor(ThemeUtils.textSecondary(this));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(8);
        root.addView(desc, dlp);

        progress = new android.widget.ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(12);
        root.addView(progress, plp);

        Button pick = new Button(this);
        pickButton = pick;
        pick.setText(getString(R.string.float_bg_pick));
        pick.setAllCaps(false);
        pick.setOnClickListener(v -> pickImage());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        blp.topMargin = dp(20);
        root.addView(pick, blp);

        Button reset = new Button(this);
        resetButton = reset;
        reset.setText(getString(R.string.float_bg_reset));
        reset.setAllCaps(false);
        reset.setOnClickListener(v -> {
            AppPrefs.setFloatBgUri(this, "");
            notifyService();
            finish();
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        rlp.topMargin = dp(10);
        root.addView(reset, rlp);
        return root;
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        try {
            startActivityForResult(i, REQ_PICK);
        } catch (Throwable t) {
            // 没有 DocumentsUI 的 ROM 退回普通选择器
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("image/*");
            try {
                startActivityForResult(fallback, REQ_PICK);
            } catch (Throwable t2) {
                Toast.makeText(this, getString(R.string.float_bg_failed), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finish();
            return;
        }
        Uri uri = data.getData();
        Toast.makeText(this, getString(R.string.float_bg_processing), Toast.LENGTH_SHORT).show();
        if (progress != null) progress.setVisibility(android.view.View.VISIBLE);
        if (pickButton != null) pickButton.setEnabled(false);
        if (resetButton != null) resetButton.setEnabled(false);
        new Thread(() -> {
            try {
                String path = ImageStore.importImage(this, uri, "float_bg", 1600);
                runOnUiThread(() -> {
                    AppPrefs.setFloatBgUri(this, path);
                    Toast.makeText(this, getString(R.string.float_bg_saved), Toast.LENGTH_SHORT).show();
                    notifyService();
                    finish();
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.float_bg_failed), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, "float-bg-save").start();
    }

    private void notifyService() {
        try {
            Intent i = new Intent(this, FloatingWindowService.class);
            i.setAction("float_bg");
            startService(i);
        } catch (Throwable ignored) {
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
