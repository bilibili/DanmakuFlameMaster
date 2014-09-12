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


public class L2RDanmaku extends R2LDanmaku {

    public L2RDanmaku(Duration duration) {
        super(duration); 
    }
    
    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        if (mTimer != null) {
            long currMS = mTimer.currMillisecond;
            long deltaDuration = currMS - time;
            if (deltaDuration > 0 && deltaDuration < duration.value) {
                this.x = getAccurateLeft(displayer, currMS);
                if (!this.isShown()) {
                    this.y = y;
                    this.setVisibility(true);
                }
                return;
            }
            this.setVisibility(false);
        }
        this.x = -paintWidth;
    }
    
    @Override
    public float[] getRectAtTime(IDisplayer displayer, long time) {
        if (!isMeasured())
            return null;
        float left = getStableLeft(displayer, time);
        if (RECT == null) {
            RECT = new float[4];
        }
        RECT[0] = left;
        RECT[1] = y;
        RECT[2] = left + paintWidth;
        RECT[3] = y + paintHeight;
        return RECT;
    }
    
    protected float getStableLeft(IDisplayer displayer, long currTime) {
        long elapsedTime = currTime - time;
        if (elapsedTime >= duration.value || this.x <= -paintWidth) {
            return -paintWidth;
        }
        
        if (Math.abs(mLastTime - currTime) > MAX_RENDERING_TIME){
            mLastTime = currTime;
            return getAccurateLeft(displayer, currTime);
        }

        long averageRenderingTime = displayer.getAverageRenderingTime();
        float layoutCount = (duration.value - elapsedTime)
                / (float) averageRenderingTime;
        float stepX = (displayer.getWidth() - (this.x + paintWidth)) / layoutCount;

        if (stepX < mStepX * 16) {
            stepX = mStepX * 16;
        }

        mLastTime = currTime;
        return this.x + stepX;
    }

    protected float getAccurateLeft(IDisplayer displayer, long currTime) {
        long elapsedTime = currTime - time;
        if (elapsedTime >= duration.value) {
            return displayer.getWidth();
        }
        return mStepX * elapsedTime - paintWidth;
    }

    @Override
    public float getLeft() {
        return x;
    }

    @Override
    public float getTop() {
        return y;
    }

    @Override
    public float getRight() {
        return x + paintWidth;
    }

    @Override
    public float getBottom() {
        return y + paintHeight;
    }

    @Override
    public int getType() {
        return TYPE_SCROLL_LR;
    }
    
    @Override
    public void measure(IDisplayer displayer) {
        super.measure(displayer);
        this.x = -paintWidth;
    }

}
