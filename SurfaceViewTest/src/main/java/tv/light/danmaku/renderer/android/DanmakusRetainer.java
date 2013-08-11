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
import tv.light.danmaku.model.IDisplayer;
import tv.light.danmaku.model.android.Danmakus;
import tv.light.danmaku.util.DanmakuUtils;

import java.util.Iterator;

public class DanmakusRetainer {

    private static IDanmakusRetainer rldrInstance = null;

    private static IDanmakusRetainer ftdrInstance = null;

    private static IDanmakusRetainer fbdrInstance = null;

    public static void fix(BaseDanmaku danmaku, IDisplayer disp) {

        int type = danmaku.getType();
        switch (type) {
            case BaseDanmaku.TYPE_SCROLL_RL:
                if (rldrInstance == null) {
                    rldrInstance = new RLDanmakusRetainer();
                }
                rldrInstance.fix(danmaku, disp);
                break;
            case BaseDanmaku.TYPE_FIX_TOP:

                break;
            case BaseDanmaku.TYPE_FIX_BOTTOM:

                break;
        }

    }

    public interface IDanmakusRetainer {
        public void fix(BaseDanmaku danmaku, IDisplayer disp);
    }

    private static class RLDanmakusRetainer implements IDanmakusRetainer {

        private Danmakus mLRDanmakus = new Danmakus();

        @Override
        public void fix(BaseDanmaku danmaku, IDisplayer disp) {

            float topPos = 0;
            boolean shown = danmaku.isShown();
            if (!shown) {
                // 确定弹幕位置
                Iterator<BaseDanmaku> it = mLRDanmakus.iterator();
                BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null;
                boolean overwriteInsert = false;
                while (it.hasNext()) {
                    BaseDanmaku item = it.next();
                    if (item.getTop() == 0)
                        firstItem = item;
                    lastItem = item;

                    if (danmaku.paintHeight + item.getTop() > disp.getHeight()) {
                        overwriteInsert = true;
                        break;
                    }
                    if (minRightRow == null) {
                        minRightRow = item;
                    } else {
                        if (minRightRow.getRight() >= item.getRight()) {
                            minRightRow = item;
                        }
                    }

                    // if(item.isOutside()){
                    // insertItem = item;
                    // break;
                    // }

                    // 检查碰撞
                    boolean willHit = DanmakuUtils.willHitInDuration(disp, item, danmaku,
                            danmaku.getDuration());
                    if (!willHit) {
                        insertItem = item;
                        break;
                    }

                }

                if (insertItem != null) {
                    topPos = insertItem.getTop();
                    mLRDanmakus.removeItem(insertItem);
                } else if (overwriteInsert) {
                    if (minRightRow != null) {
                        topPos = minRightRow.getTop();
                        mLRDanmakus.removeItem(minRightRow);
                    }
                } else if (lastItem != null && insertItem == null) {
                    topPos = lastItem.getBottom();
                } else if (topPos == 0 && firstItem != null) {
                    topPos = firstItem.getTop();
                    mLRDanmakus.removeItem(firstItem);
                } else if (firstItem == null) {
                    topPos = 0;
                }
            }

            danmaku.layout(disp, 0, topPos);

            if (!shown) {
                mLRDanmakus.addItem(danmaku);
            }

        }

    }

}
