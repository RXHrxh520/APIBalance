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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

/**
 * 统一返回逻辑（v1.5）：先回上一级，栈空回主页，最后才退出。
 * 修掉「点返回直接退回桌面」和「和主界面卡在一起」的问题。
 */
public final class Navigator {

    private Navigator() {
    }

    /** Fragment 内的返回按钮统一走这里。 */
    public static void up(Fragment f) {
        if (f == null) return;
        Activity act = f.getActivity();
        if (act == null) return;
        FragmentManager fm = f.getFragmentManager();
        if (fm != null && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return;
        }
        if (act instanceof MainActivity) {
            ((MainActivity) act).switchTab(0);
            return;
        }
        act.finish();
    }
}
