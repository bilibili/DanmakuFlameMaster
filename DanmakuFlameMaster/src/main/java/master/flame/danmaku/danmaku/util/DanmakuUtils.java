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

package master.flame.danmaku.danmaku.util;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCacheHolder;

public class DanmakuUtils {

    /**
     * 检测两个弹幕是否会碰撞
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static boolean willHitInDuration(IDisplayer disp, BaseDanmaku d1, BaseDanmaku d2,
            long duration, long currTime) {
        if (d1.getType() != d2.getType())
            return false;
        if (Math.abs(d2.time - d1.time) >= duration)
            return false;

        if (d1.getType() == BaseDanmaku.TYPE_FIX_TOP || d1.getType() == BaseDanmaku.TYPE_FIX_BOTTOM) {
            return true;
        }

        if (d1.isTimeOut() || d2.isTimeOut()) {
            return false;
        }

        float[] rectArr1 = d1.getRectAtTime(disp, currTime);
        float[] rectArr2 = d2.getRectAtTime(disp, currTime);
        if (rectArr1 == null || rectArr2 == null)
            return false;
        if (checkHit(d1, d2, rectArr1, rectArr2)) {
            return true;
        }

        long time = currTime + duration;
        rectArr1 = d1.getRectAtTime(disp, time);
        rectArr2 = d2.getRectAtTime(disp, time);
        if (rectArr1 == null || rectArr2 == null)
            return false;
        checkHit(d1, d2, rectArr1, rectArr2);

        return false;
    }

    private static boolean checkHit(BaseDanmaku d1, BaseDanmaku d2, float[] rectArr1,
            float[] rectArr2) {

        if (d1.getType() == BaseDanmaku.TYPE_SCROLL_RL
                && d2.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
            if (rectArr2[0] < rectArr1[2]) {
                return true;
            }
        } else if (d1.getType() == BaseDanmaku.TYPE_SCROLL_LR
                && d2.getType() == BaseDanmaku.TYPE_SCROLL_LR) {
            if (rectArr2[2] > rectArr1[0]) {
                return true;
            }
        }
        return false;

    }

    public static DrawingCache buildDanmakuDrawingCache(BaseDanmaku danmaku, IDisplayer disp,
            DrawingCache cache) {
        if (cache == null)
            cache = new DrawingCache();
        cache.build((int) danmaku.paintWidth + 2, (int) danmaku.paintHeight + 2, disp.getDensityDpi());
        DrawingCacheHolder holder = cache.get();
        if (holder != null) {
            AndroidDisplayer.drawDanmaku(danmaku, holder.canvas, 0, 0, false);
        }
        return cache;
    }
}
