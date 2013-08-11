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

package master.flame.danmaku.danmaku.renderer.android;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.renderer.Renderer;

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

            // measure
            if (!drawItem.isMeasured()) {
                drawItem.measure(disp);
            }

            DanmakusRetainer.fix(drawItem, disp);

            // draw
            if (drawItem.isShown()) {
                drawItem.draw(disp);
                //break;
            }

        }
    }
}
