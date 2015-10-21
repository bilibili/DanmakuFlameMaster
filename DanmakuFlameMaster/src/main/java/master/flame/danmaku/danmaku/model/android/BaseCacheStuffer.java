package master.flame.danmaku.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.BaseDanmaku;

/**
 * Created by ch on 15-7-16.
 *
 * 缓存填充器
 */
public abstract class BaseCacheStuffer {

    /**
     * set paintWidth, paintHeight to danmaku  是整个画的高度，多行的即多行的高度
     * @param danmaku
     */
    public abstract void measure(BaseDanmaku danmaku, TextPaint paint);

    /**
     * draw the danmaku-stroke on canvas with the given params
     * @param danmaku
     * @param lineText
     * @param canvas
     * @param left
     * @param top
     * @param paint
     */
    public abstract void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint);

    /**
     * draw the danmaku-text on canvas with the given params
     * @param danmaku
     * @param lineText
     * @param canvas
     * @param left
     * @param top
     * @param paint
     */
    public abstract void drawText(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint);

    /**
     * clear caches which created by this stuffer
     */
    public abstract void clearCaches();

    /**
     * draw the background in rect (left, top, left + danmaku.paintWidth, top + danmaku.paintHeight)
     * @param danmaku
     * @param canvas
     * @param left
     * @param top
     */
    public abstract void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top);
}
