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

/** 设置首页：模仿微信的大分类入口，点开进入对应的小分类设置。 */
public class SettingsHomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_home, container, false);
        int[][] pairs = {
                {R.id.cat_general, 0}, {R.id.cat_privacy, 1}, {R.id.cat_network, 2},
                {R.id.cat_appearance, 3}, {R.id.cat_account, 4}, {R.id.cat_notify, 5},
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
        // 新旧版切换：文案要跟着当前版本走，原来这行根本没接逻辑，永远显示「切换到新版」
        View switchRow = view.findViewById(R.id.cat_switch_ui);
        if (switchRow != null) {
            final boolean legacyNow = AppPrefs.isLegacyUi(getActivity());
            android.widget.TextView tTitle = switchRow.findViewById(R.id.tv_switch_ui_title);
            android.widget.TextView tDesc = switchRow.findViewById(R.id.tv_switch_ui_desc);
            if (tTitle != null) {
                tTitle.setText(legacyNow ? R.string.legacy_to_new : R.string.legacy_to_old);
            }
            if (tDesc != null) {
                tDesc.setText(legacyNow ? R.string.legacy_desc : R.string.newui_desc);
            }
            switchRow.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act == null) return;
                AppPrefs.setLegacyUi(act, !legacyNow);
                // 切换前把回退栈清空，否则旧版残留的页面会和新版叠在一起
                try {
                    act.getFragmentManager().popBackStackImmediate(null,
                            android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } catch (Throwable ignored) {
                }
                act.recreate();
            });
        }

        // v1.5：设置首页本身就是根页面，不再保留左上角返回，避免和主界面叠在一起
        ThemeUtils.apply(view);
        return view;
    }
}
