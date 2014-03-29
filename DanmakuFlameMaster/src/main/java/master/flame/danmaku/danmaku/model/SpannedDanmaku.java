package master.flame.danmaku.danmaku.model;

import android.graphics.Canvas;
import android.text.TextPaint;

/**
 * For the Danmaku which text is {@code Spanned}.
 * <br>
 * SpannedDanmaku have more text-styles, just like insert ImageSpan to support emotion
 * @author Yrom
 *
 */
public interface SpannedDanmaku {
    
    /**
     * Draw spanned text with {@code Layout}
     * @param canvas
     * @param paint
     * @param left
     * @param top
     */
    void drawLayout(Canvas canvas, TextPaint paint, float left, float top);
    
    /**
     * @param paint
     */
    void measureWithLayout(TextPaint paint);
    
    /**
     * Return {@code false} if {@code Danmaku} wouldn't want to be drawn in Layout, event 
     * though it's text under spanned.
     */
    boolean isDanmakuSpanned();
}
