
package master.flame.danmaku.danmaku.model.android;

import tv.cjump.jni.NativeBitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class DrawingCacheHolder {

    public Canvas canvas;

    public Bitmap bitmap;

    public Object extra;

    public int width;

    public int height;

    public boolean drawn;

    private int mDensity;

    public DrawingCacheHolder() {

    }

    public DrawingCacheHolder(int w, int h) {
        buildCache(w, h, 0);
    }

    public void buildCache(int w, int h, int density) {
        if (w == width && h == height && bitmap != null && !bitmap.isRecycled()) {
            bitmap.eraseColor(Color.TRANSPARENT);
            return;
        }
        if (bitmap != null) {
            recycle();
        }
        width = w;
        height = h;
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        if (density > 0) {
            mDensity = density;
            bitmap.setDensity(density);
        }
        if (canvas == null)
            canvas = new Canvas(bitmap);
        else
            canvas.setBitmap(bitmap);
    }

    public void recycle() {
        width = height = 0;
//        if (canvas != null) {
//            canvas = null;
//        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        extra = null;
    }

    public DrawingCacheHolder(int w, int h, int density) {
        mDensity = density;
        buildCache(w, h, density);
    }

}
