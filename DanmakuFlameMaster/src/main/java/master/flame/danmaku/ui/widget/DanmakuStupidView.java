/*
 * Copyright (C) 2015 zheng qian <xqq@0ginr.com>
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

import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;

import java.util.LinkedList;
import java.util.Locale;

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.ui.SkiaRedirector.*;

public class DanmakuStupidView extends SkStupidView implements IDanmakuView, SkStupidView.Callback, View.OnClickListener {

    public static final String TAG = "DanmakuStupidView";
    
    private DrawHandler.Callback mCallback;
    
    private SurfaceHolder mSurfaceHolder;
    
    private HandlerThread mDrawThread;
    
    private DrawHandler mHandler;
    
    private boolean isBackendCreated;
    
    private boolean mEnableDanmakuDrawingCache = true;
    
    private OnClickListener mOnClickListener;
    
    private boolean mShowFps;
    
    private boolean mDanmakuVisible = true;
    
    protected int mDrawingThreadType = THREAD_TYPE_NORMAL_PRIORITY;

    private Object mDrawCondition = new Object();
    
    private boolean mDrawFinished = false;
    
    private long mLastRenderingTime = 0;
    
    public DanmakuStupidView(Context context) {
        super(context, 0);
        init();
    }

    public DanmakuStupidView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init();
    }
    
    private void init() {
        setZOrderMediaOverlay(true);
        setWillNotCacheDrawing(true);
        setDrawingCacheEnabled(false);
        setWillNotDraw(true);
        mSurfaceHolder = getHolder();
        super.setCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        setOnClickListener(this);
    }
    
    @Override
    public void setOnClickListener(OnClickListener listener) {
        if (listener != this) {
            mOnClickListener = listener;
        } else {
            super.setOnClickListener(listener);
        }
    }

    @Override
    public void addDanmaku(BaseDanmaku item) {
        if (mHandler != null) {
            mHandler.addDanmaku(item);
        }
    }

    @Override
    public void removeAllDanmakus() {
        if (mHandler != null) {
            mHandler.removeAllDanmakus();
        }
    }

    @Override
    public void removeAllLiveDanmakus() {
        if (mHandler != null) {
            mHandler.removeAllLiveDanmakus();
        }
    }
    
    @Override
    public void setCallback(DrawHandler.Callback callback) {
        mCallback = callback;
        if (mHandler != null) {
            mHandler.setCallback(callback);
        }
    }
    
    @Override
    public void onBackendCreated() {
        isBackendCreated = true;
    }

    @Override
    public void onBackendChanged(int width, int height) {
        if (mHandler != null) {
            mHandler.notifyDispSizeChanged(width, height);
        }        
    }

    @Override
    public void onBackendDestroyed() {
        isBackendCreated = false;
    }
    
    @Override
    public void release() {
        stop();
        DanmakuFilters.getDefault().clear();
        if (mDrawTimes != null) {
            mDrawTimes.clear();
        }        
    }
    
    @Override
    public void stop() {
        stopDraw();
    }

    private void stopDraw() {
        if (mHandler != null) {
            mHandler.quit();
            mHandler = null;
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
        super.terminate();
    }
    
    protected Looper getLooper(int type) {
        if (mDrawThread != null) {
            mDrawThread.quit();
            mDrawThread = null;
        }
        
        int priority;
        switch (type) {
            case THREAD_TYPE_MAIN_THREAD:
                return Looper.getMainLooper();
            case THREAD_TYPE_HIGH_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY;
                break;
            case THREAD_TYPE_LOW_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_LOWEST;
                break;
            case THREAD_TYPE_NORMAL_PRIORITY:
            default:
                priority = android.os.Process.THREAD_PRIORITY_DEFAULT;
                break;
        }
        String threadName = "DFM Stupid Looper-Thread #" + priority;
        mDrawThread = new HandlerThread(threadName, priority);
        mDrawThread.start();
        return mDrawThread.getLooper();
    }
    
    private void prepare() {
        if (mHandler == null) {
            mHandler = new DrawHandler(getLooper(mDrawingThreadType), this, mDanmakuVisible);
        }
    }
    
    @Override
    public void prepare(BaseDanmakuParser parser) {
        prepare();
        mHandler.setParser(parser);
        mHandler.setCallback(mCallback);
        mHandler.prepare();
    }
    
    @Override
    public boolean isPrepared() {
        return mHandler != null && mHandler.isPrepared();
    }
    
    @Override
    public void showFPS(boolean show) {
        mShowFps = show;        
    }
    
    private static final int MAX_RECORD_SIZE = 50;
    private static final int ONE_SECOND = 1000;
    private LinkedList<Long> mDrawTimes;
    
    private float fps() {
        long lastTime = System.currentTimeMillis();
        mDrawTimes.addLast(lastTime);
        float dTime = lastTime - mDrawTimes.getFirst();
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes.removeFirst();
        }
        return dTime > 0 ? mDrawTimes.size() * ONE_SECOND / dTime : 0.0f;
    }
    
    @Override
    public long drawDanmakus() {
        if (!isBackendCreated)
            return 0;
        if (!isShown())
            return -1;
        if (mDanmakuVisible) {
            this.requestRender();
            
            synchronized (mDrawCondition) {
                while (!mDrawFinished) {
                    try {
                        mDrawCondition.wait();
                    } catch (InterruptedException e) {
                        if (mHandler == null || mHandler.isStop()) {
                            break;
                        } else {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                mDrawFinished = false;
            }
        }

        return mLastRenderingTime;
    }
    
    @Override
    protected void onSkiaDraw(Canvas canvas) {
        long stime = System.currentTimeMillis();
        long dtime = 0;
        if (canvas != null) {
            if (mHandler != null) {
                RenderingState rs = mHandler.draw(canvas);
                if (mShowFps) {
                    if (mDrawTimes == null)
                        mDrawTimes = new LinkedList<Long>();
                    dtime = System.currentTimeMillis() - stime;
                    String fpsString = String.format(Locale.getDefault(),
                            "fps %.2f,time:%d s,cache:%d,miss:%d", fps(),
                            mHandler.getCurrentTime() / 1000, rs.cacheHitCount, rs.cacheMissCount);
                    DrawHelper.drawFPS(canvas, fpsString);
                }
            }
        }
        dtime = System.currentTimeMillis() - stime;
        synchronized (mDrawCondition) {
            mLastRenderingTime = dtime;
            mDrawFinished = true;
            mDrawCondition.notifyAll();
        }
    }
    
    @Override
    public void toggle() {
        if (isBackendCreated) {
            if (mHandler == null) {
                start();
            } else if (mHandler.isStop()) {
                resume();
            } else {
                pause();
            }
        }
    }

    @Override
    public void pause() {
        if (mHandler != null) {
            mHandler.pause();
        }
    }

    @Override
    public void resume() {
        if (mHandler != null && mHandler.isPrepared()) {
            mHandler.resume();
        } else {
            restart();
        }        
    }
    
    @Override
    public boolean isPaused() {
        if(mHandler != null) {
            return mHandler.isStop();
        }
        return false;
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
        if (mHandler == null) {
            prepare();
        } else {
            mHandler.removeCallbacksAndMessages(null);
        }
        mHandler.obtainMessage(DrawHandler.START, postion).sendToTarget();
    }

    @Override
    public void onClick(View v) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v);
        }        
    }
    
    @Override
    public void seekTo(Long ms) {
        if (mHandler != null) {
            mHandler.seekTo(ms);
        }
    }

    @Override
    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDanmakuDrawingCache = enable;
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        return mEnableDanmakuDrawingCache;
    }

    @Override
    public boolean isViewReady() {
        return isBackendCreated;
    }

    @Override
    public View getView() {
        return this;
    }
    
    @Override
    public void show() {
        showAndResumeDrawTask(null);
    }
    
    @Override
    public void showAndResumeDrawTask(Long position) {
        mDanmakuVisible = true;
        if (mHandler == null) {
            return;
        }
        mHandler.showDanmakus(position);        
    }
    
    @Override
    public void hide() {
        mDanmakuVisible = false;
        if (mHandler == null) {
            return;
        }
        mHandler.hideDanmakus(false);
    }
    
    @Override
    public long hideAndPauseDrawTask() {
        mDanmakuVisible = false;
        if (mHandler == null) {
            return 0;
        }
        return mHandler.hideDanmakus(true);
    }
    
    @Override
    public void clear() {
        if (!isViewReady()) {
            return;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Canvas canvas = lockCanvas();
                if (canvas != null) {
                    DrawHelper.clearCanvas(canvas);
                    unlockCanvasAndPost(canvas);
                }
            }
        });
    }
    
    @Override
    public boolean isShown() {
        return !(mHandler == null || !isViewReady()) && mHandler.getVisibility();
    }

    @Override
    public void setDrawingThreadType(int type) {
        mDrawingThreadType = type;        
    }

    @Override
    public long getCurrentTime() {
        if (mHandler != null) {
            return mHandler.getCurrentTime();
        }
        return 0;
    }
    
}
