/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.Iterator;
import java.util.Set;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCachePoolManager;
import master.flame.danmaku.danmaku.model.objectpool.Pool;
import master.flame.danmaku.danmaku.model.objectpool.Pools;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

public class CacheManagingDrawTask extends DrawTask {

    private static final int MAX_CACHE_SCREEN_SIZE = 3;

    private int mMaxCacheSize = 2;

    private CacheManager mCacheManager;

    private DanmakuTimer mCacheTimer;

    private Object mDrawingNotify = new Object();

    public CacheManagingDrawTask(DanmakuTimer timer, Context context, int dispW, int dispH,
                                 TaskListener taskListener, int maxCacheSize) {
        super(timer, context, dispW, dispH, taskListener);
        mMaxCacheSize = maxCacheSize;
        mCacheManager = new CacheManager(maxCacheSize, MAX_CACHE_SCREEN_SIZE);
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
        mCacheTimer = new DanmakuTimer();
        mCacheTimer.update(timer.currMillisecond);
    }

    @Override
    public void addDanmaku(BaseDanmaku danmaku) {
        if (mCacheManager == null)
            return;
        mCacheManager.addDanmaku(danmaku);
    }

    @Override
    public void draw(Canvas canvas) {
        synchronized (danmakuList) {
            super.draw(canvas);
        }
        synchronized(mDrawingNotify){
            mDrawingNotify.notify();
        }
    }

    @Override
    public void reset() {
        // mCacheTimer.update(mTimer.currMillisecond);
        if (mRenderer != null)
            mRenderer.clear();
    }

    @Override
    public void seek(long mills) {
        super.seek(mills);
        mCacheManager.evictAllNotInScreen();
        mCacheManager.resume();
    }

