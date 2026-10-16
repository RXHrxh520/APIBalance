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
import android.widget.TextView;

/**
 * 「我的」页面：顶部个人信息，下方捐赠 / 设置 / 贡献者 / 开发人员 / 检查更新 / 构建信息。
 * 头像在用户未设置时使用损坏图标占位，昵称未设置时显示 NULL。
 */
public class MineFragment extends Fragment {

    private int eggTaps;
    private int avatarId = R.id.iv_mine_avatar;
    private long lastEggTap;

    private TextView tvName;
    private TextView tvSub;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        tvName = view.findViewById(R.id.tv_mine_name);
        tvSub = view.findViewById(R.id.tv_mine_sub);
        TextView tvBuildTime = view.findViewById(R.id.tv_build_time);
        TextView tvBuildVersion = view.findViewById(R.id.tv_build_version);
        refreshAccount();

        if (tvBuildTime != null) {
            tvBuildTime.setText(realBuildTime());
        }
        if (tvBuildVersion != null) {
            tvBuildVersion.setText("v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        }

        View cardUser = view.findViewById(R.id.card_user);
        if (cardUser != null) {
            cardUser.setOnClickListener(v -> editNickname());
        }
        View rowDonate = view.findViewById(R.id.row_donate);
        if (rowDonate != null) {
            rowDonate.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showDonateFragment();
            });
        }
        View rowSettings = view.findViewById(R.id.row_settings);
        if (rowSettings != null) {
            rowSettings.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showSettingsHome();
            });
        }
        View rowContrib = view.findViewById(R.id.row_contributors);
        if (rowContrib != null) {
            rowContrib.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showContributorsFragment();
            });
        }
        View rowDev = view.findViewById(R.id.row_developers);
        if (rowDev != null) {
            rowDev.setOnClickListener(v -> {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.showDevelopersFragment();
            });
        }
        View rowUpdate = view.findViewById(R.id.row_update);
        if (rowUpdate != null) {
            rowUpdate.setOnClickListener(v -> {
                if (getActivity() != null) UpdateChecker.check(getActivity(), true);
            });
        }
        // 连续点击 DeepSeek 小图标 5 次触发彩蛋
        View ds = view.findViewById(R.id.iv_deepseek);
        // 图标在代码里加载：万一矢量图解析失败也只回退图标，绝不让页面崩掉
        try {
            ((android.widget.ImageView) ds).setImageResource(R.drawable.ic_deepseek_fish);
        } catch (Throwable t) {
            try {
                ((android.widget.ImageView) ds).setImageResource(R.drawable.ic_ds_logo);
            } catch (Throwable ignored) {
            }
        }
        // 彩蛋挂在那条黑色 DeepSeek 小鱼上（头像那一条），不是整张卡片
        View eggRow = view.findViewById(R.id.egg_row);
        View eggBox = ds;
        android.view.View.OnClickListener eggTap = v -> {
                long now = System.currentTimeMillis();
                if (now - lastEggTap > 1500L) eggTaps = 0;
                lastEggTap = now;
                eggTaps++;
                if (eggTaps >= 3) {
                    eggTaps = 0;
                    EggChat.show(getActivity());
                }
        };
        if (eggBox != null) eggBox.setOnClickListener(eggTap);
        if (eggRow != null) eggRow.setOnClickListener(eggTap);
        // 点头像换照片，点名字改昵称，两者互不抢
        View avatarView = view.findViewById(avatarId);
        if (avatarView != null) {
            avatarView.setOnClickListener(v -> pickAvatar(avatarId));
        }
        // 新版「我的」来点颜色：每行图标一个色
        final int[] PALETTE = {0xFF6750A4, 0xFF0EA5E9, 0xFF10B981, 0xFFF59E0B,
                0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899};
        final int[] ROWS = {R.id.row_donate, R.id.row_settings, R.id.row_contributors,
                R.id.row_developers, R.id.row_update, R.id.row_build_time, R.id.row_build_version};
        for (int i = 0; i < ROWS.length; i++) {
            View r = view.findViewById(ROWS[i]);
            if (!(r instanceof android.view.ViewGroup)) continue;
            android.view.ViewGroup g = (android.view.ViewGroup) r;
            for (int k = 0; k < g.getChildCount(); k++) {
                View ch = g.getChildAt(k);
                if (ch instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) ch).setColorFilter(PALETTE[i % PALETTE.length]);
                    break;
                }
            }
        }
        ThemeUtils.apply(view);
        return view;
    }

    private static final int REQ_AVATAR = 8201;

    private void pickAvatar(int viewId) {
        try {
            android.content.Intent it = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
            it.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            it.setType("image/*");
            startActivityForResult(it, REQ_AVATAR);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_AVATAR || res != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        try {
            getActivity().getContentResolver().takePersistableUriPermission(data.getData(),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        AppPrefs.setAvatarUri(getActivity(), data.getData().toString());
        applyAvatar();
        android.widget.Toast.makeText(getActivity(), getString(R.string.mine_avatar_done),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void applyAvatar() {
        if (getView() == null || avatarId == 0) return;
        View v = getView().findViewById(avatarId);
        if (!(v instanceof android.widget.ImageView)) return;
        String uri = AppPrefs.getAvatarUri(getActivity());
        if (uri == null || uri.isEmpty()) return;
        try {
            java.io.InputStream in = getActivity().getContentResolver().openInputStream(android.net.Uri.parse(uri));
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(in);
            if (in != null) in.close();
            if (bm != null) {
                ((android.widget.ImageView) v).setImageBitmap(bm);
                ((android.widget.ImageView) v).setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 真实构建时间：取已安装 APK 的文件时间，比配置期写入的准。 */
    private String realBuildTime() {
        try {
            String path = null;
            if (getActivity() != null) {
                path = getActivity().getPackageCodePath();
                if (path == null) path = getActivity().getApplicationInfo().sourceDir;
            }
            if (path != null) {
                long t = new java.io.File(path).lastModified();
                if (t > 0) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()).format(new java.util.Date(t));
                }
            }
        } catch (Throwable ignored) {
        }
        return BuildConfig.BUILD_TIME;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccount();
        applyAvatar();
    }

    /** 昵称为空时显示 NULL，符合「用户自己设的账户，没有就用默认 NULL」。 */
    private void refreshAccount() {
        if (tvName == null || getActivity() == null) return;
        String nick = AppPrefs.getNickname(getActivity());
        if (nick == null || nick.trim().isEmpty()) {
            tvName.setText(getString(R.string.mine_account_null));
            if (tvSub != null) tvSub.setText(getString(R.string.mine_account_hint));
        } else {
            tvName.setText(nick);
            if (tvSub != null) tvSub.setText(getString(R.string.mine_account_edit));
        }
    }

    private void editNickname() {
        if (getActivity() == null) return;
        UiDialog.showInputWithDesc(getActivity(), getString(R.string.mine_account_hint),
                getString(R.string.mine_account_desc), getString(R.string.mine_account_null),
                AppPrefs.getNickname(getActivity()), value -> {
                    AppPrefs.setNickname(getActivity(), value == null ? "" : value.trim());
                    refreshAccount();
                });
    }
}
