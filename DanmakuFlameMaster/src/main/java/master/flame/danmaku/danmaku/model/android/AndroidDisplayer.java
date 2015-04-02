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

import android.annotation.SuppressLint;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.renderer.IRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by MoiTempete.
 */
public class AndroidDisplayer extends AbsDisplayer<Canvas> {

    private Camera camera = new Camera();

    private Matrix matrix = new Matrix();
    
    private final static Map<Float,Float> sTextHeightCache = new HashMap<Float,Float>();
    
    private static float sLastScaleTextSize;
    private final static Map<Float,Float> sCachedScaleSize = new HashMap<Float, Float>(10);

    @SuppressWarnings("unused")
    private int HIT_CACHE_COUNT = 0;

    @SuppressWarnings("unused")
    private int NO_CACHE_COUNT = 0;

    public static TextPaint PAINT, PAINT_DUPLICATE;

    private static Paint ALPHA_PAINT;

    private static Paint UNDERLINE_PAINT;
    
    private static Paint BORDER_PAINT;

    /**
     * 下划线高度
     */
    public static int UNDERLINE_HEIGHT = 4;
    
    /**
     * 边框厚度
     */
    public static final int BORDER_WIDTH = 4;

    /**
     * 阴影半径
     */
    private static float SHADOW_RADIUS = 4.0f;

    /**
     * 描边宽度
     */
    private static float STROKE_WIDTH = 3.5f;

    /**
     * 投影参数
     */
    private static float sProjectionOffsetX = 1.0f;
    private static float sProjectionOffsetY = 1.0f;
    private static int sProjectionAlpha = 0xCC;

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
     * 开启投影，可动态改变
     */
    public static boolean CONFIG_HAS_PROJECTION = false;
    private static boolean HAS_PROJECTION = CONFIG_HAS_PROJECTION;

    /**
     * 开启抗锯齿，可动态改变
     */
    public static boolean CONFIG_ANTI_ALIAS = true;
    private static boolean ANTI_ALIAS = CONFIG_ANTI_ALIAS;

    static {
        PAINT = new TextPaint();
        PAINT.setStrokeWidth(STROKE_WIDTH);
        PAINT_DUPLICATE = new TextPaint(PAINT);
        ALPHA_PAINT = new Paint();
        UNDERLINE_PAINT = new Paint();
        UNDERLINE_PAINT.setStrokeWidth(UNDERLINE_HEIGHT);
        UNDERLINE_PAINT.setStyle(Style.STROKE);
        BORDER_PAINT = new Paint();
        BORDER_PAINT.setStyle(Style.STROKE);
        BORDER_PAINT.setStrokeWidth(BORDER_WIDTH);
    }
    
    @SuppressLint("NewApi")
    private static final int getMaximumBitmapWidth(Canvas c) {
        if(Build.VERSION.SDK_INT >= 14) {
            return c.getMaximumBitmapWidth();
        } else {
            return c.getWidth();
        }
    }

    @SuppressLint("NewApi")
    private static final int getMaximumBitmapHeight(Canvas c) {
        if(Build.VERSION.SDK_INT >= 14) {
            return c.getMaximumBitmapHeight();
        } else {
            return c.getHeight();
        }
    }

    public static void setTypeFace(Typeface font){
        if(PAINT!=null)
            PAINT.setTypeface(font);
    }
    
    public static void setShadowRadius(float s){
        SHADOW_RADIUS = s;
    }
    
    public static void setPaintStorkeWidth(float s){
        PAINT.setStrokeWidth(s);
        STROKE_WIDTH = s;
    }

