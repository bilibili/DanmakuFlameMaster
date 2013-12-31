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

import java.util.HashMap;

import android.graphics.*;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.util.Log;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;

/**
 * Created by ch on 13-7-5.
 */
public class AndroidDisplayer implements IDisplayer {

    private Camera camera = new Camera();

    private Matrix matrix = new Matrix();
    
    private static HashMap<Float,Float> TextHeightCache = new HashMap<Float,Float>();

    private int HIT_CACHE_COUNT = 0;

    private int NO_CACHE_COUNT = 0;

    public static TextPaint PAINT;

    public static Paint STROKE;

    private static Paint ALPHA_PAINT;

    private static Paint UNDERLINE_PAINT;

    /**
     * 开启阴影，可动态改变
     */
    public static boolean HAS_SHADOW = true;

    /**
     * 下划线高度
     */
    public static int UNDERLINE_HEIGHT = 4;

    /**
     * 开启描边，可动态改变
     */
    public static boolean HAS_STROKE = false;

    /**
     * 开启抗锯齿，可动态改变
     */
    public static boolean ANTI_ALIAS = true;

    static {
        PAINT = new TextPaint();
        STROKE = new Paint();
        ALPHA_PAINT = new Paint();
        UNDERLINE_PAINT = new Paint();
        UNDERLINE_PAINT.setStrokeWidth(UNDERLINE_HEIGHT);
        PAINT.setTypeface(Typeface.MONOSPACE);
        PAINT.setColor(Color.RED);
        PAINT.setTextSize(50);
        STROKE.setStrokeWidth(1.5f);
        STROKE.setStyle(Style.FILL_AND_STROKE);
        // TODO: load font from file
    }

    public Canvas canvas;

    public int width;

    public int height;

    public float density = 1;

    public int densityDpi = 160;

    public float scaledDensity = 1;

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
    public int getDensityDpi() {
        return densityDpi;
    }

    @Override
    public void draw(BaseDanmaku danmaku) {
        float top = danmaku.getTop();
        float left = danmaku.getLeft();
        if (danmaku.getType() == BaseDanmaku.TYPE_FIX_BOTTOM) {
            top = height - top - danmaku.paintHeight;
        }
        if (canvas != null) {

            Paint alphaPaint = null;
            boolean restore = false;
            if (danmaku.getType() == BaseDanmaku.TYPE_SPECIAL) {
                if (danmaku.getAlpha() == AlphaValue.TRANSPARENT) {
                    return;
                }
                if (danmaku.rotationZ != 0 || danmaku.rotationY != 0) {
                    saveCanvas(danmaku, canvas, left, top);
                    restore = true;
                }

                if (danmaku.getAlpha() != AlphaValue.MAX) {
                    alphaPaint = ALPHA_PAINT;
                    alphaPaint.setAlpha(danmaku.getAlpha());
                }
            }
            // drawing cache
            boolean cacheDrawn = false;
            if (danmaku.hasDrawingCache()) {
                DrawingCacheHolder holder = ((DrawingCache) danmaku.cache).get();
                if (holder != null && holder.bitmap != null) {
                    // canvas.save();
                    // canvas.translate(left, top);
                    // canvas.drawBitmap(holder.bitmap, 0, 0, null);
                    // canvas.restore();
                    canvas.drawBitmap(holder.bitmap, left, top, alphaPaint); //Fixme check draw rect
                    cacheDrawn = true;
                }
            }
            if (!cacheDrawn) {
                if (alphaPaint != null) {
                    PAINT.setAlpha(alphaPaint.getAlpha());
                    STROKE.setAlpha(alphaPaint.getAlpha());
                } else {
                    resetPaintAlpha(PAINT);
                    resetPaintAlpha(STROKE);
                }
                drawDanmaku(danmaku, canvas, left, top, true);
            }

            if (restore) {
                // need to restore canvas
                restoreCanvas(canvas);
            }
        }
    }

    private void resetPaintAlpha(Paint paint) {
        if (paint.getAlpha() != AlphaValue.MAX) {
            paint.setAlpha(AlphaValue.MAX);
        }
    }

