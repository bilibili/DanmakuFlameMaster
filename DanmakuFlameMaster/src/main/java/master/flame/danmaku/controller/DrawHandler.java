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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Choreographer;

import java.util.LinkedList;

import master.flame.danmaku.danmaku.model.AbsDanmakuSync;
import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.util.SystemClock;
import tv.cjump.jni.DeviceUtils;

public class DrawHandler extends Handler {

    private DanmakuContext mContext;
    private FrameCallback mFrameCallback;

    public interface Callback {
        public void prepared();

        public void updateTimer(DanmakuTimer timer);

        public void danmakuShown(BaseDanmaku danmaku);

        public void drawingFinished();

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

    private static final int FORCE_RENDER = 14;

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

    private AbsDisplayer mDisp;

    private final RenderingState mRenderingState = new RenderingState();

    private static final int MAX_RECORD_SIZE = 500;

    private LinkedList<Long> mDrawTimes = new LinkedList<>();

    private UpdateThread mThread;

    private boolean mUpdateInSeparateThread;

    private long mCordonTime = 30;

    private long mCordonTime2 = 60;

    private long mFrameUpdateRate = 16;

    @SuppressWarnings("unused")
    private long mThresholdTime;

    private long mLastDeltaTime;

    private boolean mInSeekingAction;

    private long mDesireSeekingTime;

    private long mRemainingTime;

    private boolean mInSyncAction;

    private boolean mInWaitingState;

    private boolean mIdleSleep;

    private boolean mNonBlockModeEnable;

    public DrawHandler(Looper looper, IDanmakuViewController view, boolean danmakuVisibile) {
        super(looper);
        mIdleSleep = !DeviceUtils.isProblemBoxDevice();
        bindView(view);
        if (danmakuVisibile) {
            showDanmakus(null);
        } else {
            hideDanmakus(false);
        }
        mDanmakusVisible = danmakuVisibile;
    }

    private void bindView(IDanmakuViewController view) {
        this.mDanmakuView = view;
    }

    public void setIdleSleep(boolean enable) {
        mIdleSleep = enable;
    }

    public void enableNonBlockMode(boolean enable) {
        mNonBlockModeEnable = enable;
    }

