
package master.flame.danmaku.danmaku.model.android;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import tv.cjump.jni.NativeBitmapFactory;

public class DrawingCacheHolder {

    public Canvas canvas;

    public Bitmap bitmap;
    
    public Bitmap[][] bitmapArray;

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
            recycleBitmapArray();
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
        eraseBitmap(bitmap);
        eraseBitmapArray();
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
        recycleBitmapArray();
        extra = null;
    }

    @SuppressLint("NewApi")
    public void splitWith(int maximumCacheWidth, int maximumCacheHeight) {
        recycleBitmapArray();
        if (width <= 0 || height <= 0 || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        if (width <= maximumCacheWidth && height <= maximumCacheHeight) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            bitmap.setPremultiplied(true);
        }
        int xCount = width / maximumCacheWidth + (width % maximumCacheWidth == 0 ? 0 : 1);
        int yCount = height / maximumCacheHeight + (height % maximumCacheHeight == 0 ? 0 : 1);
        int averageWidth = width / xCount;
        int averageHeight = height / yCount;
        final Bitmap[][] bmpArray = new Bitmap[yCount][xCount];
        for (int yIndex = 0; yIndex < yCount; yIndex++) {
            for (int xIndex = 0; xIndex < xCount; xIndex++) {
                bmpArray[yIndex][xIndex] = Bitmap.createBitmap(bitmap, xIndex * averageWidth,
                        yIndex * averageHeight, averageWidth, averageHeight);
            }
        }
        bitmapArray = bmpArray;
    }

    private void eraseBitmap(Bitmap bmp) {
        if (bmp != null && !bmp.isRecycled()) {
            bmp.eraseColor(Color.TRANSPARENT);
        }
    }

    private void eraseBitmapArray() {
        if (bitmapArray != null) {
            for (int i = 0; i < bitmapArray.length; i++) {
                for (int j = 0; j < bitmapArray[i].length; j++) {
                    eraseBitmap(bitmapArray[i][j]);
                }
            }
        }
    }

    private void recycleBitmapArray() {
        if (bitmapArray != null) {
            for (int i = 0; i < bitmapArray.length; i++) {
                for (int j = 0; j < bitmapArray[i].length; j++) {
                    if (bitmapArray[i][j] != null) {
                        bitmapArray[i][j].recycle();
                        bitmapArray[i][j] = null;
                    }
                }
            }
            bitmapArray = null;
        }
    }

    public final boolean draw(Canvas canvas, float left, float top, Paint paint) {
        if (bitmapArray != null) {
            for (int i = 0; i < bitmapArray.length; i++) {
                for (int j = 0; j < bitmapArray[i].length; j++) {
                    Bitmap bmp = bitmapArray[i][j];
                    if (bmp != null && !bmp.isRecycled()) {
                        canvas.drawBitmap(bmp, left + j * bmp.getWidth(),
                                top + i * bmp.getHeight(), paint);
                    }
                }
            }
            return true;
        } else if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, left, top, paint);
            return true;
        }
        return false;
    }

}
