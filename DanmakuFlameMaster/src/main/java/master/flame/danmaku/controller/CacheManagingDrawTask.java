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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import master.flame.danmaku.danmaku.model.AbsDisplayer;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.android.DanmakuGlobalConfig;
import master.flame.danmaku.danmaku.model.android.DanmakuGlobalConfig.DanmakuConfigTag;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCachePoolManager;
import master.flame.danmaku.danmaku.model.objectpool.Pool;
import master.flame.danmaku.danmaku.model.objectpool.Pools;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.util.DanmakuUtils;
import tv.cjump.jni.NativeBitmapFactory;

public class CacheManagingDrawTask extends DrawTask {

    private static final int MAX_CACHE_SCREEN_SIZE = 3;

    private int mMaxCacheSize = 2;

    private CacheManager mCacheManager;

    private DanmakuTimer mCacheTimer;
    
    private final Object mDrawingNotify = new Object();

    public CacheManagingDrawTask(DanmakuTimer timer, Context context, AbsDisplayer<?> disp,
            TaskListener taskListener, int maxCacheSize) {
        super(timer, context, disp, taskListener);
        NativeBitmapFactory.loadLibs();
        mMaxCacheSize = maxCacheSize;
        if (NativeBitmapFactory.isInNativeAlloc()) {
            mMaxCacheSize = maxCacheSize * 3;
        }
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
        super.addDanmaku(danmaku);
        if (mCacheManager == null)
            return;
        mCacheManager.addDanmaku(danmaku);
    }

    @Override
    public RenderingState draw(AbsDisplayer<?> displayer) {
        RenderingState result = null;
        synchronized (danmakuList) {
            result = super.draw(displayer);
        }
        synchronized(mDrawingNotify){
            mDrawingNotify.notify();
        }
        if(result != null && mCacheManager != null) {
            if(result.incrementCount < -20) {
                mCacheManager.requestClearTimeout();
                mCacheManager.requestBuild(-DanmakuFactory.MAX_DANMAKU_DURATION);
            }
        }
        return result;
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
        mCacheManager.seek(mills);
    }

