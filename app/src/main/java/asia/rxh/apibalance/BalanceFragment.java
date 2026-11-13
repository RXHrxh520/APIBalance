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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BalanceFragment extends Fragment {

    private static final String ARG_API_KEY = "api_key";
    private static final String ARG_PROVIDER = "provider_id";

    private String apiKey;
    private int providerId = ProviderConfig.PROVIDER_DEEPSEEK;

    private TextView tvProviderTitle;
    private TextView tvCurrencySymbol, tvTotalBalance, tvCurrency;
    private TextView tvGrantedBalance, tvToppedUpBalance, tvRefreshLabel;
    private TextView tvTotalLabel, tvGrantedLabel, tvToppedLabel, tvAppNotice;
    private TextView tvBalanceError;
    private ImageView ivBalanceLogo;
    private LinearLayout layoutLoading;
    private LinearLayout cardTotal, cardGranted, cardTopped, cardDetails, btnRefresh;
    private View cardPeak;
    private Handler handler;
    private boolean isFirstLoad = true;
    private String lastCurrency = "";
    private double lastTotal = 0, lastGranted = 0, lastTopped = 0;

    public static BalanceFragment newInstance(String apiKey, int providerId) {
        BalanceFragment frag = new BalanceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_API_KEY, apiKey);
        args.putInt(ARG_PROVIDER, providerId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            apiKey = getArguments().getString(ARG_API_KEY);
            providerId = getArguments().getInt(ARG_PROVIDER, ProviderConfig.PROVIDER_DEEPSEEK);
        }
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance, container, false);

        tvProviderTitle = view.findViewById(R.id.tv_provider_title);
        tvCurrencySymbol = view.findViewById(R.id.tv_currency_symbol);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvCurrency = view.findViewById(R.id.tv_currency);
        tvGrantedBalance = view.findViewById(R.id.tv_granted_balance);
        tvToppedUpBalance = view.findViewById(R.id.tv_topped_up_balance);
        tvTotalLabel = view.findViewById(R.id.tv_total_label);
        tvGrantedLabel = view.findViewById(R.id.tv_granted_label);
        tvToppedLabel = view.findViewById(R.id.tv_topped_label);
        tvAppNotice = view.findViewById(R.id.tv_app_notice);
        tvBalanceError = view.findViewById(R.id.tv_balance_error);
        layoutLoading = view.findViewById(R.id.layout_loading);
        cardTotal = view.findViewById(R.id.card_total);
        cardGranted = view.findViewById(R.id.card_granted);
        cardTopped = view.findViewById(R.id.card_topped);
        cardDetails = view.findViewById(R.id.card_details);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvRefreshLabel = view.findViewById(R.id.tv_refresh_label);
        ivBalanceLogo = view.findViewById(R.id.iv_balance_logo);
        // 应用自定义应用图标（主界面图标），无自定义则保留默认
        AppIconManager.applyIconTo(ivBalanceLogo, getActivity());
        cardPeak = view.findViewById(R.id.card_peak);
        TextView tvPeakName = view.findViewById(R.id.tv_peak_name);
        TextView tvPeakQuip = view.findViewById(R.id.tv_peak_quip);
        TextView tvPeakBadge = view.findViewById(R.id.tv_peak_badge);

        // 标题和金额区按服务商类型显示，避免把套餐、成本说成余额。
        ProviderConfig.Provider startProvider = ProviderConfig.get(getActivity(), providerId);
        tvProviderTitle.setText(startProvider.name + " · " + startProvider.modeLabel);
        tvProviderTitle.setTextColor(ThemePrefs.getColor(getActivity()));
        if (tvTotalLabel != null) {
            tvTotalLabel.setText(startProvider.isQuotaMode() ? "套餐剩余"
                    : startProvider.isCostMode() ? "累计成本"
                    : getString(R.string.str_012));
        }
        if (startProvider.isCostMode() || startProvider.isQuotaMode()) {
            if (cardDetails != null) cardDetails.setVisibility(View.GONE);
            if (cardPeak != null) cardPeak.setVisibility(View.GONE);
        }
        bindRemoteNotice();

        // 峰谷卡片：仅 DeepSeek 展示（其它服务商无此定价规则）
        boolean showPeak = PeakOffpeak.isApplicable(providerId);
        if (cardPeak != null) {
            cardPeak.setVisibility(showPeak ? View.VISIBLE : View.GONE);
        }
        if (showPeak && tvPeakName != null) {
            tvPeakName.setText(PeakOffpeak.name(getActivity()));
            tvPeakBadge.setText(PeakOffpeak.iconText(getActivity()));
            String customQuip = AppPrefs.getCustomQuip(getActivity());
            tvPeakQuip.setText(customQuip != null && !customQuip.isEmpty()
                    ? customQuip : PeakOffpeak.quip(getActivity()));
            // 长按卡片文案即可自定义，长按对话框留空则恢复默认
            // 首次进页面提示一次：这句话可以长按自定义
            if (!AppPrefs.isQuipHintShown(getActivity())) {
                AppPrefs.setQuipHintShown(getActivity(), true);
                tvPeakQuip.postDelayed(() -> {
                    if (getActivity() == null) return;
                    android.widget.Toast.makeText(getActivity(), getString(R.string.quip_hint),
                            android.widget.Toast.LENGTH_LONG).show();
                }, 1200L);
            }
            tvPeakQuip.setOnLongClickListener(v -> {
                final String cur = AppPrefs.getCustomQuip(getActivity());
                UiDialog.showInputWithDesc(getActivity(), getString(R.string.quip_edit_title),
                        getString(R.string.quip_edit_hint), getString(R.string.quip_edit_field),
                        cur == null ? "" : cur, q -> {
                            AppPrefs.setCustomQuip(getActivity(), q == null ? "" : q.trim());
                            String nq = AppPrefs.getCustomQuip(getActivity());
                            tvPeakQuip.setText(nq.isEmpty() ? PeakOffpeak.quip(getActivity()) : nq);
                        });
                return true;
            });
            int peakColor = PeakOffpeak.isPeak() ? 0xFFE65100 : 0xFF2E7D32;
            tvPeakBadge.setBackgroundColor(peakColor);
        }
        tvProviderTitle.setClickable(true);
        tvProviderTitle.setFocusable(true);
        tvProviderTitle.setOnClickListener(v -> {
            MainActivity act = (MainActivity) getActivity();
            if (act == null) return;
            final int[] ids = ProviderConfig.pickableIds();
            final String[] opts = new String[ids.length];
            int current = 0;
            for (int i = 0; i < ids.length; i++) {
                opts[i] = ProviderConfig.get(act, ids[i]).name;
                if (ids[i] == providerId) current = i;
            }
            UiDialog.showPicker(act, getString(R.string.j_105), opts, current, w -> {
                if (w >= 0 && w < ids.length && ids[w] != providerId) act.switchProviderQuick(ids[w]);
            });
        });

        btnRefresh.setOnClickListener(v -> {
            if (tvRefreshLabel.getText().toString().equals(getString(R.string.j_106))) {
                tvRefreshLabel.setText(getString(R.string.j_107));
                btnRefresh.setAlpha(0.6f);
                fetchBalanceData();
            }
        });


        // 分享余额（品牌格式卡片图）
        ImageView btnShare = view.findViewById(R.id.btn_share);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.showShareFragment();
                }
            });
        }

        // 峰谷趣味卡片：点击查看说明
        View peakCard = view.findViewById(R.id.card_peak);
        if (peakCard != null) {
            peakCard.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                boolean peak = PeakOffpeak.isPeak();
                UiDialog.showConfirm(act, peak ? getString(R.string.j_108) : getString(R.string.j_109),
                        peak
                                ? getString(R.string.j_110)
                                : getString(R.string.j_111),
                        getString(R.string.j_112), null, null, null);
            });
        }
        // 总额/赠金/充值卡片：点击查看含义
        View totalCard = view.findViewById(R.id.card_total);
        if (totalCard != null) {
            totalCard.setOnClickListener(v -> android.widget.Toast.makeText(getActivity(),
                    getString(R.string.j_113), android.widget.Toast.LENGTH_SHORT).show());
        }
        View grantedCard = view.findViewById(R.id.card_granted);
        if (grantedCard != null) {
            grantedCard.setOnClickListener(v -> android.widget.Toast.makeText(getActivity(),
                    getString(R.string.j_114), android.widget.Toast.LENGTH_SHORT).show());
        }
        View toppedCard = view.findViewById(R.id.card_topped);
        if (toppedCard != null) {
            toppedCard.setOnClickListener(v -> android.widget.Toast.makeText(getActivity(),
                    getString(R.string.j_115), android.widget.Toast.LENGTH_SHORT).show());
        }

        showLoading();
        fetchBalanceData();

        ThemeUtils.apply(view);
        return view;
    }

    private void fetchBalanceData() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        activity.fetchBalance(apiKey, providerId, new MainActivity.OnBalanceFetchListener() {
            @Override
            public void onSuccess(int pid, ProviderConfig.BalanceResult result) {
                if (getActivity() == null) return;
                // 套餐配额没有金额，不写余额历史，避免趋势图被 0 污染。
                if (!result.isQuota()) {
                    BalanceHistoryStore.add(getActivity().getApplicationContext(), pid, result);
                    BalanceWidgetProvider.updateAll(getActivity().getApplicationContext());
                }
                MainActivity act = (MainActivity) getActivity();
                if (act != null && AppPrefs.isStickyNotify(act)) act.showStickyNotify();
                // 余额提醒
                if (!result.isQuota() && AppPrefs.isAlertEnabled(getActivity())) {
                    double threshold = AppPrefs.getAlertThreshold(getActivity());
                    if (result.totalBalance < threshold) {
                        Notifier.notifyBalance(getActivity(), getString(R.string.j_116),
                                ProviderConfig.get(getActivity(), pid).name + getString(R.string.j_117)
                                        + PrivacyPrefs.maskBalance(getActivity(), String.format("%.2f %s", result.totalBalance, result.currency))
                                        + getString(R.string.j_118) + PrivacyPrefs.maskBalance(getActivity(), String.format("%.2f", threshold)));
                    }
                }
                if (isFirstLoad) {
                    updateUIFirst(result);
                    isFirstLoad = false;
                } else {
                    updateUIQuiet(result);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                if (isFirstLoad) {
                    showError(errorMessage);
                } else {
                    // 后续刷新失败：Toast 提示，保留旧数据与界面（B3）
                    hideLoading();
                    android.widget.Toast.makeText(getActivity(), errorMessage, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** 首次加载：根据余额、成本或套餐切换整块内容。 */
    private void updateUIFirst(ProviderConfig.BalanceResult result) {
        hideLoading();
        if (result.isQuota()) {
            showQuota(result);
            animateLogo();
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.animateCardEntrance(cardTotal, 100);
            return;
        }
        ProviderConfig.Provider provider = ProviderConfig.get(getActivity(), providerId);
        tvCurrencySymbol.setVisibility(View.VISIBLE);
        tvTotalLabel.setText(provider.isCostMode() ? "累计成本" : getString(R.string.str_012));
        tvGrantedLabel.setText(provider.isCostMode() ? "说明" : getString(R.string.str_018));
        tvToppedLabel.setText(provider.isCostMode() ? "说明" : getString(R.string.str_019));
        if (cardDetails != null) cardDetails.setVisibility(provider.isCostMode() ? View.GONE : View.VISIBLE);
        if (cardPeak != null && provider.isCostMode()) cardPeak.setVisibility(View.GONE);

        double[] conv = convertBalance(result);
        lastCurrency = currencyLabel(result.currency);
        lastTotal = conv[0];
        lastGranted = conv[1];
        lastTopped = conv[2];

        tvCurrency.setText(provider.isCostMode() ? "本月至今 · " + lastCurrency : currencyLabel(result.currency));
        String symbol = lastCurrency.contains("CNY") ? "¥" : "$";
        tvCurrencySymbol.setText(symbol);

        animateLogo();

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.animateCardEntrance(cardTotal, 100);
            if (!provider.isCostMode()) {
                activity.animateCardEntrance(cardGranted, 250);
                activity.animateCardEntrance(cardTopped, 350);
            }
            if (PrivacyPrefs.isHideBalance(getActivity())) {
                tvTotalBalance.setText("****");
            } else if (lastTotal < 0) {
                tvTotalBalance.setText(formatBalance(lastTotal));
            } else {
                activity.animateNumberCount(tvTotalBalance, 0, lastTotal, "", "");
            }
        }
        tvGrantedBalance.setText(formatBalance(lastGranted));
        tvToppedUpBalance.setText(formatBalance(lastTopped));
    }

    private void animateLogo() {
        ivBalanceLogo.setAlpha(0f);
        ivBalanceLogo.setScaleX(0f);
        ivBalanceLogo.setScaleY(0f);
        ivBalanceLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                        getActivity(), R.interpolator.overshoot))
                .start();
    }

    /** 后续刷新：只更新该服务商真正有意义的数字。 */
    private void updateUIQuiet(ProviderConfig.BalanceResult result) {
        hideLoading();
        if (result.isQuota()) {
            showQuota(result);
            hideError();
            return;
        }
        ProviderConfig.Provider provider = ProviderConfig.get(getActivity(), providerId);
        tvCurrencySymbol.setVisibility(View.VISIBLE);
        tvTotalLabel.setText(provider.isCostMode() ? "累计成本" : getString(R.string.str_012));
        tvGrantedLabel.setText(provider.isCostMode() ? "说明" : getString(R.string.str_018));
        tvToppedLabel.setText(provider.isCostMode() ? "说明" : getString(R.string.str_019));
        if (cardDetails != null) {
            cardDetails.setVisibility(provider.isCostMode() ? View.GONE : View.VISIBLE);
        }
        double[] conv = convertBalance(result);
        tvCurrency.setText(provider.isCostMode() ? "本月至今 · " + currencyLabel(result.currency)
                : currencyLabel(result.currency));
        String symbol = currencyLabel(result.currency).contains("CNY") ? "¥" : "$";
        tvCurrencySymbol.setText(symbol);

        double newTotal = conv[0];
        double newGranted = conv[1];
        double newTopped = conv[2];

        if (!isFirstLoad && Math.abs(newTotal - lastTotal) > 0.0001) {
            boolean up = newTotal > lastTotal;
            int color = up ? 0xFF2E7D32 : 0xFFC62828;
            tvProviderTitle.setTextColor(color);
            tvProviderTitle.setText(provider.name + (up ? " ↑" : " ↓"));
            tvProviderTitle.postDelayed(() -> {
                if (tvProviderTitle != null && getActivity() != null) {
                    tvProviderTitle.setTextColor(ThemePrefs.getColor(getActivity()));
                    tvProviderTitle.setText(provider.name + " · " + provider.modeLabel);
                }
            }, 2500);
        } else if (getActivity() != null) {
            tvProviderTitle.setTextColor(ThemePrefs.getColor(getActivity()));
            tvProviderTitle.setText(provider.name + " · " + provider.modeLabel);
        }

        if (PrivacyPrefs.isHideBalance(getActivity())) {
            tvTotalBalance.setText("****");
        } else if (newTotal < 0) {
            tvTotalBalance.setText(formatBalance(newTotal));
        } else if (Math.abs(newTotal - lastTotal) > 0.001) {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.animateNumberCount(tvTotalBalance, lastTotal, newTotal, "", "");
        } else {
            tvTotalBalance.setText(formatBalance(newTotal));
        }
        lastTotal = newTotal;

        tvGrantedBalance.setText(formatBalance(newGranted));
        lastGranted = newGranted;
        tvToppedUpBalance.setText(formatBalance(newTopped));
        lastTopped = newTopped;

        hideError();
    }

    /** 套餐配额不伪装成金额，直接展示窗口剩余百分比与重置时间。 */
    private void showQuota(ProviderConfig.BalanceResult result) {
        ProviderConfig.Provider provider = ProviderConfig.get(getActivity(), providerId);
        tvProviderTitle.setTextColor(ThemePrefs.getColor(getActivity()));
        tvProviderTitle.setText(provider.name + " · " + provider.modeLabel);
        tvTotalLabel.setText(result.mainLabel == null ? "套餐剩余" : result.mainLabel);
        tvTotalBalance.setText(result.mainValue == null ? "—" : result.mainValue);
        tvCurrencySymbol.setVisibility(View.GONE);
        tvCurrency.setText(result.footer == null || result.footer.isEmpty()
                ? "套餐配额" : result.footer);
        if (cardDetails != null) cardDetails.setVisibility(View.VISIBLE);
        if (tvGrantedLabel != null) tvGrantedLabel.setText(result.subLabel == null ? "下一窗口" : result.subLabel);
        if (tvGrantedBalance != null) tvGrantedBalance.setText(result.subValue == null ? "—" : result.subValue);
        if (tvToppedLabel != null) tvToppedLabel.setText("状态");
        if (tvToppedUpBalance != null) tvToppedUpBalance.setText("套餐");
        if (cardPeak != null) cardPeak.setVisibility(View.GONE);
        lastTotal = 0;
        lastGranted = 0;
        lastTopped = 0;
    }

    private void bindRemoteNotice() {
        if (tvAppNotice == null || getActivity() == null) return;
        RemoteConfig.get(getActivity(), config -> {
            if (getActivity() == null || tvAppNotice == null) return;
            String notice = config == null ? "" : config.notice;
            if (notice == null || notice.trim().isEmpty()) {
                tvAppNotice.setVisibility(View.GONE);
                return;
            }
            tvAppNotice.setText(notice);
            tvAppNotice.setVisibility(View.VISIBLE);
            tvAppNotice.setOnClickListener(v -> {
                if (config.noticeUrl != null && !config.noticeUrl.isEmpty()) {
                    try {
                        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(config.noticeUrl)));
                    } catch (Throwable ignored) {
                    }
                }
            });
        });
    }

    private void hideError() {
        if (tvBalanceError.getVisibility() == View.VISIBLE) {
            tvBalanceError.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> tvBalanceError.setVisibility(View.GONE)).start();
        }
    }

    /** 按用户设置的币种显示换算：自动识别 / 强制 CNY / 强制 USD。近似汇率 1 USD = 7.2 CNY（离线参考）。 */
    private String currencyLabel(String currency) {
        int mode = AppPrefs.getDisplayCurrency(getActivity());
        if (mode == 1) return "CNY";
        if (mode == 2) return "USD";
        return currency;
    }

    private double[] convertBalance(ProviderConfig.BalanceResult r) {
        double total = r.totalBalance;
        double granted = r.grantedBalance;
        double topped = r.toppedUpBalance;
        int mode = AppPrefs.getDisplayCurrency(getActivity());
        // 用户自定义汇率（JSON）优先，否则离线近似 7.2
        final double rate = AppPrefs.getUsdCnyRate(getActivity());
        if (mode == 1 && "USD".equals(r.currency)) {
            // 强制 CNY：USD -> CNY
            total *= rate; granted *= rate; topped *= rate;
        } else if (mode == 2 && "CNY".equals(r.currency)) {
            // 强制 USD：CNY -> USD
            total /= rate; granted /= rate; topped /= rate;
        }
        return new double[]{total, granted, topped};
    }

    private String formatBalance(double d) {
        android.content.Context ctx = getContext();
        if (ctx != null && PrivacyPrefs.isHideBalance(ctx)) {
            return "****";
        }
        if (Double.isNaN(d) || Double.isInfinite(d)) return "0.00";
        try {
            java.math.BigDecimal value = java.math.BigDecimal.valueOf(d)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            // 欠费几分钱时，四舍五入会变成 -0.00；此时保留更多小数，显示真实欠费。
            if (value.signum() == 0 && d < 0) {
                return java.math.BigDecimal.valueOf(d)
                        .setScale(4, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString();
            }
            return value.toPlainString();
        } catch (Throwable ignored) {
            return String.format(java.util.Locale.US, "%.2f", d);
        }
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        tvBalanceError.setVisibility(View.GONE);
        cardTotal.setAlpha(0f);
        cardGranted.setAlpha(0f);
        cardTopped.setAlpha(0f);
    }

    private void hideLoading() {
        layoutLoading.setVisibility(View.GONE);
        cardTotal.setVisibility(View.VISIBLE);
        cardTotal.setAlpha(1f);
        cardTotal.setTranslationY(0f);
        cardTotal.setScaleX(1f);
        cardTotal.setScaleY(1f);
        cardGranted.setVisibility(View.VISIBLE);
        cardGranted.setAlpha(1f);
        cardGranted.setTranslationY(0f);
        cardGranted.setScaleX(1f);
        cardGranted.setScaleY(1f);
        cardTopped.setVisibility(View.VISIBLE);
        cardTopped.setAlpha(1f);
        cardTopped.setTranslationY(0f);
        cardTopped.setScaleX(1f);
        cardTopped.setScaleY(1f);
        tvRefreshLabel.setText(getString(R.string.j_106));
        btnRefresh.setAlpha(1f);
    }

    private void showError(String msg) {
        hideLoading();
        tvBalanceError.setText(msg);
        tvBalanceError.setVisibility(View.VISIBLE);
        tvBalanceError.setAlpha(0f);
        tvBalanceError.animate().alpha(1f).setDuration(300).start();
    }
}