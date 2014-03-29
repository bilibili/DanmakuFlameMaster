
package master.flame.danmaku.danmaku.model;

import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;

/**
 * @author Yrom
 */
public class SpannedDanmku extends R2LDanmaku {

    private StaticLayout mLayoutInner;

    public SpannedDanmku(Duration duration) {
        super(duration);
    }

    @Override
    public void measure(IDisplayer displayer) {
        super.measure(displayer);
    }

    public void drawLayout(Canvas canvas, TextPaint paint, float left, float top) {
        if (mLayoutInner == null)
            return;
        boolean needRestore = false;
        if (left != 0 || top != 0) {
            canvas.save();
            canvas.translate(-left, -top);
            needRestore = true;
        }
        mLayoutInner.draw(canvas);
        if (needRestore) {
            canvas.restore();
        }
    }

    public float getLineWidth(int line, TextPaint paint) {
        if (mLayoutInner == null) {
            mLayoutInner = new StaticLayout(text, paint, (int) StaticLayout.getDesiredWidth(text,
                    paint), Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        }
        return mLayoutInner.getLineWidth(line);
    }
}