    public void setConfig(DanmakuContext config) {
        mContext = config;
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
        DanmakuTimer timer = parser.getTimer();
        if (timer != null) {
            this.timer = timer;
        }
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    public void quit() {
        quitFlag = true;
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
                mTimeBase = SystemClock.uptimeMillis();
                if (mParser == null || !mDanmakuView.isViewReady()) {
                    sendEmptyMessageDelayed(PREPARE, 100);
                } else {
                    prepare(new Runnable() {
                        @Override
                        public void run() {
                            pausedPosition = 0;
                            mReady = true;
                            if (mCallback != null) {
                                mCallback.prepared();
                            }
                        }
                    });
                }
                break;
            case SHOW_DANMAKUS:
                mDanmakusVisible = true;
                Long start = (Long) msg.obj;
                boolean resume = false;
                if (drawTask != null) {
                    if (start == null) {
                        timer.update(getCurrentTime());
                        drawTask.requestClear();
                    } else {
                        drawTask.start();
                        drawTask.seek(start);
                        drawTask.requestClear();
                        resume = true;
                    }
                }
                if (quitFlag && mDanmakuView != null) {
                    mDanmakuView.drawDanmakus();
                }
                notifyRendering();
                if (!resume) {
                    break;
                }
            case START:
                Long startTime = (Long) msg.obj;
                if (startTime != null) {
                    pausedPosition = startTime;
                } else {
                    pausedPosition = 0;
                }
            case SEEK_POS:
                if (what == SEEK_POS) {
                    quitFlag = true;
                    quitUpdateThread();
                    Long position = (Long) msg.obj;
                    long deltaMs = position - timer.currMillisecond;
                    mTimeBase -= deltaMs;
                    timer.update(position);
                    mContext.mGlobalFlagValues.updateMeasureFlag();
                    if (drawTask != null)
                        drawTask.seek(position);
                    pausedPosition = position;
                }
            case RESUME:
                removeMessages(DrawHandler.PAUSE);
                quitFlag = false;
                if (mReady) {
                    mRenderingState.reset();
                    mDrawTimes.clear();
                    mTimeBase = SystemClock.uptimeMillis() - pausedPosition;
                    timer.update(pausedPosition);
                    removeMessages(RESUME);
                    sendEmptyMessage(UPDATE);
                    drawTask.start();
                    notifyRendering();
                    mInSeekingAction = false;
                    if (drawTask != null) {
                        drawTask.onPlayStateChanged(IDrawTask.PLAY_STATE_PLAYING);
                    }
                } else {
                    sendEmptyMessageDelayed(RESUME, 100);
                }
                break;
            case UPDATE:
                if (mContext.updateMethod == 0) {
                    updateInChoreographer();
                } else if (mContext.updateMethod == 1) {
                    updateInNewThread();
                } else if (mContext.updateMethod == 2) {
                    updateInCurrentThread();
                }
                break;
            case NOTIFY_DISP_SIZE_CHANGED:
                mContext.mDanmakuFactory.notifyDispSizeChanged(mContext);
                Boolean updateFlag = (Boolean) msg.obj;
                if (updateFlag != null && updateFlag) {
                    mContext.mGlobalFlagValues.updateMeasureFlag();
                    mContext.mGlobalFlagValues.updateVisibleFlag();
                    drawTask.requestClearRetainer();
                }
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
                removeMessages(DrawHandler.RESUME);
                removeMessages(UPDATE);
                if (drawTask != null) {
                    drawTask.onPlayStateChanged(IDrawTask.PLAY_STATE_PAUSE);
                }
            case QUIT:
                if (what == QUIT) {
                    removeCallbacksAndMessages(null);
                }
                quitFlag = true;
                syncTimerIfNeeded();
                pausedPosition = timer.currMillisecond;
                if (mUpdateInSeparateThread) {
                    notifyRendering();
                    quitUpdateThread();
                }
                if (mFrameCallback != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Choreographer.getInstance().removeFrameCallback(mFrameCallback);
                    }
                }
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
            case FORCE_RENDER:
                if (drawTask != null) {
                    drawTask.requestRender();
                }
                break;
        }
    }

