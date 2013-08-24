
package master.flame.danmaku.danmaku.model.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class DrawingCacheHolder {

    public Canvas canvas;

    public Bitmap bitmap;

    public Object extra;

    public int width;

    public int height;

    public boolean drawn;

    public DrawingCacheHolder(int w, int h) {
        width = w;
        height = h;
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    public DrawingCacheHolder(int w, int h, int density) {
        this(w, h);
        bitmap.setDensity(density);
    }

    public void recycle() {
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = null;
        canvas = null;
        extra = null;
    }

}
