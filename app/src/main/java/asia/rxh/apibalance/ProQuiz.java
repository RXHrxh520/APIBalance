package asia.rxh.apibalance;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 开启专业模式前的安全小测验。
 * 题库 20 道，每次随机抽 5 道；每道题的选项顺序也每次重新打乱。
 *
 * Copyright (C) 2026 软小寒
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
public final class ProQuiz {

    /** 每次抽几道。 */
    private static final int TOTAL = 5;

    /** 题库：{题干, 选项A, 选项B, 选项C, 正确项下标}。 */
    private static final String[][] Q_ZH = {
            {"API Key 最应该放在哪里？", "写进客户端代码里", "只在服务端使用", "发到群里让大家共用", "1"},
            {"把 Key 提交到公开的 GitHub 仓库会怎样？", "没什么事", "可能被爬虫秒扫并盗用", "只有自己能看见", "1"},
            {"有人自称「官方客服」向你索要 Key，应该？", "立刻提供", "先核实身份，绝不提供", "只发一半", "1"},
            {"在公共 WiFi 下用明文 HTTP 查余额，主要风险是？", "没有风险", "可能被同一网段抓包", "只会变慢", "1"},
            {"手机要借给别人用，最该做的是？", "直接给", "先开应用锁，必要时退出登录", "把应用卸载", "1"},
            {"截图余额准备发朋友圈之前，应该？", "直接发", "打码或先隐藏余额", "无所谓", "1"},
            {"陌生网页让你填 Key 来「查余额」，应该？", "可以填", "不要填，可能被直接收集", "填完再改", "1"},
            {"怀疑 Key 已经泄露，第一步应该？", "先观察几天", "立刻去服务商后台吊销并重建", "改一下手机密码", "1"},
            {"某应用为了「查余额」要你开无障碍权限，应该？", "直接同意", "拒绝，并重新考虑要不要用它", "随便点", "1"},
            {"在已经 Root 的手机上保存 Key，主要风险是？", "没有区别", "其他应用可能读到本应用的私有数据", "会更省电", "1"},
            {"备份文件应该怎么处理？", "随便放哪都行", "加密保存，不随便发给别人", "直接传公开网盘", "1"},
            {"同一个 Key 在多台设备上混用，会怎样？", "更安全", "泄露面变大，建议各自独立", "完全没有影响", "1"},
            {"有人「免费送额度」只要你提供 Key，应该？", "马上参加", "大概率是骗局，不要给", "试一试", "1"},
            {"手机装了来路不明的输入法，风险是？", "无所谓", "可能记录你输入的一切，包括 Key", "打字更快", "1"},
            {"有人在手机上装了抓包用的根证书，意味着？", "一切正常", "中间人可能看到全部流量", "网速更快", "1"},
            {"Key 应该多久更换一次？", "永远不用换", "怀疑泄露就换，平时定期换更稳", "每小时换一次", "1"},
            {"有人让你把 Key 发到邮箱「备份」，应该？", "照做", "不要，邮箱并不安全", "加密后发", "1"},
            {"手机丢了，第一件事应该？", "等等看", "远程吊销 Key 并修改密码", "直接买新手机", "1"},
            {"群里有人发「APIBalance 破解版」，应该？", "装上试试", "不要安装，可能被植入窃取代码", "先装再删", "1"},
            {"开启专业模式真正的意义是？", "界面更好看", "少暴露信息、少犯错、更谨慎地管理 Key", "查询更快", "1"},
    };

