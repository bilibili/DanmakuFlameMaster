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

package tv.light.danmaku.util;

import tv.light.danmaku.model.BaseDanmaku;
import tv.light.danmaku.model.IDisplayer;

import android.graphics.RectF;

public class DanmakuUtils {

    /**
     * 检测两个弹幕是否会碰撞
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static boolean willHitInDuration(IDisplayer disp, BaseDanmaku d1, BaseDanmaku d2,
            long duration) {
        if (d1.getType() != d2.getType())
            return false;
        if (Math.abs(d2.time - d1.time) >= duration)
            return false;
        float[] rectArr1 = d1.getRectAtTime(disp, d1.time + duration);
        float[] rectArr2 = d2.getRectAtTime(disp, d2.time + duration);
        if (rectArr1 == null || rectArr2 == null)
            return false;
        RectF rect1 = new RectF(rectArr1[0], rectArr1[1], rectArr1[2], rectArr1[3]);
        RectF rect2 = new RectF(rectArr2[0], rectArr2[1], rectArr2[2], rectArr2[3]);
        if (RectF.intersects(rect1, rect2)) {
            return true;
        }
        if (d1.getType() == BaseDanmaku.TYPE_SCROLL_RL
                && d2.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
            if (rect2.left < rect1.right) {
                return true;
            }
        }
        if (d1.getType() == BaseDanmaku.TYPE_SCROLL_LR
                && d2.getType() == BaseDanmaku.TYPE_SCROLL_LR) {
            if (rect2.right > rect1.left) {
                return true;
            }
        }

        // TODO: more type

        return false;
    }
}
