package asia.rxh.apibalance;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * 长按桌面图标「卸载软件」后进入的界面。
 * 不是弹窗：有返回、有紧急备份、有继续使用，绝不强行阻止卸载。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public class UninstallActivity extends Activity {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppPrefs.wrapLocale(base));
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_uninstall);
        } catch (Throwable t) {
            finish();
            return;
        }
        try {
            ThemeUtils.apply(findViewById(android.R.id.content));
        } catch (Throwable ignored) {
        }
        View back = findViewById(R.id.un_back);
        if (back != null) back.setOnClickListener(v -> finish());
        View keep = findViewById(R.id.un_keep);
        if (keep != null) keep.setOnClickListener(v -> finish());
        View backup = findViewById(R.id.un_backup);
        if (backup != null) backup.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, BackupActivity.class));
            } catch (Throwable ignored) {
            }
        });
        View go = findViewById(R.id.un_go);
        if (go != null) go.setOnClickListener(v -> doUninstall());

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            int rid = getResources().getIdentifier("uninstall_" + i, "string", getPackageName());
            if (rid != 0) {
                sb.append(getString(rid));
                if (i < 5) sb.append("\n");
            }
        }
        TextView lines = findViewById(R.id.un_lines);
        if (lines != null) lines.setText(sb.toString());
    }

    private void doUninstall() {
        // 改成直接打开本应用的「应用详情」页，由用户自己点卸载。
        // 系统卸载器在很多机型/被重签名后调不起来，绕一大圈反而什么都没发生。
        try {
            Intent it = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
            finish();
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, getString(R.string.un_manual),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
