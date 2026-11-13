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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** 悬浮窗背景选择：只替换背景图，余额数字和按钮仍由悬浮窗布局显示。 */
public class FloatBgPickerActivity extends Activity {

    private static final int REQ_PICK = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) showOptions();
    }

    private void showOptions() {
        String[] items = {getString(R.string.float_bg_pick), getString(R.string.float_bg_reset)};
        UiDialog.showPicker(this, getString(R.string.float_bg_title), items, 0, index -> {
            if (index == 0) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try {
                    startActivityForResult(i, REQ_PICK);
                } catch (Throwable t) {
                    finish();
                }
            } else {
                AppPrefs.setFloatBgUri(this, "");
                notifyService();
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finish();
            return;
        }
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
        }
        AppPrefs.setFloatBgUri(this, uri.toString());
        Toast.makeText(this, getString(R.string.float_bg_saved), Toast.LENGTH_SHORT).show();
        notifyService();
        finish();
    }

    private void notifyService() {
        try {
            Intent i = new Intent(this, FloatingWindowService.class);
            i.setAction("float_bg");
            startService(i);
        } catch (Throwable ignored) {
        }
    }
}
