
package master.flame.danmaku.controller;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.RingBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class DrawingCache {

    private static final String TAG = "DrawingCache";
    public Object mBufferLock = new Object();

    Thread mThread = new Thread() {

        public static final String TAG = "DrawingCache";

        @Override
        public void run() {

            while (!quitFlag) {

                while (true) {
                    synchronized (mBufferLock) {

                        if (mScrapList.size() > 0) {
                            DrawingCacheHolder holder = mScrapList.get(0);
                            mScrapList.remove(0);
                            drawCache(holder);
                            mBuffer.put(holder);
                        }

                        if (mScrapList.isEmpty()) {
                            break;
                        }
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                synchronized (this) {
                    try {
                        Log.e(TAG, "thread wait");
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
                mScrapList.add(holder);
            }
            Log.e(TAG, "buffered:" + mBuffer.size() + ",scrap:" + mScrapList.size());
        }
        return bmp;
    }

    /**
     * 使用cache之后调用
     */
    public void fillNext() {
        synchronized (mThread) {
            mThread.notify();
        }
    }

    protected abstract void drawCache(DrawingCacheHolder holder);

    public void start() {
        mThread.start();
    }

    public class DrawingCacheHolder {

        public Canvas canvas;

        public Bitmap bitmap;

        public Object extra;

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
