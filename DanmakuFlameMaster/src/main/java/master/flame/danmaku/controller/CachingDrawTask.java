
package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import master.flame.danmaku.danmaku.model.DanmakuTimer;

public class CachingDrawTask extends DrawTask {

    DrawingCache mCache;

    private DanmakuTimer mPlayerTimer;

    public CachingDrawTask(DanmakuTimer timer, Context context, int dispW, int dispH,
                           TaskListener taskListener) {
        super(timer, context, dispW, dispH, taskListener);
        initCache();
    }

    private void initCache() {
        mCache = new DrawingCache(3, mDisp) {
            @Override
            protected void drawCache(DrawingCacheHolder holder) {
                DrawHelper.clearCanvas(holder.canvas);
                synchronized (mCache) {
                    drawDanmakus(holder.canvas, mTimer);
                    holder.extra = mTimer.currMillisecond;
                    mTimer.add(30); // fix magic num
                }
            }
        };
        mCache.start();
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = new DanmakuTimer();
        mPlayerTimer = timer;
        mTimer.update(mPlayerTimer.currMillisecond);
    }

    @Override
    public void draw(Canvas canvas) {
        mCounter.begin();
        Bitmap bmp = mCache.getCache();
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0, 0, null);
            mCache.fillNext();
            mCounter.end().log("drawing");
        }

    }

    @Override
    public void reset() {
        synchronized (mCache) {
            super.reset();
        }
        mCache.clear();
        mTimer.update(mPlayerTimer.currMillisecond);
        mCache.fillNext();
    }

    @Override
    public void seek(long mills) {
        reset();
    }

    @Override
    public void quit() {
        mCache.quit();
    }
}
