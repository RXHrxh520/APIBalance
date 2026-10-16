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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

public class LoginFragment extends Fragment {

    private EditText etApiKey, etCaptcha;
    private Switch swAutoLogin;
    private TextView tvProvider;
    private TextView btnQuery;
    private TextView tvError;
    private ImageView ivLogo;
    private LinearLayout cardCaptcha;
    private TextView tvCaptchaQuestion;
    private View cardLogin;
    private boolean isQuerying = false;
    private int captchaAnswer;
    private int providerId = ProviderConfig.PROVIDER_DEEPSEEK;
    private Random random = new Random();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        etApiKey = view.findViewById(R.id.et_api_key);
        etCaptcha = view.findViewById(R.id.et_captcha);
        tvProvider = view.findViewById(R.id.row_provider);
        swAutoLogin = view.findViewById(R.id.sw_auto_login);
        btnQuery = view.findViewById(R.id.btn_query);
        tvError = view.findViewById(R.id.tv_error);
        ivLogo = view.findViewById(R.id.iv_logo);
        cardLogin = view.findViewById(R.id.card_login);
        cardCaptcha = view.findViewById(R.id.card_captcha);
        tvCaptchaQuestion = view.findViewById(R.id.tv_captcha_question);

        swAutoLogin.setChecked(true);

        // 服务商选择：下拉选择器（OpenAI 不支持，置灰禁用）
        MainActivity activity0 = (MainActivity) getActivity();
        if (activity0 != null) {
            providerId = activity0.getCurrentProviderId();
        }
        setupProviderSpinner();

        // 应用自定义应用图标（登录小头像），无自定义则保留默认
        AppIconManager.applyIconTo(ivLogo, getActivity());

        // Entrance animations
        ivLogo.setAlpha(0f);
        ivLogo.setScaleX(0.5f);
        ivLogo.setScaleY(0.5f);
        ivLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(100)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        cardLogin.setAlpha(0f);
        cardLogin.setTranslationY(100f);
        cardLogin.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();

        btnQuery.setOnClickListener(v -> {
            if (isQuerying) return;
            String apiKey = etApiKey.getText().toString().trim();
            if (apiKey.isEmpty()) {
                showError(getString(R.string.err_empty_key));
                return;
            }
            if (!ProviderConfig.isValidKey(providerId, apiKey)) {
                showError(getString(R.string.err_bad_key));
                return;
            }
            // If captcha not shown yet → show it first
            if (cardCaptcha.getVisibility() != View.VISIBLE) {
                showCaptcha();
                return;
            }
            // Verify captcha
            String answerStr = etCaptcha.getText().toString().trim();
            if (answerStr.isEmpty()) {
                showError(getString(R.string.err_empty_captcha));
                return;
            }
            try {
                int answer = Integer.parseInt(answerStr);
                if (answer != captchaAnswer) {
                    // Wrong answer → new question
                    showError(getString(R.string.err_bad_captcha));
                    setRandomCaptcha();
                    etCaptcha.setText("");
                    return;
                }
            } catch (NumberFormatException e) {
                showError(getString(R.string.err_captcha_num));
                return;
            }
            doQuery(apiKey);
        });

