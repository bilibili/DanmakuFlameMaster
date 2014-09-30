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

package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.GlobalFlagValues;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuGlobalConfig;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.util.AndroidUtils;

import java.util.LinkedList;

public class DrawHandler extends Handler {

    public interface Callback {
        public void prepared();

        public void updateTimer(DanmakuTimer timer);
    }

    public static final int START = 1;

    public static final int UPDATE = 2;

    public static final int RESUME = 3;

    public static final int SEEK_POS = 4;

    public static final int PREPARE = 5;

    private static final int QUIT = 6;

    private static final int PAUSE = 7;
    
    private static final int SHOW_DANMAKUS = 8;
    
    private static final int HIDE_DANMAKUS = 9;

    private static final int NOTIFY_DISP_SIZE_CHANGED = 10;

    private long pausedPostion = 0;

    private boolean quitFlag = true;

    private long mTimeBase;

    private boolean mReady;

    private Callback mCallback;

    private DanmakuTimer timer;

    private BaseDanmakuParser mParser;

    public IDrawTask drawTask;

    private IDanmakuView mDanmakuView;

    private boolean mDanmakusVisible = true;

    private AbsDisplayer<Canvas> mDisp;

    private Thread mTimerThread;

    private final RenderingState mRenderingState = new RenderingState();

    public DrawHandler(Looper looper, IDanmakuView view, boolean danmakuVisibile) {
        super(looper);
        if (timer == null) {
            timer = new DanmakuTimer();
        }
        if(danmakuVisibile){
            showDanmakus(null);
        }else{
            hideDanmakus(false);
        }
        mDanmakusVisible = danmakuVisibile;
        bindView(view);
    }

