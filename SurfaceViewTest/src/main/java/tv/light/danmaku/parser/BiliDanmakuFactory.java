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

package tv.light.danmaku.parser;

import tv.light.danmaku.model.DanmakuBase;
import tv.light.danmaku.model.R2LDanmaku;

public class BiliDanmakuFactory {

    public static float BILI_PLAYER_WIDTH = 539;

    public static float BILI_PLAYER_HEIGHT = 385;

    public static long COMMON_DANMAKU_DURATION = 4000;

    public static DanmakuBase createDanmaku(int type, float dispWidth) {
        DanmakuBase instance = null;
        if (type == 1) {
            instance = new R2LDanmaku(
                    (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH)));
        }
        // TODO: more Danmaku type

        return instance;
    }

    public static void updateDanmakuDuration(DanmakuBase danmaku, float dispWidth) {
        danmaku.duration = (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH));
    }

}
