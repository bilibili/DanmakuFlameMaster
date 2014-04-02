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
        if(d1.isOutside()){
            return false;
        }
        long dTime = d2.time - d1.time;
        if (dTime < 0)
            return true;
        if (Math.abs(dTime) >= duration || d1.isTimeOut() || d2.isTimeOut()) {
            return false;
        }

        if (d1.getType() == BaseDanmaku.TYPE_FIX_TOP || d1.getType() == BaseDanmaku.TYPE_FIX_BOTTOM) {
            return true;
        }

        float[] rectArr1 = d1.getRectAtTime(disp, currTime);
        float[] rectArr2 = d2.getRectAtTime(disp, currTime);
        if (rectArr1 == null || rectArr2 == null)
            return false;
        if (checkHit(d1, d2, rectArr1, rectArr2)) {
            return true;
        }

        long time = d1.time + d1.getDuration();
        rectArr1 = d1.getRectAtTime(disp, time);
        rectArr2 = d2.getRectAtTime(disp, time);
        if (rectArr1 == null || rectArr2 == null)
            return false;
        if (checkHit(d1, d2, rectArr1, rectArr2)) {
            return true;
        }

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

        cache.build((int) danmaku.paintWidth, (int) danmaku.paintHeight, disp.getDensityDpi(), false);
        DrawingCacheHolder holder = cache.get();
        if (holder != null) {
            AndroidDisplayer.drawDanmaku(danmaku, holder.canvas, 0, 0, false);
        }
        return cache;
    }

    public static int getCacheSize(int w, int h) {
        return (w) * (h) * 4;
    }

    public static int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
        if (obj1 == obj2) {
            return 0;
        }

        if (obj1 == null) {
            return -1;
        }

        if (obj2 == null) {
            return 1;
        }

        long val = obj1.time - obj2.time;
        if (val > 0) {
            return 1;
        } else if (val < 0) {
            return -1;
        }
        
        int result = obj1.index - obj2.index;
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }

        result = obj1.getType() - obj2.getType();
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }

        if (obj1.text == null) {
            return -1;
        }
        if (obj2.text == null) {
            return 1;
        }

        int r = obj1.text.compareTo(obj2.text);
        if (r != 0) {
            return r;
        }

        r = obj1.textColor - obj2.textColor;
        if (r != 0)
            return r < 0 ? -1 : 1;

        r = obj1.index - obj2.index;
        if (r != 0)
            return r < 0 ? -1 : 1;

        r = obj1.hashCode() - obj1.hashCode();
        return r;
    }

}