    private void bindView(IDanmakuView view) {
        this.mDanmakuView = view;
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public void quit() {
        removeCallbacksAndMessages(null);
        sendEmptyMessage(QUIT);
    }

    public boolean isStop() {
        return quitFlag;
    }

    @Override
    public void handleMessage(Message msg) {
        int what = msg.what;
        switch (what) {
            case PREPARE:
                if (mParser == null || !mDanmakuView.isViewReady()) {
                    sendEmptyMessageDelayed(PREPARE, 100);
                } else {
                    prepare(new Runnable() {
                        @Override
                        public void run() {
                            mReady = true;
                            if (mCallback != null) {
                                mCallback.prepared();
                            }
                        }
                    });
                }
                break;
            case START:
                Long startTime = (Long) msg.obj;
                if (startTime != null) {
                    pausedPostion = startTime;
                } else {
                    pausedPostion = 0;
                }
            case RESUME:
                quitFlag = false;
                if (mReady) {
                    mTimeBase = System.currentTimeMillis() - pausedPostion;
                    timer.update(pausedPostion);
                    removeMessages(RESUME);
                    sendEmptyMessage(UPDATE);
                    drawTask.start();
                } else {
                    sendEmptyMessageDelayed(RESUME, 100);
                }
                break;
            case SEEK_POS:
                Long deltaMs = (Long) msg.obj;
                mTimeBase -= deltaMs;
                timer.update(System.currentTimeMillis() - mTimeBase);
                if (drawTask != null)
                    drawTask.seek(timer.currMillisecond);
                pausedPostion = timer.currMillisecond;
                removeMessages(RESUME);
                sendEmptyMessage(RESUME);
                break;
            case UPDATE:
                if (quitFlag) {
                    break;
                }
                long startMS = System.currentTimeMillis();
                long time = startMS - mTimeBase;
                long d = 0;
                if (mRenderingState != null
                        && (mRenderingState.l2rDanmakuCount
                                + mRenderingState.r2lDanmakuCount > 30
                                || mRenderingState.consumingTime > 30
                                || getAverageRenderingTime() > 25 
                                || mRenderingState.r2lDanmakuCount + mRenderingState.l2rDanmakuCount + mRenderingState.specialDanmakuCount == 0)) {
                    d = timer.update(time);
                } else {
                    d = Math.max(16, getAverageRenderingTime());
                    d = timer.add(d);
                }
                if (mCallback != null) {
                    mCallback.updateTimer(timer);
                }
                if (d < 0) {
                    removeMessages(UPDATE);
                    sendEmptyMessageDelayed(UPDATE, 60 - d);
                    break;
                }
                d = mDanmakuView.drawDanmakus();                
                removeMessages(UPDATE);
                if (d == -1) {
                    // reduce refresh rate
                    sendEmptyMessageDelayed(UPDATE, 100);
                    break;
                }
                
                if (d <= 16) {
                    sendEmptyMessage(UPDATE);
                    SystemClock.sleep(16 - d);
                    break;
                }
                sendEmptyMessage(UPDATE);
                break;
            case NOTIFY_DISP_SIZE_CHANGED:
                DanmakuFactory.notifyDispSizeChanged(mDisp);
                Boolean updateFlag = (Boolean) msg.obj;
                if(updateFlag != null && updateFlag){
                    GlobalFlagValues.updateMeasureFlag();
                }
                break;
            case SHOW_DANMAKUS:
                GlobalFlagValues.updateVisibleFlag();
                Long start = (Long) msg.obj;
                if(drawTask != null) {
                    if (start == null) {
                        drawTask.requestClear();
                    } else {
                        drawTask.start();
                        drawTask.seek(start);
                        drawTask.requestClear();
                        obtainMessage(START, start).sendToTarget();
                    }
                }
                mDanmakusVisible = true;
                break;
            case HIDE_DANMAKUS:
                mDanmakusVisible = false;
                if (mDanmakuView != null) {
                    mDanmakuView.clear();
                }
                if(this.drawTask != null) {
                    this.drawTask.requestClear();
                }
                Boolean quitDrawTask = (Boolean) msg.obj;
                if (quitDrawTask && this.drawTask != null) {
                    this.drawTask.quit();
                }
                if (!quitDrawTask) {
                    break;
                }
            case PAUSE:
            case QUIT:
                removeCallbacksAndMessages(null);
                quitFlag = true;
                mDrawTimes.clear();
                pausedPostion = timer.currMillisecond;
                if (what == QUIT){
                    if (this.drawTask != null){
                        this.drawTask.quit();
                    }
                    if (this.getLooper() != Looper.getMainLooper())
                        this.getLooper().quit();
                    
                    if (mParser != null) {
                        mParser.release();
                    }
                }
                break;
        }
    }

    private void update() {
        if (mTimerThread != null && mTimerThread.isAlive()) {
            return;
        }

        mTimerThread = new Thread("DFM Timer Thread") {

            @Override
            public void run() {
                while (!quitFlag) {
                    long currTime = System.currentTimeMillis();
                    long time = currTime - mTimeBase;
                    long averageTime = getAverageRenderingTime();
                    if (averageTime < 20 && mRenderingState != null) {
                        timer.add(Math.max(16, mRenderingState.consumingTime));
                    } else {
                        timer.update(time);
                    }
                    if (mCallback != null) {
                        mCallback.updateTimer(timer);
                    }
                    long d = mDanmakuView.drawDanmakus();
                    if (d < 0) {
                        SystemClock.sleep(200);
                    } else if (d <= 16) {
                        SystemClock.sleep(16 - d);
                    }
                }
            }

        };
        mTimerThread.start();
    }

    private void prepare(final Runnable runnable) {
        if (drawTask == null) {
            drawTask = createTask(mDanmakuView.isDanmakuDrawingCacheEnabled(), timer,
                    mDanmakuView.getContext(), mDanmakuView.getWidth(), mDanmakuView.getHeight(),
                    new IDrawTask.TaskListener() {
                        @Override
                        public void ready() {
                            runnable.run();
                        }
                    });

        } else {
            runnable.run();
        }
    }

    public boolean isPrepared() {
        return mReady;
    }

    private IDrawTask createTask(boolean useDrwaingCache, DanmakuTimer timer, Context context,
            int width, int height, IDrawTask.TaskListener taskListener) {
        mDisp = new AndroidDisplayer();
        mDisp.setSize(width, height);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDisp.setDensities(displayMetrics.density, displayMetrics.densityDpi,
                displayMetrics.scaledDensity);
        mDisp.resetSlopPixel(DanmakuGlobalConfig.DEFAULT.scaleTextSize);
        obtainMessage(NOTIFY_DISP_SIZE_CHANGED, false).sendToTarget();
        
        IDrawTask task = useDrwaingCache ? new CacheManagingDrawTask(timer, context, mDisp,
                taskListener, 1024 * 1024 * AndroidUtils.getMemoryClass(context) / 3)
                : new DrawTask(timer, context, mDisp, taskListener);
        task.setParser(mParser);
        task.prepare();
        return task;
    }

    public void seekTo(Long ms) {
        seekBy(ms - timer.currMillisecond);
    }

    public void seekBy(Long deltaMs) {
        removeMessages(DrawHandler.UPDATE);
        obtainMessage(DrawHandler.SEEK_POS, deltaMs).sendToTarget();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (drawTask != null) {
            drawTask.addDanmaku(item);
        }
    }

    public void resume() {
        sendEmptyMessage(DrawHandler.RESUME);
    }

    public void prepare() {
        sendEmptyMessage(DrawHandler.PREPARE);
    }

    public void pause() {
        sendEmptyMessage(DrawHandler.PAUSE);
    }

    public void showDanmakus(Long position) {
        if (mDanmakusVisible)
            return;
        removeMessages(SHOW_DANMAKUS);
        removeMessages(HIDE_DANMAKUS);
        obtainMessage(SHOW_DANMAKUS, position).sendToTarget();
    }

    public long hideDanmakus(boolean quitDrawTask) {
        if (!mDanmakusVisible)
            return timer.currMillisecond;
        removeMessages(SHOW_DANMAKUS);
        removeMessages(HIDE_DANMAKUS);
        obtainMessage(HIDE_DANMAKUS, quitDrawTask).sendToTarget();
        return timer.currMillisecond;
    }

    public boolean getVisibility() {
        return mDanmakusVisible;
    }

    public void draw(Canvas canvas) {
        if (drawTask == null)
            return;
        mDisp.setAverageRenderingTime(Math.max(16, getAverageRenderingTime()));
        mDisp.setLastFrameRenderingTime(mDrawTimes.size() < 2 ? 16 : mDrawTimes.getLast()
                - mDrawTimes.get(mDrawTimes.size() - 2));
        mDisp.setExtraData(canvas);
        mRenderingState.set(drawTask.draw(mDisp));
        recordRenderingTime();
    }
        
    private long getAverageRenderingTime() {
        int frames = mDrawTimes.size();
        if(frames <= 0)
            return 0;
        long dtime = mDrawTimes.getLast() - mDrawTimes.getFirst();
        return dtime / frames;
    }

    private static final int MAX_RECORD_SIZE = 150;
    private LinkedList<Long> mDrawTimes = new LinkedList<Long>();

    private void recordRenderingTime() {
        long lastTime = System.currentTimeMillis();
        mDrawTimes.addLast(lastTime);
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes.removeFirst();
            frames = MAX_RECORD_SIZE;
        }
    }
    
    public IDisplayer getDisplayer(){
        return mDisp;
    }

    public void notifyDispSizeChanged(int width, int height) {
        if (mDisp == null) {
            return;
        }
        if (mDisp.getWidth() != width || mDisp.getHeight() != height) {
            mDisp.setSize(width, height);
            obtainMessage(NOTIFY_DISP_SIZE_CHANGED, true).sendToTarget();
        }
    }

}