        ThemeUtils.apply(view);
        return view;
    }

    private void showCaptcha() {
        setRandomCaptcha();
        cardCaptcha.setVisibility(View.VISIBLE);
        cardCaptcha.setAlpha(0f);
        cardCaptcha.setTranslationY(30f);
        cardCaptcha.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(0.6f))
                .start();
    }

    private void setRandomCaptcha() {
        int a = random.nextInt(10) + 1; // 1-10，避免 0+10 这类带零题
        int b = random.nextInt(10) + 1; // 1-10
        captchaAnswer = a + b;
        tvCaptchaQuestion.setText(a + " + " + b + " = ?");
    }

    private void doQuery(String apiKey) {
        isQuerying = true;
        hideError();
        btnQuery.setText(R.string.querying);
        btnQuery.setAlpha(0.7f);
        btnQuery.setClickable(false);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        final MainActivity act = activity; // 捕获引用：登录成功后本 Fragment 会被替换，getActivity() 会变 null
        activity.fetchBalance(apiKey, providerId, new MainActivity.OnBalanceFetchListener() {
            @Override
            public void onSuccess(int pid, ProviderConfig.BalanceResult result) {
                if (act == null) return;
                boolean autoLogin = swAutoLogin.isChecked();
                act.onLoginSuccess(apiKey, pid, autoLogin);
                // 询问是否保存为账户（多账户）；用捕获的 act 而非 getActivity()，避免 NPE
                final int fpid = pid;
                final String fkey = apiKey;
                // 关键：这里全部用 act.getString（捕获的 Activity 引用），
                // 不能用 getString()——Fragment 在登录成功后已被替换（detach），会抛 IllegalStateException
                UiDialog.showInput(act, act.getString(R.string.j_099),
                        act.getString(R.string.j_100), "",
                        name -> {
                            if (name == null || name.trim().isEmpty()) {
                                android.widget.Toast.makeText(act,
                                        act.getString(R.string.j_101), android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (AccountManager.saveAccount(act.getApplicationContext(), fpid, name.trim(), fkey)) {
                                android.widget.Toast.makeText(act,
                                        act.getString(R.string.j_102) + name.trim(), android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                android.widget.Toast.makeText(act,
                                        act.getString(R.string.j_103), android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                isQuerying = false;
                btnQuery.setText(R.string.query_balance);
                btnQuery.setAlpha(1f);
                btnQuery.setClickable(true);
                showError(errorMessage);
                // Shake error
                tvError.setTranslationX(0f);
                tvError.animate()
                        .translationX(10f).setDuration(50)
                        .withEndAction(() -> tvError.animate()
                                .translationX(-10f).setDuration(50)
                                .withEndAction(() -> tvError.animate()
                                        .translationX(5f).setDuration(50)
                                        .withEndAction(() -> tvError.animate()
                                                .translationX(0f).setDuration(50).start())))
                        .start();
            }
        });
    }

    /** 下拉选择器：列出可选服务商，末尾带「配置自定义服务商」入口。 */
    private void setupProviderSpinner() {
        final String[] base = ProviderConfig.names(getActivity());
        final int[] ids = ProviderConfig.pickableIds();
        final String[] opts = new String[ids.length + 1];
        for (int i = 0; i < ids.length; i++) {
            opts[i] = (ids[i] >= 0 && ids[i] < base.length) ? base[ids[i]] : "";
        }
        opts[ids.length] = getString(R.string.onb_custom_entry);
        int cur = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == providerId) cur = i;
        }
        final int init = cur;
        providerId = ids[init];
        final int[] sel = { init };
        if (tvProvider == null) return;
        tvProvider.setText(opts[init]);
        android.graphics.drawable.Drawable chev = getResources().getDrawable(R.drawable.ic_chevron_down);
        if (chev != null) chev.setTint(ThemeUtils.textSecondary(getActivity()));
        tvProvider.setCompoundDrawablesWithIntrinsicBounds(null, null, chev, null);
        tvProvider.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
        tvProvider.setOnClickListener(v -> UiDialog.showPicker(getActivity(),
                getString(R.string.onb_provider_title), opts, sel[0], pos -> {
                    if (pos == ids.length) {
                        showCustomProviderEditor();
                        return;
                    }
                    sel[0] = pos;
                    providerId = ids[pos];
                    tvProvider.setText(opts[pos]);
                }));
    }

    /** 配置自定义供应商：名→URL→认证方式→币种→余额字段路径，逐项输入并严格校验。 */
    private void showCustomProviderEditor() {
        final MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        UiDialog.showInput(act, act.getString(R.string.custom_title), act.getString(R.string.custom_name_hint),
                AppPrefs.getCustomProviderName(act), name -> {
            UiDialog.showInput(act, act.getString(R.string.custom_title), act.getString(R.string.custom_url_hint),
                    AppPrefs.getCustomProviderUrl(act), url -> {
                UiDialog.showPicker(act, act.getString(R.string.custom_auth),
                        new String[]{act.getString(R.string.custom_auth_bearer), act.getString(R.string.custom_auth_xkey)},
                        AppPrefs.getCustomProviderAuth(act), auth -> {
                    UiDialog.showPicker(act, act.getString(R.string.custom_currency), new String[]{"CNY", "USD"},
                            "USD".equals(AppPrefs.getCustomProviderCurrency(act)) ? 1 : 0, cur -> {
                        UiDialog.showInput(act, getString(R.string.h_050),
                                act.getString(R.string.custom_path_hint),
                                AppPrefs.getCustomProviderPath(act), path -> {
                            String currency = cur == 1 ? "USD" : "CNY";
                            String err = ProviderConfig.validateCustom(getActivity(),name, url, auth, path, currency);
                            if (err != null) {
                                android.widget.Toast.makeText(act, err, android.widget.Toast.LENGTH_SHORT).show();
                                return;
                            }
                            AppPrefs.setCustomProviderName(act, name.trim());
                            AppPrefs.setCustomProviderUrl(act, url.trim());
                            AppPrefs.setCustomProviderAuth(act, auth);
                            AppPrefs.setCustomProviderCurrency(act, currency);
                            AppPrefs.setCustomProviderPath(act, path.trim());
                            android.widget.Toast.makeText(act, act.getString(R.string.custom_saved) + name.trim(),
                                    android.widget.Toast.LENGTH_SHORT).show();
                            providerId = ProviderConfig.PROVIDER_CUSTOM;
                            setupProviderSpinner();
                        });
                    });
                });
            });
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        tvError.setAlpha(0f);
        tvError.animate().alpha(1f).setDuration(200).start();
    }

    private void hideError() {
        tvError.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> tvError.setVisibility(View.GONE)).start();
    }
}