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
import master.flame.danmaku.danmaku.loader.android.BiliDanmakuLoader;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BiliDanmakuFactory;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParse;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.android.DanmakuRenderer;
import master.flame.danmaku.activity.R;

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
