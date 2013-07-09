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

import tv.light.danmaku.model.DanmakuBase;
import tv.light.danmaku.model.IDanmakus;
import tv.light.danmaku.model.IDisplayer;
import tv.light.danmaku.model.android.Danmakus;
import tv.light.danmaku.renderer.Renderer;

import java.util.Iterator;

public class DanmakuRenderer extends Renderer {

    @Override
    public void draw(IDisplayer disp, IDanmakus danmakus) {
        Danmakus drawItems = (Danmakus) danmakus;
        Iterator<DanmakuBase> itr = drawItems.items.iterator();
        while (itr.hasNext()) {
            DanmakuBase drawItem = itr.next();
            // measure
            if (!drawItem.isMeasured()) {
                drawItem.measure(disp);
            }
            // layout
            drawItem.layout(disp, 0, 100);
            // draw
            drawItem.draw(disp);
            // break;
        }
    }
}