    @Override
    public void start() {
        super.start();
        NativeBitmapFactory.loadLibs();
        if (mCacheManager == null) {
            mCacheManager = new CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE);
            mCacheManager.begin();
        } else {
            mCacheManager.resume();
        }
    }

    @Override
    public void quit() {
        super.quit();
        reset();
        if(mCacheManager!=null){
            mCacheManager.end();
            mCacheManager = null;
        }
        NativeBitmapFactory.releaseLibs();
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
        public static final byte RESULT_SUCCESS = 0;
        public static final byte RESULT_FAILED = 1;
        public static final byte RESULT_FAILED_OVERSIZE = 2;

        public HandlerThread mThread;

        Danmakus mCaches = new Danmakus(Danmakus.ST_BY_LIST);

        DrawingCachePoolManager mCachePoolManager = new DrawingCachePoolManager();

        Pool<DrawingCache> mCachePool = Pools.finitePool(mCachePoolManager, 800);

        private int mMaxSize;

        private int mRealSize;

        private int mScreenSize = 3;

        private CacheHandler mHandler;

        private boolean mEndFlag;

        public CacheManager(int maxSize, int screenSize) {
            mEndFlag = false;
            mRealSize = 0;
            mMaxSize = maxSize;
            mScreenSize = screenSize;
        }

        public void seek(long mills) {
            if (mHandler == null)
                return;
            mHandler.requestCancelCaching();
            mHandler.removeMessages(CacheHandler.BUILD_CACHES);
            mHandler.obtainMessage(CacheHandler.SEEK, mills).sendToTarget();
        }

        public void addDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null) {
                mHandler.obtainMessage(CacheHandler.ADD_DANMAKKU, danmaku).sendToTarget();
            }
        }

        public void begin() {
            if (mThread == null) {
                mThread = new HandlerThread("DFM Cache-Building Thread");
                mThread.start();
            }
            if (mHandler == null)
                mHandler = new CacheHandler(mThread.getLooper());
            mHandler.begin();
        }

        public void end() {
            mEndFlag = true;
            synchronized (mDrawingNotify) {
                mDrawingNotify.notifyAll();
            }
            if (mHandler != null) {
                mHandler.pause();
                mHandler = null;
            }
            if (mThread != null) {
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mThread.quit();
                mThread = null;
            }
        }

        public void resume() {
            if (mHandler != null) {
                mHandler.resume();
            } else {
                begin();
            }
        }
        
        public float getPoolPercent(){
            if(mMaxSize == 0){
                return 0;
            }
            return mRealSize/(float)mMaxSize;
        }
        
        public boolean isPoolFull(){
            return mRealSize + 5120 >= mMaxSize; 
        }

        private void evictAll() {
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
        
        private void evictAllNotInScreen() {
            evictAllNotInScreen(false);
        }

        private void evictAllNotInScreen(boolean removeAllReferences) {
            if (mCaches != null) {
                IDanmakuIterator it = mCaches.iterator();
                while (it.hasNext()) {
                    BaseDanmaku danmaku = it.next();
                    IDrawingCache<?> cache = danmaku.cache;
                    boolean hasReferences = cache != null && cache.hasReferences();
                    if (removeAllReferences && hasReferences) {
                        if (cache.get() != null) {
                            mRealSize -= cache.size();
                            cache.destroy();
                        }
                        entryRemoved(true, danmaku, null);
                        it.remove();
                        continue;
                    }
                    if (danmaku.hasDrawingCache() == false || danmaku.isOutside()) {
                        entryRemoved(true, danmaku, null);
                        it.remove();
                    }
                }
                // mCaches.clear();
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

        private boolean push(BaseDanmaku item, int itemSize) {
            int size = itemSize; //sizeOf(item);
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
//Log.e("CACHE", "realsize:"+mRealSize + ",size" + size);
            return true;
        }

        private void clearTimeOutCaches() {
            clearTimeOutCaches(mTimer.currMillisecond);
        }

        private void clearTimeOutCaches(long time) {
            IDanmakuIterator it = mCaches.iterator();
            while (it.hasNext() && !mEndFlag) {
                BaseDanmaku val = it.next();
                if (val.isTimeOut()) {
                    synchronized (mDrawingNotify) {
                        try {
                            mDrawingNotify.wait(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    entryRemoved(false, val, null);
                    it.remove();
                } else {
                    break;
                }
            }
        }
        
        private BaseDanmaku findReuseableCache(BaseDanmaku refDanmaku,
                boolean strictMode) {
            IDanmakuIterator it = mCaches.iterator();
            int slopPixel = 0;
            if (!strictMode) {
                slopPixel = mDisp.getSlopPixel() * 2;
            }
            while (it.hasNext()) {
                BaseDanmaku danmaku = it.next();
                if (!danmaku.hasDrawingCache()) {
                    continue;
                }
                if (danmaku.paintWidth == refDanmaku.paintWidth
                        && danmaku.paintHeight == refDanmaku.paintHeight
                        && danmaku.underlineColor == refDanmaku.underlineColor
                        && danmaku.borderColor == refDanmaku.borderColor
                        && danmaku.textColor == refDanmaku.textColor
                        && danmaku.text.equals(refDanmaku.text)) {
                    return danmaku;
                }
                if (strictMode) {
                    continue;
                }
                if (!danmaku.isTimeOut()) {
                    break;
                }
                if (danmaku.cache.hasReferences()) {
                    continue;
                }
                float widthGap = danmaku.cache.width() - refDanmaku.paintWidth;
                float heightGap = danmaku.cache.height() - refDanmaku.paintHeight;
                if (widthGap >= 0 && widthGap <= slopPixel &&  
                    heightGap >= 0 && heightGap <= slopPixel) {
                    return danmaku;
                }
            }
            return null;
        }
        
        int danmakuAddedCount = 0;
        public class CacheHandler extends Handler {

            private static final int PREPARE = 0x1;

            public static final int ADD_DANMAKKU = 0x2;

            public static final int BUILD_CACHES = 0x3;

            public static final int CLEAR_TIMEOUT_CACHES = 0x4;
            
            public static final int SEEK = 0x5;
            
            public static final int QUIT = 0x6;

            public static final int CLEAR_ALL_CACHES = 0x7;
            
            public static final int CLEAR_OUTSIDE_CACHES = 0x8;
            
            public static final int CLEAR_OUTSIDE_CACHES_AND_RESET = 0x9;
            
            public static final int DISPATCH_ACTIONS = 0x10;

            private boolean mPause;

            private boolean mSeekedFlag;

            private boolean mCancelFlag;

            public CacheHandler(android.os.Looper looper) {
                super(looper);
            }

            public void requestCancelCaching() {
                mCancelFlag = true;
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case PREPARE:
                        evictAllNotInScreen();
                        for (int i = 0; i < 300; i++) {
                            mCachePool.release(new DrawingCache());
                        }
                    case DISPATCH_ACTIONS:
//Log.e(TAG,"dispatch_actions:"+mCacheTimer.currMillisecond+":"+mTimer.currMillisecond);
                        long delayed = dispatchAction();
                        if (delayed <= 0) {
                            delayed = DanmakuFactory.MAX_DANMAKU_DURATION / 2;
                        }
                        sendEmptyMessageDelayed(DISPATCH_ACTIONS, delayed);
                        break;
                    case BUILD_CACHES:
                        removeMessages(BUILD_CACHES);
                        boolean repositioned = ((mTaskListener != null && mReadyState == false) || mSeekedFlag);
                        prepareCaches(repositioned);
                        if (repositioned)
                            mSeekedFlag = false;
                        if (mTaskListener != null && mReadyState == false) {
                            mTaskListener.ready();
                            mReadyState = true;
                        }
//                        Log.i(TAG,"BUILD_CACHES:"+mCacheTimer.currMillisecond+":"+mTimer.currMillisecond);
                        break;
                    case ADD_DANMAKKU:
                        synchronized (danmakuList) {
                            BaseDanmaku item = (BaseDanmaku) msg.obj;
                            if(item.isTimeOut()) {
                                break;
                            }
                            if(!item.hasDrawingCache()) {
                                buildCache(item);
                            }
                            if (item.isLive) {
                                mCacheTimer.update(mTimer.currMillisecond
                                        + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize);
                            }
                        }
                        break;
                    case CLEAR_TIMEOUT_CACHES:
                        clearTimeOutCaches();
                        break;
                    case SEEK:
                        Long seekMills = (Long) msg.obj;
                        if (seekMills != null) {
                            mCacheTimer.update(seekMills.longValue());
                            mSeekedFlag = true;
                            evictAllNotInScreen();
                            resume();
                        }
                        break;
                    case QUIT:
                        removeCallbacksAndMessages(null);
                        mPause = true;
                        evictAll();                        
                        clearCachePool();
                        this.getLooper().quit();
                        break;
                    case CLEAR_ALL_CACHES:
                        evictAll();
                        mCacheTimer.update(mTimer.currMillisecond - DanmakuFactory.MAX_DANMAKU_DURATION);
                        mSeekedFlag = true;
                        break;
                    case CLEAR_OUTSIDE_CACHES:
                        evictAllNotInScreen(true);
                        mCacheTimer.update(mTimer.currMillisecond);
                        break;
                    case CLEAR_OUTSIDE_CACHES_AND_RESET:
                        evictAllNotInScreen(true);
                        mCacheTimer.update(mTimer.currMillisecond);
                        requestClear();
                        break;
                }
            }
            
            private long dispatchAction() {
                float level = getPoolPercent();
                BaseDanmaku firstCache = mCaches.first();
                //TODO 如果firstcache大于当前时间超过半屏并且水位在0.5f以下,
                long gapTime = firstCache != null ? firstCache.time - mTimer.currMillisecond : 0;
                long doubleScreenDuration = DanmakuFactory.MAX_DANMAKU_DURATION * 2;
                if (level < 0.6f && gapTime > DanmakuFactory.MAX_DANMAKU_DURATION) {
                    mCacheTimer.update(mTimer.currMillisecond);
                    removeMessages(BUILD_CACHES);
                    sendEmptyMessage(BUILD_CACHES);
                    return 0;
                } else if (level > 0.4f && gapTime < -doubleScreenDuration) {
                    // clear timeout caches
                    removeMessages(CLEAR_TIMEOUT_CACHES);
                    sendEmptyMessage(CLEAR_TIMEOUT_CACHES);
                    return 0;
                }
                
                if (level >= 0.9f) {
                    return 0;
                }
                // check cache time
                long deltaTime = mCacheTimer.currMillisecond - mTimer.currMillisecond;
                if (deltaTime < 0) {
                    mCacheTimer.update(mTimer.currMillisecond);
                    sendEmptyMessage(CLEAR_OUTSIDE_CACHES);
                    sendEmptyMessage(BUILD_CACHES);
                    return 0;
                } else if (deltaTime > doubleScreenDuration) {
                    return 0;
                }
                
                removeMessages(BUILD_CACHES);
                sendEmptyMessage(BUILD_CACHES);
                return 0;
            }

            private void releaseDanmakuCache(BaseDanmaku item, DrawingCache cache) {
                if (cache == null) {
                    cache = (DrawingCache) item.cache;
                }
                item.cache = null;
                if (cache == null) {
                    return;
                }
                cache.destroy();
                mCachePool.release(cache);
            }

            private long prepareCaches(boolean repositioned) {
                long curr = mCacheTimer.currMillisecond;
                long end = curr + DanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize * 3;
                if (end < mTimer.currMillisecond) {
                    return 0;
                }
                long startTime =  System.currentTimeMillis();
                IDanmakus danmakus = danmakuList.subnew(curr, end);
                if (danmakus == null || danmakus.isEmpty()) {
                    mCacheTimer.update(end);
                    return 0;
                }
                BaseDanmaku first = danmakus.first();
                BaseDanmaku last = danmakus.last();
                long deltaTime = first.time - mTimer.currMillisecond;
                long sleepTime = 30 + 10 * deltaTime / DanmakuFactory.MAX_DANMAKU_DURATION;
                sleepTime = Math.min(100, sleepTime);
                if (repositioned) {
                    sleepTime = 0;
                }
                
                IDanmakuIterator itr = danmakus.iterator();
                BaseDanmaku item = null;
                long consumingTime = 0;
                int orderInScreen = 0;
                int currScreenIndex = 0;
                int sizeInScreen = danmakus.size();
//                String message = "";
                while (!mPause && !mCancelFlag) {
                    boolean hasNext = itr.hasNext();
                    if(!hasNext){
//                        message = "break at not hasNext";
                        break;
                    }
                    item = itr.next();
                    
                    if (last.time < mTimer.currMillisecond) {
//                        message = "break at last.time < mTimer.currMillisecond";
                        break;
                    }
                    
                    if(item.hasDrawingCache()){
                        continue;
                    }
                    
                    if (repositioned == false && (item.isTimeOut() || !item.isOutside())) {
                        continue;
                    }
                    boolean skip = DanmakuFilters.getDefault().filter(item, orderInScreen,
                            sizeInScreen, null, true);
//Log.e("prepareCache", currScreenIndex+","+orderInScreen+"," + item.time+"skip:"+skip);
                    if (skip) {
                        continue;
                    }
                    
                    if(item.getType() == BaseDanmaku.TYPE_SCROLL_RL){
                        // 同屏弹幕密度只对滚动弹幕有效
                        int screenIndex = (int) ((item.time - curr)/DanmakuFactory.MAX_DANMAKU_DURATION);
                        if(currScreenIndex == screenIndex)
                            orderInScreen++;
                        else{
                            orderInScreen = 0;
                            currScreenIndex = screenIndex;
                        }
                    }
                    
                    if (!repositioned) {
                        try {
                            synchronized (mDrawingNotify) {
                                mDrawingNotify.wait(sleepTime);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    // build cache
                    if (buildCache(item) == RESULT_FAILED) {
                        // message = "break at build failed";
                        break;
                    }

                    if (!repositioned) {
                        consumingTime = System.currentTimeMillis() - startTime;
                        if (consumingTime >= DanmakuFactory.COMMON_DANMAKU_DURATION * mScreenSize) {
//                            message = "break at consumingTime out:" + consumingTime;
                            break;
                        }
                    }
                    
                }
                consumingTime = System.currentTimeMillis() - startTime;
                if (item != null) {
                    mCacheTimer.update(item.time);
//Log.i("cache","stop at :"+item.time+","+count+",size:"+danmakus.size()+","+message);
                }else {
                    mCacheTimer.update(end);
                }
                return consumingTime;
            }

            private byte buildCache(BaseDanmaku item) {
                
                // measure
                if (!item.isMeasured()) {
                    item.measure(mDisp);
                }

                DrawingCache cache = null;
                try {
                    // try to find reuseable cache
                    BaseDanmaku danmaku = findReuseableCache(item, true);
                    if (danmaku != null) {
                        cache = (DrawingCache) danmaku.cache;
                    }
                    if (cache != null) {
                        cache.increaseReference();
                        item.cache = cache;
                        mCacheManager.push(item, sizeOf(item));
                        return RESULT_SUCCESS;
                    }
                    
                    // try to find reuseable cache from timeout && no-refrerence caches
                    danmaku = findReuseableCache(item, false);
                    if (danmaku != null) {
                        cache = (DrawingCache) danmaku.cache;
                    }
                    if (cache != null) {
                        danmaku.cache = null;
//Log.e("cache", danmaku.text+"DrawingCache hit!!:" + item.paintWidth + "," + danmaku.paintWidth);
                        cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache);  //redraw
                        item.cache = cache;
                        mCacheManager.push(item, 0);
                        return RESULT_SUCCESS;
                    }
                    
                    // guess cache size
                    int cacheSize = DanmakuUtils.getCacheSize((int) item.paintWidth,
                            (int) item.paintHeight);
                    if (mRealSize + cacheSize > mMaxSize) {
//                        Log.d("cache", "break at MaxSize:"+mMaxSize);
                        return RESULT_FAILED;
                    }

                    cache = mCachePool.acquire();
                    synchronized (danmakuList) {
                        cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache);
                        item.cache = cache;
                        boolean pushed = mCacheManager.push(item, sizeOf(item));
                        if (!pushed) {
                            releaseDanmakuCache(item, cache);
//Log.e("cache", "break at push failed:" + mMaxSize);
                        }
                        return pushed ? RESULT_SUCCESS : RESULT_FAILED;
                    }

                } catch (OutOfMemoryError e) {
//Log.e("cache", "break at error: oom");
                    releaseDanmakuCache(item, cache);
                    return RESULT_FAILED;
                } catch (Exception e) {
//Log.e("cache", "break at exception:" + e.getMessage());
                    releaseDanmakuCache(item, cache);
                    return RESULT_FAILED;
                }
            }

            public void begin() {
                sendEmptyMessage(PREPARE);
                sendEmptyMessageDelayed(CLEAR_TIMEOUT_CACHES, DanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public void pause() {
                mPause = true;
                removeCallbacksAndMessages(null);
                sendEmptyMessage(QUIT);
            }

            public void resume() {
                mCancelFlag = false;
                mPause = false;
                removeMessages(DISPATCH_ACTIONS);
                sendEmptyMessage(DISPATCH_ACTIONS);
                sendEmptyMessageDelayed(CLEAR_TIMEOUT_CACHES, DanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public boolean isPause() {
                return mPause;
            }

            public void requestBuildCacheAndDraw(long correctionTime) {
                removeMessages(CacheHandler.BUILD_CACHES);
                mSeekedFlag = true;
                mCancelFlag = false;
                mCacheTimer.update(mTimer.currMillisecond + correctionTime);
                sendEmptyMessage(CacheHandler.BUILD_CACHES);
            }
        }

        public long getFirstCacheTime() {
            if (mCaches != null && mCaches.size() > 0) {
                BaseDanmaku firstItem = mCaches.first();
                if (firstItem == null)
                    return 0;
                return firstItem.time;
            }
            return 0;
        }

        public void requestBuild(long correctionTime) {
            if(mHandler != null) {
                mHandler.requestBuildCacheAndDraw(correctionTime);
            }
        }

        public void requestClearAll() {
            if (mHandler == null) {
                return;
            }
            mHandler.removeMessages(CacheHandler.BUILD_CACHES);
            mHandler.requestCancelCaching();
            mHandler.removeMessages(CacheHandler.CLEAR_ALL_CACHES);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_ALL_CACHES);
        }

        public void requestClearUnused() {
            if (mHandler == null) {
                return;
            }
            mHandler.removeMessages(CacheHandler.CLEAR_OUTSIDE_CACHES_AND_RESET);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_OUTSIDE_CACHES_AND_RESET);
        }
        
        public void requestClearTimeout() {
            if (mHandler == null) {
                return;
            }
            mHandler.removeMessages(CacheHandler.CLEAR_TIMEOUT_CACHES);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_TIMEOUT_CACHES);
        }

        public void post(Runnable runnable) {
            if (mHandler == null) {
                return;
            }
            mHandler.post(runnable);
        }

    }
    
    @Override
    public boolean onDanmakuConfigChanged(DanmakuGlobalConfig config, DanmakuConfigTag tag,
            Object... values) {
        if (super.handleOnDanmakuConfigChanged(config, tag, values)) {
            // do nothing
        } else if (DanmakuConfigTag.SCROLL_SPEED_FACTOR.equals(tag)) {
            mDisp.resetSlopPixel(DanmakuGlobalConfig.DEFAULT.scaleTextSize);
            requestClear();
        } else if (tag.isVisibilityRelatedTag()) {
            if (values != null && values.length > 0) {
                if (values[0] != null && ((values[0] instanceof Boolean) == false || ((Boolean) values[0]).booleanValue())) {
                    if (mCacheManager != null) {
                        mCacheManager.requestBuild(0l);
                    }
                }
            }
            requestClear();
        } else if (DanmakuConfigTag.TRANSPARENCY.equals(tag) || DanmakuConfigTag.SCALE_TEXTSIZE.equals(tag) || DanmakuConfigTag.DANMAKU_STYLE.equals(tag)) {
            if (DanmakuConfigTag.SCALE_TEXTSIZE.equals(tag)) {
                mDisp.resetSlopPixel(DanmakuGlobalConfig.DEFAULT.scaleTextSize);
            }
            if (mCacheManager != null) {
                mCacheManager.requestClearAll();
                mCacheManager.requestBuild(-DanmakuFactory.MAX_DANMAKU_DURATION);
            }
        } else {
            if (mCacheManager != null) {
                mCacheManager.requestClearUnused();
                mCacheManager.requestBuild(0l);
            }
        }

        if (mTaskListener != null && mCacheManager != null) {
            mCacheManager.post(new Runnable() {

                @Override
                public void run() {
                    mTaskListener.onDanmakuConfigChanged();
                }
            });
        }
        return true;
    }
}