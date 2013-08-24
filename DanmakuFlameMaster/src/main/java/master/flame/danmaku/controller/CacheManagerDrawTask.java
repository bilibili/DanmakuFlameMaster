
package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.util.Iterator;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class CacheManagerDrawTask extends DrawTask {

    private CacheManager mCacheManager;

    private DanmakuTimer mCacheTimer;

    public CacheManagerDrawTask(DanmakuTimer timer, Context context, int dispW, int dispH,
            TaskListener taskListener) {
        super(timer, context, dispW, dispH, null);
        mTaskListener = taskListener;
        mCacheManager = new CacheManager(1024 * 1024 * 34, 2); // fixme:use
                                                               // custom config
        mCacheManager.begin();
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
        mCacheTimer = new DanmakuTimer();
        mCacheTimer.update(mTimer.currMillisecond);
    }

    @Override
    public void draw(Canvas canvas) {
        synchronized (danmakuList) {
            super.draw(canvas);
        }
    }

    @Override
    public void reset() {
        mCacheTimer.update(mTimer.currMillisecond);
        super.reset();
    }

    @Override
    public void seek(long mills) {
        super.seek(mills);
    }

    @Override
    public void quit() {
        mCacheManager.end();
        super.quit();
    }

    public class CacheManager extends LruCache<Integer, BaseDanmaku> {

        private int mScreenSize = 2;

        public HandlerThread mThread;

        private CacheHandler mHandler;

        public CacheManager(int maxSize, int screenSize) {
            super(maxSize);
            mScreenSize = screenSize;
        }

        @Override
        protected int sizeOf(Integer key, BaseDanmaku value) {
            if (value.hasDrawingCache()) {
                return value.cache.size();
            }
            return 0;
        }

        @Override
        protected void entryRemoved(boolean evicted, Integer key, BaseDanmaku oldValue,
                BaseDanmaku newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            if (oldValue.hasDrawingCache()) {
                oldValue.cache.destroy();
                oldValue.cache = null;
            }
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
            mHandler.pause();
            mThread.quit();
            mThread = null;
            mHandler = null;
        }

        public class CacheHandler extends Handler {

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
                    case BUILD_CACHES:
                        if (!mPause) {
                            long consumingTime = prepareCaches();
                            sendEmptyMessageDelayed(BUILD_CACHES,
                                    consumingTime > DanmakuFactory.MAX_DANMAKU_DURATION ? 0
                                            : DanmakuFactory.MAX_DANMAKU_DURATION - consumingTime);
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
                        break;
                }
            }

            private long prepareCaches() {

                int count = 0;
                long curr = mCacheTimer.currMillisecond;
                long startTime = System.currentTimeMillis();
                Danmakus danmakus = null;
                synchronized (danmakuList) {
                    danmakus = (Danmakus) danmakuList.sub(curr, curr
                            + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);
                    Danmakus ndanmakus = new Danmakus();
                    for (BaseDanmaku item : danmakus.items) {
                        ndanmakus.addItem(item);
                    }
                    danmakus = ndanmakus;
                }

                if (danmakus == null || danmakus.size() == 0)
                    return 0;
                Iterator<BaseDanmaku> itr = danmakus.iterator();

                BaseDanmaku item = null;
                while (itr.hasNext()) {
                    item = itr.next();

                    // measure
                    if (!item.isMeasured()) {
                        item.measure(mDisp);
                    }

                    // build cache
                    if (!item.hasDrawingCache()) {
                        try {
                            DanmakuUtils.buildDanmakuDrawingCache(item, mDisp);
                            put(item.hashCode(), item);
                        } catch (OutOfMemoryError e) {
                            break;
                        }
                    }

                    count++;
                }

                long consumingTime = System.currentTimeMillis() - startTime;
                if (item != null) {
                    mCacheTimer.update(item.time);
                } else
                    mCacheTimer.add(DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);

                Log.e("cache consumingTime", consumingTime + "ms");
                return consumingTime;

            }

            public void begin() {
                sendEmptyMessage(BUILD_CACHES);
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
