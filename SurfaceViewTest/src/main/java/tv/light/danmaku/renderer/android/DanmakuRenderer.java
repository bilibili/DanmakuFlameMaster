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

package tv.light.danmaku.renderer.android;

import tv.light.danmaku.model.BaseDanmaku;
import tv.light.danmaku.model.IDanmakus;
import tv.light.danmaku.model.IDisplayer;
import tv.light.danmaku.model.android.Danmakus;
import tv.light.danmaku.renderer.Renderer;

import java.util.Iterator;

public class DanmakuRenderer extends Renderer {

    public Danmakus mLRDanmakus = new Danmakus(Danmakus.ST_BY_YPOS);

    @Override
    public void draw(IDisplayer disp, IDanmakus danmakus) {
        Danmakus drawItems = (Danmakus) danmakus;
        Iterator<BaseDanmaku> itr = drawItems.iterator();
        int index = 0;
        while (itr.hasNext()) {
            BaseDanmaku drawItem = itr.next();

            // 测量弹幕尺寸(measure)
            if (!drawItem.isMeasured()) {
                drawItem.measure(disp);
            }

            // 确定弹幕位置(layout)
            DanmakusRetainer.fix(drawItem, disp);

            // 绘制弹幕(draw)
            if (drawItem.isShown()) {
                drawItem.draw(disp);
                //break;
            }

        }
    }
}
