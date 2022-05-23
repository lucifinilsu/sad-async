package com.sad.core.async;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2018/5/9 0009.
 */
public class SADHandlerAssistant {

    public final static String MAIN_THREAD_NAME="main_ui";

    public static void runOnUIThread(@NonNull Runnable runnable) {
        provideUIHandler(MAIN_THREAD_NAME).post(runnable);
    }
    public static void runOnUIThread(Runnable runnable, long delayed) {
        provideUIHandler(MAIN_THREAD_NAME).postDelayed(runnable,delayed);
    }
    public static void removeUICallback(Runnable runnable) {
        removeHandlerCallback(MAIN_THREAD_NAME,runnable);
    }

    public static void removeHandlerCallback(String threadName, Runnable runnable) {
        if (runnable==null){return;}
        if(isMainThread(provideUIHandler(threadName))){
            provideUIHandler(threadName).removeCallbacks(runnable);
        }else if(mHandlerMap.get(threadName)!=null){
            mHandlerMap.get(threadName).removeCallbacks(runnable);
        }
    }

    public static boolean isMainThread(Handler handler) {
        return Thread.currentThread()== handler.getLooper().getThread();
    }
    protected static Map<String,Handler> mHandlerMap = new ConcurrentHashMap<String, Handler>();


    public static Handler provideHandler(String handlerName,Looper looper, Handler.Callback callback){
        if(mHandlerMap.containsKey(handlerName)) {
            return mHandlerMap.get(handlerName);
        }
        Handler handler = null;
        if (callback!=null){
            handler=new SafeDispatchHandler(looper,callback);
        }
        else{
            handler = new SafeDispatchHandler(looper);
        }
        mHandlerMap.put(handlerName,handler);
        return handler;
    }

    public static Handler createHandlerFromLooper(Looper looper,Handler.Callback callback){
        Handler handler = null;
        if (callback!=null){
            handler=new SafeDispatchHandler(looper,callback);
        }
        else{
            handler = new SafeDispatchHandler(looper);
        }
        return handler;
    }



    public static Handler provideThreadHandler(String handlerName, Handler.Callback callback) {

        HandlerThread handlerThread = new HandlerThread(handlerName, Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        return provideHandler(handlerName,handlerThread.getLooper(),callback);
    }
    /**
     * 获取回调到handlerName线程的handler.一般用于在一个后台线程执行同一种任务，避免线程安全问题。如数据库，文件操作
     * @param handlerName 线程名
     * @return 异步任务handler
     */
    public static Handler provideThreadHandler(String handlerName) {

        return provideThreadHandler(handlerName,null);
    }

    public static Handler provideUIHandler(String handlerName, Handler.Callback callback) {

        return provideHandler(handlerName,Looper.getMainLooper(),callback);
    }

    public static Handler provideUIHandler(String handlerName) {

       return provideUIHandler(handlerName,null);
    }

    public static void looperBlock(ILooperBlocker blocker){
        Handler handler=new Handler(blocker.currLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                blocker.onRelease(msg);
                throw new RuntimeException();
            }
        });


        try {
            blocker.onAction(LooperReleaser.newInstance(handler));
            blocker.currLooper().loop();
        } catch (RuntimeException e2) {

        }
    }

}
