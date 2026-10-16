package asia.rxh.apibalance;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 统一的「保存到下载目录」工具。
 * 走 MediaStore，失败再退回公共下载目录，让用户真的能在文件管理器里找到。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public final class Exporter {

    private Exporter() {
    }

    /** 保存字节内容，返回给用户看的位置描述；失败抛异常。 */
    public static String saveToDownloads(Context ctx, byte[] data, String name, String mime) throws Throwable {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri != null) {
                java.io.OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(data);
                    os.flush();
                    os.close();
                    return "Download/" + name;
                }
            }
        }
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) throw new java.io.IOException("no downloads dir");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("mkdir failed");
        File f = new File(dir, name);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.flush();
        fos.close();
        return f.getAbsolutePath();
    }

    /** 保存文本。 */
    public static String saveText(Context ctx, String text, String name, String mime) throws Throwable {
        return saveToDownloads(ctx, text.getBytes("UTF-8"), name, mime);
    }
}

