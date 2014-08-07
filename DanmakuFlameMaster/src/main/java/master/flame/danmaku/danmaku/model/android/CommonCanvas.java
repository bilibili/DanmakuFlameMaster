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

package master.flame.danmaku.danmaku.model.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import master.flame.danmaku.danmaku.model.ICanvas;

public class CommonCanvas implements ICanvas<Canvas> {

    Matrix mMatrix = new Matrix();
    Canvas mCanvas;

    public CommonCanvas(Canvas canvas) {
        if (canvas == null) {
            mCanvas = new Canvas();
        } else {
            mCanvas = canvas;
        }
    }
    
    public void attach(Canvas data) {
        mCanvas = data;
    }

    @Override
    public synchronized void contact(float[] matrix) {
        mMatrix.setValues(matrix);
        mCanvas.concat(mMatrix);
    }

    @Override
    public synchronized void setBitmap(IBitmap<?> bitmap) {
        mCanvas.setBitmap((Bitmap) bitmap.data());
    }

    @Override
    public void drawColor(int color, IMode<?> mode) {
        mCanvas.drawColor(color, (android.graphics.PorterDuff.Mode) mode.data());
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, IPaint<?> paint) {
        mCanvas.drawLine(startX, startY, stopX, stopY, (Paint) paint.data());
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, IPaint<?> paint) {
        mCanvas.drawRect(left, top, right, bottom, (Paint) paint.data());
    }

    @Override
    public void drawText(String text, float x, float y, IPaint<?> paint) {
        mCanvas.drawText(text, x, y, (Paint) paint.data());
    }

    @Override
    public void drawBitmap(IBitmap<?> bitmap, float left, float top, IPaint<?> paint) {
        mCanvas.drawBitmap((Bitmap) bitmap.data(), left, top, (Paint) paint.data());
    }

    @Override
    public synchronized int save() {
        return mCanvas.save();
    }

    @Override
    public synchronized void restore() {
        mCanvas.restore();
    }

    @Override
    public int getWidth() {
        return mCanvas.getWidth();
    }

    @Override
    public int getHeight() {
        return mCanvas.getHeight();
    }

    @Override
    public boolean isHardwareAccelerated() {
        return mCanvas.isHardwareAccelerated();
    }

}
