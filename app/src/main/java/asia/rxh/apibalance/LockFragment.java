package asia.rxh.apibalance;

/*
 * Copyright (C) 2026 软小寒
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 专用密码输入界面：圆点指示 + 数字键盘，而不是一个光秃秃的输入框。
 * 三种状态：首次设置密码 / 再次确认密码 / 输入密码解锁。
 * 另提供指纹（带 CryptoObject，能真正唤起）与系统原生验证（KeyguardManager）。
 * 忘记密码提供「清除所有数据」与「用 F2a 验证代码重置」两条路径。
 */
public class LockFragment extends Fragment {

    private static final String KEY_ALIAS = "rxh_lock_key";
    private static final int MODE_VERIFY = 0;
    private static final int MODE_SET_FIRST = 1;
    private static final int MODE_SET_CONFIRM = 2;

    private int mode = MODE_VERIFY;
    private final StringBuilder input = new StringBuilder();
    private String firstPass = "";
    private boolean credentialLaunched;
    private boolean autoTried;

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 只自动弹一次：验证器返回时 onResume 会再触发，不设闸就会反复横跳卡死
        if (mode != MODE_VERIFY || autoTried) return;
        autoTried = true;
        if (false) { // 系统原生验证已下线
            trySystemCredential();
        } else {
            tryBiometric();
        }
    }

    // ---------- 键盘 ----------

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

    /** 每输入一位就画一个点，让用户看得见自己输了几位。 */
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
            boolean filled = i < input.length();
            if (filled) {
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

    // ---------- 提交 ----------

    private void submit() {
        String pw = input.toString();
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
            PrivacyPrefs.setLockEnabled(getActivity(), true);
            Toast.makeText(getActivity(), getString(R.string.lock_set_done), Toast.LENGTH_SHORT).show();
            unlock();
            return;
        }
        if (PrivacyPrefs.checkPassword(getActivity(), pw)) {
            unlock();
        } else {
            showError(getString(R.string.lock_wrong));
            input.setLength(0);
            renderDots();
        }
    }

    private void unlock() {
        MainActivity act = (MainActivity) getActivity();
        if (act != null) act.onLockPassed();
    }

    // ---------- 系统原生验证 ----------

    private void trySystemCredential() {
        if (true) return; // 系统密码验证器已下线，见下方说明
        final android.app.Activity act = getActivity();
        if (act == null) return;
        try {
            android.app.KeyguardManager km =
                    (android.app.KeyguardManager) act.getSystemService(android.content.Context.KEYGUARD_SERVICE);
            if (km == null || !km.isKeyguardSecure()) return;
            android.content.Intent it = km.createConfirmDeviceCredentialIntent(
                    getString(R.string.lock_enter), getString(R.string.lock_enter));
            if (it == null) return;
            credentialLaunched = true;
            startActivityForResult(it, 7001);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7001) {
            credentialLaunched = false;
            if (resultCode == android.app.Activity.RESULT_OK) unlock();
        }
    }

    // ---------- 指纹 ----------

    private void tryBiometric() {
        final android.app.Activity act = getActivity();
        if (act == null) return;
        try {
            android.hardware.fingerprint.FingerprintManager fm =
                    act.getSystemService(android.hardware.fingerprint.FingerprintManager.class);
            if (fm == null || !fm.isHardwareDetected() || !fm.hasEnrolledFingerprints()) {
                Toast.makeText(act, getString(R.string.lock_fp_none), Toast.LENGTH_SHORT).show();
                return;
            }
            android.hardware.fingerprint.FingerprintManager.CryptoObject crypto = cryptoObject();
            android.hardware.fingerprint.FingerprintManager.AuthenticationCallback cb =
                    new android.hardware.fingerprint.FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                android.hardware.fingerprint.FingerprintManager.AuthenticationResult result) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(LockFragment.this::unlock);
                            }
                        }
                    };
            fm.authenticate(crypto, null, 0, cb, null);
        } catch (Throwable ignored) {
        }
    }

    /** 在 AndroidKeyStore 里准备一把必须验证身份才能使用的密钥，指纹才能真正被唤起。 */
    private android.hardware.fingerprint.FingerprintManager.CryptoObject cryptoObject() {
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (!ks.containsAlias(KEY_ALIAS)) {
                javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance(
                        android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                kg.init(new android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .build());
                kg.generateKey();
            }
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES + "/"
                            + android.security.keystore.KeyProperties.BLOCK_MODE_CBC + "/"
                            + android.security.keystore.KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, (java.security.Key) ks.getKey(KEY_ALIAS, null));
            return new android.hardware.fingerprint.FingerprintManager.CryptoObject(cipher);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------- 忘记密码 ----------

    private void showForgot() {
        UiDialog.showConfirm(getActivity(), getString(R.string.lock_forgot),
                getString(R.string.lock_forgot_clear_desc),
                getString(R.string.lock_forgot_code_new), getString(R.string.lock_forgot_clear),
                this::askF2a, this::confirmClear);
    }

    private void askF2a() {
        // 还没有恢复代码就地设一个，然后再验证，不用跑去设置里
        if (!PrivacyPrefs.hasF2a(getActivity())) {
            UiDialog.showInputWithDesc(getActivity(), getString(R.string.f2a_set_now_title),
                    getString(R.string.f2a_set_now_desc), getString(R.string.f2a_row_hint), "", code -> {
                        if (code == null || code.trim().isEmpty()) return;
                        PrivacyPrefs.setF2a(getActivity(), code.trim());
                        android.widget.Toast.makeText(getActivity(), getString(R.string.f2a_saved),
                                android.widget.Toast.LENGTH_SHORT).show();
                        askF2a();
                    });
            return;
        }
        UiDialog.showInput(getActivity(), getString(R.string.lock_forgot_code),
                getString(R.string.lock_forgot_code_hint), "", code -> {
                    if (PrivacyPrefs.getF2a(getActivity()).equals(code == null ? "" : code.trim())) {
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

    /** 清空全部数据，恢复到初始状态。 */
    private void clearEverything() {
        try {
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.wipeAndRestart();
        } catch (Throwable ignored) {
        }
    }
}
