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
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.Renderer;


public class DanmakuRenderer extends Renderer {

    private final DanmakuTimer mStartTimer = new DanmakuTimer();
    private final RenderingState mRenderingState = new RenderingState();
    
    @Override
    public void clear() {
        DanmakusRetainer.clear();
        DanmakuFilters.getDefault().clear();
    }

    @Override
    public void release() {
        DanmakusRetainer.release();
        DanmakuFilters.getDefault().release();
    }
    
    @Override
    public RenderingState draw(IDisplayer disp, IDanmakus danmakus, long startRenderTime) {
        int lastTotalDanmakuCount = mRenderingState.totalDanmakuCount;
        mRenderingState.reset();       
        IDanmakuIterator itr = danmakus.iterator();
        int orderInScreen = 0;        
        mStartTimer.update(System.currentTimeMillis());
        int sizeInScreen = danmakus.size();
        BaseDanmaku drawItem = null;
        while (itr.hasNext()) {

            drawItem = itr.next();
            
            if (drawItem.isLate()) {
                break;
            }

            if (drawItem.time < startRenderTime
                    || (drawItem.priority == 0 && DanmakuFilters.getDefault().filter(drawItem,
                            orderInScreen, sizeInScreen, mStartTimer, false))) {
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
                int renderingType = drawItem.draw(disp);
                if(renderingType == IRenderer.CACHE_RENDERING) {
                    mRenderingState.cacheHitCount++;
                } else if(renderingType == IRenderer.TEXT_RENDERING) {
                    mRenderingState.cacheMissCount++;
                }
                mRenderingState.addCount(drawItem.getType(), 1);
                mRenderingState.addTotalCount(1);
            }

        }
        
        mRenderingState.nothingRendered = (mRenderingState.totalDanmakuCount == 0);
        if (mRenderingState.nothingRendered) {
            mRenderingState.beginTime = RenderingState.UNKNOWN_TIME;
            mRenderingState.endTime = drawItem != null ? drawItem.time : RenderingState.UNKNOWN_TIME;
        }
        mRenderingState.incrementCount = mRenderingState.totalDanmakuCount - lastTotalDanmakuCount;
        mRenderingState.consumingTime = mStartTimer.update(System.currentTimeMillis());
        return mRenderingState;
    }
    
}
