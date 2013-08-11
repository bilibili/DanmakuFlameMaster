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
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

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
                if (ftdrInstance == null) {
                    ftdrInstance = new FTDanmakusRetainer();
                }
                ftdrInstance.fix(danmaku, disp);
                break;
            case BaseDanmaku.TYPE_FIX_BOTTOM:
                if (fbdrInstance == null) {
                    fbdrInstance = new FBDanmakusRetainer();
                }
                fbdrInstance.fix(danmaku, disp);
                break;
        }

    }

    public interface IDanmakusRetainer {
        public void fix(BaseDanmaku drawItem, IDisplayer disp);
    }

    private static class RLDanmakusRetainer implements IDanmakusRetainer {

        protected Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS);

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp) {
            if (drawItem.isOutside())
                return;
            float topPos = 0;
            boolean shown = drawItem.isShown();
            if (!shown) {
                // 确定弹幕位置
                Iterator<BaseDanmaku> it = mVisibleDanmakus.iterator();
                BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null;
                boolean overwriteInsert = false;
                while (it.hasNext()) {
                    BaseDanmaku item = it.next();
                    if (firstItem == null)
                        firstItem = item;
                    lastItem = item;

                    if (drawItem.paintHeight + item.getTop() > disp.getHeight()) {
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

                    // 检查碰撞
                    boolean willHit = DanmakuUtils.willHitInDuration(disp, item, drawItem,
                            drawItem.getDuration());
                    if (!willHit) {
                        insertItem = item;
                        break;
                    }

                }

                if (insertItem != null) {
                    topPos = insertItem.getTop();
                    mVisibleDanmakus.removeItem(insertItem);
                } else if (overwriteInsert) {
                    if (minRightRow != null) {
                        topPos = minRightRow.getTop();
                        mVisibleDanmakus.removeItem(minRightRow);
                    }
                } else if (lastItem != null && insertItem == null) {
                    topPos = lastItem.getBottom();// checkVerticalEdge(drawItem,
                    // disp, topPos, firstItem,
                    // lastItem);
                } else if (topPos == 0 && firstItem != null) {
                    topPos = firstItem.getTop();
                    mVisibleDanmakus.removeItem(firstItem);
                } else if (firstItem == null) {
                    topPos = 0;
                }

                topPos = checkVerticalEdge(overwriteInsert, drawItem, disp, topPos, firstItem,
                        lastItem);
            }

            // layout
            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                          IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            return topPos;
        }

    }

    private static class FTDanmakusRetainer extends RLDanmakusRetainer {

        @Override
        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                          IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos + drawItem.paintHeight > disp.getHeight()) {
                topPos = 0;
                mVisibleDanmakus.clear();
            }
            return topPos;
        }

    }

    private static class FBDanmakusRetainer implements IDanmakusRetainer {

        private Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS_DESC);

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp) {
            if (drawItem.isOutside())
                return;
            float topPos = disp.getHeight() - drawItem.paintHeight;
            boolean shown = drawItem.isShown();
            if (!shown) {
                // 确定弹幕位置
                Iterator<BaseDanmaku> it = mVisibleDanmakus.iterator();
                BaseDanmaku insertItem = null, firstItem = null, lastItem = null;
                boolean overwriteInsert = false;
                while (it.hasNext()) {
                    BaseDanmaku item = it.next();
                    if (firstItem == null)
                        firstItem = item;
                    lastItem = item;

                    if (item.getBottom() - drawItem.paintHeight < 0) {
                        overwriteInsert = true;
                        break;
                    }


                    // 检查碰撞
                    boolean willHit = DanmakuUtils.willHitInDuration(disp, item, drawItem,
                            drawItem.getDuration());
                    if (!willHit) {
                        insertItem = item;
                        break;
                    }

                }

                if (insertItem != null) {
                    topPos = insertItem.getBottom() - insertItem.paintHeight;
                    mVisibleDanmakus.removeItem(insertItem);
                } else if (overwriteInsert) {
                    topPos = disp.getHeight() - drawItem.paintHeight;
                    mVisibleDanmakus.clear();
                } else if (lastItem != null && insertItem == null) {
                    topPos = lastItem.getBottom() - lastItem.paintHeight - drawItem.paintHeight;
                } else if (lastItem != null && topPos <= lastItem.getBottom() - drawItem.paintHeight) {
                    mVisibleDanmakus.removeItem(lastItem);
                }

                topPos = checkVerticalEdge(overwriteInsert, drawItem, disp, topPos, firstItem,
                        lastItem);
            }

            // layout
            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                          IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos < 0 || topPos + drawItem.paintHeight > disp.getHeight()) {
                topPos = disp.getHeight() - drawItem.paintHeight;
                mVisibleDanmakus.clear();
            }
            return topPos;
        }
    }

}
