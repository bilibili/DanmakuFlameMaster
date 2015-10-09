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


public interface IDisplayer {

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract float getDensity();

    public abstract int getDensityDpi();

    public abstract int draw(BaseDanmaku danmaku);

    public abstract float getScaledDensity();

    public abstract int getSlopPixel();

    public abstract void measure(BaseDanmaku danmaku);

    public abstract float getStrokeWidth();

    public abstract void setHardwareAccelerated(boolean enable);

    public abstract boolean isHardwareAccelerated();

    public abstract int getMaximumCacheWidth();

    public abstract int getMaximumCacheHeight();


    ////////////////// setter ///////////////////////////

    public abstract void resetSlopPixel(float factor);

    public abstract void setDensities(float density, int densityDpi, float scaledDensity);

    public abstract void setSize(int width, int height);

}
