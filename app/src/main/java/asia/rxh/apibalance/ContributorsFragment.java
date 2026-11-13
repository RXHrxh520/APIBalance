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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** GitHub 贡献者：头像、昵称、简介、贡献次数。只读取公开接口，不推测性别等未提供的信息。 */
public class ContributorsFragment extends Fragment {

    private static final String API =
            "https://api.github.com/repos/RXHrxh520/APIBalance";
    private LinearLayout container;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_contributors, parent, false);
        View back = view.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> Navigator.up(this));
        container = view.findViewById(R.id.contrib_container);
        load();
        ThemeUtils.apply(view);
        return view;
    }

    private void load() {
        AppExecutors.io().execute(() -> {
            List<Person> people = new ArrayList<>();
            String error = null;
            try {
                JSONArray arr = new JSONArray(get(API + "/contributors?per_page=20"));
                int limit = Math.min(arr.length(), 12);
                for (int i = 0; i < limit; i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) continue;
                    Person p = new Person();
                    p.login = c.optString("login", "");
                    p.avatar = c.optString("avatar_url", "");
                    p.contributions = c.optInt("contributions", 0);
                    p.url = c.optString("html_url", "https://github.com/" + p.login);
                    if (p.login.isEmpty()) continue;
                    try {
                        JSONObject u = new JSONObject(get("https://api.github.com/users/" + p.login));
                        p.name = u.optString("name", "");
                        p.bio = u.optString("bio", "");
                        if (p.avatar.isEmpty()) p.avatar = u.optString("avatar_url", "");
                    } catch (Throwable ignored) {
                    }
                    people.add(p);
                }
            } catch (Throwable t) {
                error = t.getClass().getSimpleName();
            }
            final List<Person> result = people;
            final String err = error;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> render(result, err));
        });
    }

    private String get(String url) throws Exception {
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME)
                .build();
        try (okhttp3.Response resp = NetworkClient.http().newCall(req).execute()) {
            if (resp.code() != 200 || resp.body() == null) {
                throw new java.io.IOException("HTTP " + resp.code());
            }
            return resp.body().string();
        }
    }

    private void render(List<Person> people, String error) {
        if (container == null || getActivity() == null) return;
        container.removeAllViews();
        if (people.isEmpty()) {
            TextView tv = new TextView(getActivity());
            tv.setText(error == null ? getString(R.string.contrib_empty)
                    : getString(R.string.contrib_error));
            tv.setTextColor(ThemeUtils.textSecondary(getActivity()));
            tv.setTextSize(14);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dp(40), 0, dp(40));
            container.addView(tv);
            if (error != null) {
                tv.setOnClickListener(v -> {
                    container.removeAllViews();
                    load();
                });
            }
            return;
        }
        for (Person p : people) {
            container.addView(buildCard(p));
        }
    }

    private View buildCard(Person p) {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card_balance);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(10);
        card.setLayoutParams(cp);

        ImageView avatar = new ImageView(getActivity());
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundResource(R.drawable.bg_avatar_ring);
        avatar.setClipToOutline(true);
        avatar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(54), dp(54));
        card.addView(avatar, ap);
        loadAvatar(p.avatar, avatar);

        LinearLayout info = new LinearLayout(getActivity());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), 0, 0, 0);
        card.addView(info, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(getActivity());
        name.setText(p.name == null || p.name.trim().isEmpty() ? p.login : p.name);
        name.setTextColor(ThemeUtils.textPrimary(getActivity()));
        name.setTextSize(16);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        info.addView(name);

        TextView login = new TextView(getActivity());
        login.setText("@" + p.login);
        login.setTextColor(ThemePrefs.getColor(getActivity()));
        login.setTextSize(12);
        info.addView(login);

        TextView bio = new TextView(getActivity());
        bio.setText(p.bio == null || p.bio.trim().isEmpty()
                ? getString(R.string.contrib_no_bio) : p.bio.trim());
        bio.setTextColor(ThemeUtils.textSecondary(getActivity()));
        bio.setTextSize(12);
        bio.setMaxLines(2);
        bio.setPadding(0, dp(3), 0, 0);
        info.addView(bio);

        TextView count = new TextView(getActivity());
        count.setText(getString(R.string.contrib_count, p.contributions));
        count.setTextColor(ThemeUtils.textSecondary(getActivity()));
        count.setTextSize(11);
        count.setPadding(0, dp(4), 0, 0);
        info.addView(count);

        card.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(p.url)));
            } catch (Throwable t) {
                Toast.makeText(getActivity(), getString(R.string.contrib_error),
                        Toast.LENGTH_SHORT).show();
            }
        });
        return card;
    }

    private void loadAvatar(String url, ImageView iv) {
        if (url == null || url.isEmpty()) return;
        AppExecutors.io().execute(() -> {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder().url(url)
                        .header("User-Agent", "APIBalance/" + BuildConfig.VERSION_NAME).build();
                try (okhttp3.Response resp = NetworkClient.http().newCall(req).execute()) {
                    if (resp.body() == null) return;
                    Bitmap bm = BitmapFactory.decodeStream(resp.body().byteStream());
                    if (bm != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> iv.setImageBitmap(bm));
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class Person {
        String login, name, bio, avatar, url;
        int contributions;
    }
}
