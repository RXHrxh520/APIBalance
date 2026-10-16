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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * 开发人员页面：头像取自设备上的 RXH.jpg，背景板取自 RXH520.jpg。
 * 文件不存在或没有读取权限时自动回退到内置占位图，不会因此崩溃。
 */
public class DevelopersFragment extends Fragment {

    private static final String AVATAR_PATH = "/storage/emulated/0/RxhAndroid/RXH666/RXH.jpg";
    private static final String BG_PATH = "/storage/emulated/0/RxhAndroid/RXH666/RXH520.jpg";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_developer, container, false);
        ImageView avatar = view.findViewById(R.id.iv_dev_avatar);
        ImageView bg = view.findViewById(R.id.iv_dev_bg);
        // 不再申请文件权限，读不到就让用户点一下自己选
        loadDeveloperAvatar(avatar);
        loadInto(bg, BG_PATH);
        bindBack(view);
        bindLinks(view);
        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }

    private static final String GH_USER = "RXHrxh520";
    private static final String GH_AVATAR_URL = "https://github.com/" + GH_USER + ".png";

    /**
     * 开发者头像（v1.5 起改为从 GitHub 拉取）：
     * 优先用户自选图 → 本地 GitHub 头像缓存 → 内置占位图；
     * 同时在后台线程刷新 GitHub 上的最新头像，全程不占用主线程、不影响启动。
     */
    private void loadDeveloperAvatar(final ImageView iv) {
        if (iv == null || getActivity() == null) return;
        String picked = AppPrefs.getDevImage(getActivity(), "avatar");
        boolean userPicked = picked != null && !picked.isEmpty();
        if (userPicked) {
            loadInto(iv, AVATAR_PATH, "avatar");
            return;
        }
        final java.io.File cache;
        try {
            cache = ghAvatarFile();
        } catch (Throwable t) {
            loadInto(iv, AVATAR_PATH, "avatar");
            return;
        }
        boolean shown = false;
        try {
            if (cache.exists() && cache.length() > 0) {
                android.graphics.Bitmap bm = BitmapFactory.decodeFile(cache.getAbsolutePath());
                if (bm != null) {
                    iv.setImageBitmap(bm);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    shown = true;
                }
            }
        } catch (Throwable ignored) {
        }
        if (!shown) loadInto(iv, AVATAR_PATH, "avatar");

        final android.app.Activity act = getActivity();
        AppExecutors.io().execute(() -> {
            final android.graphics.Bitmap bm;
            try {
                bm = downloadGithubAvatar(cache);
            } catch (Throwable t) {
                return;
            }
            if (bm == null) return;
            act.runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                ImageView target = (ImageView) getView().findViewById(R.id.iv_dev_avatar);
                if (target != null) {
                    target.setImageBitmap(bm);
                    target.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            });
        });
    }

    private java.io.File ghAvatarFile() {
        return new java.io.File(getActivity().getCacheDir(), "gh_avatar_" + GH_USER + ".png");
    }

    /** 从 GitHub 头像地址下载并写入缓存；任何失败都返回 null，不抛出。 */
    private static android.graphics.Bitmap downloadGithubAvatar(java.io.File cache) {
        java.io.File tmp = new java.io.File(cache.getAbsolutePath() + ".tmp");
        try (okhttp3.Response resp = NetworkClient.http().newCall(
                new okhttp3.Request.Builder()
                        .url(GH_AVATAR_URL)
                        .header("User-Agent", "APIBalance")
                        .build()).execute()) {
            if (resp.code() != 200 || resp.body() == null) return null;
            try (java.io.InputStream in = resp.body().byteStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
                fos.flush();
            }
            if (tmp.length() <= 0) {
                tmp.delete();
                return null;
            }
            if (cache.exists() && !cache.delete()) {
                tmp.delete();
                return null;
            }
            if (!tmp.renameTo(cache)) {
                tmp.delete();
                return null;
            }
            return BitmapFactory.decodeFile(cache.getAbsolutePath());
        } catch (Throwable t) {
            if (tmp.exists()) tmp.delete();
            return null;
        }
    }

    /** 只在文件可读时替换图片，任何异常都静默回退到占位图。 */
    private void loadInto(final ImageView iv, final String path) {
        loadInto(iv, path, null);
    }

    private void loadInto(final ImageView iv, final String path, final String key) {
        if (iv == null) return;
        // 优先用用户自己选过的图（SAF 授权，永久有效，不需要任何权限）
        if (key != null && getActivity() != null) {
            String uri = AppPrefs.getDevImage(getActivity(), key);
            if (uri != null && !uri.isEmpty()) {
                try {
                    java.io.InputStream in = getActivity().getContentResolver().openInputStream(android.net.Uri.parse(uri));
                    android.graphics.Bitmap bm = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                    if (bm != null) {
                        iv.setImageBitmap(bm);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        // 第一步：直接按路径读（有存储权限时最快）
        try {
            java.io.File f = new java.io.File(path);
            if (f.exists() && f.canRead()) {
                android.graphics.Bitmap bm = BitmapFactory.decodeFile(path);
                if (bm != null) {
                    iv.setImageBitmap(bm);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        // 第二步：交给系统相册索引去找同名文件（Android 13+ 不用申请所有文件权限）
        try {
            android.app.Activity act = getActivity();
            if (act == null) return;
            String name = new java.io.File(path).getName();
            android.net.Uri col = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            android.database.Cursor c = act.getContentResolver().query(col,
                    new String[]{android.provider.MediaStore.Images.Media._ID},
                    android.provider.MediaStore.Images.Media.DISPLAY_NAME + "=?",
                    new String[]{name}, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    android.net.Uri img = android.content.ContentUris.withAppendedId(col, c.getLong(0));
                    java.io.InputStream is = act.getContentResolver().openInputStream(img);
                    android.graphics.Bitmap bm = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    if (bm != null) {
                        iv.setImageBitmap(bm);
                        c.close();
                        return;
                    }
                }
                c.close();
            }
        } catch (Throwable ignored) {
        }
        // 都读不到：点一下自己选一张，全程不申请任何权限
        if (key != null) {
            iv.setOnClickListener(v -> pickImage("avatar".equals(key) ? 1 : 2));
        }
    }

    private static final int REQ_PICK_AVATAR = 8301;
    private static final int REQ_PICK_BG = 8302;
    private int picking = 0;

    /** 不申请任何权限，让用户自己选一次图，之后一直有授权。 */
    private void pickImage(int which) {
        picking = which;
        try {
            android.content.Intent it = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
            it.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            it.setType("image/*");
            startActivityForResult(it, which == 1 ? REQ_PICK_AVATAR : REQ_PICK_BG);
        } catch (Throwable ignored) {
        }
    }

    /** v1.5：相关链接区（GitHub 主页 / 仓库 / QQ 群）。 */
    private void bindLinks(View v) {
        bindUrl(v.findViewById(R.id.row_github), "https://github.com/" + GH_USER);
        bindUrl(v.findViewById(R.id.row_repo), "https://github.com/" + GH_USER + "/APIBalance");
        android.widget.TextView from = v.findViewById(R.id.tv_dev_avatar_from);
        if (from != null) {
            from.setText(getString(R.string.dev_avatar_from));
        }
    }

    private void bindUrl(View row, final String url) {
        if (row == null) return;
        row.setOnClickListener(x -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)));
            } catch (Throwable ignored) {
            }
        });
    }

    private void bindBack(View v) {
        View b = v.findViewById(R.id.btn_dev_back);
        if (b != null) b.setOnClickListener(x -> Navigator.up(this));
    }

    @Override
    public void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (res != android.app.Activity.RESULT_OK || data == null || data.getData() == null) return;
        if (req != REQ_PICK_AVATAR && req != REQ_PICK_BG) return;
        try {
            getActivity().getContentResolver().takePersistableUriPermission(data.getData(),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        AppPrefs.setDevImage(getActivity(), req == REQ_PICK_AVATAR ? "avatar" : "bg", data.getData().toString());
        if (getView() != null) {
            loadInto((android.widget.ImageView) getView().findViewById(R.id.iv_dev_avatar), AVATAR_PATH, "avatar");
            loadInto((android.widget.ImageView) getView().findViewById(R.id.iv_dev_bg), BG_PATH, "bg");
        }
    }

    private void askPermission() {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            String perm = sdk >= 33
                    ? android.Manifest.permission.READ_MEDIA_IMAGES
                    : android.Manifest.permission.READ_EXTERNAL_STORAGE;
            if (getActivity() == null) return;
            if (getActivity().checkSelfPermission(perm)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{perm}, 9001);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 9001 && getView() != null) {
            loadInto((ImageView) getView().findViewById(R.id.iv_dev_avatar), AVATAR_PATH, "avatar");
            loadInto((ImageView) getView().findViewById(R.id.iv_dev_bg), BG_PATH, "bg");
        }
    }
}

