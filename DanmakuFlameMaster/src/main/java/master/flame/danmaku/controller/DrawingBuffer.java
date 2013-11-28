
package master.flame.danmaku.controller;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.RingBuffer;
import master.flame.danmaku.danmaku.model.android.DrawingCacheHolder;

public abstract class DrawingBuffer {

    private static final String TAG = "DrawingBuffer";

    public Object mBufferLock = new Object();

    Thread mThread = new Thread(TAG) {

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
                    }

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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

    public DrawingBuffer(int capacity, IDisplayer disp) {
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

    public void quit() {
        quitFlag = true;
        fillNext();
        recyle();
    }

    public void recyle() {
        clear();
        synchronized (mBufferLock) {
            if (mScrapList != null) {
                Iterator<DrawingCacheHolder> it = mScrapList.iterator();
                while (it.hasNext()) {
                    DrawingCacheHolder holder = it.next();
                    holder.recycle();
                    it.remove();
                }
            }
        }
    }

    public void clear() {
        synchronized (mBufferLock) {
            while (!mBuffer.isEmpty()) {
                getCache();
            }
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
