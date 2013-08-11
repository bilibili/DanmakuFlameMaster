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

package tv.light.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import tv.light.danmaku.model.BaseDanmaku;
import tv.light.danmaku.model.IDisplayer;

/**
 * Created by ch on 13-7-5.
 */
public class AndroidDisplayer implements IDisplayer {

    public static Paint PAINT;
    static {
        PAINT = new Paint();
        PAINT.setColor(Color.RED);
        PAINT.setTextSize(50);
        //PAINT.setAntiAlias(true);
        // TODO: load font from file
    }

    public Canvas canvas;

    public int width;

    public int height;

    public float density = 1;

    public void update(Canvas c) {
        canvas = c;
        if (c != null) {
            width = c.getWidth();
            height = c.getHeight();
        }
    }

    @Override
    public int getWidth() {

        return width;
    }

    @Override
    public int getHeight() {

        return height;
    }

    @Override
    public float getDensity() {
        return density;
    }

    @Override
    public void draw(BaseDanmaku danmaku) {
        if (canvas != null) {
            Paint paint = getPaint(danmaku);
            canvas.drawText(danmaku.text, danmaku.getLeft(), danmaku.getTop() - paint.ascent(),
                    paint);
        }
    }

    @Override
    public void measure(BaseDanmaku danmaku) {
        Paint paint = getPaint(danmaku);
        danmaku.paintWidth = paint.measureText(danmaku.text);
        danmaku.paintHeight = paint.getTextSize();
    }

    private static Paint getPaint(BaseDanmaku danmaku) {
        PAINT.setTextSize(danmaku.textSize); // TODO: fixme
        PAINT.setColor(danmaku.textColor);
        // TODO: set the text shadow color
        return PAINT;
    }

}
