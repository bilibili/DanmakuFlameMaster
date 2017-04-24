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
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.ICacheManager;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.renderer.Renderer;


public class DanmakuRenderer extends Renderer {

    private class Consumer extends IDanmakus.DefaultConsumer<BaseDanmaku> {
        private BaseDanmaku lastItem;
        public IDisplayer disp;
        public RenderingState renderingState;
        public long startRenderTime;

        @Override
        public int accept(BaseDanmaku drawItem) {
            lastItem = drawItem;
            if (drawItem.isTimeOut()) {
                disp.recycle(drawItem);
                return renderingState.isRunningDanmakus ? ACTION_REMOVE : ACTION_CONTINUE;
            }

            if (!renderingState.isRunningDanmakus && drawItem.isOffset()) {
                return ACTION_CONTINUE;
            }

            if (!drawItem.hasPassedFilter()) {
                mContext.mDanmakuFilters.filter(drawItem, renderingState.indexInScreen, renderingState.totalSizeInScreen, renderingState.timer, false, mContext);
            }
            if (drawItem.getActualTime() < startRenderTime
                    || (drawItem.priority == 0 && drawItem.isFiltered())) {
                return ACTION_CONTINUE;
            }

            if (drawItem.isLate()) {
                IDrawingCache<?> cache = drawItem.getDrawingCache();
                if (mCacheManager != null && (cache == null || cache.get() == null)) {
                    mCacheManager.addDanmaku(drawItem);
                }
                return ACTION_BREAK;
            }

            if (drawItem.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
                // 同屏弹幕密度只对滚动弹幕有效
                renderingState.indexInScreen++;
            }

            // measure
            if (!drawItem.isMeasured()) {
                drawItem.measure(disp, false);
            }

            // notify prepare drawing
            if (!drawItem.isPrepared()) {
                drawItem.prepare(disp, false);
            }

            // layout
            mDanmakusRetainer.fix(drawItem, disp, mVerifier);

            // draw
            if (drawItem.isShown()) {
                if (drawItem.lines == null && drawItem.getBottom() > disp.getHeight()) {
                    return ACTION_CONTINUE;    // skip bottom outside danmaku
                }
                int renderingType = drawItem.draw(disp);
                if (renderingType == IRenderer.CACHE_RENDERING) {
                    renderingState.cacheHitCount++;
                } else if (renderingType == IRenderer.TEXT_RENDERING) {
                    renderingState.cacheMissCount++;
                    if (mCacheManager != null) {
                        mCacheManager.addDanmaku(drawItem);
                    }
                }
                renderingState.addCount(drawItem.getType(), 1);
                renderingState.addTotalCount(1);
                renderingState.appendToRunningDanmakus(drawItem);

                if (mOnDanmakuShownListener != null
                        && drawItem.firstShownFlag != mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG) {
                    drawItem.firstShownFlag = mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG;
                    mOnDanmakuShownListener.onDanmakuShown(drawItem);
                }
            }
            return ACTION_CONTINUE;
        }

        @Override
        public void after() {
            renderingState.lastDanmaku = lastItem;
            super.after();
        }
    }

    private DanmakuTimer mStartTimer;
    private final DanmakuContext mContext;
    private DanmakusRetainer.Verifier mVerifier;
    private final DanmakusRetainer.Verifier verifier = new DanmakusRetainer.Verifier() {
        @Override
        public boolean skipLayout(BaseDanmaku danmaku, float fixedTop, int lines, boolean willHit) {
            if (danmaku.priority == 0 && mContext.mDanmakuFilters.filterSecondary(danmaku, lines, 0, mStartTimer, willHit, mContext)) {
                danmaku.setVisibility(false);
                return true;
            }
            return false;
        }
    };
    private final DanmakusRetainer mDanmakusRetainer;
    private ICacheManager mCacheManager;
    private OnDanmakuShownListener mOnDanmakuShownListener;
    private Consumer mConsumer = new Consumer();

    public DanmakuRenderer(DanmakuContext config) {
        mContext = config;
        mDanmakusRetainer = new DanmakusRetainer(config.isAlignBottom());
    }

    @Override
    public void clear() {
        clearRetainer();
        mContext.mDanmakuFilters.clear();
    }

    @Override
    public void clearRetainer() {
        mDanmakusRetainer.clear();
    }

    @Override
    public void release() {
        mDanmakusRetainer.release();
        mContext.mDanmakuFilters.clear();
    }

    @Override
    public void setVerifierEnabled(boolean enabled) {
        mVerifier = (enabled ? verifier : null);
    }

    @Override
    public void draw(final IDisplayer disp, IDanmakus danmakus, long startRenderTime, final RenderingState renderingState) {
        mStartTimer = renderingState.timer;
        mConsumer.disp = disp;
        mConsumer.renderingState = renderingState;
        mConsumer.startRenderTime = startRenderTime;
        danmakus.forEachSync(mConsumer);
    }

    public void setCacheManager(ICacheManager cacheManager) {
        mCacheManager = cacheManager;
    }

    @Override
    public void setOnDanmakuShownListener(OnDanmakuShownListener onDanmakuShownListener) {
        mOnDanmakuShownListener = onDanmakuShownListener;
    }

    @Override
    public void removeOnDanmakuShownListener() {
        mOnDanmakuShownListener = null;
    }

    public void alignBottom(boolean enable) {
        if (mDanmakusRetainer != null) {
            mDanmakusRetainer.alignBottom(enable);
        }
    }
}
