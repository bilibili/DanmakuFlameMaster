
package master.flame.danmaku.controller;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.RingBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class DrawingCache {

    public Object mBufferLock = new Object();

    Thread mThread = new Thread() {

        @Override
        public void run() {

            while (!quitFlag) {

                while (true) {
                    synchronized (mBufferLock) {

                        if (mScrapList.size() > 0) {
                            DrawingCacheHolder holder = mScrapList.get(0);
                            drawCache(holder);
                            mBuffer.put(holder);
                        }

                        if (mBuffer.isFull()) {
                            break;
                        }
                    }
                }

                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    };

    private IDisplayer mDisp;

    private HolderRingBuffer mBuffer;

    private List<DrawingCacheHolder> mScrapList = new ArrayList<DrawingCacheHolder>();

    private boolean quitFlag;

    public DrawingCache(int capacity, IDisplayer disp) {
        mBuffer = new HolderRingBuffer(capacity);
        mDisp = disp;
        for (int i = 0; i < capacity; i++) {
            mScrapList.add(new DrawingCacheHolder(disp.getWidth(), disp.getHeight()));
        }
    }

    public Bitmap getCache() {
        Bitmap bmp = null;
        synchronized (mBufferLock) {
            DrawingCacheHolder holder = mBuffer.get();
            if (holder != null) {
                bmp = holder.bitmap;
            }
        }
        return bmp;
    }

    /**
     * 使用cache之后调用
     */
    public void fillNext() {
        synchronized (this) {
            notify();
        }
    }

    protected abstract void drawCache(DrawingCacheHolder holder);

    public class DrawingCacheHolder {

        public Canvas canvas;

        public Bitmap bitmap;

        public int time;

        public DrawingCacheHolder(int w, int h) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
        }

    }

    public class HolderRingBuffer extends RingBuffer<DrawingCacheHolder> {

        public HolderRingBuffer(int capacity) {
            super(capacity);
        }

        @Override
        protected DrawingCacheHolder[] createBuffer(int capacity) {
            return new DrawingCacheHolder[capacity];
        }
    }

}
