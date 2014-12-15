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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

import java.util.LinkedList;
import java.util.Locale;

/**
 * DanmakuTextureView需要开启GPU加速才能显示弹幕
 * 很遗憾...经过测试TextureView没有提升绘制速度,也许哪里用的不对
 * @author ch
 *
 */
@SuppressLint("NewApi")
public class DanmakuTextureView extends TextureView implements IDanmakuView,
        TextureView.SurfaceTextureListener, View.OnClickListener {

    public static final String TAG = "DanmakuTextureView";

    private Callback mCallback;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache = true;

    private OnClickListener mOnClickListener;

    private boolean mShowFps;

    private boolean mDanmakuVisibile = true;
    
    protected int mDrawingThreadType = THREAD_TYPE_NORMAL_PRIORITY;

    public DanmakuTextureView(Context context) {
        super(context);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setOpaque(false);
        setWillNotCacheDrawing(true);
        setDrawingCacheEnabled(false);
        setWillNotDraw(true);
        setSurfaceTextureListener(this);
        setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (l != this) {
            mOnClickListener = l;
        } else
            super.setOnClickListener(l);
    }

    public DanmakuTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) {
            handler.addDanmaku(item);
        }
    }
    
    @Override
    public void removeAllDanmakus() {
        if (handler != null) {
            handler.removeAllDanmakus();
        }
    }
    
    @Override
    public void removeAllLiveDanmakus() {
        if (handler != null) {
            handler.removeAllLiveDanmakus();
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
        if (handler != null) {
            handler.setCallback(callback);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        isSurfaceCreated = true;
    }

    @Override
    public synchronized boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        isSurfaceCreated = false;
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (handler != null) {
            handler.notifyDispSizeChanged(width, height);
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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
        String threadName = "DFM Drawing thread #"+priority;
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
    private static final int MAX_RECORD_SIZE = 50;
    private static final int ONE_SECOND = 1000;
    private LinkedList<Long> mDrawTimes;
    private float fps() {
        long lastTime = System.currentTimeMillis();
        mDrawTimes.addLast(lastTime);
        float dtime = lastTime - mDrawTimes.getFirst();
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes.removeFirst();
        }
        return dtime > 0 ? mDrawTimes.size() * ONE_SECOND / dtime : 0.0f;
    }
    
    @Override
    public synchronized long drawDanmakus() {
        if (!isSurfaceCreated)
            return 0;
        long stime = System.currentTimeMillis();
        if (!isShown())
            return -1;
        long dtime = 0;
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            if (handler != null) {
                handler.draw(canvas);
                if (mShowFps) {
                    if(mDrawTimes == null) mDrawTimes = new LinkedList<Long>();
                    dtime = System.currentTimeMillis() - stime;  //not so accurate
                    String fps = String.format(Locale.getDefault(), "%02d MS, fps %.2f", dtime,
                            fps());
                    DrawHelper.drawFPS(canvas, fps);
                }
            }
            if (isSurfaceCreated)
                unlockCanvasAndPost(canvas);
        }
        dtime = System.currentTimeMillis() - stime;
        return dtime;
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
            handler.pause();
    }

    @Override
    public void resume() {
        if (handler != null && mDrawThread != null && handler.isPrepared())
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
        showAndResumeDrawTask(null);
    }
    
    @Override
    public void showAndResumeDrawTask(Long position) {
        mDanmakuVisibile = true;
        if (handler == null) {
            return;
        }
        handler.showDanmakus(position);
    }

    @Override
    public void hide() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return;
        }
        handler.hideDanmakus(false);
    }
    
    @Override
    public long hideAndPauseDrawTask() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return 0;
        }
        return handler.hideDanmakus(true);
    }

    @Override
    public synchronized void clear() {
        if (!isViewReady()) {
            return;
        }        
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            DrawHelper.clearCanvas(canvas);
            unlockCanvasAndPost(canvas);
        }

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
        mDrawingThreadType = type;
    }

    @Override
    public long getCurrentTime() {
        if (handler != null) {
            return handler.getCurrentTime();
        }
        return 0;
    }

}
