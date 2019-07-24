package com.sad.core.async;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;


public class SADTaskSchedulerClient {

    //private Context context;
    private Executor mParallelExecutor = getDefaultExecutor();
    private ExecutorService mTimeOutExecutor ;


    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 60L;

    private SADTaskSchedulerClient(){
        //this.context=context;
        /*
          没有核心线程的线程池要用 SynchronousQueue 而不是LinkedBlockingQueue，SynchronousQueue是一个只有一个任务的队列，
          这样每次就会创建非核心线程执行任务,因为线程池任务放入队列的优先级比创建非核心线程优先级大.
         */
        mTimeOutExecutor = new ThreadPoolExecutor(0,MAXIMUM_POOL_SIZE,
                KEEP_ALIVE, TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),
                TIME_OUT_THREAD_FACTORY);
    }


    public static SADTaskSchedulerClient newInstance(){
        return new SADTaskSchedulerClient();
    }

    public SADTaskSchedulerClient executor(Executor executor){
        if (executor!=null){
            this.mParallelExecutor=executor;
        }

        return this;
    }

    private Executor getDefaultExecutor(){
        SADExecutor executor = new SADExecutor();

        // set temporary parameter just for test
        // 一下参数设置仅用来测试，具体设置看实际情况。

        // number of concurrent threads at the same time, recommended core size is CPU count
        // 开发者均衡性能和业务场景，自己调整同一时段的最大并发数量
        executor.setCoreSize(CPU_COUNT);

        // adjust maximum number of waiting queue size by yourself or based on phone performance
        // 开发者均衡性能和业务场景，自己调整最大排队线程数量
        executor.setQueueSize(MAXIMUM_POOL_SIZE);

        // 任务数量超出[最大并发数]后，自动进入[等待队列]，等待当前执行任务完成后按策略进入执行状态：后进先执行。
        executor.setSchedulePolicy(SchedulePolicy.LastInFirstRun);

        // 后续添加新任务数量超出[等待队列]大小时，执行过载策略：抛弃队列内最旧任务。
        executor.setOverloadPolicy(OverloadPolicy.DiscardOldTaskInQueue);

        return executor;
    }

    private final ThreadFactory TIME_OUT_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "TaskScheduler timeoutThread #" + mCount.getAndIncrement());
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };

    /**
     *执行一个后台任务，无回调
     * **/
    public void execute(Runnable task) {
        mParallelExecutor.execute(task);
    }



    /**
     *执行一个后台任务，如果不需回调
     * @see #execute(Runnable)
     **/
    public <R> void execute(SADTaskRunnable<R> task) {
        mParallelExecutor.execute(task);
    }

    /**
     * 取消一个任务
     * @param task 被取消的任务
     */
    public static void cancelTask(SADTaskRunnable task) {
        if(task!=null) {
            task.cancel();
        }
    }




    /**
     * 使用一个单独的线程池来执行超时任务，避免引起他线程不够用导致超时
     *  @param timeOutMillis  超时时间，单位毫秒
     ** 通过实现error(Exception) 判断是否为 TimeoutException 来判断是否超时,
     *                        不能100%保证实际的超时时间就是timeOutMillis，但一般没必要那么精确
     * */
    public <R> void executeTimeOutTask(final long timeOutMillis, final SADTaskRunnable<R> timeOutTask) {
        final Future future =mTimeOutExecutor.submit(timeOutTask);

        mTimeOutExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get(timeOutMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e ) {
                    SADHandlerAssistant.runOnUIThread(new Runnable()  {
                        @Override
                        public void run() {
                            if(!timeOutTask.isCanceled()) {
                                timeOutTask.cancel();
                            }
                        }
                    });
                }

            }
        });
    }

    public static <R> ScheduledFuture<R> executeScheduledTask(Callable<R> callable, long delay){
        ScheduledExecutorService executor= Executors.newScheduledThreadPool(1);
        ScheduledFuture<R> sf=executor.schedule(callable, delay, TimeUnit.SECONDS);
        return sf;
    }
    public static ScheduledFuture executeScheduledTask(Runnable runnable, long delay, long period){
        ScheduledExecutorService executor= Executors.newScheduledThreadPool(1);
        ScheduledFuture sf=executor.scheduleWithFixedDelay(runnable, delay,period, TimeUnit.SECONDS);
        return sf;
    }

}
