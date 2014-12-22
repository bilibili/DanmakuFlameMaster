
package master.flame.danmaku.danmaku.model.android;

import android.graphics.Bitmap;

import master.flame.danmaku.danmaku.model.ICanvas.IBitmapHolder;

public class BitmapHolder implements IBitmapHolder<android.graphics.Bitmap> {

    private android.graphics.Bitmap mBitmap;
    
    public BitmapHolder() {
        
    }

    public BitmapHolder(Bitmap bmp) {
        attach(bmp);
    }

    @Override
    public void attach(Bitmap t) {
        mBitmap = t;
    }

    @Override
    public android.graphics.Bitmap data() {
        return mBitmap;
    }

    @Override
    public void detach() {
        mBitmap = null;
    }

    @Override
    public void recycle() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        detach();
    }

}
