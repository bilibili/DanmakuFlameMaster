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

import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ch on 13-7-5.
 */
public class AndroidDisplayer extends AbsDisplayer<Canvas> {

    private Camera camera = new Camera();

    private Matrix matrix = new Matrix();
    
    private final static HashMap<Float,Float> TextHeightCache = new HashMap<Float,Float>(); // thread safe is not Necessary
    
    private static float sLastScaleTextSize;
    private static Map<Float,Float> cachedScaleSize = new HashMap<Float, Float>(10);

    @SuppressWarnings("unused")
    private int HIT_CACHE_COUNT = 0;

    @SuppressWarnings("unused")
    private int NO_CACHE_COUNT = 0;

    public static TextPaint PAINT, PAINT_DUPLICATE;

    private static Paint ALPHA_PAINT;

    private static Paint UNDERLINE_PAINT;
    
    /**
     * 下划线高度
     */
    public static int UNDERLINE_HEIGHT = 4;
    

    /**
     * 开启阴影，可动态改变
     */
    public static boolean CONFIG_HAS_SHADOW = false;
    private static boolean HAS_SHADOW = CONFIG_HAS_SHADOW;

    /**
     * 开启描边，可动态改变
     */
    public static boolean CONFIG_HAS_STROKE = true;
    private static boolean HAS_STROKE = CONFIG_HAS_STROKE;

    /**
     * 开启抗锯齿，可动态改变
     */
    public static boolean CONFIG_ANTI_ALIAS = true;
    private static boolean ANTI_ALIAS = CONFIG_ANTI_ALIAS;

    static {
        PAINT = new TextPaint();
        PAINT.setStrokeWidth(3.5f);
        PAINT_DUPLICATE = new TextPaint(PAINT);
        ALPHA_PAINT = new Paint();
        UNDERLINE_PAINT = new Paint();
        UNDERLINE_PAINT.setStrokeWidth(UNDERLINE_HEIGHT);
        UNDERLINE_PAINT.setStyle(Style.STROKE);
    }
    
    public static void setTypeFace(Typeface font){
        if(PAINT!=null)
            PAINT.setTypeface(font);
    }
    
    public static void setPaintStorkeWidth(float s){
        PAINT.setStrokeWidth(s);
    }
    
    public static void setFakeBoldText(boolean fakeBoldText){
        PAINT.setFakeBoldText(fakeBoldText);
    }

    public Canvas canvas;

    private int width;

    private int height;

    private float density = 1;

    private int densityDpi = 160;

    private float scaledDensity = 1;

    private int slopPixel = 0;
    
    private long lastAverageRenderingTime = 16;

