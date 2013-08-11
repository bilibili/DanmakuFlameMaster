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

package tv.light.controller;

import android.content.Context;
import android.graphics.Canvas;
import tv.light.danmaku.loader.android.BiliDanmakuLoader;
import tv.light.danmaku.loader.android.DanmakuLoaderFactory;
import tv.light.danmaku.model.DanmakuTimer;
import tv.light.danmaku.model.IDanmakus;
import tv.light.danmaku.model.android.AndroidDisplayer;
import tv.light.danmaku.model.android.Danmakus;
import tv.light.danmaku.parser.BiliDanmakuFactory;
import tv.light.danmaku.parser.IDataSource;
import tv.light.danmaku.parser.android.BiliDanmukuParse;
import tv.light.danmaku.renderer.IRenderer;
import tv.light.danmaku.renderer.android.DanmakuRenderer;
import tv.light.surfaceviewtest.R;

import java.io.InputStream;

public class DrawTask {

    private static final long DURATION = 4000;

    private final AndroidDisplayer disp;

    Context mContext;

    IRenderer mRenderer;

    DanmakuTimer mTimer;

    InputStream stream;

    private BiliDanmakuLoader mLoader;

    private IDataSource dataSource;

    private BiliDanmukuParse mParser;

    private Danmakus danmakuList;

    private IDanmakus danmakus;

    public DrawTask(DanmakuTimer timer, Context context, int dispW, int dispH) {
        this.mTimer = timer;
        mContext = context;
        mRenderer = new DanmakuRenderer();
        disp = new AndroidDisplayer();
        disp.width = dispW;
        disp.height = dispH;
        disp.density = context.getResources().getDisplayMetrics().density;

        loadBiliDanmakus(context.getResources().openRawResource(R.raw.comments));
    }

    private void loadBiliDanmakus(InputStream stream) {
        mLoader = (BiliDanmakuLoader) DanmakuLoaderFactory.create("bili");
        mLoader.load(stream);
        dataSource = mLoader.getDataSource();
        mParser = new BiliDanmukuParse(disp);

        mParser.load(dataSource);
        danmakuList = mParser.parse(mTimer);
    }

    public void draw(Canvas canvas) {
        if (danmakuList != null) {
            long currMills = mTimer.currMillisecond;
            // if(danmakus==null)
            danmakus = danmakuList.sub(currMills - BiliDanmakuFactory.REAL_DANMAKU_DURATION,
                    currMills);
            if (danmakus != null) {
                disp.update(canvas);
                mRenderer.draw(disp, danmakus);
            }
        }
    }

}
