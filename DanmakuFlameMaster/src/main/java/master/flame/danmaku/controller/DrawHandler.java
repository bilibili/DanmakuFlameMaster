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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import tv.cjump.jni.DeviceUtils;

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
    
    private static final int NOTIFY_RENDERING = 11;

    private static final int UPDATE_WHEN_PAUSED = 12;

    private static final int CLEAR_DANMAKUS_ON_SCREEN = 13;

    private static final long INDEFINITE_TIME = 10000000;

    private long pausedPosition = 0;

    private boolean quitFlag = true;

    private long mTimeBase;

    private boolean mReady;

    private Callback mCallback;

    private DanmakuTimer timer = new DanmakuTimer();

    private BaseDanmakuParser mParser;

    public IDrawTask drawTask;

    private IDanmakuViewController mDanmakuView;

    private boolean mDanmakusVisible = true;

    private AbsDisplayer<Canvas> mDisp;

    private final RenderingState mRenderingState = new RenderingState();

    @SuppressLint("Deprecated")
    private int mSkipFrames;

    private static final int MAX_RECORD_SIZE = 500;

    private LinkedList<Long> mDrawTimes = new LinkedList<Long>();

    private UpdateThread mThread;

    private final boolean mUpdateInNewThread;

    private long mCordonTime = 30;
    
    @SuppressWarnings("unused")
    private long mCordonTime2 = 60;

    private long mFrameUpdateRate = 16;

    @SuppressWarnings("unused")
    private long mThresholdTime;

    private long mLastDeltaTime;

    private boolean mInSeekingAction;

    private long mRemainingTime;

    private boolean mInSyncAction;

    private boolean mInWaitingState;

    private boolean mIdleSleep;

    public DrawHandler(Looper looper, IDanmakuViewController view, boolean danmakuVisibile) {
        super(looper);
        mUpdateInNewThread = (Runtime.getRuntime().availableProcessors() > 3);
        mIdleSleep = !DeviceUtils.isProblemBoxDevice();
        bindView(view);
        if(danmakuVisibile){
            showDanmakus(null);
        }else{
            hideDanmakus(false);
        }
        mDanmakusVisible = danmakuVisibile;
    }

    private void bindView(IDanmakuViewController view) {
        this.mDanmakuView = view;
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public void quit() {
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
                    pausedPosition = startTime;
                } else {
                    pausedPosition = 0;
                }
            case RESUME:
                quitFlag = false;
                if (mReady) {
                    mDrawTimes.clear();
                    mTimeBase = System.currentTimeMillis() - pausedPosition;
                    timer.update(pausedPosition);
                    removeMessages(RESUME);
                    sendEmptyMessage(UPDATE);
                    drawTask.start();
                    notifyRendering();
                    mInSeekingAction = false;
                } else {
                    sendEmptyMessageDelayed(RESUME, 100);
                }
                break;
            case SEEK_POS:
                quitFlag = true;
                quitUpdateThread();
                Long position = (Long) msg.obj;
                long deltaMs = position - timer.currMillisecond;
                mTimeBase -= deltaMs;
                timer.update(System.currentTimeMillis() - mTimeBase);
                if (drawTask != null)
                    drawTask.seek(timer.currMillisecond);
                pausedPosition = timer.currMillisecond;
                removeMessages(RESUME);
                sendEmptyMessage(RESUME);
                break;
            case UPDATE:
                if (mUpdateInNewThread) {
                    updateInNewThread();
                } else {
                    updateInCurrentThread();
                }
                break;
            case NOTIFY_DISP_SIZE_CHANGED:
                DanmakuFactory.notifyDispSizeChanged(mDisp);
                Boolean updateFlag = (Boolean) msg.obj;
                if(updateFlag != null && updateFlag){
                    GlobalFlagValues.updateMeasureFlag();
                }
                break;
            case SHOW_DANMAKUS:
                Long start = (Long) msg.obj;
                if(drawTask != null) {
                    if (start == null) {
                        timer.update(getCurrentTime());
                        drawTask.requestClear();
                    } else {
                        drawTask.start();
                        drawTask.seek(start);
                        drawTask.requestClear();
                        obtainMessage(START, start).sendToTarget();
                    }
                }
                mDanmakusVisible = true;
                if(quitFlag && mDanmakuView != null) {
                    mDanmakuView.drawDanmakus(); 
                }
                notifyRendering();
                break;
            case HIDE_DANMAKUS:
                mDanmakusVisible = false;
                if (mDanmakuView != null) {
                    mDanmakuView.clear();
                }
                if(this.drawTask != null) {
                    this.drawTask.requestClear();
                    this.drawTask.requestHide();
                }
                Boolean quitDrawTask = (Boolean) msg.obj;
                if (quitDrawTask && this.drawTask != null) {
                    this.drawTask.quit();
                }
                if (!quitDrawTask) {
                    break;
                }
            case PAUSE:
                removeMessages(UPDATE);
            case QUIT:
                if (what == QUIT) {
                    removeCallbacksAndMessages(null);
                }
                quitFlag = true;
                syncTimerIfNeeded();
                mSkipFrames = 0;
                if (mThread != null) {
                    notifyRendering();
                    quitUpdateThread();
                }
                pausedPosition = timer.currMillisecond;
                if (what == QUIT){
                    if (this.drawTask != null){
                        this.drawTask.quit();
                    }
                    if (mParser != null) {
                        mParser.release();
                    }
                    if (this.getLooper() != Looper.getMainLooper())
                        this.getLooper().quit();
                }
                break;
            case NOTIFY_RENDERING:
                notifyRendering();
                break;
            case UPDATE_WHEN_PAUSED:
                if (quitFlag && mDanmakuView != null) {
                    drawTask.requestClear();
                    mDanmakuView.drawDanmakus();
                    notifyRendering();
                }
                break;
            case CLEAR_DANMAKUS_ON_SCREEN:
                if (drawTask != null) {
                    drawTask.clearDanmakusOnScreen(getCurrentTime());
                }
                break;
        }
    }

    private void quitUpdateThread() {
        if (mThread != null) {
            synchronized (drawTask) {
                drawTask.notifyAll();
            }
            mThread.quit();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }
    }

    private void updateInCurrentThread() {
        if (quitFlag) {
            return;
        }
        long startMS = System.currentTimeMillis();
        long d = syncTimer(startMS);
        if (d < 0) {
            removeMessages(UPDATE);
            sendEmptyMessageDelayed(UPDATE, 60 - d);
            return;
        }
        d = mDanmakuView.drawDanmakus();
        removeMessages(UPDATE);
        if (!mDanmakusVisible) {
            waitRendering(INDEFINITE_TIME);
            return;
        } else if (mRenderingState.nothingRendered && mIdleSleep) {
            long dTime = mRenderingState.endTime - timer.currMillisecond;
            if (dTime > 500) {
                waitRendering(dTime - 400);
                return;
            }
        }

        if (d < mFrameUpdateRate) {
            sendEmptyMessageDelayed(UPDATE, mFrameUpdateRate - d);
            return;
        }
        sendEmptyMessage(UPDATE);
    }

    private void updateInNewThread() {
        if (mThread != null) {
            return;
        }
        mThread = new UpdateThread("DFM Update") {
            @Override
            public void run() {
                long lastTime = System.currentTimeMillis();
                long dTime = 0;
                while (!isQuited() && !quitFlag) {
                    long startMS = System.currentTimeMillis();
                    dTime = System.currentTimeMillis() - lastTime;
                    long diffTime = mFrameUpdateRate - dTime;
                    if (diffTime > 1) {
                        SystemClock.sleep(1);
                        continue;
                    }
                    lastTime = startMS;
                    long d = syncTimer(startMS);
                    if (d < 0) {
                        SystemClock.sleep(60 - d);
                        continue;
                    }
                    d = mDanmakuView.drawDanmakus();
                    if (!mDanmakusVisible) {
                        waitRendering(INDEFINITE_TIME);
                    } else if (mRenderingState.nothingRendered && mIdleSleep) {
                        dTime = mRenderingState.endTime - timer.currMillisecond;
                        if (dTime > 500) {
                            notifyRendering();
                            waitRendering(dTime - 400);
                        }
                    }
                }
            }
        };
        mThread.start();
    }

    private final long syncTimer(long startMS) {
        if (mInSeekingAction || mInSyncAction) {
            return 0;
        }
        mInSyncAction = true;
        long d = 0;
        long time = startMS - mTimeBase;
        if (!mDanmakusVisible || mRenderingState.nothingRendered || mInWaitingState) {
            timer.update(time);
            mRemainingTime = 0;
        } else {
            long gapTime = time - timer.currMillisecond;
            long averageTime = Math.max(mFrameUpdateRate, getAverageRenderingTime());
            if (gapTime > 2000 || mRenderingState.consumingTime > mCordonTime || averageTime > mCordonTime) {
                d = gapTime;
                gapTime = 0;
            } else {
                d = averageTime + gapTime / mFrameUpdateRate;
                d = Math.max(mFrameUpdateRate, d);
                d = Math.min(mCordonTime, d);
                long a = d - mLastDeltaTime;
                if (Math.abs(a) < 4 && d > mFrameUpdateRate && mLastDeltaTime > mFrameUpdateRate) {
                    d = mLastDeltaTime;
                }
                gapTime -= d;
            }
            mLastDeltaTime = d;
            mRemainingTime = gapTime;
            timer.add(d);
//            Log.e("DrawHandler", time+"|d:" + d  + "RemaingTime:" + mRemainingTime + ",gapTime:" + gapTime + ",rtim:" + mRenderingState.consumingTime + ",average:" + averageTime);
        }
        if (mCallback != null) {
            mCallback.updateTimer(timer);
        }
        mInSyncAction = false;
        return d;
    }
    
    private void syncTimerIfNeeded() {
        if (mInWaitingState) {
            syncTimer(System.currentTimeMillis());
        }
    }
    
    private void initRenderingConfigs() {
        long averageFrameConsumingTime = 16;
        mCordonTime = Math.max(33, (long) (averageFrameConsumingTime * 2.5f));
        mCordonTime2 = mCordonTime * 2;
        mFrameUpdateRate = Math.max(16, averageFrameConsumingTime / 15 * 15);
        mLastDeltaTime = mFrameUpdateRate;
        mThresholdTime = mFrameUpdateRate + 3;
//        Log.i("DrawHandler", "initRenderingConfigs test-fps:" + averageFrameConsumingTime + "ms,mCordonTime:"
//                + mCordonTime + ",mFrameRefreshingRate:" + mFrameUpdateRate);
    }

    private void prepare(final Runnable runnable) {
        if (drawTask == null) {
            drawTask = createDrawTask(mDanmakuView.isDanmakuDrawingCacheEnabled(), timer,
                    mDanmakuView.getContext(), mDanmakuView.getWidth(), mDanmakuView.getHeight(),
                    mDanmakuView.isHardwareAccelerated(), new IDrawTask.TaskListener() {
                        @Override
                        public void ready() {
                            initRenderingConfigs();
                            runnable.run();
                        }

                        @Override
                        public void onDanmakuAdd(BaseDanmaku danmaku) {
                            obtainMessage(NOTIFY_RENDERING).sendToTarget();
                        }

                        @Override
                        public void onDanmakuConfigChanged() {
                            if (quitFlag && mDanmakusVisible) {
                                obtainMessage(UPDATE_WHEN_PAUSED).sendToTarget();
                            }
                        }
                    });
        } else {
            runnable.run();
        }
    }

    public boolean isPrepared() {
        return mReady;
    }

    private IDrawTask createDrawTask(boolean useDrwaingCache, DanmakuTimer timer, Context context,
            int width, int height, boolean isHardwareAccelerated,
            IDrawTask.TaskListener taskListener) {
        mDisp = new AndroidDisplayer();
        mDisp.setSize(width, height);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDisp.setDensities(displayMetrics.density, displayMetrics.densityDpi,
                displayMetrics.scaledDensity);
        mDisp.resetSlopPixel(DanmakuGlobalConfig.DEFAULT.scaleTextSize);
        mDisp.setHardwareAccelerated(isHardwareAccelerated);

        IDrawTask task = useDrwaingCache ? new CacheManagingDrawTask(timer, context, mDisp,
                taskListener, 1024 * 1024 * AndroidUtils.getMemoryClass(context) / 3)
                : new DrawTask(timer, context, mDisp, taskListener);
        task.setParser(mParser);
        task.prepare();
        obtainMessage(NOTIFY_DISP_SIZE_CHANGED, false).sendToTarget();
        return task;
    }

    public void seekTo(Long ms) {
        mInSeekingAction = true;
        removeMessages(DrawHandler.UPDATE);
        removeMessages(DrawHandler.RESUME);
        removeMessages(DrawHandler.SEEK_POS);
        obtainMessage(DrawHandler.SEEK_POS, ms).sendToTarget();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (drawTask != null) {
            item.setTimer(timer);
            drawTask.addDanmaku(item);
            obtainMessage(NOTIFY_RENDERING).sendToTarget();
        }
    }

    public void resume() {
        sendEmptyMessage(DrawHandler.RESUME);
    }

    public void prepare() {
        sendEmptyMessage(DrawHandler.PREPARE);
    }

    public void pause() {
        syncTimerIfNeeded();
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

    public RenderingState draw(Canvas canvas) {
        if (drawTask == null)
            return mRenderingState;
        mDisp.setExtraData(canvas);
        mRenderingState.set(drawTask.draw(mDisp));
        recordRenderingTime();
        return mRenderingState;
    }
    
    private void notifyRendering() {
        if (!mInWaitingState) {
            return;
        }
        if(drawTask != null) {
            drawTask.requestClear();
        }
        mSkipFrames = 0;
        if (mUpdateInNewThread) {
            synchronized (this) {
                mDrawTimes.clear();
            }
            synchronized (drawTask) {
                drawTask.notifyAll();
            }
        } else {
            mDrawTimes.clear();
            removeMessages(UPDATE);
            sendEmptyMessage(UPDATE);
        }
        mInWaitingState = false;
    }
        
    private void waitRendering(long dTime) {
        mRenderingState.sysTime = System.currentTimeMillis();
        mInWaitingState = true;
        if (mUpdateInNewThread) {
            try {
                synchronized (drawTask) {
                    if (dTime == INDEFINITE_TIME) {
                        drawTask.wait();
                    } else {
                        drawTask.wait(dTime);
                    }
                    sendEmptyMessage(NOTIFY_RENDERING);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (dTime == INDEFINITE_TIME) {
                removeMessages(NOTIFY_RENDERING);
                removeMessages(UPDATE);
            } else {
                removeMessages(NOTIFY_RENDERING);
                removeMessages(UPDATE);
                sendEmptyMessageDelayed(NOTIFY_RENDERING, dTime);
            }
        }
    }

    private synchronized long getAverageRenderingTime() {
        int frames = mDrawTimes.size();
        if(frames <= 0)
            return 0;
        long dtime = mDrawTimes.getLast() - mDrawTimes.getFirst();
        return dtime / frames;
    }

    private synchronized void recordRenderingTime() {
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

    public void removeAllDanmakus() {
        if (drawTask != null) {
            drawTask.removeAllDanmakus();
        }
    }

    public void removeAllLiveDanmakus() {
        if (drawTask != null) {
            drawTask.removeAllLiveDanmakus();
        }
    }

    public long getCurrentTime() {
        if (quitFlag || !mInWaitingState) {
            return timer.currMillisecond - mRemainingTime;
        }
        return System.currentTimeMillis() - mTimeBase;
    }

    public void clearDanmakusOnScreen() {
        obtainMessage(CLEAR_DANMAKUS_ON_SCREEN).sendToTarget();
    }

}
