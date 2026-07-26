# DeepseekBalance
# DS余额查询

一个极简的 DeepSeek API 余额查询安卓工具。

## 功能
- 输入API Key查询赠金、总余额、充值余额
- 密钥仅保存在本地，不上传任何服务器
- 无广告、无多余SDK

## 如何使用
第一步：输入密钥
打开 App，在输入框中粘贴你的 DeepSeek API Key（密钥只存在你手机里，不上传任何服务器）。

第二步：开启自动登录（可选）
打开“自动登录”开关，下次打开 App 会自动填入密钥，不用重复输入。

第三步：查询余额
点击“查询余额”，会弹出一道简单的算术题，算对后再次点击查询，等待几秒就能看到余额。

第四步：查看数据
主界面会显示三个数字：

· 总可用金额
· 赠金余额
· 充值余额

刷新数据
点击左上角的刷新图标，可以手动更新最新余额。

切换账号
点击右上角的设置 → 更换 API 密钥，返回登录页重新输入即可。

关于与反馈
在设置 → 关于里，可以找到：

· 开发人员信息
· QQ交流群
· GitHub仓库地址

隐藏彩蛋
在关于页面，连续点击版本号 8 次，会进入调试模式（内含崩溃测试，仅供娱乐）。

## 开源协议
本项目基于 GNU Affero General Public License v3.0 开源。

## 我们开发的意义

📖 关于这个项目，我想说的

一、我为什么要做这个 App？

DeepSeek 是一家很棒的公司，他们的 API 文档写得清清楚楚，调用方式明明白白挂在官网上。任何人都可以复制那几行 Python 代码，在自己的电脑上跑起来查余额。

但是，不是每个人都想打开电脑，不是每个人都想打开浏览器只为了看一眼余额。

我只是想：如果手机桌面上有一个图标，点开，输入 Key，看到三个数字，关掉。 就这一件事，不需要别的。

于是这个 App 就诞生了。

二、它不是什么？

它不是一个“完整的 DeepSeek 客户端”。
它没有聊天功能，没有搜索功能，没有新闻推送，没有广告，没有统计 SDK，没有崩溃上报，没有后台服务，没有多余的网络请求。

它只做一件事：查余额。

而且它只做这一件事的方式是：

· 不联网传你的 Key（Key 只存在你手机本地）
· 不申请任何多余权限（不需要存储、位置、通讯录）
· 不加载任何第三方库（除了安卓系统本身，0 个第三方 SDK）

你打开它，它就在那里。你关掉它，它就像没存在过一样。

三、我为什么要开源？

说实话，这个 App 的代码量很小，技术含量也不高。它就是一个安卓壳子，包住了 DeepSeek 官方公开的 API。任何一个学过安卓开发的人，花两三小时都能写出来，哪怕是Vibe Coding也仅需要一两小时就能开发。

但我还是选择开源。 原因有三个：

1. 因为我希望它是透明的。
      我说“Key 只存本地”“不上传任何服务器”，光靠我嘴上说没用。源码摆在那里，谁都可以看，谁都可以审计。信任不是靠承诺建立的，是靠可验证建立的。
2. 因为我希望它是免费的。
      这个 API 本身就是免费的，我做的只是一个调用工具。如果有人拿它去卖 9.9 元、19.9 元，赚的是信息差的钱，是那些“不知道 GitHub 是什么”的普通用户的钱。
      我觉得这不公平。所以我把源码公开，让任何人都可以自己编译、自己用，也让那些差点付费的人有一个“免费的原版”可以对比。
3. 因为我希望它不止是我的。
      未来如果有人想给它加功能、修 bug、适配更多服务商，欢迎。它不姓我的姓，它属于所有需要它的人。

四、我为什么要选 AGPLv3？

网上很多人骂这个协议，说它“毒”、说它“传染性强”、说它是“伪开源”。

但我选它，恰恰因为它够“狠”。

我不怕别人用我的代码，我怕的是：

· 闭源售卖
· 让无辜的用户为信息差付费
· 反咬原作者
· 删掉名字，假装是“原创”

AGPLv3 不会阻止任何人使用、修改、分发这个代码。它只做一件事：如果你把它拿去商用或闭源，你必须把你的修改也开源出来。

这就是我的态度：

“你可以站在我的肩膀上，但你不能假装这个肩膀不存在。”

五、关于我自己

我不是 DeepSeek 的员工，我和他们没有任何雇佣关系。
我只是他们的一个普通用户，一个用了他们的 API、觉得很好用、希望他们能活得久一点的普通人，我现在也年仅14。

我没有权利代表他们说话，也没有义务替他们做事。
但我还是做了。

不是因为责任，是因为：

“喜欢一个东西的时候，你就是会忍不住为它做点什么。”

这个 App 就是我做的那么一点点事。

---

六、最后

如果你看到这里，谢谢你。

如果你下载了这个 App，希望你觉得它“干净、简单、不打扰”。

如果你想改它、用它、甚至骂它，都欢迎。源码在 GitHub 上，你想怎么折腾都行——只要记得遵守 AGPLv3。

如果你在淘宝、酷安、闲鱼上看到有人卖这个 App 的付费版……
麻烦你帮我举报一下，然后告诉他：这玩意儿是免费的，作者说了，谁卖谁是狗。

