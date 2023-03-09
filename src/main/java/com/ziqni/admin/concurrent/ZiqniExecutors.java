/*
 * Copyright (c) 2022. ZIQNI LTD registered in England and Wales, company registration number-09693684
 */
package com.ziqni.admin.concurrent;

import java.util.concurrent.*;

public abstract class ZiqniExecutors {

    // MANAGEMENT BUS //
    public static final ScheduledThreadPoolExecutor ReconnectScheduledExecutor = ZiqniExecutors.newSingleThreadScheduledExecutor("ziqni-reconnect");

    // COMMON SINGLE THREADED RATE REDUCER //
    public static final ZiqniLinkedBlockingQueue<Runnable> StoresSingleThreadedExecutorWorkQueue = new ZiqniLinkedBlockingQueue<>("ziqni-stores-worker", 1000);
    public static final ThreadPoolExecutor StoresSingleThreadedExecutor = ZiqniExecutors.newSingleThreadedExecutor(StoresSingleThreadedExecutorWorkQueue,"ziqni-stores-worker");

    // EVENT STORE CACHE MODIFICATION RATE REDUCER //
    public static final ZiqniLinkedBlockingQueue<Runnable> SingleThreadedEventStoreExecutorWorkQueue = new ZiqniLinkedBlockingQueue<>("ziqni-event-stores-worker", 1000);
    public static final ThreadPoolExecutor EventStoreSingleThreadedExecutor = ZiqniExecutors.newSingleThreadedExecutor(SingleThreadedEventStoreExecutorWorkQueue,"ziqni-event-stores-worker");

    // CACHES CONNECTION //
    public static ForkJoinPool GlobalZiqniCachesExecutor = newForkJoinPool(Runtime.getRuntime().availableProcessors(), "ziqni-caches");

    // UTILS //

    public static ThreadPoolExecutor newSingleThreadedExecutor(final ZiqniLinkedBlockingQueue<Runnable> workQueue, final String executorNamePrefix){
        final var ziqniThreadFactory = new ZiqniThreadFactory(executorNamePrefix);
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, ziqniThreadFactory);
    }
    public static ThreadPoolExecutor newMultiThreadedExecutor(final int nThreads, final ZiqniLinkedBlockingQueue<Runnable> workQueue, final String executorNamePrefix){
        final var ziqniThreadFactory = new ZiqniThreadFactory(executorNamePrefix);
        return new ThreadPoolExecutor(nThreads, nThreads*8, 0L, TimeUnit.MILLISECONDS, workQueue, ziqniThreadFactory);
    }

    public static ExecutorService newCachedThreadPool(final String executorNamePrefix){
        final var ziqniThreadFactory = new ZiqniThreadFactory(executorNamePrefix);
        return Executors.newCachedThreadPool(ziqniThreadFactory);
    }

    public static ExecutorService newFixedThreadPool(final int nThreads, final String executorNamePrefix){
        final var ziqniThreadFactory = new ZiqniThreadFactory(executorNamePrefix);
        return Executors.newFixedThreadPool(10, ziqniThreadFactory);
    }

    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(final String executorNamePrefix){
        final var ziqniThreadFactory = new ZiqniThreadFactory(executorNamePrefix);
        return new ScheduledThreadPoolExecutor(1, ziqniThreadFactory);
    }

    public static ForkJoinPool newForkJoinPool(final int nThreads, final String threadPrefix){
        final ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName(threadPrefix + "-forkjoin-worker-thread-" + worker.getPoolIndex());
            return worker;
        };
        return new ForkJoinPool(nThreads, factory, null,true);
    }
}
