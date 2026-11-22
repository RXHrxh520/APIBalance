package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 把用户选中的图片复制进应用私有目录。
 *
 * 为什么不用 content:// 直接长期保存：国产 ROM / 魔改系统的 DocumentsUI 经常不兑现
 * 持久化 URI 授权，或进程重启后授权丢失，表现为“选完图没反应 / 背景变回默认”。
 * 复制成自己的文件后只依赖本地路径，和系统权限彻底解耦。
 */
public final class ImageStore {

    private ImageStore() {
    }

    /**
     * 导入图片并压缩到长边不超过 maxDim 的 JPEG。
     *
     * @return 应用私有目录里的绝对路径
     */
    public static String importImage(Context ctx, Uri uri, String baseName, int maxDim) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(ctx, uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new java.io.IOException("unsupported image");
        }

        int sample = 1;
        int longSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (longSide / (sample * 2) > maxDim) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = open(ctx, uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new java.io.IOException("decode failed");

        // 二次缩放到精确长边，避免小图被放大
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int target = Math.max(w, h);
        if (target > maxDim) {
            float scale = maxDim / (float) target;
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)), true);
            if (scaled != bitmap) {
                bitmap.recycle();
                bitmap = scaled;
            }
        }

        File dir = new File(ctx.getFilesDir(), "images");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("mkdir failed");
        File out = new File(dir, baseName + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos);
        } finally {
            bitmap.recycle();
        }
        return out.getAbsolutePath();
    }

    /** 打开 content:// 或本地文件路径。 */
    public static InputStream open(Context ctx, Uri uri) throws Exception {
        String scheme = uri.getScheme();
        if (scheme == null || "file".equals(scheme)) {
            return new FileInputStream(uri.getPath());
        }
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) throw new java.io.IOException("open failed");
        return in;
    }

    /** 把 http(s)/应用内路径统一解析成本地文件路径；不是本地文件返回 null。 */
    public static String localPathOf(String stored) {
        if (stored == null) return null;
        String s = stored.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("file://")) {
            try {
                return Uri.parse(s).getPath();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return s.startsWith("/") ? s : null;
    }
}
