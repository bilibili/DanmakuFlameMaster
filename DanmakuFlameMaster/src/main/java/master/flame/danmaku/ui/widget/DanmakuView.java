/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

/**
 * DanmakuView使用View的canvas绘制弹幕,只能在THREAD_TYPE_NORMAL_PRIORITY下使用
 * 
 * @author ch
 */
public class DanmakuView extends View implements IDanmakuView, View.OnClickListener {

    public static final String TAG = "DanmakuView";

    private Callback mCallback;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache = true;

    private OnClickListener mOnClickListener;

    private boolean mShowFps;

    private boolean mDanmakuVisibile = true;
    
    protected int mDrawingThreadType = THREAD_TYPE_MAIN_THREAD;

    public DanmakuView(Context context) {
        super(context);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        // 透明
        setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (l != this) {
            mOnClickListener = l;
        } else
            super.setOnClickListener(l);
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) {
            handler.addDanmaku(item);
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
        if (handler != null) {
            handler.setCallback(callback);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            isSurfaceCreated = true;
        } else {
            isSurfaceCreated = false;
        }
    }
    
    @Override
    protected void onAttachedToWindow() { 
        super.onAttachedToWindow();
        isSurfaceCreated = true;
    }

    @Override
    public void release() {
        stop();
        DanmakuFilters.getDefault().clear();
    }

    @Override
    public void stop() {
        stopDraw();
    }

    private void stopDraw() {
        if (handler != null) {
            handler.quit();
            handler = null;
        }
        if (mDrawThread != null) {
            try {
                mDrawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrawThread.quit();
            mDrawThread = null;
        }
    }

    protected Looper getLooper(int type){
        if (mDrawThread != null) {
            mDrawThread.quit();
            mDrawThread = null;
        }
        int priority = Thread.NORM_PRIORITY;
        String threadName = "DFM Drawing thread";
        switch (type) {
            case THREAD_TYPE_MAIN_THREAD: {                
                return Looper.getMainLooper();
            }
            case THREAD_TYPE_HIGH_PRIORITY: {
                priority = Thread.MAX_PRIORITY;
                threadName += Thread.MAX_PRIORITY;
            }
                break;
            case THREAD_TYPE_NORMAL_PRIORITY: {
                priority = Thread.NORM_PRIORITY;
                threadName += Thread.NORM_PRIORITY;
            }
                break;
            case THREAD_TYPE_LOW_PRIORITY: {
                priority = Thread.MIN_PRIORITY;
                threadName += Thread.MIN_PRIORITY;
            }
                break;
        }
        
        mDrawThread = new HandlerThread(threadName, priority);
        mDrawThread.start();
        return mDrawThread.getLooper();
    }

    private void prepare() {
        if (handler == null)
            handler = new DrawHandler(getLooper(mDrawingThreadType), this, mDanmakuVisibile);
    }

    @Override
    public void prepare(BaseDanmakuParser parser) {
        prepare();
        handler.setParser(parser);
        handler.setCallback(mCallback);
        handler.prepare();
    }

    @Override
    public boolean isPrepared() {
        return handler != null && handler.isPrepared();
    }

    @Override
    public void showFPS(boolean show) {
        mShowFps = show;
    }

    long dtime = 0;

    @Override
    public long drawDanmakus() {
        synchronized (this) {
            if (dtime < 16) {
                this.postInvalidateDelayed(16 - dtime);
            } else {
                this.postInvalidate();
            }
            return dtime;
        }
    }

    @Override
    public void draw(Canvas canvas) {

        synchronized (this) {
            DrawHelper.drawFPS(canvas, "TEST");
            canvas.drawText("TEST", 0, 0, DrawHelper.PAINT_FPS);
            Log.e(TAG, "ondraw:" + isSurfaceCreated + ",isshow:" + isShown());
            if (!isSurfaceCreated)
                return;
            if (!isShown()) {
                // DrawHelper.clearCanvas(canvas);
                return;
            }
            long stime = System.currentTimeMillis();
            if (canvas != null) {
                if (handler != null) {
                    Log.e(TAG, "ondraw:" + isSurfaceCreated + ",isshow:" + isShown()
                            + "handler.DRAWTASK:" + (handler.drawTask == null));
                    handler.draw(canvas);
                    dtime = System.currentTimeMillis() - stime;
                    if (mShowFps) {
                        String fps = String.format("%02d MS, fps %.2f", dtime,
                                1000 / (float) Math.max(dtime, 1));
                        DrawHelper.drawFPS(canvas, fps);
                    }
                }
            }
        }
    }
 

    public void toggle() {
        if (isSurfaceCreated) {
            if (handler == null)
                start();
            else if (handler.isStop()) {
                resume();
            } else
                pause();
        }
    }

    @Override
    public void pause() {
        if (handler != null)
            handler.quit();
    }

    @Override
    public void resume() {
        if (handler != null && handler.isPrepared())
            handler.resume();
        else {
            restart();
        }
    }

    public void restart() {
        stop();
        start();
    }

    @Override
    public void start() {
        start(0);
    }

    @Override
    public void start(long postion) {
        if (handler == null) {
            prepare();
        } else {
            handler.removeCallbacksAndMessages(null);
        }
        handler.obtainMessage(DrawHandler.START, postion).sendToTarget();
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(view);
        }
    }

    public void seekTo(Long ms) {
        if (handler != null) {
            handler.seekTo(ms);
        }
    }

    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDanmakuDrwaingCache = enable;
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        return mEnableDanmakuDrwaingCache;
    }

    @Override
    public boolean isViewReady() {
        return isSurfaceCreated;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {
        mDanmakuVisibile = true;
        if (handler == null) {
            return;
        }
        handler.showDanmakus();
    }

    @Override
    public void hide() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return;
        }
        handler.hideDanmakus();
    }

    @Override
    public void clear() {
        if (!isViewReady()) {
            return;
        }
        this.postInvalidate();
    }

    @Override
    public boolean isShown() {
        if (handler == null || !isViewReady()) {
            return false;
        }
        return handler.getVisibility();
    }
    
    @Override
    public void setDrawingThreadType(int type) {
        //mDrawingThreadType = type;
    }

}
