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
import android.util.Log;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.AcFunDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParse;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;
import master.flame.danmaku.danmaku.util.AndroidCounter;

import java.io.IOException;
import java.io.InputStream;

public class DrawTask implements IDrawTask {

    private static final String TAG = "DrawTask";

    private final int DEBUG_OPTION = 1;

    protected AndroidDisplayer mDisp;

    TaskListener mTaskListener;

    Context mContext;

    IRenderer mRenderer;

    DanmakuTimer mTimer;

    AndroidCounter mCounter;

    private ILoader mLoader;

    private IDataSource dataSource;

    private BaseDanmakuParser mParser;

    private Danmakus danmakuList;

    private IDanmakus danmakus;

    public DrawTask(DanmakuTimer timer, Context context, int dispW, int dispH, TaskListener taskListener) {
        setTaskListener(taskListener);
        mCounter = new AndroidCounter();
        mContext = context;
        mRenderer = new DanmakuRenderer();
        mDisp = new AndroidDisplayer();
        mDisp.width = dispW;
        mDisp.height = dispH;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mDisp.density = displayMetrics.density;
        mDisp.scaledDensity = displayMetrics.scaledDensity;
        initTimer(timer);
        loadDanmakus(context, mTimer);


    }

    protected void loadDanmakus(Context context, DanmakuTimer timer) {
        try {
            if (DEBUG_OPTION == 0) {
                loadAcDanmakus(context.getAssets().open("comment.json"), timer);
            } else {
                loadBiliDanmakus(
                        context.getResources().openRawResource(
                                master.flame.danmaku.activity.R.raw.comments), timer);
            }
            Thread.sleep(2000);
            if (mTaskListener != null) {
                mTaskListener.ready();
            }
        } catch (IOException e) {
            Log.e(TAG, "open assets error", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadAcDanmakus(InputStream stream, DanmakuTimer timer) {
        mLoader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN);
        try {
            mLoader.load(stream);
            dataSource = mLoader.getDataSource();
            mParser = new AcFunDanmakuParser(mDisp);
            danmakuList = mParser.load(dataSource).setTimer(timer).parse();
        } catch (IllegalDataException e) {
            Log.e(TAG, "load error", e);
        }
    }

    private void loadBiliDanmakus(InputStream stream, DanmakuTimer timer) {
        mLoader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);
        try {
            mLoader.load(stream);
            dataSource = mLoader.getDataSource();
            mParser = new BiliDanmukuParse(mDisp);
            danmakuList = mParser.load(dataSource).setTimer(timer).parse();
        } catch (IllegalDataException e) {
            Log.e(TAG, "load error", e);
        }
    }

    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
    }

    public void setTaskListener(TaskListener taskListener) {
        mTaskListener = taskListener;
    }

    @Override
    public void draw(Canvas canvas) {
        mCounter.begin();
        drawDanmakus(canvas, mTimer);
        mCounter.end().log("drawing");
    }

    protected void drawDanmakus(Canvas canvas, DanmakuTimer timer) {
        if (danmakuList != null) {
            long currMills = timer.currMillisecond;
            danmakus = danmakuList.sub(currMills - DanmakuFactory.MAX_DANMAKU_DURATION, currMills);
            if (danmakus != null) {
                mDisp.update(canvas);
                mRenderer.draw(mDisp, danmakus);
            }
        }
    }

}