    private void restoreCanvas(Canvas canvas) {
        canvas.restore();
    }

    private int saveCanvas(BaseDanmaku danmaku, Canvas canvas, float left, float top) {
        camera.save();
        camera.rotateY(-danmaku.rotationY);
        camera.rotateZ(-danmaku.rotationZ);
        camera.getMatrix(matrix);
        matrix.preTranslate(-left, -top);
        matrix.postTranslate(left, top);
        camera.restore();
        int count = canvas.save();
        canvas.concat(matrix);
        return count;
    }

    public static void drawDanmaku(BaseDanmaku danmaku, Canvas canvas, float left, float top,
                                   boolean quick) {
        if (quick) {
            HAS_STROKE = false;
            HAS_SHADOW = false;
            ANTI_ALIAS = false;
        } else {
            HAS_STROKE = false;
            HAS_SHADOW = true;
            ANTI_ALIAS = true;
        }
        TextPaint paint = getPaint(danmaku);
        if (danmaku.lines != null) {
            String[] lines = danmaku.lines;
            if (lines.length == 1) {
                if (HAS_STROKE)
                    canvas.drawText(lines[0], left, top - STROKE.ascent(), STROKE);
                canvas.drawText(lines[0], left, top - paint.ascent(), paint);
            } else {
                Float textHeight = getTextHeight(paint);
                for (int t = 0; t < lines.length; t++) {
                    if (lines[t].length() > 0) {
                        canvas.drawText(lines[t], left,
                                t * textHeight + top - paint.ascent(), paint);
                    }
                }
            }
        } else {
            if (HAS_STROKE)
                canvas.drawText(danmaku.text, left, top - STROKE.ascent(), STROKE);
            canvas.drawText(danmaku.text, left, top - paint.ascent(), paint);
        }

        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = getUnderlinePaint(danmaku);
            float bottom = top + danmaku.paintHeight;
            canvas.drawLine(left, bottom - UNDERLINE_HEIGHT, left + danmaku.paintWidth, bottom, linePaint);
        }

    }

    public static Paint getUnderlinePaint(BaseDanmaku danmaku){
        UNDERLINE_PAINT.setColor(danmaku.underlineColor);
        return UNDERLINE_PAINT;
    }

    public static TextPaint getPaint(BaseDanmaku danmaku) {
        PAINT.setTextSize(danmaku.textSize);
        PAINT.setColor(danmaku.textColor);
        PAINT.setAntiAlias(ANTI_ALIAS);
        if (HAS_STROKE) {
            // STROKE.setAntiAlias(ANTI_ALIAS);
            STROKE.setTextSize(danmaku.textSize);
            STROKE.setColor(danmaku.textShadowColor);
        }
        if (HAS_SHADOW) {
            PAINT.setShadowLayer(3.0f, 0, 0, danmaku.textShadowColor);
        } else {
            PAINT.clearShadowLayer();
        }
        return PAINT;
    }

    @Override
    public void measure(BaseDanmaku danmaku) {
        TextPaint paint = getPaint(danmaku);
        calcPaintWH(danmaku, paint);
    }   
    
    private void calcPaintWH(BaseDanmaku danmaku, TextPaint paint) {
        float w = 0;
        Float textHeight = getTextHeight(paint);
        if (danmaku.lines == null) {
            w = paint.measureText(danmaku.text);
            danmaku.paintWidth = w;
            danmaku.paintHeight = textHeight;
            return;
        }

        for(String tempStr : danmaku.lines){
            if (tempStr.length() > 0) {
                float tr = paint.measureText(tempStr);
                w = Math.max(tr, w);
            }
        }

        danmaku.paintWidth = w;
        danmaku.paintHeight = danmaku.lines.length * textHeight;
    }

    private static Float getTextHeight(TextPaint paint) {
        Float textSize = paint.getTextSize();
        Float textHeight = TextHeightCache.get(textSize);
        if(textHeight == null){
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            textHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading;
            TextHeightCache.put(textSize, textHeight);
        }  
        return textHeight;
    }

    @Override
    public float getScaledDensity() {
        return scaledDensity;
    }

}