    private synchronized void quitUpdateThread() {
        UpdateThread thread = mThread;
        mThread = null;
        if (thread != null) {
            synchronized (drawTask) {
                drawTask.notifyAll();
            }
            thread.quit();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateInCurrentThread() {
        if (quitFlag) {
            return;
        }
        long startMS = SystemClock.uptimeMillis();
        long d = syncTimer(startMS);
        if (d < 0 && !mNonBlockModeEnable) {
            removeMessages(UPDATE);
            sendEmptyMessageDelayed(UPDATE, 60 - d);
            return;
        }
        d = mDanmakuView.drawDanmakus();
        removeMessages(UPDATE);
        if (d > mCordonTime2) {  // this situation may be cuased by ui-thread waiting of DanmakuView, so we sync-timer at once
            timer.add(d);
            mDrawTimes.clear();
        }
        if (!mDanmakusVisible) {
            waitRendering(INDEFINITE_TIME);
            return;
        } else if (mRenderingState.nothingRendered && mIdleSleep) {
            long dTime = mRenderingState.endTime - timer.currMillisecond;
            if (dTime > 500) {
                waitRendering(dTime - 10);
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
                long lastTime = SystemClock.uptimeMillis();
                long dTime = 0;
                while (!isQuited() && !quitFlag) {
                    long startMS = SystemClock.uptimeMillis();
                    dTime = SystemClock.uptimeMillis() - lastTime;
                    long diffTime = mFrameUpdateRate - dTime;
                    if (diffTime > 1 && !mNonBlockModeEnable) {
                        SystemClock.sleep(1);
                        continue;
                    }
                    lastTime = startMS;
                    long d = syncTimer(startMS);
                    if (d < 0 && !mNonBlockModeEnable) {
                        SystemClock.sleep(60 - d);
                        continue;
                    }
                    d = mDanmakuView.drawDanmakus();
                    if (d > mCordonTime2) {  // this situation may be cuased by ui-thread waiting of DanmakuView, so we sync-timer at once
                        timer.add(d);
                        mDrawTimes.clear();
                    }
                    if (!mDanmakusVisible) {
                        waitRendering(INDEFINITE_TIME);
                    } else if (mRenderingState.nothingRendered && mIdleSleep) {
                        dTime = mRenderingState.endTime - timer.currMillisecond;
                        if (dTime > 500) {
                            notifyRendering();
                            waitRendering(dTime - 10);
                        }
                    }
                }
            }
        };
        mThread.start();
    }

    @TargetApi(16)
    private class FrameCallback implements Choreographer.FrameCallback {
        @Override
        public void doFrame(long frameTimeNanos) {
            sendEmptyMessage(UPDATE);
        }
    };

    @TargetApi(16)
    private void updateInChoreographer() {
        if (quitFlag) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(mFrameCallback);
        long startMS = SystemClock.uptimeMillis();
        long d = syncTimer(startMS);
        if (d < 0) {
            removeMessages(UPDATE);
            return;
        }
        d = mDanmakuView.drawDanmakus();
        removeMessages(UPDATE);
        if (d > mCordonTime2) {  // this situation may be cuased by ui-thread waiting of DanmakuView, so we sync-timer at once
            timer.add(d);
            mDrawTimes.clear();
        }
        if (!mDanmakusVisible) {
            waitRendering(INDEFINITE_TIME);
            return;
        } else if (mRenderingState.nothingRendered && mIdleSleep) {
            long dTime = mRenderingState.endTime - timer.currMillisecond;
            if (dTime > 500) {
                waitRendering(dTime - 10);
                return;
            }
        }

    }

    private final long syncTimer(long startMS) {
        if (mInSeekingAction || mInSyncAction) {
            return 0;
        }
        mInSyncAction = true;
        long d = 0;
        long time = startMS - mTimeBase;
        if (mNonBlockModeEnable) {
            if (mCallback != null) {
                mCallback.updateTimer(timer);
                d = timer.lastInterval();
            }
        } else if (!mDanmakusVisible || mRenderingState.nothingRendered || mInWaitingState) {
            timer.update(time);
            mRemainingTime = 0;
            if (mCallback != null) {
                mCallback.updateTimer(timer);
            }
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
                if (a > 3 && a < 8 && mLastDeltaTime >= mFrameUpdateRate && mLastDeltaTime <= mCordonTime) {
                    d = mLastDeltaTime;
                }
                gapTime -= d;
                mLastDeltaTime = d;
            }
            mRemainingTime = gapTime;
            timer.add(d);
            if (mCallback != null) {
                mCallback.updateTimer(timer);
            }
//            Log.e("DrawHandler", time+"|d:" + d  + "RemaingTime:" + mRemainingTime + ",gapTime:" + gapTime + ",rtim:" + mRenderingState.consumingTime + ",average:" + averageTime);
        }

        mInSyncAction = false;
        return d;
    }

    private void syncTimerIfNeeded() {
        if (mInWaitingState) {
            syncTimer(SystemClock.uptimeMillis());
        }
    }

    private void initRenderingConfigs() {
        long averageFrameConsumingTime = 16;
        mCordonTime = Math.max(33, (long) (averageFrameConsumingTime * 2.5f));
        mCordonTime2 = (long) (mCordonTime * 2.5f);
        mFrameUpdateRate = Math.max(16, averageFrameConsumingTime / 15 * 15);
        mThresholdTime = mFrameUpdateRate + 3;
//        Log.i("DrawHandler", "initRenderingConfigs test-fps:" + averageFrameConsumingTime + "ms,mCordonTime:"
//                + mCordonTime + ",mFrameRefreshingRate:" + mFrameUpdateRate);
    }

    private void prepare(final Runnable runnable) {
        if (drawTask == null) {
            drawTask = createDrawTask(mDanmakuView.isDanmakuDrawingCacheEnabled(), timer,
                    mDanmakuView.getContext(), mDanmakuView.getViewWidth(), mDanmakuView.getViewHeight(),
                    mDanmakuView.isHardwareAccelerated(), new IDrawTask.TaskListener() {
                        @Override
                        public void ready() {
                            initRenderingConfigs();
                            runnable.run();
                        }

                        @Override
                        public void onDanmakuAdd(BaseDanmaku danmaku) {
                            if (danmaku.isTimeOut()) {
                                return;
                            }
                            long delay = danmaku.getActualTime() - getCurrentTime();
                            if (delay < mContext.mDanmakuFactory.MAX_DANMAKU_DURATION && (mInWaitingState || mRenderingState.nothingRendered)) {
                                notifyRendering();
                            } else if (delay > 0 && delay <= mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                                sendEmptyMessageDelayed(NOTIFY_RENDERING, delay);
                            }
                        }

                        @Override
                        public void onDanmakuShown(BaseDanmaku danmaku) {
                            if (mCallback != null) {
                                mCallback.danmakuShown(danmaku);
                            }
                        }

                        @Override
                        public void onDanmakusDrawingFinished() {
                            if (mCallback != null) {
                                mCallback.drawingFinished();
                            }
                        }

                        @Override
                        public void onDanmakuConfigChanged() {
                            redrawIfNeeded();
                        }
                    });
        } else {
            runnable.run();
        }
    }

    public boolean isPrepared() {
        return mReady;
    }

    private IDrawTask createDrawTask(boolean useDrwaingCache, DanmakuTimer timer,
                                     Context context,
                                     int width, int height,
                                     boolean isHardwareAccelerated,
                                     IDrawTask.TaskListener taskListener) {
        mDisp = mContext.getDisplayer();
        mDisp.setSize(width, height);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDisp.setDensities(displayMetrics.density, displayMetrics.densityDpi,
                displayMetrics.scaledDensity);
        mDisp.resetSlopPixel(mContext.scaleTextSize);
        mDisp.setHardwareAccelerated(isHardwareAccelerated);
        IDrawTask task = useDrwaingCache ?
                new CacheManagingDrawTask(timer, mContext, taskListener)
                : new DrawTask(timer, mContext, taskListener);
        task.setParser(mParser);
        task.prepare();
        obtainMessage(NOTIFY_DISP_SIZE_CHANGED, false).sendToTarget();
        return task;
    }

    public void seekTo(Long ms) {
        mInSeekingAction = true;
        mDesireSeekingTime = ms;
        removeMessages(DrawHandler.UPDATE);
        removeMessages(DrawHandler.RESUME);
        removeMessages(DrawHandler.SEEK_POS);
        obtainMessage(DrawHandler.SEEK_POS, ms).sendToTarget();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (drawTask != null) {
            item.flags = mContext.mGlobalFlagValues;
            item.setTimer(timer);
            drawTask.addDanmaku(item);
            obtainMessage(NOTIFY_RENDERING).sendToTarget();
        }
    }

    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        if (drawTask != null && item != null) {
            drawTask.invalidateDanmaku(item, remeasure);
        }
        redrawIfNeeded();
    }

