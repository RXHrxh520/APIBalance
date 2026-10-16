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

/**
 * 贡献者页面（v1.5）：先把骨架和排版搭好，数据暂不接入。
 * 列表项使用 ShimmerView 做斜向扫光，等数据源定下来直接换成真实内容。
 */
public class ContributorsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributors, container, false);
        View back = view.findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> Navigator.up(this));
        }
        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }
}
