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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 余额分享页：生成品牌格式的余额卡片图（非截图），支持分享到微信/QQ 或保存相册。
 * Android 10+ (API 29) 通过 MediaStore 保存/分享图片；低版本降级为文字分享。
 */
public class ShareFragment extends Fragment {

    private Bitmap cardBitmap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);

        ImageView btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null && getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.switchTab(0);
            }
        });

        ImageView ivPreview = view.findViewById(R.id.iv_preview);
        TextView btnShare = view.findViewById(R.id.btn_share);
        TextView btnSave = view.findViewById(R.id.btn_save);

        // 用最近一条余额历史生成分享图
        cardBitmap = buildCard();
        if (cardBitmap != null) {
            ivPreview.setImageBitmap(cardBitmap);
        }

        // v1.5：长按预览图选择卡片配色
        ivPreview.setOnLongClickListener(v -> {
            showCardStylePicker();
            return true;
        });

        btnShare.setOnClickListener(v -> shareImage());
        btnShare.setOnLongClickListener(v -> {
            showTemplatePicker();
            return true;
        });
        btnSave.setOnClickListener(v -> saveToGallery());

        ThemeUtils.apply(view);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cardBitmap != null && !cardBitmap.isRecycled()) {
            cardBitmap.recycle();
            cardBitmap = null;
        }
    }

    /** 用 Canvas 绘制品牌格式余额卡（1080x1440）。 */
    private Bitmap buildCard() {
        Context ctx = getActivity();
        if (ctx == null) return null;
        int W = 1080, H = 1440;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        // v1.5：卡片配色由用户在预览图上长按选择（默认 / 粉色 / 极简白 / 跟随主题）
        final int cardStyle = AppPrefs.getShareCardStyle(ctx);
        final int cTop, cBottom, cTitle, cMain, cSub, cLine;
        if (cardStyle == 1) {
            cTop = 0xFFFFE3F1; cBottom = 0xFFFFC2DE; cTitle = 0xFFD81B60;
            cMain = 0xFF4A1F33; cSub = 0xFF8A5A72; cLine = 0x33D81B60;
        } else if (cardStyle == 2) {
            cTop = 0xFFFFFFFF; cBottom = 0xFFF2F3F5; cTitle = 0xFF111111;
            cMain = 0xFF111111; cSub = 0xFF6B7280; cLine = 0x22000000;
        } else if (cardStyle == 3) {
            int acc = ThemePrefs.getColor(ctx);
            cTop = 0xFF101828; cBottom = acc; cTitle = 0xFFFFFFFF;
            cMain = 0xFFFFFFFF; cSub = 0xFFC7CDD8; cLine = 0x33FFFFFF;
        } else {
            cTop = 0xFF101828; cBottom = 0xFF1E1B4B; cTitle = 0xFF00E676;
            cMain = 0xFFFFFFFF; cSub = 0xFF8B949E; cLine = 0xFF2A3550;
        }

        Paint bg = new Paint();
        android.graphics.LinearGradient lg = new android.graphics.LinearGradient(
                0, 0, 0, H, cTop, cBottom, android.graphics.Shader.TileMode.CLAMP);
        bg.setShader(lg);
        c.drawRect(0, 0, W, H, bg);

        // 品牌标题
        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(cTitle);
        title.setTextSize(44);
        title.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        c.drawText("APIBalance", 80, 120, title);

        Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        sub.setColor(cSub);
        sub.setTextSize(30);
        c.drawText("Multi-Provider API Balance", 80, 170, sub);

        // 服务商
        MainActivity act = (MainActivity) getActivity();
        int pid = act != null ? act.getCurrentProviderId() : ProviderConfig.PROVIDER_DEEPSEEK;
        ProviderConfig.Provider provider = ProviderConfig.get(pid);

        Paint providerName = new Paint(Paint.ANTI_ALIAS_FLAG);
        providerName.setColor(cMain);
        providerName.setTextSize(56);
        providerName.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        c.drawText(provider.name, 80, 340, providerName);

        // 余额大字
        List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(ctx);
        double total = 0;
        double granted = 0;
        double topped = 0;
        String currency = "CNY";
        BalanceHistoryStore.HistoryItem last = null;
        if (!items.isEmpty()) {
            last = items.get(items.size() - 1);
            total = last.total;
            granted = last.granted;
            topped = last.topped;
            currency = last.currency;
        }
        String symbol = "CNY".equals(currency) ? "¥" : "$";

        Paint balance = new Paint(Paint.ANTI_ALIAS_FLAG);
        balance.setColor(cMain);
        balance.setTextSize(150);
        balance.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String balanceText = symbol + String.format("%.2f", total);
        c.drawText(balanceText, 80, 560, balance);

        Paint currencyTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        currencyTxt.setColor(cSub);
        currencyTxt.setTextSize(36);
        c.drawText(currency + " · " + provider.name + getString(R.string.j_104), 80, 640, currencyTxt);

        // 分隔线
        Paint line = new Paint();
        line.setColor(cLine);
        line.setStrokeWidth(4);
        c.drawLine(80, 720, W - 80, 720, line);

        // 明细两列
        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(cSub);
        label.setTextSize(34);
        c.drawText(getString(R.string.j_160), 80, 820, label);
        c.drawText(getString(R.string.j_161), 580, 820, label);

        Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
        value.setColor(cMain);
        value.setTextSize(48);
        value.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        c.drawText(symbol + String.format("%.2f", granted), 80, 890, value);
        c.drawText(symbol + String.format("%.2f", topped), 580, 890, value);

        // 时间戳
        Paint time = new Paint(Paint.ANTI_ALIAS_FLAG);
        time.setColor(cSub);
        time.setTextSize(30);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        long ts = (last != null) ? last.ts : System.currentTimeMillis();
        c.drawText(getString(R.string.j_162) + sdf.format(new Date(ts)), 80, 1060, time);

        // 品牌水印
        Paint watermark = new Paint(Paint.ANTI_ALIAS_FLAG);
        watermark.setColor(0xFF3A4660);
        watermark.setTextSize(28);
        c.drawText(getString(R.string.j_163), 80, H - 90, watermark);

        return bmp;
    }


    /** 内置 10 套分享文案，最后一项是自定义。%1$s=服务商  %2$s=余额 */
    public static final String[] TEMPLATES = {
            "我用 APIBalance 看余额，%1$s 还剩 %2$s",
            "余额播报 · %1$s：%2$s",
            "%1$s 余额查询结果：%2$s，快去看看吧",
            "刚用 APIBalance 查了下，%1$s 余额 %2$s",
            "%1$s 余额：%2$s  —— 来自 APIBalance",
            "又到看余额的时间了，%1$s：%2$s",
            "APIBalance 播报：%1$s %2$s",
            "余额提醒：%1$s 现在 %2$s",
            "悄悄告诉你，%1$s 余额是 %2$s",
            "%1$s 余额 %2$s，APIBalance 一键查询"};
    private static final String PLACEHOLDER_PROVIDER = "[服务商]";
    private static final String PLACEHOLDER_BALANCE = "[余额]";

    private String templatePreview(int idx) {
        if (idx == TEMPLATES.length) {
            String c = AppPrefs.getShareTplCustom(getActivity());
            return (c == null || c.isEmpty()) ? getString(R.string.share_tpl_custom) : c;
        }
        return TEMPLATES[idx].replace("%1$s", PLACEHOLDER_PROVIDER).replace("%2$s", PLACEHOLDER_BALANCE);
    }

    private String buildShareText(String provider, double total) {
        Context ctx = getActivity();
        if (ctx == null) return provider + " " + String.format(java.util.Locale.getDefault(), "%.2f", total);
        int idx = AppPrefs.getShareTpl(ctx);
        String tpl;
        if (idx == TEMPLATES.length) {
            tpl = AppPrefs.getShareTplCustom(ctx);
            if (tpl == null || tpl.isEmpty()) tpl = TEMPLATES[0];
        } else {
            if (idx < 0 || idx >= TEMPLATES.length) idx = 0;
            tpl = TEMPLATES[idx];
        }
        try {
            return String.format(java.util.Locale.getDefault(), tpl, provider,
                    String.format(java.util.Locale.getDefault(), "%.2f", total));
        } catch (Throwable t) {
            return tpl;
        }
    }

    /** 长按分享按钮：挑一套文案，或者自己写。 */
    private void showTemplatePicker() {
        final Context ctx = getActivity();
        if (ctx == null) return;
        String[] items = new String[TEMPLATES.length + 1];
        for (int i = 0; i < TEMPLATES.length; i++) items[i] = templatePreview(i);
        items[TEMPLATES.length] = getString(R.string.share_tpl_custom);
        UiDialog.showPicker(ctx, getString(R.string.share_tpl_title), items,
                Math.min(AppPrefs.getShareTpl(ctx), TEMPLATES.length), idx -> {
                    if (idx == TEMPLATES.length) {
                        UiDialog.showInputWithDesc(ctx, getString(R.string.share_tpl_custom),
                                getString(R.string.share_tpl_hint),
                                getString(R.string.share_tpl_hint), AppPrefs.getShareTplCustom(ctx),
                                text -> {
                                    AppPrefs.setShareTplCustom(ctx, text);
                                    AppPrefs.setShareTpl(ctx, TEMPLATES.length);
                                    Toast.makeText(ctx, getString(R.string.share_tpl_saved),
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        AppPrefs.setShareTpl(ctx, idx);
                        Toast.makeText(ctx, getString(R.string.share_tpl_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private double lastTotal() {
        try {
            List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(getActivity());
            if (!items.isEmpty()) return items.get(items.size() - 1).total;
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** 分享卡片配色选择（v1.5 新增自定义项）。 */
    private void showCardStylePicker() {
        final android.content.Context ctx = getActivity();
        if (ctx == null) return;
        final String[] items = {
                getString(R.string.share_style_default),
                getString(R.string.share_style_pink),
                getString(R.string.share_style_minimal),
                getString(R.string.share_style_theme)};
        UiDialog.showPicker(ctx, getString(R.string.share_style_title), items,
                AppPrefs.getShareCardStyle(ctx), which -> {
                    AppPrefs.setShareCardStyle(ctx, which);
                    if (cardBitmap != null && !cardBitmap.isRecycled()) cardBitmap.recycle();
                    cardBitmap = buildCard();
                    ImageView iv = getView() == null ? null
                            : (ImageView) getView().findViewById(R.id.iv_preview);
                    if (iv != null && cardBitmap != null) iv.setImageBitmap(cardBitmap);
                    android.widget.Toast.makeText(ctx, getString(R.string.share_style_saved),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void shareImage() {
        Context ctx = getActivity();
        if (ctx == null || cardBitmap == null) return;
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                Uri uri = saveToMediaStore(ctx, cardBitmap, "APIBalance_share_" + System.currentTimeMillis() + ".png");
                if (uri == null) {
                    Toast.makeText(ctx, getString(R.string.j_164), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.setClipData(android.content.ClipData.newRawUri("APIBalance", uri));
                share.putExtra(Intent.EXTRA_TEXT, buildShareText(ProviderConfig.get(currentPid()).name, lastTotal()));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, getString(R.string.j_167)));
            } catch (Exception e) {
                Toast.makeText(ctx, getString(R.string.j_168) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Android 9-：降级为文字分享
            MainActivity act = (MainActivity) getActivity();
            double total = 0;
            List<BalanceHistoryStore.HistoryItem> items = BalanceHistoryStore.getAll(ctx);
            if (!items.isEmpty()) {
                total = items.get(items.size() - 1).total;
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, buildShareText(ProviderConfig.get(currentPid()).name, total));
            startActivity(Intent.createChooser(share, getString(R.string.j_172)));
        }
    }

    private int currentPid() {
        MainActivity act = (MainActivity) getActivity();
        return act != null ? act.getCurrentProviderId() : ProviderConfig.PROVIDER_DEEPSEEK;
    }

    private void saveToGallery() {
        Context ctx = getActivity();
        if (ctx == null || cardBitmap == null) return;
        if (Build.VERSION.SDK_INT >= 29) {
            Uri uri = saveToMediaStore(ctx, cardBitmap, "APIBalance_" + System.currentTimeMillis() + ".png");
            Toast.makeText(ctx, uri != null ? getString(R.string.j_173) : getString(R.string.j_174), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ctx, getString(R.string.j_175), Toast.LENGTH_SHORT).show();
        }
    }

    /** API 29+：MediaStore 保存图片（无需存储权限）。 */
    private Uri saveToMediaStore(Context ctx, Bitmap bmp, String name) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= 29) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/APIBalance");
            }
            Uri uri = ctx.getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            java.io.OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            if (os == null) {
                ctx.getContentResolver().delete(uri, null, null);
                return null;
            }
            try {
                if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                    ctx.getContentResolver().delete(uri, null, null);
                    return null;
                }
            } finally {
                try { os.close(); } catch (Exception ignored) { }
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }
}
