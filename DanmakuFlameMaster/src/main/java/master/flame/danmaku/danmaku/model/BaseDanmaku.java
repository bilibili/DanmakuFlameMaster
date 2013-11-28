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

package master.flame.danmaku.danmaku.model;

public abstract class BaseDanmaku {

    public final static String DANMAKU_BR_CHAR = "/n";

    public final static int TYPE_SCROLL_RL = 1;

    public final static int TYPE_SCROLL_LR = 6;

    public final static int TYPE_FIX_TOP = 5;

    public final static int TYPE_FIX_BOTTOM = 4;

    public final static int TYPE_SPECIAL = 7;

    public final static int TYPE_MOVEABLE_XXX = 0; // TODO: add more type

    public final static int INVISIBLE = 0;

    public final static int VISIBLE = 1;

    /**
     * 显示时间(毫秒)
     */
    public long time;

    /**
     * 文本
     */
    public String text;

    /**
     * 文本颜色
     */
    public int textColor;

    /**
     * Z轴角度
     */
    public float rotationZ;

    /**
     * Y轴角度
     */
    public float rotationY;

    /**
     * 阴影/描边颜色
     */
    public int textShadowColor;

    /**
     * 字体大小
     */
    public float textSize = -1;

    /**
     * 占位宽度
     */
    public float paintWidth = -1;

    /**
     * 占位高度
     */
    public float paintHeight = -1;

    /**
     * 存活时间(毫秒)
     */
    public long duration;

    /**
     * 索引/编号
     */
    public int index;

    /**
     * 是否可见
     */
    public int visibility;

    /**
     * 绘制用缓存
     */
    public IDrawingCache cache;

    /**
     * 计时
     */
    protected DanmakuTimer mTimer;

    /**
     * 透明度
     */
    protected int alpha = AlphaValue.MAX;

    private long timer;

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void draw(IDisplayer displayer) {
        displayer.draw(this);
    }

    public boolean isMeasured() {
        return paintWidth >= 0 && paintHeight >= 0;
    }

    public void measure(IDisplayer displayer) {
        displayer.measure(this);
    }

    public boolean hasDrawingCache() {
        return cache != null && cache.get() != null;
    }

    public boolean isShown() {
        return this.visibility == VISIBLE;
    }

    public boolean isTimeOut() {
        if (mTimer != null) {
            return isTimeOut(mTimer.currMillisecond);
        }
        return true;
    }

    public boolean isTimeOut(long ctime) {
        return ctime - time > duration;
    }

    public boolean isOutside() {
        if (mTimer != null) {
            return isOutside(mTimer.currMillisecond);
        }
        return true;
    }

    public boolean isOutside(long ctime) {
        return time > ctime || ctime - time > duration;
    }

    public void setVisibility(boolean b) {
        this.visibility = (b ? VISIBLE : INVISIBLE);
    }

    public abstract void layout(IDisplayer displayer, float x, float y);

    public abstract float[] getRectAtTime(IDisplayer displayer, long currTime);

    public abstract float getLeft();

    public abstract float getTop();

    public abstract float getRight();

    public abstract float getBottom();

    /**
     * return the type of Danmaku
     * 
     * @return TYPE_SCROLL_RL = 0 TYPE_SCROLL_RL = 1 TYPE_SCROLL_LR = 2
     *         TYPE_FIX_TOP = 3; TYPE_FIX_BOTTOM = 4;
     */
    public abstract int getType();

    public DanmakuTimer getTimer() {
        return mTimer;
    }

    public void setTimer(DanmakuTimer timer) {
        mTimer = timer;
    }

    public int getAlpha() {
        return alpha;
    }
}
