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

public class R2LDanmaku extends BaseDanmaku {

    private float x = 0;

    private float y = -1;

    private int mDistance;

    private float[] RECT = null;

    private float mStepX;

    private float mOldPaintWidth;

    public R2LDanmaku(long duration) {
        this.duration = duration;
    }

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        if (mTimer != null) {
            long deltaDuration = mTimer.currMillisecond - time;
            if (deltaDuration > 0 && deltaDuration < duration) {
                this.x = getLeft(displayer, mTimer.currMillisecond);
                if (!this.isShown()) {
                    this.y = y;
                    this.setVisibility(true);
                }
            } else if (deltaDuration >= duration) {
                this.setVisibility(false);
            } else if (deltaDuration <= 0) {
                this.setVisibility(false);
            }
        }
    }

    private float getLeft(IDisplayer displayer, long currTime) {
        long elapsedTime = currTime - time;
        if (elapsedTime >= duration) {
            return -paintWidth;
        }

        return displayer.getWidth() - elapsedTime * mStepX;
    }

    @Override
    public float[] getRectAtTime(IDisplayer displayer, long time) {
        if (!isMeasured())
            return null;
        float left = getLeft(displayer, time);
        if (RECT == null) {
            RECT = new float[4];
        }
        RECT[0] = left;
        RECT[1] = y;
        RECT[2] = left + paintWidth;
        RECT[3] = y + paintHeight;
        return RECT;
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
        return TYPE_SCROLL_RL;
    }
    
    @Override
    public void measure(IDisplayer displayer) {
        super.measure(displayer);
        if (mOldPaintWidth != paintWidth) {
            mDistance = (int) (displayer.getWidth() + paintWidth);
            mStepX = mDistance / (float) duration;
            mOldPaintWidth = paintWidth;
        }
    }

}