    public void resume() {
        removeMessages(DrawHandler.PAUSE);
        sendEmptyMessage(DrawHandler.RESUME);
    }

    public void prepare() {
        mReady = false;
        if (Build.VERSION.SDK_INT < 16 && mContext.updateMethod == 0) {
            mContext.updateMethod = 2;
        }
        if (mContext.updateMethod == 0) {
            mFrameCallback = new FrameCallback();
        }
        mUpdateInSeparateThread = (mContext.updateMethod == 1);
        sendEmptyMessage(DrawHandler.PREPARE);
    }

    public void pause() {
        removeMessages(DrawHandler.RESUME);
        syncTimerIfNeeded();
        sendEmptyMessage(DrawHandler.PAUSE);
    }

    public void showDanmakus(Long position) {
        if (mDanmakusVisible)
            return;
        mDanmakusVisible = true;
        removeMessages(SHOW_DANMAKUS);
        removeMessages(HIDE_DANMAKUS);
        obtainMessage(SHOW_DANMAKUS, position).sendToTarget();
    }

    public long hideDanmakus(boolean quitDrawTask) {
        if (!mDanmakusVisible)
            return timer.currMillisecond;
        mDanmakusVisible = false;
        removeMessages(SHOW_DANMAKUS);
        removeMessages(HIDE_DANMAKUS);
        obtainMessage(HIDE_DANMAKUS, quitDrawTask).sendToTarget();
        return timer.currMillisecond;
    }