    @Override
    public void start() {
        if (mCacheManager == null) {
            mCacheManager = new CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE);
            mCacheManager.begin();
        } else {
            mCacheManager.resume();
        }
    }

    @Override
    public void quit() {
        mCacheManager.end();
        super.quit();
        reset();
    }

    @Override
    public void prepare() {
        assert (mParser != null);
        loadDanmakus(mParser);
        mCacheManager.begin();
    }

    public class CacheManager {

        @SuppressWarnings("unused")
        private static final String TAG = "CacheManager";

        public HandlerThread mThread;

        Danmakus mCaches = new Danmakus();

        DrawingCachePoolManager mCachePoolManager = new DrawingCachePoolManager();

        Pool<DrawingCache> mCachePool = Pools.finitePool(mCachePoolManager, 500);

        private int mMaxSize;

        private int mRealSize;

        private int mScreenSize = 2;

        private CacheHandler mHandler;

        public CacheManager(int maxSize, int screenSize) {
            mRealSize = 0;
            mMaxSize = maxSize;
            mScreenSize = screenSize;
        }

        public void addDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null) {
                mHandler.obtainMessage(CacheHandler.ADD_DANMAKKU, danmaku).sendToTarget();
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
            clearCachePool();
        }

        public void resume() {
            if (mHandler != null) {
                mHandler.resume();
            } else {
                begin();
            }
        }
        
        public synchronized boolean isPoolFull(){
            return mRealSize + 5120 >= mMaxSize; 
        }

        private synchronized void evictAll() {
            if (mCaches != null) {
                IDanmakuIterator it = mCaches.iterator();
                while(it.hasNext()) {
                    BaseDanmaku danmaku = it.next();
                    entryRemoved(true, danmaku, null);
                }
                mCaches.clear();
            }
            mRealSize = 0;
        }

        private synchronized void evictAllNotInScreen() {
            if (mCaches != null) {
                IDanmakuIterator it = mCaches.iterator();
                while(it.hasNext()) {
                    BaseDanmaku danmaku = it.next();
                    if(danmaku.isOutside()){
                        entryRemoved(true, danmaku, null);
                    }
                }
                mCaches.clear();
            }
            mRealSize = 0;
        }

        protected void entryRemoved(boolean evicted, BaseDanmaku oldValue, BaseDanmaku newValue) {
            if (oldValue.cache != null) {
                if (oldValue.cache.hasReferences()) {
                    oldValue.cache.decreaseReference();
                    oldValue.cache = null;
                    return;
                }
                mRealSize -= sizeOf(oldValue);
                oldValue.cache.destroy();                
                mCachePool.release((DrawingCache) oldValue.cache);
                oldValue.cache = null;
            }
        }

        protected int sizeOf(BaseDanmaku value) {
            if (value.cache != null && !value.cache.hasReferences()) {                
                return value.cache.size();
            }
            return 0;
        }

        private void clearCachePool() {
            DrawingCache item;
            while ((item = mCachePool.acquire()) != null) {
                item.destroy();
            }
        }

        private synchronized boolean push(BaseDanmaku item) {
            int size = sizeOf(item);
            while (mRealSize + size > mMaxSize && mCaches.size() > 0) {
                BaseDanmaku oldValue = mCaches.first();
                if (oldValue.isTimeOut()) {
                    entryRemoved(false, oldValue, item);
                    mCaches.removeItem(oldValue);
                } else {
                    return false;
                }
            }
            this.mCaches.addItem(item);
            mRealSize += size;
            return true;
        }

        private synchronized void clearTimeOutCaches() {
            clearTimeOutCaches(mTimer.currMillisecond);
        }

        private synchronized void clearTimeOutCaches(long time) {
            IDanmakuIterator it = mCaches.iterator();
            while (it.hasNext()) {
                BaseDanmaku val = it.next();
                if (val.isTimeOut(time)) {
                    try {
                        synchronized (mDrawingNotify) {
                            mDrawingNotify.wait(20);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    entryRemoved(false, val, null);
                    it.remove();
                }else{
                    break;
                }
            }
        }
        
        private synchronized DrawingCache findReuseableCache(BaseDanmaku refDanmaku) {
            IDanmakuIterator it = mCaches.iterator();
            while (it.hasNext()) {
                BaseDanmaku danmaku = it.next();
                if (danmaku.paintWidth == refDanmaku.paintWidth
                        && danmaku.paintHeight == refDanmaku.paintHeight
                        && danmaku.textColor == refDanmaku.textColor
                        && danmaku.text.equals(refDanmaku.text)) {
                    return (DrawingCache) danmaku.cache;
                }
            }
            return null;
        }
        
        int danmakuAddedCount = 0;
        public class CacheHandler extends Handler {

            private static final int PREPARE = 0x1;

            public static final int ADD_DANMAKKU = 0x2;

            public static final int BUILD_CACHES = 0x3;

            private static final int CLEAR_CACHES = 0x4;

            private boolean mPause;

            private boolean buildSuccess;

            public CacheHandler(android.os.Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case PREPARE:
                        evictAllNotInScreen();
                        for (int i = 0; i < 200; i++) {
                            mCachePool.release(new DrawingCache());
                        }
                    case BUILD_CACHES:
                        if (!mPause) {
                            long waitTime = mCacheTimer.currMillisecond - mTimer.currMillisecond;
                            long maxCacheDuration = DanmakuFactory.MAX_DANMAKU_DURATION
                                    * mScreenSize;
//                            Log.e("count", waitTime+"ms");
                            if (waitTime > 1000 && waitTime <= maxCacheDuration) {
                                removeMessages(BUILD_CACHES);
                                sendEmptyMessageDelayed(BUILD_CACHES, waitTime - 1000);
                                return;
                            } else if (waitTime < 0 || getFirstCacheTime() - mTimer.currMillisecond > maxCacheDuration){
                                evictAllNotInScreen();
                            }
                            if(Math.abs(waitTime) > maxCacheDuration){
                                mCacheTimer.update(mTimer.currMillisecond + 100);
                            }
                            prepareCaches(mTaskListener != null);
                            removeMessages(BUILD_CACHES);
                            if(!mPause)
                                sendEmptyMessageDelayed(BUILD_CACHES,2000);
                            if (mTaskListener != null) {
                                mTaskListener.ready();
                                mTaskListener = null;
                            }
                        }
                        break;
                    case ADD_DANMAKKU:
                        synchronized (danmakuList) {
                            BaseDanmaku item = (BaseDanmaku) msg.obj;
                            buildCache(item);
                            CacheManagingDrawTask.super.addDanmaku(item);
                            if (item.isLive) {
                                mCacheTimer.update(mTimer.currMillisecond
                                        + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);
                            }
                        }
                        break;
                    case CLEAR_CACHES:
                        if (!mPause) {
                            clearTimeOutCaches();
                            removeMessages(CLEAR_CACHES);
                            sendEmptyMessageDelayed(CLEAR_CACHES, DanmakuFactory.MAX_DANMAKU_DURATION);
                        }
                        break;
                }
            }

            private long prepareCaches(boolean init) {
                long curr = mCacheTimer.currMillisecond;                
                long end = curr + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize;
                long startTime = System.currentTimeMillis();
                Set<BaseDanmaku> danmakus = null;
                danmakus = danmakuList.subset(curr, curr
                        + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);

                if (danmakus == null || danmakus.size() == 0)
                    return 0;
                Iterator<BaseDanmaku> itr = danmakus.iterator();

                BaseDanmaku item = null;
                long consumingTime = 0;
                int count = 0;
                while (itr.hasNext() && !mPause) {
                    item = itr.next();
                    count++;
                    
                    if(item.hasDrawingCache()){
                        continue;
                    }
                    
                    if (init==false && (item.isTimeOut() || !item.isOutside())) {
                        continue;
                    }

                    // build cache

                    if (!init) {
                        try {
                            synchronized (mDrawingNotify) {
                                mDrawingNotify.wait(50);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    buildSuccess = buildCache(item);
                    if (!buildSuccess) {
                        break;
                    }

                    if (!init) {
                        consumingTime = System.currentTimeMillis() - startTime;
                        if (consumingTime >= DanmakuFactory.COMMON_DANMAKU_DURATION) {
//                          Log.d("cache", "break at consumingTime out:"+consumingTime);
                            break;
                        }
                    }
                }

                consumingTime = System.currentTimeMillis() - startTime;
                if (item==null || count==0 || danmakus.isEmpty() || count == danmakus.size() || isPoolFull()) {
                    mCacheTimer.update(end);
                    sendEmptyMessage(CLEAR_CACHES);
                } else if (item != null) {
                    mCacheTimer.update(item.time);
//                    Log.i("cache","stop at :"+item.time+","+count+",size:"+danmakus.size());
                }
                return consumingTime;
            }

            private boolean buildCache(BaseDanmaku item) {
                
                // measure
                if (!item.isMeasured()) {
                    item.measure(mDisp);
                }

                DrawingCache cache = null;
                try {
                    // try to find reuseable cache
                    cache = findReuseableCache(item);
                    if (cache != null) {
//Log.e("count", item.text+"DrawingCache hit!!");
                        cache.increaseReference();
                        item.cache = cache;
                        mCacheManager.push(item);
                        return true;
                    }
                    
                    // guess cache size
                    int cacheSize = DanmakuUtils.getCacheSize((int) item.paintWidth,
                            (int) item.paintHeight);
                    if (mRealSize + cacheSize > mMaxSize) {
                        // Log.d("cache", "break at MaxSize:"+mMaxSize);
                        return false;
                    }

                    cache = mCachePool.acquire();
                    synchronized (danmakuList) {
                        cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache);
                        boolean pushed = mCacheManager.push(item);
                        if (pushed) {
                            item.cache = cache;
                        } else {
                            mCachePool.release(cache);
                            cache.destroy();
                            // Log.d("cache", "break at push failed:"+mMaxSize);
                        }
                        return pushed;
                    }

                } catch (OutOfMemoryError e) {
                    if (cache != null) {
                        mCachePool.release(cache);
                        cache.destroy();
                    }
                    return false;
                } catch (Exception e) {
                    if (cache != null) {
                        mCachePool.release(cache);
                        cache.destroy();
                    }
                    return false;
                }
            }

            public void begin() {
                sendEmptyMessage(PREPARE);
                sendEmptyMessageDelayed(CLEAR_CACHES, DanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public void pause() {
                mPause = true;
                removeCallbacksAndMessages(null);
            }

            public void resume() {
                mPause = false;
                removeMessages(BUILD_CACHES);
                sendEmptyMessage(BUILD_CACHES);
                sendEmptyMessageDelayed(CLEAR_CACHES, DanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public boolean isPause() {
                return mPause;
            }
        }

        public long getFirstCacheTime() {
            if(mCaches!=null && mCaches.size()>0){
                return mCaches.first().time;
            }
            return 0;
        }

    }
}