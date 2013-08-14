
package master.flame.danmaku.danmaku.renderer.android;

import android.graphics.Bitmap;

import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.renderer.RingBuffer;

public class BitmapRingBuffer extends RingBuffer<Bitmap> {

    private Bitmap[] bmpArray;

    public BitmapRingBuffer(int capacity, IDisplayer disp) {
        super(capacity);
    }

    @Override
    protected Bitmap[] createBuffer(int capacity) {
        bmpArray = new Bitmap[capacity];
        return bmpArray;
    }

}
