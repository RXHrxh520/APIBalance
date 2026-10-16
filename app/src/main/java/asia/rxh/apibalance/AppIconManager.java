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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 应用图标管理：主界面图标 + 登录界面小头像 + 桌面图标。
 *
 * 用户可上传一张图片作为应用内图标（主界面 logo / 登录头像），可随时清除回退默认。
 * 桌面（Launcher）图标受 Android 系统限制不能运行时指向任意图片，这里通过
 * {@link #applyIconTo} 统一给各 ImageView 应用自定义图，Launcher 图标见 activity-alias。
 */
public final class AppIconManager {

    private static final String FILE_NAME = "custom_app_icon.png";

    private AppIconManager() {
    }

    private static File iconFile(Context c) {
        return new File(c.getFilesDir(), FILE_NAME);
    }

    /** 是否有用户自定义图标。 */
    public static boolean hasCustom(Context c) {
        File f = iconFile(c);
        return f.exists() && f.length() > 0;
    }

    /** 读取自定义图标位图；无则返回 null（调用方回退默认 drawable）。 */
    public static Bitmap load(Context c) {
        File f = iconFile(c);
        if (!f.exists() || f.length() == 0) return null;
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception e) {
            return null;
        }
    }

    /** 保存自定义图标（从输入流复制到应用私有目录）。 */
    public static boolean save(Context c, InputStream in) {
        FileOutputStream fos = null;
        try {
            File f = iconFile(c);
            if (f.exists()) f.delete();
            fos = new FileOutputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
            fos.flush();
            return f.length() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (fos != null) { try { fos.close(); } catch (Exception ignored) { } }
        }
    }

    /** 清除自定义图标，回退默认。 */
    public static void clear(Context c) {
        File f = iconFile(c);
        if (f.exists()) f.delete();
    }

    /** 把自定义图标应用到 ImageView；无自定义图时不做任何事（保留默认）。 */
    public static void applyIconTo(android.widget.ImageView iv, Context c) {
        if (iv == null || c == null) return;
        Bitmap bmp = load(c);
        if (bmp != null) iv.setImageBitmap(bmp);
    }
}
