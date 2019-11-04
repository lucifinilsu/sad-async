package com.sad.core.async;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SADTaskRunnable<R> extends PriorityRunnable {

    private static final String TAG = "SADTaskRunnable";
    private static final String NAME_PREFIX=TAG+"_"+"workThread_";
    private AtomicBoolean mCanceledAtomic = new AtomicBoolean(false);
    private AtomicReference<Thread> mTaskThread = new AtomicReference<>();
    private String name=NAME_PREFIX+"0";
    private ISADTaskProccessListener<R> proccessListener;

    public SADTaskRunnable(String name) {
        this.name=NAME_PREFIX+name;
    }

    public SADTaskRunnable(String name,ISADTaskProccessListener<R> proccessListener) {
        this.name=NAME_PREFIX+name;
        this.proccessListener=proccessListener;
    }

    public abstract R doInBackground() throws Exception;

    /**
     * 将任务标记为取消，没法真正取消正在执行的任务，只是结果不在onSuccess里回调
     * cancel 不一定能让任务停止，和AsyncTask同样道理，可参考
     * {#link http://silencedut.com/2016/07/08/%E5%9F%BA%E4%BA%8E%E6%9C%80%E6%96%B0%E7%89%88%E6%9C%AC%E7%9A%84AsyncTask%E6%BA%90%E7%A0%81%E8%A7%A3%E8%AF%BB%E5%8F%8AAsyncTask%E7%9A%84%E9%BB%91%E6%9A%97%E9%9D%A2/}
     **/

    void cancel() {

        this.mCanceledAtomic.set(true);

        Thread t = mTaskThread.get();
        if(t!=null) {
            Log.d(TAG,"SADTaskRunnable cancel: "+t.getName());
            try{
                t.interrupt();
            }catch (Exception e){
                e.printStackTrace();
            }
            catch (Error err){
                err.printStackTrace();
            }

        }

        SADHandlerAssistant.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (proccessListener!=null){
                    proccessListener.onCancel();
                }

            }
        });
    }

    /**
     * 任务是已取消
     * @return 任务是否已被取消
     */
    public boolean isCanceled() {

        return mCanceledAtomic.get();
    }

    @Override
    public void run() {
        try {

            Log.d(TAG,"SADTaskRunnable : "+ Thread.currentThread().getName());
            mTaskThread.compareAndSet(null, Thread.currentThread());

            mCanceledAtomic.set(false);
            final R result = doInBackground();

            SADHandlerAssistant.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if(!isCanceled()){
                        if (proccessListener!=null){
                            proccessListener.onSuccess(result);
                        }

                    }
                }
            });
        }  catch (final Throwable throwable) {

            Log.e(TAG,"handle background SADTaskRunnable  error " +throwable);
            SADHandlerAssistant.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if(!isCanceled()){
                        if (proccessListener!=null){
                            proccessListener.onFail(throwable);
                        }

                    }
                }
            });
        }
    }

}
