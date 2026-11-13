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
import android.widget.Switch;
import android.widget.TextView;

/** 实验性功能开关：默认关闭，开启前统一弹出风险确认。 */
public class ExperimentalFragment extends Fragment {

    private boolean suppress;
    private View navRow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_experimental, container, false);
        View back = view.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> Navigator.up(this));

        bindSwitch(view.findViewById(R.id.sw_md3),
                AppPrefs.isExperimentalMd3(getActivity()),
                v -> AppPrefs.setExperimentalMd3(getActivity(), v));

        bindSwitch(view.findViewById(R.id.sw_emergency),
                AppPrefs.isEmergencyBackup(getActivity()),
                v -> AppPrefs.setEmergencyBackup(getActivity(), v));

        bindSwitch(view.findViewById(R.id.sw_responsive),
                AppPrefs.isResponsiveSettings(getActivity()),
                v -> AppPrefs.setResponsiveSettings(getActivity(), v));

        bindSwitch(view.findViewById(R.id.sw_back_guard),
                AppPrefs.isBackGuard(getActivity()),
                v -> AppPrefs.setBackGuard(getActivity(), v));

        navRow = view.findViewById(R.id.row_nav_position);
        updateNavRow();
        if (navRow != null) {
            navRow.setOnClickListener(v -> {
                int pos = (AppPrefs.getSettingsNavPosition(getActivity()) + 1) % 3;
                AppPrefs.setSettingsNavPosition(getActivity(), pos);
                updateNavRow();
            });
        }

        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }

    private void bindSwitch(Switch sw, boolean initial, BoolSetter setter) {
        if (sw == null) return;
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((button, checked) -> {
            if (suppress) return;
            if (!checked) {
                setter.set(false);
                if (button.getId() == R.id.sw_responsive) updateNavRow();
                return;
            }
            // 开启前先回弹，用户确认风险后再真正写入。
            suppress = true;
            button.setChecked(false);
            suppress = false;
            UiDialog.showConfirm(getActivity(),
                    getString(R.string.exp_risk_title),
                    getString(R.string.exp_risk_body),
                    getString(R.string.exp_risk_ok),
                    getString(R.string.h_004),
                    () -> {
                        setter.set(true);
                        suppress = true;
                        button.setChecked(true);
                        suppress = false;
                        if (button.getId() == R.id.sw_responsive) updateNavRow();
                    },
                    null);
        });
    }

    private void updateNavRow() {
        View row = navRow != null ? navRow : (getView() == null ? null
                : getView().findViewById(R.id.row_nav_position));
        TextView value = getView() == null ? null : getView().findViewById(R.id.tv_nav_position);
        boolean on = AppPrefs.isResponsiveSettings(getActivity());
        if (row != null) row.setVisibility(on ? View.VISIBLE : View.GONE);
        if (value != null) {
            int pos = AppPrefs.getSettingsNavPosition(getActivity());
            value.setText(pos == 1 ? getString(R.string.exp_nav_top)
                    : pos == 2 ? getString(R.string.exp_nav_right)
                    : getString(R.string.exp_nav_left));
        }
    }

    private interface BoolSetter {
        void set(boolean value);
    }
}
