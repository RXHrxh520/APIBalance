package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

public class AboutFragment extends Fragment {

    private static final String PREFS_DEBUG = "debug_prefs";
    private static final String KEY_DEBUG = "debug_enabled";

    private int clickCount = 0;
    private long firstClick = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvVersion = view.findViewById(R.id.tv_version);
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);

        tvVersion.setOnClickListener(v -> {
            // Press animation
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(new OvershootInterpolator(0.5f))
                            .start())
                    .start();

            long now = System.currentTimeMillis();
            if (now - firstClick > 3000) {
                clickCount = 0;
                firstClick = now;
            }
            clickCount++;
            if (clickCount >= 8) {
                clickCount = 0;
                SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_DEBUG, 0);
                prefs.edit().putBoolean(KEY_DEBUG, true).apply();
                Toast.makeText(getActivity(), getString(R.string.j_147), Toast.LENGTH_SHORT).show();
                // Go back to settings so user can see the debug entry
                getFragmentManager().popBackStack();
            } else if (clickCount >= 5) {
                Toast.makeText(getActivity(), getString(R.string.j_148) + (8 - clickCount) + getString(R.string.j_149), Toast.LENGTH_SHORT).show();
            }
        });

        TextView tvRepo = view.findViewById(R.id.tv_repo);
        tvRepo.setText("GitHub");
        tvRepo.setTextColor(0xFF6750A4);
        tvRepo.setClickable(true);
        tvRepo.setFocusable(true);
        tvRepo.setOnClickListener(v2 -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/RXHrxh520/APIBalance")));
            } catch (Exception e) { /* no browser */ }
        });


        TextView tvAgreement = view.findViewById(R.id.tv_agreement);
        if (tvAgreement != null) {
            tvAgreement.setOnClickListener(v -> UiDialog.showInfo(getActivity(),
                    getString(R.string.about_policy_user), getString(R.string.agreement_user)));
        }
        TextView tvPrivacy = view.findViewById(R.id.tv_privacy);
        if (tvPrivacy != null) {
            tvPrivacy.setOnClickListener(v -> UiDialog.showInfo(getActivity(),
                    getString(R.string.about_policy_privacy), getString(R.string.agreement_privacy)));
        }

        // 内容入场动画：淡入 + 上滑
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(getActivity(), android.R.interpolator.decelerate_cubic))
                .start();

        ThemeUtils.apply(view);
        return view;
    }
}