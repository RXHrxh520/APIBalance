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
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 设置首页：默认保持原布局；实验开关开启且屏幕足够宽时使用侧边/顶部导航。 */
public class SettingsHomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!AppPrefs.isResponsiveSettings(getActivity()) || !isWideScreen()) {
            View legacy = inflater.inflate(R.layout.fragment_settings_home, container, false);
            bindContent(legacy);
            ThemeUtils.apply(legacy);
            return legacy;
        }

        int position = AppPrefs.getSettingsNavPosition(getActivity());
        LinearLayout outer = new LinearLayout(getActivity());
        outer.setOrientation(position == 1 ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        outer.setBackgroundColor(getResources().getColor(R.color.md_surface));

        FrameLayout content = new FrameLayout(getActivity());
        View contentView = inflater.inflate(R.layout.fragment_settings_home, content, false);
        content.addView(contentView);
        bindContent(contentView);

        View rail = buildRail(position == 1);
        if (position == 2) {
            outer.addView(content, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            outer.addView(rail, railParams(position));
        } else if (position == 1) {
            outer.addView(rail, railParams(position));
            outer.addView(content, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            outer.addView(rail, railParams(position));
            outer.addView(content, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        ThemeUtils.apply(outer);
        return outer;
    }

    private LinearLayout.LayoutParams railParams(int position) {
        return position == 1
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
                : new LinearLayout.LayoutParams(dp(184), ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private View buildRail(boolean top) {
        LinearLayout list = new LinearLayout(getActivity());
        list.setOrientation(top ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        list.setBackgroundColor(ThemeUtils.sectionBg(getActivity()));
        for (int i = 0; i < SettingsFragment.GROUPS.length; i++) {
            addRailButton(list, SettingsFragment.GROUPS[i],
                    SettingsCatalog.GROUP_TITLES[i], top);
        }

        if (top) {
            HorizontalScrollView scroll = new HorizontalScrollView(getActivity());
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.addView(list);
            return scroll;
        }
        ScrollView scroll = new ScrollView(getActivity());
        scroll.addView(list);
        return scroll;
    }

    private void addRailButton(LinearLayout parent, final String group, int titleRes, boolean top) {
        TextView tv = new TextView(getActivity());
        tv.setText(titleRes);
        tv.setTextSize(14);
        tv.setTextColor(ThemeUtils.textPrimary(getActivity()));
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        tv.setBackgroundResource(R.drawable.bg_ripple_8);
        tv.setOnClickListener(v -> {
            MainActivity act = (MainActivity) getActivity();
            if (act != null) act.showSettingsGroup(group);
        });
        if (top) {
            tv.setSingleLine(true);
            parent.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            parent.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private boolean isWideScreen() {
        if (getActivity() == null) return false;
        Configuration cfg = getResources().getConfiguration();
        return cfg.screenWidthDp >= 600;
    }

    private void bindContent(View view) {
        int[][] pairs = {
                {R.id.cat_general, 0}, {R.id.cat_appearance, 1}, {R.id.cat_network, 2},
                {R.id.cat_account, 3}, {R.id.cat_notify, 4}, {R.id.cat_privacy, 5},
                {R.id.cat_advanced, 6}, {R.id.cat_about, 7}};
        for (int[] p : pairs) {
            final String group = SettingsFragment.GROUPS[p[1]];
            View row = view.findViewById(p[0]);
            if (row != null) {
                row.setOnClickListener(v -> {
                    MainActivity act = (MainActivity) getActivity();
                    if (act != null) act.showSettingsGroup(group);
                });
            }
        }
        View switchRow = view.findViewById(R.id.cat_switch_ui);
        if (switchRow != null) {
            final boolean legacyNow = AppPrefs.isLegacyUi(getActivity());
            TextView tTitle = switchRow.findViewById(R.id.tv_switch_ui_title);
            TextView tDesc = switchRow.findViewById(R.id.tv_switch_ui_desc);
            if (tTitle != null) tTitle.setText(legacyNow ? R.string.legacy_to_new : R.string.legacy_to_old);
            if (tDesc != null) tDesc.setText(legacyNow ? R.string.legacy_desc : R.string.newui_desc);
            switchRow.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                AppPrefs.setLegacyUi(act, !legacyNow);
                try {
                    act.getFragmentManager().popBackStackImmediate(null,
                            android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } catch (Throwable ignored) {
                }
                act.recreate();
            });
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