    public void forceRender() {
        removeMessages(FORCE_RENDER);
        obtainMessage(FORCE_RENDER).sendToTarget();
    }

    public boolean getVisibility() {
        return mDanmakusVisible;
    }

    public RenderingState draw(Canvas canvas) {
        if (drawTask == null)
            return mRenderingState;

        if (!mInWaitingState) {
            AbsDanmakuSync danmakuSync = mContext.danmakuSync;
            if (danmakuSync != null) {
                do {
                    boolean isSyncPlayingState = danmakuSync.isSyncPlayingState();
                    if (!isSyncPlayingState && quitFlag) {
                        break;
                    }
                    int syncState = danmakuSync.getSyncState();
                    if (syncState == AbsDanmakuSync.SYNC_STATE_PLAYING) {
                        long fromTime = timer.currMillisecond;
                        long toTime = danmakuSync.getUptimeMillis();
                        long offset = toTime - fromTime;
                        if (Math.abs(offset) > danmakuSync.getThresholdTimeMills()) {
                            if (isSyncPlayingState && quitFlag) {
                                resume();
                            }
                            drawTask.requestSync(fromTime, toTime, offset);
                            timer.update(toTime);
                            mTimeBase -= offset;
                            mRemainingTime = 0;
                        }
                    } else if (syncState == AbsDanmakuSync.SYNC_STATE_HALT) {
                        if (isSyncPlayingState && !quitFlag) {
                            pause();
                        }
                    }
                } while (false);
            }
        }
        mDisp.setExtraData(canvas);
        mRenderingState.set(drawTask.draw(mDisp));
        recordRenderingTime();
        return mRenderingState;
    }

    private void redrawIfNeeded() {
        if (quitFlag && mDanmakusVisible) {
            removeMessages(UPDATE_WHEN_PAUSED);
            sendEmptyMessageDelayed(UPDATE_WHEN_PAUSED, 100);
        }
    }

    private void notifyRendering() {
        if (!mInWaitingState) {
            return;
        }
        if(drawTask != null) {
            drawTask.requestClear();
        }
        if (mUpdateInSeparateThread) {
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
        if (isStop() || !isPrepared() || mInSeekingAction) {
            return;
        }
        mRenderingState.sysTime = SystemClock.uptimeMillis();
        mInWaitingState = true;
        if (mUpdateInSeparateThread) {
            if (mThread == null) {
                return;
            }
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
        Long first = mDrawTimes.peekFirst();
        Long last = mDrawTimes.peekLast();
        if (first == null || last == null) {
            return 0;
        }
        long dtime = last - first;
        return dtime / frames;
    }

    private synchronized void recordRenderingTime() {
        long lastTime = SystemClock.uptimeMillis();
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

    public void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        if (drawTask != null) {
            drawTask.removeAllDanmakus(isClearDanmakusOnScreen);
        }
    }

    public void removeAllLiveDanmakus() {
        if (drawTask != null) {
            drawTask.removeAllLiveDanmakus();
        }
    }

    public IDanmakus getCurrentVisibleDanmakus() {
        if (drawTask != null) {
            return drawTask.getVisibleDanmakusOnTime(getCurrentTime());
        }

        return null;
    }

    public long getCurrentTime() {
        if (!mReady) {
            return 0;
        }
        if (mInSeekingAction) {
            return mDesireSeekingTime;
        }
        if (quitFlag || !mInWaitingState) {
            return timer.currMillisecond - mRemainingTime;
        }
        return SystemClock.uptimeMillis() - mTimeBase;
    }

    public void clearDanmakusOnScreen() {
        obtainMessage(CLEAR_DANMAKUS_ON_SCREEN).sendToTarget();
    }

    public DanmakuContext getConfig() {
        return mContext;
    }

}
