package master.flame.danmaku.danmaku.model;

import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;

/**
 * 
 * @author Yrom
 *
 */
public class SpannedDanmku extends R2LDanmaku {

    private StaticLayout mLayoutInner;

    public SpannedDanmku(Duration duration) {
        super(duration);
    }
    
    @Override
    public void measure(IDisplayer displayer) {
        super.measure(displayer);
        if(mLayoutInner == null){
            TextPaint paint = AndroidDisplayer.getPaint(this);
            mLayoutInner = new StaticLayout(text, paint,  (int) paintWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        }
    }
    
    public void drawLayout(Canvas canvas){
        mLayoutInner.draw(canvas);
    }
}
