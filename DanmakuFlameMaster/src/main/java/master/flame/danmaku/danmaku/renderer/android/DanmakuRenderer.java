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

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.renderer.Renderer;


public class DanmakuRenderer extends Renderer {

    private final Area mRefreshArea = new Area();    
    private final DanmakuTimer mStartTimer = new DanmakuTimer();

    @Override
    public void clear() {
        DanmakusRetainer.clear();
        mRefreshArea.resizeToMax();
    }

    @Override
    public void release() {
        DanmakusRetainer.release();
    }
    
    @Override
    public void draw(IDisplayer disp, IDanmakus danmakus, long startRenderTime) {
        
        float left = disp.getWidth(),top = disp.getHeight(), right = 0 ,bottom = 0;
        boolean fullScreenRefreshing = false;
        
        IDanmakuIterator itr = danmakus.iterator();

        int orderInScreen = 0;        
        mStartTimer.update(System.currentTimeMillis());
        int sizeInScreen = danmakus.size();
        while (itr.hasNext()) {

            BaseDanmaku drawItem = itr.next();

            if (drawItem.time < startRenderTime
                    || (drawItem.priority == 0 && DanmakuFilters.getDefault().filter(drawItem,
                            orderInScreen, sizeInScreen, mStartTimer))) {
                continue;
            }
            
            if(drawItem.getType() == BaseDanmaku.TYPE_SCROLL_RL){
                // 同屏弹幕密度只对滚动弹幕有效
                orderInScreen++;
            }

            // measure
            if (!drawItem.isMeasured()) {
                drawItem.measure(disp);
            }

            // layout
            DanmakusRetainer.fix(drawItem, disp);

            // draw
            if (!drawItem.isOutside() && drawItem.isShown()) {
                drawItem.draw(disp);
            }
            
            if (fullScreenRefreshing)
                continue;
            
            // calculate the refreshing area
            if (drawItem.getType() == BaseDanmaku.TYPE_SPECIAL
                    && (drawItem.rotationY != 0 || drawItem.rotationZ != 0)) {
                left = 0;
                top = 0;
                right = disp.getWidth();
                bottom = disp.getHeight();
                fullScreenRefreshing = true;
                continue;
            }
            
            float dtop = 0, dbottom = 0;            
            float dleft = drawItem.getLeft();
            float dright = drawItem.getRight();
            dtop = drawItem.getTop();
            dbottom = drawItem.getBottom();
            left = Math.min(dleft, left);
            top = Math.min(dtop, top);
            right = Math.max(dright, right);
            bottom = Math.max(dbottom, bottom);

        }
        float borderWidth = disp.getStrokeWidth() * 2;
        mRefreshArea.set(left, top, right + borderWidth, bottom + borderWidth);
    }

    @Override
    public Area getRefreshArea() {
        return mRefreshArea ;
    }

}
