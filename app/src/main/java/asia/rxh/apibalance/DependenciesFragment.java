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
        sb.append(getString(R.string.dep_build_r8));

        // ===== 运行时依赖 =====
        sb.append(getString(R.string.dep_section_runtime)).append("\n");
        sb.append(getString(R.string.dep_runtime_okhttp));
        sb.append(getString(R.string.dep_runtime_gson));
        sb.append(getString(R.string.dep_runtime_selfmade));

        // ===== 平台能力 =====
        sb.append(getString(R.string.dep_section_platform)).append("\n");
        sb.append("• Android API 37（minSdk 26 / targetSdk 37 / compileSdk 37）\n");
        sb.append(getString(R.string.dep_platform_keystore));
        sb.append(getString(R.string.dep_platform_biometric));
        sb.append(getString(R.string.dep_platform_alarm));
        sb.append("• MediaStore / ShortcutManager / WindowManager / NotificationManager\n\n");

        // ===== 服务商接口 =====
        sb.append(getString(R.string.dep_section_service)).append("\n");
        sb.append(getString(R.string.dep_svc_deepseek));
        sb.append(getString(R.string.dep_svc_kimi));
        sb.append(getString(R.string.dep_svc_glm));
        sb.append(getString(R.string.dep_svc_anthropic));
        sb.append(getString(R.string.dep_svc_volc));
        sb.append(getString(R.string.dep_svc_kimicode));
        sb.append(getString(R.string.dep_svc_glmcode));
        sb.append(getString(R.string.dep_svc_codex));
        sb.append(getString(R.string.dep_svc_openai));
        sb.append(getString(R.string.dep_svc_custom)).append("\n");

        // ===== 许可与合规 =====
        sb.append(getString(R.string.dep_section_license)).append("\n");
        sb.append(getString(R.string.dep_license_agpl));
        sb.append(getString(R.string.dep_license_avatar));
        sb.append("• ").append(getString(R.string.dep_note_no_tracking)).append("\n");
        sb.append(getString(R.string.dep_license_fx));
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
