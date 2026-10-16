package asia.rxh.apibalance;/*
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

/**
 * 实验性功能（v1.5 新增）：目前只有 MD3 组件一个开关。
 * 开启前必须先在风险协议上点「我已知晓」，否则不接受开启。
 */
public class ExperimentalFragment extends Fragment {

    /** 防止 setChecked 触发监听器造成递归。 */
    private boolean suppress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_experimental, container, false);
        View back = view.findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> Navigator.up(this));
        }
        final Switch sw = view.findViewById(R.id.sw_md3);
        if (sw != null) {
            sw.setChecked(AppPrefs.isExperimentalMd3(getActivity()));
            sw.setOnCheckedChangeListener((b, checked) -> {
                if (suppress) return;
                if (!checked) {
                    AppPrefs.setExperimentalMd3(getActivity(), false);
                    return;
                }
                // 开启前必须过风险协议：先回弹开关，用户点了同意再真正打开
                suppress = true;
                b.setChecked(false);
                suppress = false;
                UiDialog.showConfirm(getActivity(),
                        getString(R.string.exp_risk_title),
                        getString(R.string.exp_risk_body),
                        getString(R.string.exp_risk_ok),
                        getString(R.string.h_004),
                        () -> {
                            AppPrefs.setExperimentalMd3(getActivity(), true);
                            suppress = true;
                            sw.setChecked(true);
                            suppress = false;
                        },
                        null);
            });
        }
        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }
}
