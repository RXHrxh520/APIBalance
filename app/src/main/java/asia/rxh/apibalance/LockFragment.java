package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 密码锁界面：支持首次设置、确认和验证。指纹使用系统 BiometricPrompt。 */
public class LockFragment extends Fragment {

    private static final int MODE_VERIFY = 0;
    private static final int MODE_SET_FIRST = 1;
    private static final int MODE_SET_CONFIRM = 2;

    private int mode = MODE_VERIFY;
    private final StringBuilder input = new StringBuilder();
    private String firstPass = "";
    private boolean autoTried;
    private android.os.CancellationSignal biometricCancel;

    private TextView tvTitle;
    private TextView tvError;
    private TextView tvFinger;
    private LinearLayout dotRow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lock, container, false);
        tvTitle = view.findViewById(R.id.tv_lock_title);
        tvError = view.findViewById(R.id.tv_lock_error);
        tvFinger = view.findViewById(R.id.tv_lock_finger);
        dotRow = view.findViewById(R.id.dot_row);
        TextView tvForgot = view.findViewById(R.id.tv_forgot);

        int[] digits = {R.id.btn_key_1, R.id.btn_key_2, R.id.btn_key_3, R.id.btn_key_4,
                R.id.btn_key_5, R.id.btn_key_6, R.id.btn_key_7, R.id.btn_key_8, R.id.btn_key_9};
        for (int i = 0; i < digits.length; i++) {
            final String d = String.valueOf(i + 1);
            View k = view.findViewById(digits[i]);
            if (k != null) k.setOnClickListener(v -> onKey(d));
        }
        View zero = view.findViewById(R.id.btn_key_0);
        if (zero != null) zero.setOnClickListener(v -> onKey("0"));
        View del = view.findViewById(R.id.btn_key_del);
        if (del != null) del.setOnClickListener(v -> onDelete());
        View ok = view.findViewById(R.id.btn_key_ok);
        if (ok != null) ok.setOnClickListener(v -> submit());
        if (tvForgot != null) tvForgot.setOnClickListener(v -> showForgot());
        if (tvFinger != null) tvFinger.setOnClickListener(v -> tryBiometric());

        mode = PrivacyPrefs.hasPassword(getActivity()) ? MODE_VERIFY : MODE_SET_FIRST;
        updateTitle();
        renderDots();
        ThemeUtils.apply(view);
        if (tvFinger != null) tvFinger.setTextColor(ThemeUtils.accent(getActivity()));
        if (tvForgot != null) tvForgot.setTextColor(ThemeUtils.textSecondary(getActivity()));
        if (tvError != null) tvError.setTextColor(0xFFEF4444);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mode != MODE_VERIFY || autoTried) return;
        autoTried = true;
        tryBiometric();
    }

    private void onKey(String d) {
        if (input.length() >= 16) return;
        input.append(d);
        hideError();
        renderDots();
    }

    private void onDelete() {
        if (input.length() > 0) {
            input.deleteCharAt(input.length() - 1);
            renderDots();
        }
    }

    private void renderDots() {
        if (dotRow == null || getActivity() == null) return;
        dotRow.removeAllViews();
        int n = Math.max(4, input.length());
        int density = (int) getResources().getDisplayMetrics().density;
        for (int i = 0; i < n; i++) {
            View dot = new View(getActivity());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(12 * density, 12 * density);
            lp.leftMargin = 7 * density;
            lp.rightMargin = 7 * density;
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_lock_dot);
            if (i < input.length()) {
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                g.setColor(ThemeUtils.accent(getActivity()));
                dot.setBackground(g);
            }
            dotRow.addView(dot);
        }
    }

    private void updateTitle() {
        if (tvTitle == null) return;
        if (mode == MODE_SET_FIRST) {
            tvTitle.setText(getString(R.string.j_lock_set_title) + " · " + getString(R.string.j_lock_set_hint));
        } else if (mode == MODE_SET_CONFIRM) {
            tvTitle.setText(getString(R.string.lock_confirm_title));
        } else {
            tvTitle.setText(getString(R.string.lock_enter));
        }
    }

    private void showError(String msg) {
        if (tvError == null) return;
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private static final String GUARD_PREFS = "lock_guard";
    private static final int MAX_FAILS = 5;
    private static final long BLOCK_MS = 60_000L;

    /** 连续输错达到上限后返回剩余锁定毫秒数，未锁定返回 0。 */
    private long lockRemainMs() {
        if (getActivity() == null || !AppPrefs.isLockLimitEnabled(getActivity())) return 0;
        long until = getActivity().getSharedPreferences(GUARD_PREFS, 0).getLong("until", 0);
        return Math.max(0, until - System.currentTimeMillis());
    }

    private void registerFailure() {
        if (getActivity() == null) return;
        android.content.SharedPreferences sp = getActivity().getSharedPreferences(GUARD_PREFS, 0);
        int fails = sp.getInt("fails", 0) + 1;
        android.content.SharedPreferences.Editor e = sp.edit();
        if (fails >= MAX_FAILS) {
            e.putInt("fails", 0).putLong("until", System.currentTimeMillis() + BLOCK_MS);
        } else {
            e.putInt("fails", fails);
        }
        e.apply();
    }

    private void clearFailures() {
        if (getActivity() == null) return;
        getActivity().getSharedPreferences(GUARD_PREFS, 0).edit()
                .putInt("fails", 0).putLong("until", 0).apply();
    }

    private void submit() {
        String pw = input.toString();
        long remain = lockRemainMs();
        if (remain > 0) {
            showError(getString(R.string.lock_too_many_fmt, (int) Math.ceil(remain / 1000.0)));
            input.setLength(0);
            renderDots();
            return;
        }
        if (mode == MODE_SET_FIRST) {
            if (pw.length() < 4) {
                showError(getString(R.string.j_lock_set_hint));
                return;
            }
            firstPass = pw;
            input.setLength(0);
            mode = MODE_SET_CONFIRM;
            updateTitle();
            renderDots();
            hideError();
            return;
        }
        if (mode == MODE_SET_CONFIRM) {
            if (!pw.equals(firstPass)) {
                showError(getString(R.string.lock_mismatch));
                input.setLength(0);
                mode = MODE_SET_FIRST;
                updateTitle();
                renderDots();
                return;
            }
            PrivacyPrefs.setPassword(getActivity(), pw);
            if (!PrivacyPrefs.hasPassword(getActivity())) {
                showError(getString(R.string.lock_wrong));
                input.setLength(0);
                return;
            }
            PrivacyPrefs.setLockEnabled(getActivity(), true);
            Toast.makeText(getActivity(), getString(R.string.lock_set_done), Toast.LENGTH_SHORT).show();
            unlock();
            return;
        }
        if (PrivacyPrefs.checkPassword(getActivity(), pw)) {
            clearFailures();
            unlock();
        } else {
            registerFailure();
            showError(getString(R.string.lock_wrong));
            input.setLength(0);
            renderDots();
        }
    }

    private void unlock() {
        MainActivity act = (MainActivity) getActivity();
        if (act != null) act.onLockPassed();
    }

    /** API 28+ 使用系统 BiometricPrompt；更低版本没有统一接口，保留密码验证。 */
    private void tryBiometric() {
        final android.app.Activity act = getActivity();
        if (act == null) return;
        if (Build.VERSION.SDK_INT < 28) {
            Toast.makeText(act, getString(R.string.lock_fp_none), Toast.LENGTH_SHORT).show();
            return;
        }
        // 先确认本机有指纹硬件，避免点下去毫无反应。
        if (!act.getPackageManager().hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_FINGERPRINT)) {
            Toast.makeText(act, getString(R.string.lock_fp_none), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (biometricCancel != null) biometricCancel.cancel();
            biometricCancel = new android.os.CancellationSignal();
            android.hardware.biometrics.BiometricPrompt prompt =
                    new android.hardware.biometrics.BiometricPrompt.Builder(act)
                            .setTitle(getString(R.string.lock_enter))
                            .setNegativeButton(getString(R.string.dlg_cancel),
                                    act.getMainExecutor(), (dialog, which) -> {
                                    })
                            .build();
            prompt.authenticate(biometricCancel, act.getMainExecutor(),
                    new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                android.hardware.biometrics.BiometricPrompt.AuthenticationResult result) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(LockFragment.this::unlock);
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(), getString(R.string.lock_fp_fail),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(act, getString(R.string.lock_fp_none), Toast.LENGTH_SHORT).show();
        }
    }

    private void showForgot() {
        UiDialog.showConfirm(getActivity(), getString(R.string.lock_forgot),
                getString(R.string.lock_forgot_clear_desc),
                getString(R.string.lock_forgot_code_new), getString(R.string.lock_forgot_clear),
                this::askF2a, this::confirmClear);
    }

    /**
     * 恢复代码必须提前在已解锁状态设置。锁屏页不再允许“现场新建”，
     * 否则等于绕过锁屏；没设过只能走清空数据。
     */
    private void askF2a() {
        if (!PrivacyPrefs.hasF2a(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.lock_forgot_no_code),
                    Toast.LENGTH_LONG).show();
            return;
        }
        UiDialog.showInput(getActivity(), getString(R.string.lock_forgot_code),
                getString(R.string.lock_forgot_code_hint), "", code -> {
                    if (PrivacyPrefs.checkF2a(getActivity(), code)) {
                        PrivacyPrefs.clearPassword(getActivity());
                        PrivacyPrefs.setLockEnabled(getActivity(), false);
                        Toast.makeText(getActivity(), getString(R.string.lock_reset_done),
                                Toast.LENGTH_SHORT).show();
                        unlock();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.lock_forgot_wrong),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmClear() {
        UiDialog.showConfirm(getActivity(), getString(R.string.lock_forgot_clear),
                getString(R.string.lock_clear_confirm),
                getString(R.string.lock_forgot_clear), getString(R.string.dlg_cancel),
                this::clearEverything, null);
    }

    private void clearEverything() {
        MainActivity act = (MainActivity) getActivity();
        if (act != null) act.wipeAndRestart();
    }
}

