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
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.*;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;

/**
 * Created by ch on 13-7-5.
 */
public class AndroidDisplayer implements IDisplayer {

    private Camera camera = new Camera();

    private Matrix matrix = new Matrix();
    
    private final static HashMap<Float,Float> TextHeightCache = new HashMap<Float,Float>(); // thread safe is not Necessary

    private int HIT_CACHE_COUNT = 0;

    private int NO_CACHE_COUNT = 0;

    public static TextPaint PAINT;

    private static Paint ALPHA_PAINT;

    private static Paint UNDERLINE_PAINT;
    
    /**
     * 下划线高度
     */
    public static int UNDERLINE_HEIGHT = 4;
    

    /**
     * 开启阴影，可动态改变
     */
    public static boolean CONFIG_HAS_SHADOW = true;
    private static boolean HAS_SHADOW = CONFIG_HAS_SHADOW;

    /**
     * 开启描边，可动态改变
     */
    public static boolean CONFIG_HAS_STROKE = false;
    private static boolean HAS_STROKE = CONFIG_HAS_STROKE;

    /**
     * 开启抗锯齿，可动态改变
     */
    public static boolean CONFIG_ANTI_ALIAS = true;
    private static boolean ANTI_ALIAS = CONFIG_ANTI_ALIAS;

    static {
        PAINT = new TextPaint();
        PAINT.setStrokeWidth(4);
        ALPHA_PAINT = new Paint();
        UNDERLINE_PAINT = new Paint();
        UNDERLINE_PAINT.setStrokeWidth(UNDERLINE_HEIGHT);        
        // TODO: load font from file
    }
    
    public static void setTypeFace(Typeface font){
        if(PAINT!=null)
            PAINT.setTypeface(font);
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
        int top = (int)danmaku.getTop();
        int left = (int)danmaku.getLeft();
        int paintHeight = (int)danmaku.paintHeight;
        if (danmaku.getType() == BaseDanmaku.TYPE_FIX_BOTTOM) {
            top = height - top - paintHeight;
        }
        if (canvas != null) {

            Paint alphaPaint = null;
            boolean needRestore = false;
            if (danmaku.getType() == BaseDanmaku.TYPE_SPECIAL) {
                if (danmaku.getAlpha() == AlphaValue.TRANSPARENT) {
                    return;
                }
                if (danmaku.rotationZ != 0 || danmaku.rotationY != 0) {
                    saveCanvas(danmaku, canvas, left, top);
                    needRestore = true;
                }

                int alpha = danmaku.getAlpha();
                if ( alpha != AlphaValue.MAX) {
                    alphaPaint = ALPHA_PAINT;
                    alphaPaint.setAlpha(danmaku.getAlpha());
                }
            }
            
            // drawing cache
            boolean cacheDrawn = false;
            if (danmaku.hasDrawingCache()) {
                DrawingCacheHolder holder = ((DrawingCache) danmaku.cache).get();
                if (holder != null && holder.bitmap != null) {
                    if(alphaPaint!=null && alphaPaint.getAlpha()== AlphaValue.TRANSPARENT){
                        holder.bitmap.eraseColor(Color.TRANSPARENT);
                    }else{
                        canvas.drawBitmap(holder.bitmap, left, top, alphaPaint);                    
                    }
                    cacheDrawn = true;
                }
            }
            if (!cacheDrawn) {
                if (alphaPaint != null) {
                    PAINT.setAlpha(alphaPaint.getAlpha());
                } else {
                    resetPaintAlpha(PAINT);
                }
                drawDanmaku(danmaku, canvas, left, top, true);
            }

            if (needRestore) {
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
            HAS_STROKE = CONFIG_HAS_STROKE;
            HAS_SHADOW = CONFIG_HAS_SHADOW;
            ANTI_ALIAS = CONFIG_ANTI_ALIAS;
        }
        TextPaint paint = getPaint(danmaku);
        if (danmaku.lines != null) {
            String[] lines = danmaku.lines;
            if (lines.length == 1) {
                if (HAS_STROKE){
                    paint.setStyle(Style.STROKE);
                    paint.setColor(danmaku.textShadowColor);
                    canvas.drawText(lines[0], left, top - paint.ascent(), paint);
                }
                paint.setStyle(Style.FILL);
                paint.setColor(danmaku.textColor);
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
            if (HAS_STROKE){                
                paint.setStyle(Style.STROKE);
                paint.setColor(danmaku.textShadowColor);
                canvas.drawText(danmaku.text, left, top - paint.ascent(), paint);
            }
            paint.setStyle(Style.FILL);
            paint.setColor(danmaku.textColor);
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
        applyPaintConfig(danmaku, PAINT);
        if (HAS_SHADOW) {
            PAINT.setShadowLayer(3.0f, 0, 0, danmaku.textShadowColor);
        } else {
            PAINT.clearShadowLayer();
        }
        return PAINT;
    }
    
    private static void applyPaintConfig(BaseDanmaku danmaku, Paint paint) {
        if (danmaku.getType() != BaseDanmaku.TYPE_SPECIAL) {
            paint.setAlpha(AlphaValue.MAX);
        } else {
            paint.setAlpha(DanmakuGlobalConfig.DEFAULT.alpha);
        }
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
    
    public static void clearTextHeightCache(){
        TextHeightCache.clear();
    }

    @Override
    public float getScaledDensity() {
        return scaledDensity;
    }

}
