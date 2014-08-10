
package master.flame.danmaku.danmaku.model.android;

import android.graphics.Bitmap;
import android.graphics.Color;

import master.flame.danmaku.danmaku.model.ICanvas;
import tv.cjump.jni.NativeBitmapFactory;

public class DrawingCacheHolder {

    public ICanvas<?> canvas;

    public Bitmap bitmap;

    public BitmapHolder bitmapHolder;

    public Object extra;

    public int width;

    public int height;

    public boolean drawn;

    @SuppressWarnings("unused")
    private int mDensity;

    public DrawingCacheHolder() {

    }

    public DrawingCacheHolder(int w, int h) {
        buildCache(w, h, 0, true);
    }

    public DrawingCacheHolder(int w, int h, int density) {
        mDensity = density;
        buildCache(w, h, density, true);
    }

    public void buildCache(int w, int h, int density, boolean checkSizeEquals) {
        if (bitmapHolder == null) {
            bitmapHolder = new BitmapHolder();
        }
        boolean reuse = checkSizeEquals ? (w == width && h == height) : (w <= width && h <= height);
        if (reuse && bitmap != null && !bitmap.isRecycled()) {
            // canvas.drawColor(Color.TRANSPARENT);
            canvas.setBitmap(null);
            bitmap.eraseColor(Color.TRANSPARENT);
            bitmapHolder.attach(bitmap);
            canvas.setBitmap(bitmapHolder);
            return;
        }
        if (bitmap != null) {
            recycle();
        }
        width = w;
        height = h;
        bitmap = NativeBitmapFactory.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        if (density > 0) {
            mDensity = density;
            bitmap.setDensity(density);
        }
        bitmapHolder.attach(bitmap);
        if (canvas == null) {
            canvas = new CommonCanvas(bitmapHolder);
            canvas.setDensity(density);
        } else {
            canvas.setBitmap(bitmapHolder);
        }
    }

    public void erase() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.eraseColor(Color.TRANSPARENT);
        }
    }

    public void recycle() {
        width = height = 0;
        // if (canvas != null) {
        // canvas = null;
        // }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        extra = null;
        if (bitmapHolder != null) {
            bitmapHolder.detach();
        }
    }

}
