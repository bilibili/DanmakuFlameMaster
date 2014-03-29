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

import android.graphics.Canvas;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;

public class R2LDanmaku extends BaseDanmaku implements SpannedDanmaku{

    protected float x = 0;

    protected float y = -1;

    protected int mDistance;

    private float[] RECT = null;

    protected float mStepX;

    public R2LDanmaku(Duration duration) {
        this.duration = duration;
    }

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        if (mTimer != null) {
            long currMS = mTimer.currMillisecond;
            long deltaDuration = currMS - time;
            if (deltaDuration > 0 && deltaDuration < duration.value) {
                this.x = getLeft(displayer, currMS);
                if (!this.isShown()) {
                    this.y = y;
                    this.setVisibility(true);
                }
                return;
            } 
            this.setVisibility(false);
        }
    }

    protected float getLeft(IDisplayer displayer, long currTime) {
        long elapsedTime = currTime - time;
        if (elapsedTime >= duration.value) {
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
        mDistance = (int) (displayer.getWidth() + paintWidth);
        mStepX = mDistance / (float) duration.value;        
    }

    private Layout mLayoutInner;
    @Override
    public void drawLayout(Canvas canvas, TextPaint paint, float left, float top) {
        if(mLayoutInner == null) return;
        boolean needRestore = false;
        if (left != 0 || top != 0) {
            canvas.save();
            canvas.translate(-left, -top);
            needRestore = true;
        }
        mLayoutInner.draw(canvas);
        if (needRestore) {
            canvas.restore();
        }
    }

    @Override
    public void measureWithLayout(TextPaint paint) {
        if (mLayoutInner == null) {
            mLayoutInner = new StaticLayout(text, paint, (int) StaticLayout.getDesiredWidth(text,
                    paint), Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        }
        paintWidth = mLayoutInner.getWidth();
        paintHeight = mLayoutInner.getLineTop(1);
    }

    @Override
    public boolean isDanmakuSpanned() {
        return text instanceof Spanned;
    }

}
