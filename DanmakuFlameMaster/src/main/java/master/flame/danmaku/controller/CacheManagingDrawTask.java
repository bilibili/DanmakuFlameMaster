
package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCachePoolManager;
import master.flame.danmaku.danmaku.model.objectpool.Pool;
import master.flame.danmaku.danmaku.model.objectpool.Pools;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CacheManagingDrawTask extends DrawTask {

    private CacheManager mCacheManager;

    private DanmakuTimer mCacheTimer;

    public CacheManagingDrawTask(DanmakuTimer timer, Context context, int dispW, int dispH,
            TaskListener taskListener, int maxCacheSize) {
        super(timer, context, dispW, dispH, null);
        mTaskListener = taskListener;
        mCacheManager = new CacheManager(maxCacheSize, 2);
        mCacheManager.begin();
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
        mCacheTimer = new DanmakuTimer();
        mCacheTimer.update(timer.currMillisecond);
    }

    @Override
    public void draw(Canvas canvas) {
        synchronized (danmakuList) {
            super.draw(canvas);
        }
    }

    @Override
    public void reset() {
        // mCacheTimer.update(mTimer.currMillisecond);
        super.reset();
    }

    @Override
    public void seek(long mills) {
        super.seek(mills);
        mTimer.update(mills);
        mCacheTimer.update(mills);
    }

    @Override
    public void quit() {
        mCacheManager.end();
        super.quit();
    }

    public class CacheManager {

        private static final String TAG = "CacheManager";

        public HandlerThread mThread;

        List<BaseDanmaku> mCaches = new ArrayList<BaseDanmaku>();

        DrawingCachePoolManager mCachePoolManager = new DrawingCachePoolManager();

        Pool<DrawingCache> mCachePool = Pools.finitePool(mCachePoolManager, 300);

        private int mMaxSize;

        private int mRealSize;

        private int mScreenSize = 2;

        private CacheHandler mHandler;

        public CacheManager(int maxSize, int screenSize) {
            mRealSize = 0;
            mMaxSize = maxSize;
            mScreenSize = screenSize;
        }

        public void begin() {
            if (mThread == null) {
                mThread = new HandlerThread("Cache Building Thread");
                mThread.start();
            }
            if (mHandler == null)
                mHandler = new CacheHandler(mThread.getLooper());
            mHandler.begin();
        }

        public void end() {
            if (mHandler != null) {
                mHandler.pause();
                mHandler = null;
            }
            if (mThread != null) {
                mThread.quit();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mThread = null;
            }
            evictAll();
        }

        private void evictAll() {
            if (mCaches != null) {
                for (BaseDanmaku danmaku : mCaches) {
                    entryRemoved(true, danmaku, null);
                }
                mCaches.clear();
            }
        }

        protected void entryRemoved(boolean evicted, BaseDanmaku oldValue, BaseDanmaku newValue) {
            mRealSize -= sizeOf(oldValue);
            if (oldValue.cache != null) {
                oldValue.cache.destroy();
                mCachePool.release((DrawingCache) oldValue.cache);
                oldValue.cache = null;
            }
        }

        protected int sizeOf(BaseDanmaku value) {
            if (value.cache != null) {
                return value.cache.size();
            }
            return 0;
        }

        public void resume() {
            mHandler.sendEmptyMessage(CacheHandler.RESUME);
        }

        public void pause() {
            mHandler.sendEmptyMessage(CacheHandler.PAUSE);
        }

        private void put(BaseDanmaku item) {
            int size = sizeOf(item);
            while (mRealSize + size > mMaxSize && mCaches.size() > 0) {
                BaseDanmaku oldValue = mCaches.get(0);
                entryRemoved(false, oldValue, item);
                mCaches.remove(oldValue);
            }
            this.mCaches.add(item);
            mRealSize += size;
        }

        private void clearTimeOutCaches() {
            Iterator<BaseDanmaku> it = mCaches.iterator();
            while (it.hasNext()) {
                if (it.next().isTimeOut()) {
                    it.remove();
                }
            }
        }

        public class CacheHandler extends Handler {

            private static final int PREPARE = 4;

            public static final int BUILD_CACHES = 1;

            public static final int PAUSE = 2;

            public static final int RESUME = 3;

            private boolean mPause;

            public CacheHandler(android.os.Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case PREPARE:
                        for (int i = 0; i < 200; i++) {
                            mCachePool.release(new DrawingCache());
                        }
                    case BUILD_CACHES:
                        if (!mPause) {
                            long waitTime = mCacheTimer.currMillisecond - mTimer.currMillisecond;
                            if (waitTime > 1000) {
                                sendEmptyMessageDelayed(BUILD_CACHES, waitTime - 1000);
                                return;
                            }
                            mCacheTimer.update(mTimer.currMillisecond);
                            prepareCaches(mTaskListener == null);
                            sendEmptyMessage(BUILD_CACHES);
                            if (mTaskListener != null) {
                                mTaskListener.ready();
                                mTaskListener = null;
                            }
                        }
                        break;
                    case PAUSE:
                        mPause = true;
                        break;
                    case RESUME:
                        mPause = false;
                        sendEmptyMessage(BUILD_CACHES);
                        break;
                }
            }

            private long prepareCaches(boolean useTimeCounter) {

                long curr = mCacheTimer.currMillisecond;
                long startTime = System.currentTimeMillis();
                Danmakus danmakus = null;
                synchronized (danmakuList) {
                    danmakus = (Danmakus) danmakuList.sub(curr, curr
                            + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);
                }

                if (danmakus == null || danmakus.size() == 0)
                    return 0;
                Iterator<BaseDanmaku> itr = danmakus.iterator();

                BaseDanmaku item = null;
                long consumingTime = 0;
                while (itr.hasNext()) {
                    item = itr.next();

                    // measure
                    if (!item.isMeasured()) {
                        synchronized (danmakuList) {
                            item.measure(mDisp);
                        }
                    }

                    // build cache
                    if (!item.hasDrawingCache()) {
                        try {
                            synchronized (danmakuList) {
                                DrawingCache cache = mCachePool.acquire();
                                DrawingCache newCache = DanmakuUtils.buildDanmakuDrawingCache(item,
                                        mDisp, cache);
                                item.cache = newCache;
                                boolean quitLoop = false;
                                if (mRealSize + newCache.size() > mMaxSize) {
                                    clearTimeOutCaches();
                                    quitLoop = true;
                                }
                                mCacheManager.put(item);
                                if (quitLoop)
                                    break;
                            }

                            // put(item.hashCode(), item);

                        } catch (OutOfMemoryError e) {
                            break;
                        } catch (Exception e) {
                            break;
                        }
                    }

                    if (useTimeCounter) {
                        consumingTime = System.currentTimeMillis() - startTime;
                        if (consumingTime >= DanmakuFactory.MAX_DANMAKU_DURATION) {
                            break;
                        }
                    }

                }

                consumingTime = System.currentTimeMillis() - startTime;
                if (item != null) {
                    mCacheTimer.update(item.time);
                } else
                    mCacheTimer.add(DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);

                Log.e("cache consumingTime", consumingTime + "ms");
                return consumingTime;
            }

            public void begin() {
                // sendEmptyMessage(BUILD_CACHES);
                sendEmptyMessage(PREPARE);
            }

            public void pause() {
                sendEmptyMessage(PAUSE);
            }

            public void resume() {
                sendEmptyMessage(RESUME);
            }
        }

    }
}
