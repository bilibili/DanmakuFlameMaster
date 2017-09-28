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

import android.graphics.Canvas;

import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.DanmakuContext.ConfigChangedCallback;
import master.flame.danmaku.danmaku.model.android.DanmakuContext.DanmakuConfigTag;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;
import master.flame.danmaku.danmaku.util.SystemClock;

public class DrawTask implements IDrawTask {

    protected final DanmakuContext mContext;
    
    protected final AbsDisplayer mDisp;

    protected IDanmakus danmakuList;

    protected BaseDanmakuParser mParser;

    TaskListener mTaskListener;

    final IRenderer mRenderer;

    DanmakuTimer mTimer;

    private IDanmakus danmakus = new Danmakus(Danmakus.ST_BY_LIST);

    protected boolean clearRetainerFlag;

    private long mStartRenderTime = 0;

    private final RenderingState mRenderingState = new RenderingState();

    protected boolean mReadyState;

    private long mLastBeginMills;

    private long mLastEndMills;

    protected int mPlayState;

    private boolean mIsHidden;

    private BaseDanmaku mLastDanmaku;

    private Danmakus mLiveDanmakus = new Danmakus(Danmakus.ST_BY_LIST);

    private IDanmakus mRunningDanmakus;

    private boolean mRequestRender;

    private ConfigChangedCallback mConfigChangedCallback = new ConfigChangedCallback() {
        @Override
        public boolean onDanmakuConfigChanged(DanmakuContext config, DanmakuConfigTag tag, Object... values) {
            return DrawTask.this.onDanmakuConfigChanged(config, tag, values);
        }
    };

