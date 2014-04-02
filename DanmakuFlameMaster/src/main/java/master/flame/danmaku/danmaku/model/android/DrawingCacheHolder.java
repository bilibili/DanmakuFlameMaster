
package master.flame.danmaku.danmaku.model.android;

import tv.cjump.jni.NativeBitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import master.flame.danmaku.controller.DrawHelper;

public class DrawingCacheHolder {

    public Canvas canvas;

    public Bitmap bitmap;

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
        boolean reuse = checkSizeEquals ? (w == width && h == height) : (w <= width && h <= height);
        if (reuse && bitmap != null && !bitmap.isRecycled()) {
//            canvas.drawColor(Color.TRANSPARENT);
            canvas.setBitmap(null);
            bitmap.eraseColor(Color.TRANSPARENT);
            canvas.setBitmap(bitmap);            
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
        if (canvas == null){
            canvas = new Canvas(bitmap);
            canvas.setDensity(density);
        }else
            canvas.setBitmap(bitmap);
    }
    
    public void erase() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.eraseColor(Color.TRANSPARENT);
            return;
        }
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


}