    private void update(Canvas c) {
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
            
            // skip drawing when danmaku is transparent
            if(alphaPaint!=null && alphaPaint.getAlpha()== AlphaValue.TRANSPARENT){
                return;
            }
            // drawing cache
            boolean cacheDrawn = false;
            if (danmaku.hasDrawingCache()) {
                DrawingCacheHolder holder = ((DrawingCache) danmaku.cache).get();
                if (holder != null && holder.bitmap != null) {
                    canvas.drawBitmap(holder.bitmap, left, top, alphaPaint);                    
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
        HAS_STROKE = CONFIG_HAS_STROKE;
        HAS_SHADOW = CONFIG_HAS_SHADOW;
        if (quick) {
            ANTI_ALIAS = false;
        } else {
            ANTI_ALIAS = CONFIG_ANTI_ALIAS;
        }
        TextPaint paint = getPaint(danmaku, quick);
        if (danmaku.lines != null) {
            String[] lines = danmaku.lines;
            if (lines.length == 1) {
                if (HAS_STROKE){
                    applyPaintConfig(danmaku, paint, true);
                    canvas.drawText(lines[0], left, top - paint.ascent(), paint);
                }
                applyPaintConfig(danmaku, paint, false);
                canvas.drawText(lines[0], left, top - paint.ascent(), paint);
            } else {
                Float textHeight = getTextHeight(paint);
                for (int t = 0; t < lines.length; t++) {
                    if (lines[t] == null || lines[t].length() == 0) {
                        continue;
                    }
                    if (HAS_STROKE){
                        applyPaintConfig(danmaku, paint, true);
                        canvas.drawText(lines[t], left, t * textHeight + top - paint.ascent(), paint);
                    }
                    applyPaintConfig(danmaku, paint, false);
                    canvas.drawText(lines[t], left, t * textHeight + top - paint.ascent(), paint);
                }
            }
        } else {
            if (HAS_STROKE){                
                applyPaintConfig(danmaku, paint, true);
                canvas.drawText(danmaku.text, left, top - paint.ascent(), paint);
            }
            applyPaintConfig(danmaku, paint, false);
            canvas.drawText(danmaku.text, left, top - paint.ascent(), paint);
        }

        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = getUnderlinePaint(danmaku);
            float bottom = top + danmaku.paintHeight - UNDERLINE_HEIGHT;
            canvas.drawLine(left, bottom, left + danmaku.paintWidth, bottom, linePaint);
        }

    }

    public static Paint getUnderlinePaint(BaseDanmaku danmaku){
        UNDERLINE_PAINT.setColor(danmaku.underlineColor);
        return UNDERLINE_PAINT;
    }
    
    private static TextPaint getPaint(BaseDanmaku danmaku, boolean quick) {
        TextPaint paint = null;
        if (quick && HAS_SHADOW) {
            paint = PAINT_DUPLICATE;
            paint.set(PAINT);
        } else {
            paint = PAINT;
        }
        paint.setTextSize(danmaku.textSize);
        applyTextScaleConfig(danmaku, paint);
        if (HAS_SHADOW) {
            paint.setShadowLayer(3.0f, 0, 0, danmaku.textShadowColor);
        } else {
            paint.clearShadowLayer();
        }
        paint.setAntiAlias(ANTI_ALIAS);
        return paint;
    }

    public static TextPaint getPaint(BaseDanmaku danmaku) {
        return getPaint(danmaku, false);
    }
    
    private static void applyPaintConfig(BaseDanmaku danmaku, Paint paint,boolean stroke) {

        if (DanmakuGlobalConfig.DEFAULT.isTranslucent) {
            if(stroke){
                paint.setStyle(Style.STROKE);
                int color = (danmaku.textShadowColor & 0x00FFFFFF) | (DanmakuGlobalConfig.DEFAULT.transparency<<24);
                paint.setColor(color);
            }else{
                paint.setStyle(Style.FILL);
                int color = (danmaku.textColor & 0x00FFFFFF) | (DanmakuGlobalConfig.DEFAULT.transparency<<24);
                paint.setColor(color);
            }
            paint.setAlpha(DanmakuGlobalConfig.DEFAULT.transparency);
        } else {
            if(stroke){
                paint.setStyle(Style.STROKE);
                paint.setColor(danmaku.textShadowColor);
            }else{
                paint.setStyle(Style.FILL);
                paint.setColor(danmaku.textColor);
            }
            paint.setAlpha(AlphaValue.MAX);
        }
            
    }

    private static void applyTextScaleConfig(BaseDanmaku danmaku, Paint paint) {
        if (!DanmakuGlobalConfig.DEFAULT.isTextScaled) {
            return;
        }
        Float size = cachedScaleSize.get(danmaku.textSize);
        if (size == null || sLastScaleTextSize != DanmakuGlobalConfig.DEFAULT.scaleTextSize) {
            sLastScaleTextSize = DanmakuGlobalConfig.DEFAULT.scaleTextSize;
            size = Float.valueOf(danmaku.textSize * DanmakuGlobalConfig.DEFAULT.scaleTextSize);
            cachedScaleSize.put(danmaku.textSize, size);
        }
        paint.setTextSize(size.floatValue());
    }
    
    @Override
    public void measure(BaseDanmaku danmaku) {
        TextPaint paint = getPaint(danmaku);
        if (HAS_STROKE) {
            applyPaintConfig(danmaku, paint, true);
        }
        calcPaintWH(danmaku, paint);
        if (HAS_STROKE) {
            applyPaintConfig(danmaku, paint, false);
        }
    } 
    
    private void calcPaintWH(BaseDanmaku danmaku, TextPaint paint) {
        float w = 0;
        Float textHeight = getTextHeight(paint);
        if (danmaku.lines == null) {
            w = danmaku.text == null ? 0 : paint.measureText(danmaku.text);
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
        cachedScaleSize.clear();
    }

    @Override
    public float getScaledDensity() {
        return scaledDensity;
    }

    @Override
    public void resetSlopPixel(float factor) {
        float d = Math.max(density, scaledDensity);
        d = Math.max(factor, getWidth() / (float) DanmakuFactory.BILI_PLAYER_WIDTH); //correct for low density and high resolution
        float slop = d * DanmakuFactory.DANMAKU_MEDIUM_TEXTSIZE; 
        slopPixel = (int) slop;
        if (factor > 1f)
            slopPixel = (int) (slop * factor);
    }

    @Override
    public int getSlopPixel() {
        return slopPixel;
    }

    @Override
    public void setDensities(float density, int densityDpi, float scaledDensity) {
        this.density = density;
        this.densityDpi = densityDpi;
        this.scaledDensity = scaledDensity;
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setExtraData(Canvas data) {
        update(data);
    }

    @Override
    public Canvas getExtraData() {
        return this.canvas;
    }

    @Override
    public long getAverageRenderingTime() {
        return this.lastAverageRenderingTime;
    }

    @Override
    public void setAverageRenderingTime(long ms) {
        this.lastAverageRenderingTime = ms;
    }

}