    private static final String[][] Q_EN = {
            {"Where should an API key live?", "Inside client code", "Only on the server", "Shared in a group chat", "1"},
            {"What happens if you push a key to a public GitHub repo?", "Nothing", "Crawlers may steal it in seconds", "Only you can see it", "1"},
            {"Someone claims to be official support and asks for your key. You should:", "Hand it over", "Verify identity and never share it", "Send half of it", "1"},
            {"Main risk of querying over plain HTTP on public WiFi?", "None", "Someone on the same network can sniff it", "Just slower", "1"},
            {"Before lending your phone, you should:", "Just hand it over", "Enable app lock, log out if needed", "Uninstall the app", "1"},
            {"Before posting a balance screenshot, you should:", "Post it as is", "Blur or hide the balance first", "Not bother", "1"},
            {"A strange website asks for your key to check balance. You should:", "Enter it", "Not enter it, it may be harvested", "Enter then change it", "1"},
            {"You suspect your key leaked. First step?", "Wait and see", "Revoke and recreate it at the provider", "Change your phone password", "1"},
            {"An app wants accessibility permission just to show balance. You should:", "Allow it", "Refuse and reconsider the app", "Tap whatever", "1"},
            {"Main risk of storing keys on a rooted phone?", "No difference", "Other apps may read this app private data", "Better battery", "1"},
            {"How should backups be handled?", "Anywhere is fine", "Encrypted, and not casually shared", "Upload to a public drive", "1"},
            {"Reusing one key across many devices:", "Is safer", "Widens exposure; keep them separate", "Has no effect", "1"},
            {"Someone offers free credits for your key. You should:", "Join now", "Treat it as a scam and refuse", "Give it a try", "1"},
            {"Risk of installing an unknown keyboard?", "None", "It may log everything you type, keys included", "Faster typing", "1"},
            {"A capture proxy installed a root certificate. That means:", "All normal", "A man-in-the-middle may see all traffic", "Faster network", "1"},
            {"How often should a key be rotated?", "Never", "Rotate on suspicion, and periodically", "Every hour", "1"},
            {"Someone asks you to email your key as a backup. You should:", "Do it", "Refuse, email is not safe enough", "Encrypt and send", "1"},
            {"Your phone is lost. First thing to do?", "Wait", "Revoke the key remotely and change passwords", "Buy a new phone", "1"},
            {"Someone posts a cracked APIBalance build. You should:", "Install it", "Do not install; it may contain stealers", "Install then delete", "1"},
            {"The real point of Pro mode is:", "Prettier UI", "Expose less, make fewer mistakes, manage keys carefully", "Faster queries", "1"},
    };

    private ProQuiz() {
    }

    private static String[][] pool() {
        try {
            String lang = java.util.Locale.getDefault().getLanguage();
            if (lang != null && lang.startsWith("en")) return Q_EN;
        } catch (Throwable ignored) {
        }
        return Q_ZH;
    }

    public static void start(final Activity act, final Runnable onPass) {
        start(act, onPass, null);
    }

    /** onFinish 无论答对答错都会走一次。 */
    public static void start(final Activity act, final Runnable onPass, final Runnable onFinish) {
        if (act == null) return;
        String[][] pool = pool();
        List<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < pool.length; i++) idx.add(i);
        Collections.shuffle(idx, new Random(System.nanoTime()));
        int[] picked = new int[Math.min(TOTAL, idx.size())];
        for (int i = 0; i < picked.length; i++) picked[i] = idx.get(i);
        ask(act, pool, picked, 0, 0, onPass, onFinish);
    }

    private static void ask(final Activity act, final String[][] pool, final int[] picked,
                            final int step, final int score, final Runnable onPass,
                            final Runnable onFinish) {
        if (step >= picked.length) {
            if (score >= picked.length) {
                UiDialog.showConfirm(act, act.getString(R.string.quiz_pass),
                        act.getString(R.string.quiz_pass_desc),
                        act.getString(R.string.h_004), null,
                        () -> {
                            if (onPass != null) onPass.run();
                            if (onFinish != null) onFinish.run();
                        }, null);
            } else {
                UiDialog.showConfirm(act, act.getString(R.string.quiz_fail),
                        act.getString(R.string.quiz_fail_desc),
                        act.getString(R.string.h_004), null,
                        () -> {
                            if (onFinish != null) onFinish.run();
                        }, null);
            }
            return;
        }
        String[] q = pool[picked[step]];
        final int correct = Integer.parseInt(q[4]);
        // 选项每次重新打乱
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 1; i <= 3; i++) order.add(i);
        Collections.shuffle(order, new Random(System.nanoTime() + step));
        final String[] shown = new String[]{
                q[order.get(0)], q[order.get(1)], q[order.get(2)]};
        final int correctShown = order.indexOf(correct + 1);
        final int no = step + 1;
        UiDialog.showPicker(act, no + "/" + picked.length + "  " + q[0], shown, -1, which -> {
            if (which == correctShown) {
                ask(act, pool, picked, step + 1, score + 1, onPass, onFinish);
            } else {
                UiDialog.showConfirm(act, act.getString(R.string.quiz_wrong),
                        act.getString(R.string.quiz_wrong_desc),
                        act.getString(R.string.h_004), null, null, null);
            }
        });
    }
}