---

愿每一个好用的 API 都不被滥用。
愿每一个干净的 App 都不被埋没。
# DeepseekBalance
# DS Balance Checker

A minimalist Android tool for checking your DeepSeek API balance.

## Features
- Enter your API Key to view grant balance, total balance, and top-up balance
- Key is stored locally only, never uploaded to any server
- No ads, no extra SDKs

## How to Use

**Step 1: Enter your key**
Open the app and paste your DeepSeek API Key into the input field (the key stays only on your device and is never sent to any server).

**Step 2: Enable auto-login (optional)**
Turn on the "Auto Login" switch and the app will remember your key for next time – no need to re-enter.

**Step 3: Check your balance**
Tap "Check Balance". A simple arithmetic puzzle will pop up. Solve it correctly, then tap "Check Balance" again. Wait a few seconds and your balance will appear.

**Step 4: View your data**
The main screen shows three numbers:
- Total available amount
- Grant balance
- Top-up balance

**Refresh data**
Tap the refresh icon in the top‑left corner to manually update your balance.

**Switch accounts**
Go to Settings (top‑right) → Change API Key, which returns you to the login screen to enter a new key.

**About & Feedback**
In Settings → About, you can find:
- Developer info
- QQ group
- GitHub repository link

**Hidden Easter egg**
On the About page, tap the version number 8 times to enter debug mode (contains a crash test – for fun only).

## License
This project is open‑sourced under the GNU Affero General Public License v3.0.

---

## 📖 About This Project – What I Want to Say

### I. Why did I make this app?

DeepSeek is a great company. Their API documentation is clear, and the calling method is publicly available on their official website. Anyone can copy those few lines of Python code and run it on their own computer to check balances.

But not everyone wants to turn on a computer, and not everyone wants to open a browser just to glance at a number.

I just wanted this: an icon on my phone screen. Tap it, enter my key, see three numbers, close it. That’s it. Nothing else.

So this app was born.

### II. What it is NOT

It is **not** a full DeepSeek client.  
It has no chat, no search, no news feed, no ads, no analytics SDK, no crash reporting, no background services, no extra network requests.

It does only one thing: **check your balance**.

And the way it does that one thing:
- Does **not** transmit your key over the network (the key stays locally on your device)
- Does **not** request any unnecessary permissions (no storage, location, contacts)
- Does **not** load any third‑party libraries (except for the Android system itself – zero third‑party SDKs)

You open it, it’s there. You close it, it’s as if it never existed.

### III. Why did I open‑source it?

To be honest, the codebase is small and technically not complex. It’s just an Android wrapper around DeepSeek’s public API. Anyone with some Android development experience could write it in a couple of hours – even with Vibe Coding, it might only take one or two hours.

But I still chose to open‑source it. For three reasons:

1. **Because I want it to be transparent.**  
   When I say “the key stays local” or “never uploaded to any server”, words alone aren’t enough. With the source code open, anyone can inspect it and verify. Trust isn’t built on promises – it’s built on verifiability.

2. **Because I want it to be free.**  
   The API itself is free. I only built a tool to call it. If someone took this and sold it for ¥9.99 or ¥19.99, they’d be profiting from the information gap – from users who don’t know what GitHub is.  
   I think that’s unfair. So I made the source public, so that anyone can compile and use it themselves, and so that those who almost paid can find a free original version to compare.

3. **Because I want it to be more than mine.**  
   In the future, if someone wants to add features, fix bugs, or adapt it for more service providers – welcome. It doesn’t carry only my name; it belongs to everyone who needs it.

### IV. Why did I choose AGPLv3?

Many people online criticise this license, calling it “toxic”, “viral”, or “pseudo‑open‑source”.

But I chose it precisely because it is **strong**.

I’m not afraid of others using my code. What I fear is:
- Closed‑source commercial redistribution
- Making innocent users pay for information asymmetry
- Biting back at the original author
- Removing my name and pretending it’s “original”

AGPLv3 does not stop anyone from using, modifying, or distributing this code. It does only one thing: **if you take it and make it commercial or closed‑source, you must also open‑source your modifications.**

That is my stance:

> *“You can stand on my shoulders, but you cannot pretend those shoulders aren’t there.”*

### V. About Myself

I am not an employee of DeepSeek – I have no employment relationship with them.  
I’m just an ordinary user who found their API useful and hopes they can survive and thrive. I’m only 14 years old now.

I have no right to speak for them, nor any obligation to act on their behalf.  
But I did it anyway.

Not out of duty, but because:

> *“When you truly like something, you can’t help but do something for it.”*

This app is that small something I did.

---

### VI. Finally

If you’ve read this far – thank you.

If you download this app, I hope you find it “clean, simple, and non‑intrusive”.

If you want to modify it, use it, or even criticise it – you’re welcome. The source is on GitHub, you can do whatever you like – as long as you respect the AGPLv3.

If you see someone selling a paid version of this app on Taobao, Coolapk, or Xianyu……  
please help me report it, and tell them: **this thing is free – the author said, whoever sells it is a dog.**

---

May every good API be used responsibly.  
May every clean app be discovered.  
May every ordinary user’s love be treated with kindness.
