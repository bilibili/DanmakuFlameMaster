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

public interface ICanvas<T> {

    public static interface IBitmap<T> {

        public abstract T data();

    }

    public static interface IPaint<T> {

        public abstract T data();

        public abstract float getTextSize();

        public abstract int getColor();

        public abstract void setColor(int color);

        public abstract void setTextSize(float textSize);

    }

    public static interface IMode<T> {

        public abstract T data();

    }

    public abstract void attach(T data);

    public abstract int save();

    public abstract void restore();

    public abstract void contact(float[] matrix);

    public void setBitmap(IBitmap<?> bitmap);

    public abstract void drawColor(int color, IMode<?> mode);

    public abstract void drawLine(float startX, float startY, float stopX, float stopY,
            IPaint<?> paint);

    public abstract void drawRect(float left, float top, float right, float bottom, IPaint<?> paint);

    public abstract void drawText(String text, float x, float y, IPaint<?> paint);

    public abstract void drawBitmap(IBitmap<?> bitmap, float left, float top, IPaint<?> paint);

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract boolean isHardwareAccelerated();

}
