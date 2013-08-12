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

package master.flame.danmaku.danmaku.parser;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.FBDanmaku;
import master.flame.danmaku.danmaku.model.FTDanmaku;
import master.flame.danmaku.danmaku.model.R2LDanmaku;

public class DanmakuFactory {

    public static float BILI_PLAYER_WIDTH = 539;

    public static float BILI_PLAYER_HEIGHT = 385;

    public static long COMMON_DANMAKU_DURATION = 3500; //B站原始分辨率下弹幕存活时间

    public static long REAL_DANMAKU_DURATION = -1;

    public static long MAX_DANMAKU_DURATION = -1;

    public static BaseDanmaku createDanmaku(int type, float dispWidth) {
        if (REAL_DANMAKU_DURATION == -1)
            REAL_DANMAKU_DURATION = (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH));
        if (MAX_DANMAKU_DURATION == -1) {
            MAX_DANMAKU_DURATION = Math.max(REAL_DANMAKU_DURATION, COMMON_DANMAKU_DURATION);
        }
        BaseDanmaku instance = null;
        switch (type) {
            case 1: // 从右往左滚动
                instance = new R2LDanmaku(REAL_DANMAKU_DURATION);
                break;
            case 4: // 底端固定
                instance = new FBDanmaku(COMMON_DANMAKU_DURATION);
                break;
            case 5: // 顶端固定
                instance = new FTDanmaku(COMMON_DANMAKU_DURATION);
                break;
            // TODO: more Danmaku type
        }
        return instance;
    }

    public static void updateDanmakuDuration(BaseDanmaku danmaku, float dispWidth) {
        danmaku.duration = (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH));
    }

}