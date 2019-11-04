package com.sad.core.async;

/**
 * Created by Administrator on 2018/5/9 0009.
 */

public interface ISADTaskProccessListener<R> {
    /**
     * 异步线程处理任务，在非主线程执行
     * @return 处理后的结果
     * @throws InterruptedException 获取InterruptedException异常，来判断任务是否被取消
    public R doInBackground() throws Exception;*/


    /**
     * 异步线程处理后返回的结果，在主线程执行
     * @param result 结果
     */
    public void onSuccess(R result);

    /**
     * 异步线程处理出现异常的回调，按需处理，未置成抽象，主线程执行
     * @param throwable 异常
     */
    public void onFail(Throwable throwable);

    /**
     * 任务被取消的回调，主线程执行
     *
     */
    public void onCancel();


}
