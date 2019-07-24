package com.sad.core.async;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Administrator on 2018/11/29 0029.
 */

public class LooperReleaser {
    private Handler handler;
    private LooperReleaser (Handler handler){
        this.handler=handler;
    }

    public static LooperReleaser newInstance(Handler handler){
        return new LooperReleaser(handler);
    }

    public void releaseBlock(Object o,int delayed){
        Message m = handler.obtainMessage();
        m.obj=o;
        handler.sendMessageDelayed(m,delayed);
    }

}
