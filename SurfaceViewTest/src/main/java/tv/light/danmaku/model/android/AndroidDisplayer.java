
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
import android.graphics.Paint;
import tv.light.danmaku.model.DanmakuBase;
import tv.light.danmaku.model.IDisplayer;

/**
 * Created by ch on 13-7-5.
 */
public class AndroidDisplayer implements IDisplayer {

    private static Paint PAINT = new Paint();
    static {
        PAINT.setAntiAlias(true);
        // TODO: load font from file
    }

    public Canvas canvas;

    public AndroidDisplayer() {

        this(null);
    }

    public AndroidDisplayer(Canvas c) {

        canvas = c;
    }

    private static Paint getPaint(DanmakuBase danmaku) {
        PAINT.setTextSize(danmaku.textSize);
        PAINT.setColor(danmaku.textColor);
        // TODO: set the text shadow color
        return PAINT;
    }

    public void init(Canvas c) {
        canvas = c;
    }

    @Override
    public int getWidth() {
        if (canvas != null) {
            return canvas.getWidth();
        }
        return 0;
    }

    @Override
    public int getHeight() {
        if (canvas != null) {
            return canvas.getHeight();
        }
        return 0;
    }

    @Override
    public void draw(DanmakuBase danmaku) {
        if (canvas != null) {
            Paint paint = getPaint(danmaku);
            canvas.drawText(danmaku.text, danmaku.getLeft(), danmaku.getTop() - paint.ascent(), paint);
        }
    }

    @Override
    public void measure(DanmakuBase danmaku) {
        Paint paint = getPaint(danmaku);
        danmaku.paintWidth = paint.measureText(danmaku.text);
        danmaku.paintHeight = paint.getTextSize();
    }

}
