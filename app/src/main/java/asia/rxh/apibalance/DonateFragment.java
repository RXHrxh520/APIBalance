package asia.rxh.apibalance;/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 捐赠页面（v1.5）：中间放一个方形占位框，二维码数据后续再补。
 * 骨架先定下来，避免以后再动布局。
 */
public class DonateFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donate, container, false);
        View back = view.findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(v -> Navigator.up(this));
        }
        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }
}
