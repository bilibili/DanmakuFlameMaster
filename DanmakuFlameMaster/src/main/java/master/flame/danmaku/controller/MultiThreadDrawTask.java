
package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Canvas;

import master.flame.danmaku.danmaku.model.DanmakuTimer;

public class MultiThreadDrawTask extends DrawTask {

    private DanmakuTimer mPlayerTimer;

    public MultiThreadDrawTask(DanmakuTimer timer, Context context, int dispW, int dispH) {
        super(timer, context, dispW, dispH);
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = new DanmakuTimer();
        mPlayerTimer = timer;
    }

    @Override
    public void draw(Canvas canvas) {

    }

}
