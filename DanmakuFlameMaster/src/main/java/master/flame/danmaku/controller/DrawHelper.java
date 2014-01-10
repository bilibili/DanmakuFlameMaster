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

package master.flame.danmaku.controller;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;

public class DrawHelper {

    public static Paint PAINT;

    public static Rect RECT;
    static {
        PAINT = new Paint();
        PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        RECT = new Rect();
    }

    public static void drawText(Canvas canvas, String text) {

        canvas.drawText(text, 10, canvas.getHeight() - 50, PAINT);

    }

    public static void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    public static void clearCanvas(Canvas canvas, int left, int top, int right, int bottom) {
        RECT.set(Math.max(0, left), Math.max(0, top), Math.min(canvas.getWidth(),right), Math.min(canvas.getHeight(),bottom));
        clearCanvas(canvas, RECT);
    }

    private static void clearCanvas(Canvas canvas, Rect rect) {
        Log.e("DrawHelper rect", rect.toString());
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        if (rect.contains(0, 0, canvas.getWidth(), canvas.getHeight()))
            clearCanvas(canvas);
        else
            canvas.drawRect(rect, PAINT);
    }
}
