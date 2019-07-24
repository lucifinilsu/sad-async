package com.sad.core.async;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Administrator on 2018/11/29 0029.
 */

public interface ILooperBlocker {

    public void onAction(LooperReleaser releaser);

    public void onRelease(Message message);

    default public Looper currLooper(){return Looper.getMainLooper();}

    /*default public void releaseBlock(Handler handler,Object o){
        Message m = handler.obtainMessage();
        m.obj=o;
        handler.sendMessage(m);
    }*/
}
