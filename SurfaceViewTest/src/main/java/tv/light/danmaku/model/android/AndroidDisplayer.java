
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
    public void drawDanmaku(DanmakuBase danmaku) {
        if (canvas != null) {
            canvas.drawText(danmaku.text, danmaku.getLeft(), danmaku.getTop(), getPaint(danmaku));
        }
    }

    private static Paint getPaint(DanmakuBase danmaku) {
        PAINT.setTextSize(danmaku.textSize);
        PAINT.setColor(danmaku.textColor);
        // TODO: set the text shadow color
        return PAINT;
    }

}