    public DrawTask(DanmakuTimer timer, DanmakuContext context,
            TaskListener taskListener) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        mContext = context;
        mDisp = context.getDisplayer();
        mTaskListener = taskListener;
        mRenderer = new DanmakuRenderer(context);
        mRenderer.setOnDanmakuShownListener(new IRenderer.OnDanmakuShownListener() {

            @Override
            public void onDanmakuShown(BaseDanmaku danmaku) {
                if (mTaskListener != null) {
                    mTaskListener.onDanmakuShown(danmaku);
                }
            }
        });
        mRenderer.setVerifierEnabled(mContext.isPreventOverlappingEnabled() || mContext.isMaxLinesLimited());
        initTimer(timer);
        Boolean enable = mContext.isDuplicateMergingEnabled();
        if (enable != null) {
            if(enable) {
                mContext.mDanmakuFilters.registerFilter(DanmakuFilters.TAG_DUPLICATE_FILTER);
            } else {
                mContext.mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_DUPLICATE_FILTER);
            }
        }
    }

    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
    }

    @Override
    public synchronized void addDanmaku(BaseDanmaku item) {
        if (danmakuList == null)
            return;
        if (item.isLive) {
            mLiveDanmakus.addItem(item);
            removeUnusedLiveDanmakusIn(10);
        }
        item.index = danmakuList.size();
        boolean subAdded = true;
        if (mLastBeginMills <= item.getActualTime() && item.getActualTime() <= mLastEndMills) {
            synchronized (danmakus) {
                subAdded = danmakus.addItem(item);
            }
        } else if (item.isLive) {
            subAdded = false;
        }
        boolean added = false;
        synchronized (danmakuList) {
            added = danmakuList.addItem(item);
        }
        if (!subAdded || !added) {
            mLastBeginMills = mLastEndMills = 0;
        }
        if (added && mTaskListener != null) {
            mTaskListener.onDanmakuAdd(item);
        }
        if (mLastDanmaku == null || (item != null && mLastDanmaku != null && item.getActualTime() > mLastDanmaku.getActualTime())) {
            mLastDanmaku = item;
        }
    }

    @Override
    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        mContext.getDisplayer().getCacheStuffer().clearCache(item);
        item.requestFlags |= BaseDanmaku.FLAG_REQUEST_INVALIDATE;
        if (remeasure) {
            item.paintWidth = -1;
            item.paintHeight = -1;
            item.requestFlags |= BaseDanmaku.FLAG_REQUEST_REMEASURE;
            item.measureResetFlag++;
        }
    }

    @Override
    public synchronized void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        if (danmakuList == null || danmakuList.isEmpty())
            return;
        synchronized (danmakuList) {
            if (!isClearDanmakusOnScreen) {
                long beginMills = mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100;
                long endMills = mTimer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION;
                IDanmakus tempDanmakus = danmakuList.subnew(beginMills, endMills);
                if (tempDanmakus != null)
                    danmakus = tempDanmakus;
            }
            danmakuList.clear();
        }
    }

    protected void onDanmakuRemoved(BaseDanmaku danmaku) {
        // override by CacheManagingDrawTask
    }

    @Override
    public synchronized void removeAllLiveDanmakus() {
        if (danmakus == null || danmakus.isEmpty())
            return;
        synchronized (danmakus) {
            danmakus.forEachSync(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    if (danmaku.isLive) {
                        onDanmakuRemoved(danmaku);
                        return ACTION_REMOVE;
                    }
                    return ACTION_CONTINUE;
                }
            });
        }
    }

    protected synchronized void removeUnusedLiveDanmakusIn(final int msec) {
        if (danmakuList == null || danmakuList.isEmpty() || mLiveDanmakus.isEmpty())
            return;
        mLiveDanmakus.forEachSync(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
            long startTime = SystemClock.uptimeMillis();

            @Override
            public int accept(BaseDanmaku danmaku) {
                boolean isTimeout = danmaku.isTimeOut();
                if (SystemClock.uptimeMillis() - startTime > msec) {
                    return ACTION_BREAK;
                }
                if (isTimeout) {
                    danmakuList.removeItem(danmaku);
                    onDanmakuRemoved(danmaku);
                    return ACTION_REMOVE;
                } else {
                    return ACTION_BREAK;
                }

            }
        });
    }

    @Override
    public IDanmakus getVisibleDanmakusOnTime(long time) {
        long beginMills = time - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100;
        long endMills = time + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION;
        IDanmakus subDanmakus = null;
        int i = 0;
        while (i++ < 3) {  //avoid ConcurrentModificationException
            try {
                subDanmakus = danmakuList.subnew(beginMills, endMills);
                break;
            } catch (Exception e) {

            }
        }
        final IDanmakus visibleDanmakus = new Danmakus();
        if (null != subDanmakus && !subDanmakus.isEmpty()) {
            subDanmakus.forEachSync(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    if (danmaku.isShown() && !danmaku.isOutside()) {
                        visibleDanmakus.addItem(danmaku);
                    }
                    return ACTION_CONTINUE;
                }
            });
        }

        return visibleDanmakus;
    }

    @Override
    public synchronized RenderingState draw(AbsDisplayer displayer) {
        return drawDanmakus(displayer,mTimer);
    }

    @Override
    public void reset() {
        if (danmakus != null)
            danmakus = new Danmakus();
        if (mRenderer != null)
            mRenderer.clear();
    }

    @Override
    public void seek(long mills) {
        reset();
        mContext.mGlobalFlagValues.updateVisibleFlag();
        mContext.mGlobalFlagValues.updateFirstShownFlag();
        mContext.mGlobalFlagValues.updateSyncOffsetTimeFlag();
        mContext.mGlobalFlagValues.updatePrepareFlag();
        mRunningDanmakus = new Danmakus(Danmakus.ST_BY_LIST);
        mStartRenderTime = mills < 1000 ? 0 : mills;
        mRenderingState.reset();
        mRenderingState.endTime = mStartRenderTime;
        mLastBeginMills = mLastEndMills = 0;

        if (danmakuList != null) {
            BaseDanmaku last = danmakuList.last();
            if (last != null && !last.isTimeOut()) {
                mLastDanmaku = last;
            }
        }
    }

    @Override
    public void clearDanmakusOnScreen(long currMillis) {
        reset();
        mContext.mGlobalFlagValues.updateVisibleFlag();
        mContext.mGlobalFlagValues.updateFirstShownFlag();
        mStartRenderTime = currMillis;
    }

    @Override
    public void start() {
        mContext.registerConfigChangedCallback(mConfigChangedCallback);
    }

    @Override
    public void quit() {
        mContext.unregisterAllConfigChangedCallbacks();
        if (mRenderer != null)
            mRenderer.release();
    }

    public void prepare() {
        if (mParser == null) {
            return;
        }
        loadDanmakus(mParser);
        mLastBeginMills = mLastEndMills = 0;
        if (mTaskListener != null) {
            mTaskListener.ready();
            mReadyState = true;
        }
    }

    @Override
    public void onPlayStateChanged(int state) {
        mPlayState = state;
    }

    protected void loadDanmakus(BaseDanmakuParser parser) {
        danmakuList = parser.setConfig(mContext).setDisplayer(mDisp).setTimer(mTimer).setListener(new BaseDanmakuParser.Listener() {
            @Override
            public void onDanmakuAdd(BaseDanmaku danmaku) {
                if (mTaskListener != null) {
                    mTaskListener.onDanmakuAdd(danmaku);
                }
            }
        }).getDanmakus();
        mContext.mGlobalFlagValues.resetAll();
        if(danmakuList != null) {
            mLastDanmaku = danmakuList.last();
        }
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
        mReadyState = false;
    }

    protected RenderingState drawDanmakus(AbsDisplayer disp, DanmakuTimer timer) {
        if (clearRetainerFlag) {
            mRenderer.clearRetainer();
            clearRetainerFlag = false;
        }
        if (danmakuList != null) {
            Canvas canvas = (Canvas) disp.getExtraData();
            DrawHelper.clearCanvas(canvas);
            if (mIsHidden && !mRequestRender) {
                return mRenderingState;
            }

            mRequestRender = false;
            RenderingState renderingState = mRenderingState;
            // prepare screenDanmakus
            long beginMills = timer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100;
            long endMills = timer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION;
            IDanmakus screenDanmakus = danmakus;
            if(mLastBeginMills > beginMills || timer.currMillisecond > mLastEndMills) {
                screenDanmakus = danmakuList.sub(beginMills, endMills);
                if (screenDanmakus != null) {
                    danmakus = screenDanmakus;
                }
                mLastBeginMills = beginMills;
                mLastEndMills = endMills;
            } else {
                beginMills = mLastBeginMills;
                endMills = mLastEndMills;
            }

            // prepare runningDanmakus to draw (in sync-mode)
            IDanmakus runningDanmakus = mRunningDanmakus;
            beginTracing(renderingState, runningDanmakus, screenDanmakus);
            if (runningDanmakus != null && !runningDanmakus.isEmpty()) {
                mRenderingState.isRunningDanmakus = true;
                mRenderer.draw(disp, runningDanmakus, 0, mRenderingState);
            }

            // draw screenDanmakus
            mRenderingState.isRunningDanmakus = false;
            if (screenDanmakus != null && !screenDanmakus.isEmpty()) {
                mRenderer.draw(mDisp, screenDanmakus, mStartRenderTime, renderingState);
                endTracing(renderingState);
                if (renderingState.nothingRendered) {
                    if(mLastDanmaku != null && mLastDanmaku.isTimeOut()) {
                        mLastDanmaku = null;
                        if (mTaskListener != null) {
                            mTaskListener.onDanmakusDrawingFinished();
                        }
                    }
                    if (renderingState.beginTime == RenderingState.UNKNOWN_TIME) {
                        renderingState.beginTime = beginMills;
                    }
                    if (renderingState.endTime == RenderingState.UNKNOWN_TIME) {
                        renderingState.endTime = endMills;
                    }
                }
                return renderingState;
            } else {
                renderingState.nothingRendered = true;
                renderingState.beginTime = beginMills;
                renderingState.endTime = endMills;
                return renderingState;
            }
        }
        return null;
    }

    @Override
    public void requestClear() {
        mLastBeginMills = mLastEndMills = 0;
        mIsHidden = false;
    }

    @Override
    public void requestClearRetainer() {
        clearRetainerFlag = true;
    }

    @Override
    public void requestSync(long fromTimeMills, long toTimeMills, final long offsetMills) {
        // obtain the running-danmakus which was drawn on screen
        IDanmakus runningDanmakus = mRenderingState.obtainRunningDanmakus();
        mRunningDanmakus = runningDanmakus;
        // set offset time for each running-danmakus
        runningDanmakus.forEachSync(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
            @Override
            public int accept(BaseDanmaku danmaku) {
                if (danmaku.isOutside()) {
                    return ACTION_REMOVE;
                }
                danmaku.setTimeOffset(offsetMills + danmaku.timeOffset);
                if (danmaku.timeOffset == 0) {
                    return ACTION_REMOVE;
                }
                return ACTION_CONTINUE;
            }
        });
        mStartRenderTime = toTimeMills;
    }

    public boolean onDanmakuConfigChanged(DanmakuContext config, DanmakuConfigTag tag,
            Object... values) {
        boolean handled = handleOnDanmakuConfigChanged(config, tag, values);
        if (mTaskListener != null) {
            mTaskListener.onDanmakuConfigChanged();
        }
        return handled;
    }

    protected boolean handleOnDanmakuConfigChanged(DanmakuContext config, DanmakuConfigTag tag, Object[] values) {
        boolean handled = false;
        if (tag == null || DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN.equals(tag)) {
            handled = true;
        } else if (DanmakuConfigTag.DUPLICATE_MERGING_ENABLED.equals(tag)) {
            Boolean enable = (Boolean) values[0];
            if (enable != null) {
                if (enable) {
                    mContext.mDanmakuFilters.registerFilter(DanmakuFilters.TAG_DUPLICATE_FILTER);
                } else {
                    mContext.mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_DUPLICATE_FILTER);
                }
                handled = true;
            }
        } else if (DanmakuConfigTag.SCALE_TEXTSIZE.equals(tag) || DanmakuConfigTag.SCROLL_SPEED_FACTOR.equals(tag) || DanmakuConfigTag.DANMAKU_MARGIN.equals(tag)) {
            requestClearRetainer();
            handled = false;
        } else if (DanmakuConfigTag.MAXIMUN_LINES.equals(tag) || DanmakuConfigTag.OVERLAPPING_ENABLE.equals(tag)) {
            if (mRenderer != null) {
                mRenderer.setVerifierEnabled(mContext.isPreventOverlappingEnabled() || mContext.isMaxLinesLimited());
            }
            handled = true;
        } else if (DanmakuConfigTag.ALIGN_BOTTOM.equals(tag)) {
            Boolean enable = (Boolean) values[0];
            if (enable != null) {
                if (mRenderer != null) {
                    mRenderer.alignBottom(enable);
                }
                handled = true;
            }
        }
        return handled;
    }

    @Override
    public void requestHide() {
        mIsHidden = true;
    }

    @Override
    public void requestRender() {
        this.mRequestRender = true;
    }

    private void beginTracing(RenderingState renderingState, IDanmakus runningDanmakus, IDanmakus screenDanmakus) {
        renderingState.reset();
        renderingState.timer.update(SystemClock.uptimeMillis());
        renderingState.indexInScreen = 0;
        renderingState.totalSizeInScreen = (runningDanmakus != null ? runningDanmakus.size() : 0) + (screenDanmakus != null ? screenDanmakus.size() : 0);
    }

    private void endTracing(RenderingState renderingState) {
        renderingState.nothingRendered = (renderingState.totalDanmakuCount == 0);
        if (renderingState.nothingRendered) {
            renderingState.beginTime = RenderingState.UNKNOWN_TIME;
        }
        BaseDanmaku lastDanmaku = renderingState.lastDanmaku;
        renderingState.lastDanmaku = null;
        renderingState.endTime = lastDanmaku != null ? lastDanmaku.getActualTime() : RenderingState.UNKNOWN_TIME;
        renderingState.consumingTime = renderingState.timer.update(SystemClock.uptimeMillis());
    }
}