    public static void setProjectionConfig(float offsetX, float offsetY, int alpha) {
        if (sProjectionOffsetX != offsetX || sProjectionOffsetY != offsetY || sProjectionAlpha != alpha) {
            sProjectionOffsetX = (offsetX > 1.0f) ? offsetX : 1.0f;
            sProjectionOffsetY = (offsetY > 1.0f) ? offsetY : 1.0f;
            sProjectionAlpha = (alpha < 0) ? 0 : ((alpha > 255) ? 255 : alpha);
        }
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

    private int mSlopPixel = 0;

    private boolean mIsHardwareAccelerated = true;

    private int mMaximumBitmapWidth = 2048;

    private int mMaximumBitmapHeight = 2048;

    private void update(Canvas c) {
        canvas = c;
        if (c != null) {
            width = c.getWidth();
            height = c.getHeight();
            if (mIsHardwareAccelerated) {
                mMaximumBitmapWidth = getMaximumBitmapWidth(c);
                mMaximumBitmapHeight = getMaximumBitmapHeight(c);
            }
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
    public int draw(BaseDanmaku danmaku) {
        float top = danmaku.getTop();
        float left = danmaku.getLeft();
        if (canvas != null) {

            Paint alphaPaint = null;
            boolean needRestore = false;
            if (danmaku.getType() == BaseDanmaku.TYPE_SPECIAL) {
                if (danmaku.getAlpha() == AlphaValue.TRANSPARENT) {
                    return IRenderer.NOTHING_RENDERING;
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
                return IRenderer.NOTHING_RENDERING;
            }
            // drawing cache
            boolean cacheDrawn = false;
            int result = IRenderer.CACHE_RENDERING;
            if (danmaku.hasDrawingCache()) {
                DrawingCacheHolder holder = ((DrawingCache) danmaku.cache).get();
                if (holder != null) {
                    cacheDrawn = holder.draw(canvas, left, top, alphaPaint);
                }
            }
            if (!cacheDrawn) {
                if (alphaPaint != null) {
                    PAINT.setAlpha(alphaPaint.getAlpha());
                } else {
                    resetPaintAlpha(PAINT);
                }
                drawDanmaku(danmaku, canvas, left, top, true);
                result = IRenderer.TEXT_RENDERING;
            }

            if (needRestore) {
                restoreCanvas(canvas);
            }
            
            return result;
        }
        
        return IRenderer.NOTHING_RENDERING;
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
        float _left = left;
        float _top = top;
        left += danmaku.padding;
        top += danmaku.padding;
        if (danmaku.borderColor != 0) {
            left += BORDER_WIDTH;
            top += BORDER_WIDTH;
        }
        
        HAS_STROKE = CONFIG_HAS_STROKE;
        HAS_SHADOW = CONFIG_HAS_SHADOW;
        HAS_PROJECTION = CONFIG_HAS_PROJECTION;
        ANTI_ALIAS = !quick && CONFIG_ANTI_ALIAS;
        TextPaint paint = getPaint(danmaku, quick);
        if (danmaku.lines != null) {
            String[] lines = danmaku.lines;
            if (lines.length == 1) {
                if (hasStroke(danmaku)) {
                    applyPaintConfig(danmaku, paint, true);
                    float strokeLeft = left;
                    float strokeTop = top - paint.ascent();
                    if (HAS_PROJECTION) {
                        strokeLeft += sProjectionOffsetX;
                        strokeTop += sProjectionOffsetY;
                    }
                    canvas.drawText(lines[0], strokeLeft, strokeTop, paint);
                }
                applyPaintConfig(danmaku, paint, false);
                canvas.drawText(lines[0], left, top - paint.ascent(), paint);
            } else {
                float textHeight = (danmaku.paintHeight - 2 * danmaku.padding) / lines.length;
                for (int t = 0; t < lines.length; t++) {
                    if (lines[t] == null || lines[t].length() == 0) {
                        continue;
                    }
                    if (hasStroke(danmaku)) {
                        applyPaintConfig(danmaku, paint, true);
                        float strokeLeft = left;
                        float strokeTop = t * textHeight + top - paint.ascent();
                        if (HAS_PROJECTION) {
                            strokeLeft += sProjectionOffsetX;
                            strokeTop += sProjectionOffsetY;
                        }
                        canvas.drawText(lines[t], strokeLeft, strokeTop, paint);
                    }
                    applyPaintConfig(danmaku, paint, false);
                    canvas.drawText(lines[t], left, t * textHeight + top - paint.ascent(), paint);
                }
            }
        } else {
            if (hasStroke(danmaku)) {                
                applyPaintConfig(danmaku, paint, true);
                float strokeLeft = left;
                float strokeTop = top - paint.ascent();
                if (HAS_PROJECTION) {
                    strokeLeft += sProjectionOffsetX;
                    strokeTop += sProjectionOffsetY;
                }
                canvas.drawText(danmaku.text, strokeLeft, strokeTop, paint);
            }
            applyPaintConfig(danmaku, paint, false);
            canvas.drawText(danmaku.text, left, top - paint.ascent(), paint);
        }

        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = getUnderlinePaint(danmaku);
            float bottom = _top + danmaku.paintHeight - UNDERLINE_HEIGHT;
            canvas.drawLine(_left, bottom, _left + danmaku.paintWidth, bottom, linePaint);
        }
        
        //draw border
        if (danmaku.borderColor != 0) {
            Paint borderPaint = getBorderPaint(danmaku);
            canvas.drawRect(_left, _top, _left + danmaku.paintWidth, _top + danmaku.paintHeight,
                    borderPaint);
        }

    }
    
    private static boolean hasStroke(BaseDanmaku danmaku) {
        return (HAS_STROKE || HAS_PROJECTION) && STROKE_WIDTH > 0 && danmaku.textShadowColor != 0;
    }

    public static Paint getBorderPaint(BaseDanmaku danmaku) {
        BORDER_PAINT.setColor(danmaku.borderColor);
        return BORDER_PAINT;
    }

    public static Paint getUnderlinePaint(BaseDanmaku danmaku){
        UNDERLINE_PAINT.setColor(danmaku.underlineColor);
        return UNDERLINE_PAINT;
    }
    
    private static TextPaint getPaint(BaseDanmaku danmaku, boolean quick) {
        TextPaint paint;
        if (quick) {
            paint = PAINT_DUPLICATE;
            paint.set(PAINT);
        } else {
            paint = PAINT;
        }
        paint.setTextSize(danmaku.textSize);
        applyTextScaleConfig(danmaku, paint);
        
        //ignore the transparent textShadowColor
        if (!HAS_SHADOW || SHADOW_RADIUS <= 0 || danmaku.textShadowColor == 0) {
            paint.clearShadowLayer();
        } else {
            paint.setShadowLayer(SHADOW_RADIUS, 0, 0, danmaku.textShadowColor);
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
                paint.setStyle(HAS_PROJECTION ? Style.FILL : Style.STROKE);
                paint.setColor(danmaku.textShadowColor & 0x00FFFFFF);
                int alpha = HAS_PROJECTION ? (sProjectionAlpha * (DanmakuGlobalConfig.DEFAULT.transparency / AlphaValue.MAX))
                        : DanmakuGlobalConfig.DEFAULT.transparency;
                paint.setAlpha(alpha);
            }else{
                paint.setStyle(Style.FILL);
                paint.setColor(danmaku.textColor & 0x00FFFFFF);
                paint.setAlpha(DanmakuGlobalConfig.DEFAULT.transparency);
            }
        } else {
            if(stroke){
                paint.setStyle(HAS_PROJECTION ? Style.FILL : Style.STROKE);
                paint.setColor(danmaku.textShadowColor & 0x00FFFFFF);
                int alpha = HAS_PROJECTION ? sProjectionAlpha : AlphaValue.MAX;
                paint.setAlpha(alpha);
            }else{
                paint.setStyle(Style.FILL);
                paint.setColor(danmaku.textColor & 0x00FFFFFF);
                paint.setAlpha(AlphaValue.MAX);
            }
        }
            
    }

    private static void applyTextScaleConfig(BaseDanmaku danmaku, Paint paint) {
        if (!DanmakuGlobalConfig.DEFAULT.isTextScaled) {
            return;
        }
        Float size = sCachedScaleSize.get(danmaku.textSize);
        if (size == null || sLastScaleTextSize != DanmakuGlobalConfig.DEFAULT.scaleTextSize) {
            sLastScaleTextSize = DanmakuGlobalConfig.DEFAULT.scaleTextSize;
            size = danmaku.textSize * DanmakuGlobalConfig.DEFAULT.scaleTextSize;
            sCachedScaleSize.put(danmaku.textSize, size);
        }
        paint.setTextSize(size);
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
            setDanmakuPaintWidthAndHeight(danmaku,w,textHeight);
            return;
        }

        for(String tempStr : danmaku.lines){
            if (tempStr.length() > 0) {
                float tr = paint.measureText(tempStr);
                w = Math.max(tr, w);
            }
        }

        setDanmakuPaintWidthAndHeight(danmaku,w,danmaku.lines.length * textHeight);
    }
        
    private void setDanmakuPaintWidthAndHeight(BaseDanmaku danmaku, float w, float h) {
        float pw = w + 2 * danmaku.padding;
        float ph = h + 2 * danmaku.padding;
        if (danmaku.borderColor != 0) {
            pw += 2 * BORDER_WIDTH;
            ph += 2 * BORDER_WIDTH;
        }
        danmaku.paintWidth = pw + getStrokeWidth();
        danmaku.paintHeight = ph;
    }

    private static float getTextHeight(TextPaint paint) {
        Float textSize = paint.getTextSize();
        Float textHeight = sTextHeightCache.get(textSize);
        if(textHeight == null){
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            textHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading;
            sTextHeightCache.put(textSize, textHeight);
        }  
        return textHeight;
    }
    
    public static void clearTextHeightCache(){
        sTextHeightCache.clear();
        sCachedScaleSize.clear();
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
        mSlopPixel = (int) slop;
        if (factor > 1f)
            mSlopPixel = (int) (slop * factor);
    }

    @Override
    public int getSlopPixel() {
        return mSlopPixel;
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
    public float getStrokeWidth() {
        if (HAS_SHADOW && HAS_STROKE) {
            return Math.max(SHADOW_RADIUS, STROKE_WIDTH);
        }
        if (HAS_SHADOW) {
            return SHADOW_RADIUS;
        }
        if (HAS_STROKE) {
            return STROKE_WIDTH;
        }
        return 0f;
    }

    @Override
    public void setHardwareAccelerated(boolean enable) {
        mIsHardwareAccelerated = enable;
    }

    @Override
    public boolean isHardwareAccelerated() {
        return mIsHardwareAccelerated ;
    }

    @Override
    public int getMaximumCacheWidth() {
        return mMaximumBitmapWidth;
    }

    @Override
    public int getMaximumCacheHeight() {
        return mMaximumBitmapHeight;
    }


}
