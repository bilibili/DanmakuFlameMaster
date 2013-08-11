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

import android.content.Context;
import android.graphics.Canvas;

import java.io.InputStream;

public class DrawTask {

    private static final long DURATION = 4000;

    private final AndroidDisplayer disp;

    Context context;

    IRenderer renderer;

    DanmakuTimer timer;

    InputStream stream;

    private BiliDanmakuLoader loader;

    private IDataSource dataSource;

    private BiliDanmukuParse parser;

    private Danmakus danmakuList;

    private IDanmakus danmakus;

    public DrawTask(DanmakuTimer timer, Context context, int dispW, int dispH) {
        this.timer = timer;
        renderer = new DanmakuRenderer();
        disp = new AndroidDisplayer();
        disp.width = dispW;
        disp.height = dispH;
        loadBiliDanmakus(context.getResources().openRawResource(R.raw.comments));
    }

    private void loadBiliDanmakus(InputStream stream) {
        loader = (BiliDanmakuLoader) DanmakuLoaderFactory.create("bili");
        loader.load(stream);
        dataSource = loader.getDataSource();
        parser = new BiliDanmukuParse(disp.width);
        parser.load(dataSource);
        danmakuList = parser.parse(timer);
    }

    public void draw(Canvas canvas) {
        if (danmakuList != null) {
            long currMills = timer.currMillisecond;
            // if(danmakus==null)
            danmakus = danmakuList.sub(currMills - BiliDanmakuFactory.REAL_DANMAKU_DURATION,
                    currMills);
            if (danmakus != null) {
                disp.update(canvas);
                renderer.draw(disp, danmakus);
            }
        }
    }

}
