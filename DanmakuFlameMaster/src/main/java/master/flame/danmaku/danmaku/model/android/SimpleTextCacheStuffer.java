package master.flame.danmaku.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import java.util.HashMap;
import java.util.Map;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.SpecialDanmaku;

/**
 * Created by ch on 15-7-16.
 */
public class SimpleTextCacheStuffer extends BaseCacheStuffer {

    private final static Map<Float, Float> sTextHeightCache = new HashMap<Float, Float>();

    protected Float getCacheHeight(BaseDanmaku danmaku, Paint paint) {
        Float textSize = paint.getTextSize();
        Float textHeight = sTextHeightCache.get(textSize);
        if (textHeight == null) {
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            textHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading;
            sTextHeightCache.put(textSize, textHeight);
        }
        return textHeight;
    }

    @Override
    public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
        float w = 0;
        Float textHeight = 0f;
        if (danmaku.lines == null) {
            if (danmaku.text == null) {
                w = 0;
            } else {
                w = paint.measureText(danmaku.text.toString());
                textHeight = getCacheHeight(danmaku, paint);
            }
            danmaku.paintWidth = w;
            danmaku.paintHeight = textHeight;
        } else {
            textHeight = getCacheHeight(danmaku, paint);
            for (String tempStr : danmaku.lines) {
                if (tempStr.length() > 0) {
                    float tr = paint.measureText(tempStr);
                    w = Math.max(tr, w);
                }
            }
            danmaku.paintWidth = w;
            danmaku.paintHeight = danmaku.lines.length * textHeight;
        }
    }

    protected void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
        if (lineText != null) {
            canvas.drawText(lineText, left, top, paint);
        } else {
            canvas.drawText(danmaku.text.toString(), left, top, paint);
        }
    }

    protected void drawText(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, TextPaint paint, boolean fromWorkerThread) {
        if (fromWorkerThread && danmaku instanceof SpecialDanmaku) {
            paint.setAlpha(255);
        }
        if (lineText != null) {
            canvas.drawText(lineText, left, top, paint);
        } else {
            canvas.drawText(danmaku.text.toString(), left, top, paint);
        }
    }

    @Override
    public void clearCaches() {
        sTextHeightCache.clear();
    }

    protected void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top) {

    }

    @Override
    public void drawDanmaku(BaseDanmaku danmaku, Canvas canvas, float left, float top, boolean fromWorkerThread, AndroidDisplayer.DisplayerConfig displayerConfig) {
        float _left = left;
        float _top = top;
        left += danmaku.padding;
        top += danmaku.padding;
        if (danmaku.borderColor != 0) {
            left += displayerConfig.BORDER_WIDTH;
            top += displayerConfig.BORDER_WIDTH;
        }

        displayerConfig.definePaintParams(fromWorkerThread);
        TextPaint paint = displayerConfig.getPaint(danmaku, fromWorkerThread);
        drawBackground(danmaku, canvas, _left, _top);
        if (danmaku.lines != null) {
            String[] lines = danmaku.lines;
            if (lines.length == 1) {
                if (displayerConfig.hasStroke(danmaku)) {
                    displayerConfig.applyPaintConfig(danmaku, paint, true);
                    float strokeLeft = left;
                    float strokeTop = top - paint.ascent();
                    if (displayerConfig.HAS_PROJECTION) {
                        strokeLeft += displayerConfig.sProjectionOffsetX;
                        strokeTop += displayerConfig.sProjectionOffsetY;
                    }
                    drawStroke(danmaku, lines[0], canvas, strokeLeft, strokeTop, paint);
                }
                displayerConfig.applyPaintConfig(danmaku, paint, false);
                drawText(danmaku, lines[0], canvas, left, top - paint.ascent(), paint, fromWorkerThread);
            } else {
                float textHeight = (danmaku.paintHeight - 2 * danmaku.padding) / lines.length;
                for (int t = 0; t < lines.length; t++) {
                    if (lines[t] == null || lines[t].length() == 0) {
                        continue;
                    }
                    if (displayerConfig.hasStroke(danmaku)) {
                        displayerConfig.applyPaintConfig(danmaku, paint, true);
                        float strokeLeft = left;
                        float strokeTop = t * textHeight + top - paint.ascent();
                        if (displayerConfig.HAS_PROJECTION) {
                            strokeLeft += displayerConfig.sProjectionOffsetX;
                            strokeTop += displayerConfig.sProjectionOffsetY;
                        }
                        drawStroke(danmaku, lines[t], canvas, strokeLeft, strokeTop, paint);
                    }
                    displayerConfig.applyPaintConfig(danmaku, paint, false);
                    drawText(danmaku, lines[t], canvas, left, t * textHeight + top - paint.ascent(), paint, fromWorkerThread);
                }
            }
        } else {
            if (displayerConfig.hasStroke(danmaku)) {
                displayerConfig.applyPaintConfig(danmaku, paint, true);
                float strokeLeft = left;
                float strokeTop = top - paint.ascent();

                if (displayerConfig.HAS_PROJECTION) {
                    strokeLeft += displayerConfig.sProjectionOffsetX;
                    strokeTop += displayerConfig.sProjectionOffsetY;
                }
                drawStroke(danmaku, null, canvas, strokeLeft, strokeTop, paint);
            }

            displayerConfig.applyPaintConfig(danmaku, paint, false);
            drawText(danmaku, null, canvas, left, top - paint.ascent(), paint, fromWorkerThread);
        }

        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = displayerConfig.getUnderlinePaint(danmaku);
            float bottom = _top + danmaku.paintHeight - displayerConfig.UNDERLINE_HEIGHT;
            canvas.drawLine(_left, bottom, _left + danmaku.paintWidth, bottom, linePaint);
        }

        //draw border
        if (danmaku.borderColor != 0) {
            Paint borderPaint = displayerConfig.getBorderPaint(danmaku);
            canvas.drawRect(_left, _top, _left + danmaku.paintWidth, _top + danmaku.paintHeight,
                    borderPaint);
        }

    }

}
