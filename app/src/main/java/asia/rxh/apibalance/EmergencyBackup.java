package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Activity;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * 崩溃页紧急备份：有密码时必须验证密码；没有密码时只有实验开关开启才允许导出，
 * 并生成一次性口令，避免导出后无法恢复。
 */
public final class EmergencyBackup {

    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private EmergencyBackup() {
    }

    public static void request(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (PrivacyPrefs.hasPassword(activity)) {
            UiDialog.showInputWithDesc(activity, activity.getString(R.string.emergency_auth_title),
                    activity.getString(R.string.emergency_auth_desc),
                    activity.getString(R.string.emergency_password_hint), "",
                    value -> {
                        if (value == null || !PrivacyPrefs.checkPassword(activity, value.trim())) {
                            Toast.makeText(activity, activity.getString(R.string.lock_wrong),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        export(activity, value.trim(), false);
                    });
            return;
        }
        if (!AppPrefs.isEmergencyBackup(activity)) {
            Toast.makeText(activity, activity.getString(R.string.emergency_disabled),
                    Toast.LENGTH_LONG).show();
            return;
        }
        UiDialog.showConfirm(activity, activity.getString(R.string.emergency_risk_title),
                activity.getString(R.string.emergency_risk_desc),
                activity.getString(R.string.emergency_continue),
                activity.getString(R.string.dlg_cancel),
                () -> {
                    String pwd = randomPassword();
                    export(activity, pwd, true);
                }, null);
    }

    private static void export(final Activity activity, final String password,
                               final boolean showPassword) {
        Toast.makeText(activity, activity.getString(R.string.emergency_exporting),
                Toast.LENGTH_SHORT).show();
        AppExecutors.io().execute(() -> {
            try {
                String path = BackupActivity.emergencyExport(activity, password);
                activity.runOnUiThread(() -> {
                    String message = activity.getString(R.string.emergency_saved, path);
                    if (showPassword) {
                        message += "\n\n" + activity.getString(R.string.emergency_generated_password,
                                password);
                    }
                    UiDialog.showConfirm(activity,
                            activity.getString(R.string.emergency_done_title), message,
                            activity.getString(R.string.h_004), null, null, null);
                });
            } catch (Throwable t) {
                activity.runOnUiThread(() -> Toast.makeText(activity,
                        activity.getString(R.string.emergency_failed),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
