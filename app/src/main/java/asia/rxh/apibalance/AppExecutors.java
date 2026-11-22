package asia.rxh.apibalance;

/*
 * Copyright (c) 2026, 软小寒
 * SPDX-License-Identifier: BSD-3-Clause
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局后台线程池（v1.5 架构升级）：替代散落各处的 new Thread，
 * 统一线程命名、统一守护线程、避免无上限创建线程导致的调度与内存问题。
 */
public final class AppExecutors {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private static final ExecutorService IO = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "rxh-io-" + SEQ.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private AppExecutors() {
    }

    /** 网络/文件等阻塞型任务统一提交到这里。 */
    public static ExecutorService io() {
        return IO;
    }
}
