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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * 第三方依赖统计页：如实列出本项目使用的所有技术栈/依赖/许可。
 */
public class DependenciesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dependencies, container, false);

        ImageViewBtnBack(view);

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.f_001));
        sb.append("• v" + BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")\n\n");
        // ===== 构建工具链 =====
        sb.append(getString(R.string.dep_section_build)).append("\n");
        sb.append("• JDK 27 (Temurin, aarch64)\n");
        sb.append("• Gradle 9.8.2\n");
        sb.append("• Android Gradle Plugin 9.4.0\n");
        sb.append("• Android SDK build-tools 37.0.0（aapt2 / d8 / zipalign）\n");
        sb.append("• R8 / ProGuard（release 混淆与裁剪）\n\n");

        // ===== 运行时依赖 =====
        sb.append(getString(R.string.dep_section_runtime)).append("\n");
        sb.append("• OkHttp 4.12.1（Apache License 2.0）——HTTP 客户端\n");
        sb.append("• Gson 2.14.0（Apache License 2.0）——JSON 解析\n");
        sb.append("• 其余功能（图表、图表动画、骨架屏、主题）均为本项目自绘，无第三方 UI 库\n\n");

        // ===== 平台能力 =====
        sb.append(getString(R.string.dep_section_platform)).append("\n");
        sb.append("• Android API 37（minSdk 26 / targetSdk 37 / compileSdk 37）\n");
        sb.append("• Android Keystore + AES/GCM（API Key 本地加密存储）\n");
        sb.append("• 平台 BiometricPrompt（指纹/人脸解锁，API 28+）\n");
        sb.append("• AlarmManager（定时刷新，系统可能延迟）\n");
        sb.append("• MediaStore / ShortcutManager / WindowManager / NotificationManager\n\n");

        // ===== 服务商接口 =====
        sb.append(getString(R.string.dep_section_service)).append("\n");
        sb.append("• DeepSeek   GET /user/balance\n");
        sb.append("• Kimi       GET /v1/users/me/balance\n");
        sb.append("• 智谱 GLM   GET /api/paas/v4/balance\n");
        sb.append("• Anthropic  GET /v1/organizations/cost_report\n");
        sb.append("• 自定义服务商：由用户自行填写地址与认证方式\n\n");

        // ===== 许可与合规 =====
        sb.append(getString(R.string.dep_section_license)).append("\n");
        sb.append("• 本项目以 AGPL-3.0 分发，源码公开可查\n");
        sb.append("• 开发者头像取自 GitHub 公开头像接口 github.com/RXHrxh520.png，仅做展示\n");
        sb.append("• ").append(getString(R.string.dep_note_no_tracking)).append("\n");
        sb.append("• 汇率换算默认离线近似，若使用自定义汇率地址则由用户自行提供\n\n");
        TextView tv = view.findViewById(R.id.tv_deps);
        tv.setText(sb.toString());

        NavAnim.animateIn(view);
        ThemeUtils.apply(view);
        return view;
    }

    private void ImageViewBtnBack(final View view) {
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null && getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) act.switchTab(0);
            }
        });
    }
}
