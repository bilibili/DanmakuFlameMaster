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

import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.GlobalFlagValues;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.objectpool.Pool;
import master.flame.danmaku.danmaku.model.objectpool.Poolable;
import master.flame.danmaku.danmaku.model.objectpool.PoolableManager;
import master.flame.danmaku.danmaku.model.objectpool.Pools;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;
import master.flame.danmaku.danmaku.util.AndroidCounter;

import java.util.LinkedList;

public class DrawTask implements IDrawTask {
    
    private static class RectPoolableCache implements Poolable<RectPoolableCache> {

        private RectPoolableCache mNextElement;
        private boolean mIsPooled;

        public float[] mRect;

        public RectPoolableCache(float[] rect) {
            mRect = rect;
        }

        @Override
        public void setNextPoolable(RectPoolableCache element) {
            mNextElement = element;
        }

        @Override
        public RectPoolableCache getNextPoolable() {
            return mNextElement;
        }

        @Override
        public boolean isPooled() {
            return mIsPooled;
        }

        @Override
        public void setPooled(boolean isPooled) {
            mIsPooled = isPooled;
        }

        public void setRect(float[] rect) {
            for (int j = 0; j < mRect.length; j++) {
                mRect[j] = 0;
            }
            for (int i = 0; i < mRect.length && i < rect.length; i++) {
                mRect[i] = rect[i];
            }
        }

    }

    public class RectCache {

        private int mCapity;
        private Pool<RectPoolableCache> mRectsCachePool;
        private PoolableManager<RectPoolableCache> manager = new PoolableManager<DrawTask.RectPoolableCache>() {

            @Override
            public void onReleased(RectPoolableCache element) {

            }

            @Override
            public void onAcquired(RectPoolableCache element) {

            }

            @Override
            public RectPoolableCache newInstance() {
                return null;
            }
        };
        private LinkedList<RectPoolableCache> mRects = new LinkedList<RectPoolableCache>();
        private float[] mRect = new float[4];

        public RectCache(int capity) {
            mCapity = capity;
            mRectsCachePool = Pools.finitePool(manager, mCapity);
            for (int i = 0; i < mCapity; i++) {
                mRectsCachePool.release(new RectPoolableCache(new float[4]));
            }
        }

        public void push(float[] rect) {
            if (mRects.size() >= mCapity) {
                RectPoolableCache rc = mRects.removeFirst();
                mRectsCachePool.release(rc);
            }
            RectPoolableCache rc = mRectsCachePool.acquire();
            if (rc != null) {
                rc.setRect(rect);
                mRects.add(rc);
            }
        }

        private void resetRect() {
            mRect[0] = Integer.MAX_VALUE;
            mRect[1] = Integer.MAX_VALUE;
            mRect[2] = Integer.MIN_VALUE;
            mRect[3] = Integer.MIN_VALUE;
        }

        public float[] getRect() {
            resetRect();
            for (RectPoolableCache rc : mRects) {
                mRect[0] = Math.min(mRect[0], rc.mRect[0]);
                mRect[1] = Math.min(mRect[1], rc.mRect[1]);
                mRect[2] = Math.max(mRect[2], rc.mRect[2]);
                mRect[3] = Math.max(mRect[3], rc.mRect[3]);
            }
            return mRect;
        }
    }

    protected AbsDisplayer<?> mDisp;

    protected IDanmakus danmakuList;

    protected BaseDanmakuParser mParser;

    TaskListener mTaskListener;

    Context mContext;

    IRenderer mRenderer;

    DanmakuTimer mTimer;

    AndroidCounter mCounter;

    private IDanmakus danmakus;

    protected int clearFlag;

    private long mStartRenderTime = 0;
    
    RectCache mRectCache = new RectCache(3);

    public DrawTask(DanmakuTimer timer, Context context, AbsDisplayer<?> disp,
            TaskListener taskListener) {
        mTaskListener = taskListener;
        mCounter = new AndroidCounter();
        mContext = context;
        mRenderer = new DanmakuRenderer();
        mDisp = disp;
        initTimer(timer);
    }

    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
    }

    @Override
    public void addDanmaku(BaseDanmaku item) {
        if(danmakuList == null)
            return;
        synchronized (danmakuList){
            item.setTimer(mTimer);
            item.index = danmakuList.size();
            danmakuList.addItem(item);
        }
    }

    @Override
    public void draw(AbsDisplayer<?> displayer) {
        drawDanmakus(displayer,mTimer);
    }

    @Override
    public void reset() {
        if (danmakus != null)
            danmakus.clear();
        if (mRenderer != null)
            mRenderer.clear();
    }

    @Override
    public void seek(long mills) {
        reset();
        requestClear();
        GlobalFlagValues.updateVisibleFlag();
        mStartRenderTime = mills < 1000 ? 0 : mills;
    }

    @Override
    public void start() {

    }

    @Override
    public void quit() {
        if (mRenderer != null)
            mRenderer.release();
    }

    public void prepare() {
        assert (mParser != null);
        loadDanmakus(mParser);
        if (mTaskListener != null) {
            mTaskListener.ready();
        }
    }

    protected void loadDanmakus(BaseDanmakuParser parser) {
        danmakuList = parser.setDisplayer(mDisp).setTimer(mTimer).getDanmakus();
        GlobalFlagValues.resetAll();
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
    }

    protected void drawDanmakus(AbsDisplayer<?> disp, DanmakuTimer timer) {
        if (danmakuList != null) {
            Canvas canvas = (Canvas) disp.getExtraData();
            if (clearFlag > 0) {
                DrawHelper.clearCanvas(canvas);
                clearFlag--;
            } else {
                float[] refreshRect = mRenderer.getRefreshArea().mRefreshRect;
                mRectCache.push(refreshRect);
                float[] rect = mRectCache.getRect();
                DrawHelper.clearCanvas(canvas, Math.max(0, rect[0]), Math.max(0, rect[1]),
                        Math.min(disp.getWidth(), rect[2]), Math.min(disp.getHeight(), rect[3]));
            }
            long currMills = timer.currMillisecond;
            danmakus = danmakuList.sub(currMills - DanmakuFactory.MAX_DANMAKU_DURATION - 100,
                    currMills);
            if (danmakus != null) {
                mRenderer.draw(mDisp, danmakus, mStartRenderTime);
            }
        }
    }

    public void requestClear() {
        clearFlag = 5;
    }

}
