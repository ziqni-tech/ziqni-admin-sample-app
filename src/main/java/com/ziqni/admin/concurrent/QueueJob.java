package com.ziqni.admin.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class QueueJob<T> implements Runnable {

    public final Supplier<CompletableFuture<T>> supplier;
    public final CompletableFuture<T> completableFuture;
    public final ThreadPoolExecutor executor;

    public final Long duration;
    public final TimeUnit timeUnit;


    public QueueJob(Supplier<CompletableFuture<T>> supplier, Long duration, TimeUnit timeUnit, ThreadPoolExecutor executor) {
        this.supplier = supplier;
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.completableFuture = new CompletableFuture<>();
        this.executor = executor;
    }

    private CompletableFuture<T> submit(){
        this.executor.submit(this);
        return this.completableFuture;
    }

    public static <T1> CompletableFuture<T1> Submit(ThreadPoolExecutor executor, Supplier<CompletableFuture<T1>> supplier){
        return new QueueJob<>(supplier, 20L, TimeUnit.SECONDS, executor).submit();
    }

    public static <T1> CompletableFuture<T1> Submit(Long duration, TimeUnit timeUnit, ThreadPoolExecutor executor, Supplier<CompletableFuture<T1>> supplier){
        return new QueueJob<T1>(supplier, duration, timeUnit, executor).submit();
    }

    @Override
    public void run() {
        try {
            supplier.get()
                    .orTimeout(this.duration,this.timeUnit)
                    .thenApply(this.completableFuture::complete)
                    .exceptionally(this.completableFuture::completeExceptionally);
        }
        catch (Throwable throwable){
            completableFuture.completeExceptionally(throwable);
        }
    }
}
