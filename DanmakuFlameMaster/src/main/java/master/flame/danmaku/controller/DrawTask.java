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
import android.util.DisplayMetrics;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.GlobalFlagValues;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;
import master.flame.danmaku.danmaku.util.AndroidCounter;

public class DrawTask implements IDrawTask {

    protected AndroidDisplayer mDisp;

    protected Danmakus danmakuList;

    protected BaseDanmakuParser mParser;

    TaskListener mTaskListener;

    Context mContext;

    IRenderer mRenderer;

    DanmakuTimer mTimer;

    AndroidCounter mCounter;

    private IDanmakus danmakus;

    private int clearFlag;

    private long mStartRenderTime = 0;

    public DrawTask(DanmakuTimer timer, Context context, int dispW, int dispH,
            TaskListener taskListener) {
        mTaskListener = taskListener;
        mCounter = new AndroidCounter();
        mContext = context;
        mRenderer = new DanmakuRenderer();
        mDisp = new AndroidDisplayer();
        mDisp.width = dispW;
        mDisp.height = dispH;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDisp.density = displayMetrics.density;
        mDisp.densityDpi = displayMetrics.densityDpi;
        mDisp.scaledDensity = displayMetrics.scaledDensity;
        mDisp.slopPixel = (int) (Math.max(displayMetrics.density,displayMetrics.scaledDensity) * 18);
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
    public void draw(Canvas canvas) {
        drawDanmakus(canvas, mTimer);
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
        clearFlag = 5;
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
        danmakuList = parser.setDisp(mDisp).setTimer(mTimer).parse();
        GlobalFlagValues.resetAll();
    }

    public void setParser(BaseDanmakuParser parser) {
        mParser = parser;
    }

    protected void drawDanmakus(Canvas canvas, DanmakuTimer timer) {
        if (danmakuList != null) {
            if (clearFlag > 0) {
                DrawHelper.clearCanvas(canvas);
                clearFlag--;
            } else {
                int[] refreshRect = mRenderer.getRefreshArea().mRefreshRect;
                DrawHelper.clearCanvas(canvas, Math.max(0, refreshRect[0] - mDisp.slopPixel),
                        Math.max(0, refreshRect[1] - mDisp.slopPixel),
                        Math.min(canvas.getWidth(), refreshRect[2] + mDisp.slopPixel),
                        Math.min(canvas.getHeight(), refreshRect[3] + mDisp.slopPixel));
            }
            long currMills = timer.currMillisecond;
            danmakus = danmakuList.sub(currMills - DanmakuFactory.MAX_DANMAKU_DURATION - 100,
                    currMills);
            if (danmakus != null) {
                mDisp.update(canvas);
                mRenderer.draw(mDisp, danmakus, mStartRenderTime);
            }
        }
    }

}
