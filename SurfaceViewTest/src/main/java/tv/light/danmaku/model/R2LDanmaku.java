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

package tv.light.danmaku.model;

public class R2LDanmaku extends DanmakuBase {

    private float x = 0;

    private float y = -1;

    public R2LDanmaku() {
        duration = 4000;
    }

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        if (mTimer != null) {
            //if (time <= mTimer.currMillisecond && mTimer.currMillisecond - time <= duration) {
            this.x = (1 - (mTimer.currMillisecond - time) / (float) duration)
                        * (displayer.getWidth() + paintWidth) - paintWidth;
                this.y = y;
            //}
        }
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
}
