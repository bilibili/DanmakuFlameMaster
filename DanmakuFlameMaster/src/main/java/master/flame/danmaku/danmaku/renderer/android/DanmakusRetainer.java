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
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class DanmakusRetainer {

    private static IDanmakusRetainer rldrInstance = null;

    private static IDanmakusRetainer lrdrInstance = null;

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
            case BaseDanmaku.TYPE_SCROLL_LR:
                if (lrdrInstance == null) {
                    lrdrInstance = new RLDanmakusRetainer();
                }
                lrdrInstance.fix(danmaku, disp);
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
            case BaseDanmaku.TYPE_SPECIAL:
                danmaku.layout(disp, 0, 0);
                break;
        }

    }

    public static void clear() {
        if (rldrInstance != null) {
            rldrInstance.clear();
        }
        if (lrdrInstance != null) {
            lrdrInstance.clear();
        }
        if (ftdrInstance != null) {
            ftdrInstance.clear();
        }
        if (fbdrInstance != null) {
            fbdrInstance.clear();
        }
    }
    
    public static void release(){
        clear();
        rldrInstance = null;
        lrdrInstance = null;
        ftdrInstance = null;
        fbdrInstance = null;
    }

    public interface IDanmakusRetainer {
        public void fix(BaseDanmaku drawItem, IDisplayer disp);

        public void clear();

    }

    private static class RLDanmakusRetainer implements IDanmakusRetainer {

        protected Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS);
        protected boolean mCancelFixingFlag = false;

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp) {
            if (drawItem.isOutside())
                return;
            float topPos = 0;
            boolean shown = drawItem.isShown();
            if (!shown) {
                mCancelFixingFlag = false;
                // 确定弹幕位置
                IDanmakuIterator it = mVisibleDanmakus.iterator();
                BaseDanmaku insertItem = null, firstItem = null, lastItem = null, minRightRow = null;
                boolean overwriteInsert = false;
                while (!mCancelFixingFlag && it.hasNext()) {
                    BaseDanmaku item = it.next();

                    if(item == drawItem){
                        insertItem = item;
                        lastItem = null;
                        shown = true;
                        break;
                    }

                    if (firstItem == null)
                        firstItem = item;

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
                            drawItem.getDuration(), drawItem.getTimer().currMillisecond);
                    if (!willHit) {
                        insertItem = item;
                        break;
                    }

                    lastItem = item;

                }
                boolean checkEdge = true;
                if (insertItem != null) {
                    if (lastItem != null)
                        topPos = lastItem.getBottom();
                    else
                        topPos = insertItem.getTop();
                    if (insertItem != drawItem){
                        mVisibleDanmakus.removeItem(insertItem);
                        shown = false;
                    }
                } else if (overwriteInsert && minRightRow != null) {
                    topPos = minRightRow.getTop();
                    checkEdge = false;
                    shown = false;
                } else if (lastItem != null) {
                    topPos = lastItem.getBottom();
                } else if (firstItem != null) {
                    topPos = firstItem.getTop();
                    mVisibleDanmakus.removeItem(firstItem);
                    shown = false;
                } else {
                    topPos = 0;
                }
                if (checkEdge) {
                    topPos = checkVerticalEdge(overwriteInsert, drawItem, disp, topPos, firstItem,
                            lastItem);
                }
                if (topPos == 0 && mVisibleDanmakus.size()==0) {
                    shown = false;
                }
            }

            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                                          IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos < 0 || (firstItem != null && firstItem.getTop() > 0) || topPos + drawItem.paintHeight > disp.getHeight()) {
                topPos = 0;
                clear();
            }
            return topPos;
        }

        @Override
        public void clear() {
            mCancelFixingFlag = true;
            mVisibleDanmakus.clear();
        }

    }

    private static class FTDanmakusRetainer extends RLDanmakusRetainer {

        @Override
        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos + drawItem.paintHeight > disp.getHeight()) {
                topPos = 0;
                clear();
            }
            return topPos;
        }

    }

    private static class FBDanmakusRetainer extends FTDanmakusRetainer {

        protected Danmakus mVisibleDanmakus = new Danmakus(Danmakus.ST_BY_YPOS_DESC);

        @Override
        public void fix(BaseDanmaku drawItem, IDisplayer disp) {
            if (drawItem.isOutside())
                return;
            boolean shown = drawItem.isShown();
            float topPos = drawItem.getTop();
            if (topPos < 0) {
                topPos = disp.getHeight() - drawItem.paintHeight;
            }
            BaseDanmaku removeItem = null, firstItem = null;
            if (!shown) {
                mCancelFixingFlag = false;
                IDanmakuIterator it = mVisibleDanmakus.iterator();
                while (!mCancelFixingFlag && it.hasNext()) {
                    BaseDanmaku item = it.next();

                    if (item == drawItem) {
                        removeItem = null;
                        break;
                    }

                    if (firstItem == null) {
                        firstItem = item;
                        if (firstItem.getBottom() != disp.getHeight()) {
                            break;
                        }
                    }

                    if (topPos < 0) {
                        removeItem = null;
                        break;
                    }

                    // 检查碰撞
                    boolean willHit = DanmakuUtils.willHitInDuration(disp, item, drawItem,
                            drawItem.getDuration(), drawItem.getTimer().currMillisecond);
                    if (!willHit) {
                        removeItem = item;
                        // topPos = item.getBottom() - drawItem.paintHeight;
                        break;
                    }

                    topPos = item.getTop() - drawItem.paintHeight;

                }

                topPos = checkVerticalEdge(false, drawItem, disp, topPos, firstItem, null);

            }

            drawItem.layout(disp, drawItem.getLeft(), topPos);

            if (!shown) {
                mVisibleDanmakus.removeItem(removeItem);
                mVisibleDanmakus.addItem(drawItem);
            }

        }

        protected float checkVerticalEdge(boolean overwriteInsert, BaseDanmaku drawItem,
                IDisplayer disp, float topPos, BaseDanmaku firstItem, BaseDanmaku lastItem) {
            if (topPos < 0 || (firstItem != null && firstItem.getBottom() != disp.getHeight())) {
                topPos = disp.getHeight() - drawItem.paintHeight;
                clear();
            }
            return topPos;
        }

        @Override
        public void clear() {
            mCancelFixingFlag = true;
            mVisibleDanmakus.clear();
        }

    }

}
