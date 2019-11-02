package master.flame.danmaku.gl;

import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import master.flame.danmaku.controller.DrawTask;
import master.flame.danmaku.controller.IDrawTask;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.ICacheManager;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DrawingCacheHolder;
import master.flame.danmaku.danmaku.util.DanmakuUtils;
import master.flame.danmaku.gl.glview.GLUtils;
import master.flame.danmaku.gl.wedget.GLShareable;
import tv.cjump.jni.NativeBitmapFactory;

import static master.flame.danmaku.danmaku.model.IDanmakus.ST_BY_LIST;

/**
 * 创建人:yangzhiqian
 * 创建时间:2018/7/11 14:20
 * 备注:
 */
public class GLDrawTask extends DrawTask {
    private static final boolean DEBUG = Constants.DEBUG_GLDRAWTASK;
    private static final String TAG = "GLDrawTask";
    private GLCacheManager mCacheManager;
    private AndroidGLDisplayer mDiplayer;
    private Looper mLooper;

    public GLDrawTask(DanmakuTimer timer, DanmakuContext context, TaskListener taskListener) {
        this(null, timer, context, taskListener);
    }

    public GLDrawTask(Looper lopper, DanmakuTimer timer, DanmakuContext context, TaskListener taskListener) {
        super(timer, context, taskListener);
        NativeBitmapFactory.loadLibs();
        mLooper = lopper;
        mCacheManager = new GLCacheManager();
        mRenderer.setCacheManager(mCacheManager);
        //此GLDrawTask必须配合AndroidGLDisplayer使用
        mDiplayer = (AndroidGLDisplayer) context.mDisplayer;
    }

    @Override
    public void addDanmaku(BaseDanmaku danmaku) {
        super.addDanmaku(danmaku);
        if (mCacheManager != null) {
            if (DEBUG) {
                Log.i(TAG, "addDanmaku id = " + danmaku.id);
            }
            mCacheManager.addDanmaku(danmaku);
        }
    }

    @Override
    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        super.invalidateDanmaku(item, remeasure);
        if (mCacheManager != null) {
            if (DEBUG) {
                Log.i(TAG, "invalidateDanmaku id = " + item.id);
            }
            mCacheManager.rebuildDanmaku(item);
        }
    }

    @Override
    public void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        IDanmakus subnew = subnew(0, Long.MAX_VALUE);
        super.removeAllDanmakus(isClearDanmakusOnScreen);
        if (DEBUG) {
            Log.i(TAG, "removeAllDanmakus isClearDanmakusOnScreen = " + isClearDanmakusOnScreen + "\ttotal size =  " + (subnew == null ? 0 : subnew.size()));
        }
        if (isClearDanmakusOnScreen) {
            mDiplayer.getRenderer().getGLDanmakuHandler().removeAllDanmaku();
        }
        if (mCacheManager != null) {
            mCacheManager.removeAllCachedDanmaku();
        } else if (subnew != null && !subnew.isEmpty()) {
            //此处不应该被调用到
            Log.w(TAG, "此处不应该被调用到，请检查一下代码逻辑");
            subnew.forEach(new IDanmakus.Consumer<BaseDanmaku, Void>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    IDrawingCache<?> cache = danmaku.getDrawingCache();
                    if (cache != null) {
                        cache.destroy();
                        danmaku.cache = null;
                    }
                    return ACTION_CONTINUE;
                }
            });
        }
    }

    private IDanmakus subnew(long start, long end) {
        IDanmakus subnew;
        int exceptionTimes = 0;
        while (exceptionTimes < 3) {
            try {
                //subnew调用不安全，可能会抛异常
                subnew = danmakuList.subnew(start, end);
                return subnew;
            } catch (Exception e) {
                exceptionTimes++;
            }
        }
        return null;
    }

    @Override
    protected void onDanmakuRemoved(BaseDanmaku danmaku) {
        super.onDanmakuRemoved(danmaku);
        if (danmaku == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "onDanmakuRemoved id = " + danmaku.id);
        }
        mDiplayer.getRenderer().getGLDanmakuHandler().removeDamaku(danmaku);
        if (mCacheManager != null) {
            mCacheManager.removeDanmaku(danmaku);
        } else {
            //此处不应该被调用到
            Log.w(TAG, "此处不应该被调用到，请检查一下代码逻辑");
            IDrawingCache<?> cache = danmaku.getDrawingCache();
            if (cache != null) {
                cache.destroy();
                danmaku.cache = null;
            }
        }
    }

    @Override
    public void start() {
        super.start();
        if (DEBUG) {
            Log.i(TAG, "GLDrawTask start");
        }
        NativeBitmapFactory.loadLibs();
        if (mCacheManager == null) {
            mCacheManager = new GLCacheManager();
            mRenderer.setCacheManager(mCacheManager);
        }
        mCacheManager.start();
    }

    @Override
    public void onPlayStateChanged(int state) {
        super.onPlayStateChanged(state);
        if (DEBUG) {
            Log.i(TAG, "onPlayStateChanged state = " + state);
        }
        if (mCacheManager != null) {
            mCacheManager.onPlayStateChanged(state);
        }
    }

    @Override
    public void quit() {
        super.quit();
        if (DEBUG) {
            Log.i(TAG, "GLDrawTask quit");
        }
        long startTime = System.nanoTime();
        mRenderer.setCacheManager(null);
        if (mCacheManager != null) {
            mCacheManager.quit();
            mCacheManager = null;
        }
        NativeBitmapFactory.releaseLibs();
        if (DEBUG) {
            Log.i(TAG, "GLDrawTask quit time = " + (System.nanoTime() - startTime));
        }
    }


    public class GLCacheManager implements ICacheManager {
        private static final String TAG = "GLCacheManager";
        private static final boolean DEBUG = Constants.DEBUG_GLCACHEMANAGER;

        private HandlerThread mThread;
        private GLCacheDrawHandler mHandler;
        private final Object mMonitor = new Object();
        private boolean mExited = false;

        private final TreeSet<Pair<BaseDanmaku, Integer>> mCacheTasks = new TreeSet<>(new Comparator<Pair<BaseDanmaku, Integer>>() {
            @Override
            public int compare(Pair<BaseDanmaku, Integer> o1, Pair<BaseDanmaku, Integer> o2) {
                if (o2.first == o1.first) {
                    return 0;
                }
                return DanmakuUtils.compare(o1.first, o2.first);
            }
        });

        /**
         * 存储已经缓存过的弹幕，里面包含bitmap和纹理,所以必须保证这些弹幕以后做一次释放操作，否则会有内存泄漏
         */
        private final Danmakus mCachedDanmakus = new Danmakus(ST_BY_LIST);

        @Override
        public void addDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null && danmaku != null) {
                if (DEBUG) {
                    Log.i(TAG, "addDanmaku id = " + danmaku.id);
                }
                synchronized (mCacheTasks) {
                    mCacheTasks.add(new Pair<>(danmaku, GLCacheDrawHandler.ADD_DANMAKU));
                    mHandler.sendEmptyMessage(GLCacheDrawHandler.HANDLE_DANMAKU);
                }
            }
        }

        @Override
        public void buildDanmakuCache(BaseDanmaku danmaku) {
            //不需要做任何操作，等待异步处理完成
        }

        void rebuildDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null && danmaku != null) {
                if (DEBUG) {
                    Log.i(TAG, "rebuildDanmaku id = " + danmaku.id);
                }
                synchronized (mCacheTasks) {
                    mCacheTasks.add(new Pair<>(danmaku, GLCacheDrawHandler.REBUILD_DANMAKU));
                    mHandler.sendEmptyMessage(GLCacheDrawHandler.HANDLE_DANMAKU);
                }
            }
        }

        void removeDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null) {
                if (DEBUG) {
                    Log.i(TAG, "removeCachedDanmaku id = " + danmaku.id);
                }
                synchronized (mCacheTasks) {
                    mCacheTasks.add(new Pair<>(danmaku, GLCacheDrawHandler.REMOVE_DANMAKU));
                    mHandler.sendEmptyMessage(GLCacheDrawHandler.HANDLE_DANMAKU);
                }
            }
        }

        void removeAllCachedDanmaku() {
            if (mHandler != null) {
                if (DEBUG) {
                    Log.i(TAG, "removeAllCachedDanmaku");
                }
                synchronized (mCacheTasks) {
                    mCacheTasks.clear();
                    mHandler.obtainMessage(GLCacheDrawHandler.REMOVE_ALL_CACHED_DANMAKU).sendToTarget();
                }
            }
        }

        public void start() {
            if (DEBUG) {
                Log.i(TAG, "start");
            }
            Looper workLooper = mLooper;
            if (workLooper == null) {
                //开启handler
                if (mThread == null) {
                    mThread = new HandlerThread("GLCacheManager Cache-Building Thread");
                    mThread.start();
                }
                workLooper = mThread.getLooper();
            }
            if (mHandler == null) {
                mHandler = new GLCacheDrawHandler(workLooper);
            }
            mExited = false;
            mHandler.start();
        }

        public void pause() {
            if (DEBUG) {
                Log.i(TAG, "pause");
            }
            if (mHandler != null) {
                mHandler.pause();
            }
        }

        void onPlayStateChanged(int state) {
            if (DEBUG) {
                Log.i(TAG, "onPlayStateChanged state = " + state);
            }
            if (state == IDrawTask.PLAY_STATE_PAUSE) {
                pause();
            } else if (state == IDrawTask.PLAY_STATE_PLAYING) {
                start();
            }
        }

        public void quit() {
            if (DEBUG) {
                Log.i(TAG, "quit ");
            }
            synchronized (mCacheTasks) {
                mCacheTasks.clear();
            }
            long startTime = System.nanoTime();
            if (mHandler != null) {
                mHandler.pause();
                mHandler.removeCallbacksAndMessages(null);
                mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(GLCacheDrawHandler.QUIT));
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
            } else {
                synchronized (mMonitor) {
                    while (!mExited) {
                        try {
                            mMonitor.wait();
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
            if (DEBUG) {
                Log.i(TAG, "quit time = " + (System.nanoTime() - startTime));
            }
        }

        private class GLCacheDrawHandler extends Handler {
            private static final String TAG = "GLCacheDrawHandler";
            private static final boolean DEBUG = Constants.DEBUG_GLCACHEDRAWHANDLER;
            private static final int HANDLE_DANMAKU = 10000;
            private static final int ADD_DANMAKU = 0x1;
            private static final int REBUILD_DANMAKU = 0x2;
            private static final int REMOVE_DANMAKU = 0x3;

            private static final int REMOVE_ALL_CACHED_DANMAKU = 0x4;
            /**
             * 定时清理弹幕缓存和构建将来的弹幕
             */
            private static final int DISPATCH_ACTIONS = 0x5;
            private static final int QUIT = 0x6;

            private GLShareable.GLShareHelper mGLShareHelper;
            private boolean mPause = true;

            /**
             * 构建将来缓存的参数
             */
            private long mFutureBeginOffsetBegin = -mContext.mDanmakuFactory.MAX_DANMAKU_DURATION;
            private long mFutureBeginOffsetEnd = 1000;
            private long mHandleTime = 100000000;//每次最多构建100ms
            private long mDispatchActionsTimeGap = 1000;//1秒中轮询一次缓存和构建

            GLCacheDrawHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (DEBUG) {
                    Log.i(TAG, "handleMessage what = " + what);
                }
                if (mPause && what != QUIT) {
                    //对于停止状态，只处理QUIT操作
                    return;
                }
                if (mGLShareHelper == null &&
                        what != QUIT) {
                    //共享渲染线程的glcontext
                    int tryTimes = 0;
                    //尝试三次
                    while (tryTimes++ < 3 && (mGLShareHelper = GLShareable.GLShareHelper.makeSharedGlContext(mDiplayer.getRenderer())) == null) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mGLShareHelper == null) {
                        return;
                    }
                }
                switch (what) {
                    case DISPATCH_ACTIONS:
                        clearAllCachedDanmakus(false);
                        //todo 构建将来需要显示弹幕缓存,当前问题是,如果购将这些缓存会耗时比较大
                        //buildFutureDanmaku();
                        removeMessages(DISPATCH_ACTIONS);
                        sendEmptyMessageDelayed(DISPATCH_ACTIONS, mDispatchActionsTimeGap);
                        break;
                    case HANDLE_DANMAKU:
                        mHandler.removeMessages(GLCacheDrawHandler.HANDLE_DANMAKU);
                        int size = 0;
                        long startTime = System.nanoTime();
                        while ((System.nanoTime() - startTime < mHandleTime) && (size = handleDanmaku()) > 0) {
                            if (DEBUG) {
                                Log.d(TAG, "remain size=" + size);
                            }
                        }
                        if (size > 0) {
                            //还有
                            mHandler.sendEmptyMessage(GLCacheDrawHandler.HANDLE_DANMAKU);
                        }
                        break;
                    case REMOVE_ALL_CACHED_DANMAKU:
                        clearAllCachedDanmakus(true);
                        break;
                    case QUIT:
                        removeCallbacksAndMessages(null);
                        clearAllCachedDanmakus(true);
                        GLShareable.GLShareHelper.release();
                        mGLShareHelper = null;
                        if (mThread != null) {
                            this.getLooper().quit();
                        } else {
                            synchronized (mMonitor) {
                                mExited = true;
                                mMonitor.notifyAll();
                            }
                        }
                        break;
                }
            }

            public void start() {
                if (DEBUG) {
                    Log.i(TAG, "start");
                }
                mPause = false;
                removeMessages(DISPATCH_ACTIONS);
                //开启缓存操作
                obtainMessage(DISPATCH_ACTIONS).sendToTarget();
            }

            public void pause() {
                if (DEBUG) {
                    Log.i(TAG, "pause");
                }
                mPause = true;
                //pause默认没有glcontext
                mGLShareHelper = null;
            }

            private int handleDanmaku() {
                int remainSize;
                Pair<BaseDanmaku, Integer> cacheTask;
                synchronized (mCacheTasks) {
                    cacheTask = mCacheTasks.pollFirst();
                    remainSize = mCacheTasks.size();
                }
                if (cacheTask != null) {
                    switch (cacheTask.second) {
                        case ADD_DANMAKU:
                            if (buildDanmakuCache(cacheTask.first, false)) {
                                //通知gl bitmap准备好了
                                mCachedDanmakus.addItem(cacheTask.first);
                                mDiplayer.getRenderer().getGLDanmakuHandler().addDanmaku((cacheTask.first));
                            }
                            break;
                        case REBUILD_DANMAKU:
                            if (buildDanmakuCache(cacheTask.first, true)) {
                                mCachedDanmakus.addItem((cacheTask.first));
                                mDiplayer.getRenderer().getGLDanmakuHandler().addDanmaku(cacheTask.first);
                            }
                            break;
                        case REMOVE_DANMAKU:
                            if (destroyCache(cacheTask.first, true)) {
                                mCachedDanmakus.removeItem(cacheTask.first);
                            }
                            break;
                    }
                }
                return remainSize;
            }

            private boolean buildDanmakuCache(BaseDanmaku item, boolean force) {
                if (mPause) {
                    return false;
                }
                if (!force) {
                    if (item.mGLTextureId != 0) {
                        return false;
                    }
                    if (DanmakuUtils.isCacheOk(item)) {
                        //已经被映射到纹理了或者缓存有效
                        return createTexture(item);
                    }
                }
                //先销毁先前的缓存，防止内存泄漏
                if (destroyCache(item, true)) {
                    mCachedDanmakus.removeItem(item);
                }
                // measure
                if (!item.isMeasured()) {
                    item.measure(mDisp, true);
                }
                if (!item.isPrepared()) {
                    item.prepare(mDisp, true);
                }
                if (DEBUG) {
                    Log.i(TAG, "buildDanmakuCache id = " + item.id);
                }
                //构建缓存
                item.cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, null, mContext.cachingPolicy.bitsPerPixelOfCache);
                return createTexture(item);
            }

            private boolean destroyCache(BaseDanmaku item, boolean includeTexture) {
                if (item == null) {
                    return false;
                }
                if (includeTexture) {
                    //销毁之前的纹理
                    destroyTexture(item);
                }
                IDrawingCache<?> cache = item.getDrawingCache();
                if (cache == null) {
                    return false;
                }
                if (DEBUG) {
                    Log.i(TAG, "destroyCache id = " + item.id);
                }
                cache.destroy();
                item.cache = null;
                return true;
            }

            private boolean createTexture(BaseDanmaku item) {
                if (item == null) {
                    return false;
                }
                destroyTexture(item);
                //更新对应的纹理id为失效状态
                IDrawingCache<?> drawingCache = item.cache;
                if (drawingCache == null || drawingCache.get() == null) {
                    return false;
                }
                DrawingCacheHolder holder = (DrawingCacheHolder) drawingCache.get();
                if (holder == null || holder.bitmap == null || holder.bitmap.isRecycled()) {
                    return false;
                }
                item.mGLTextureId = GLUtils.createBitmapTexture2D(holder.bitmap);
                item.mTextureWidth = holder.bitmap.getWidth();
                item.mTextureHeight = holder.bitmap.getHeight();
                if (item.mGLTextureId != 0) {
                    //已经成功创建了纹理，可以删除bitmap缓存了
                    destroyCache(item, false);
                }
                if (DEBUG) {
                    Log.d(TAG, "createTexture textid=" + item.mGLTextureId);
                }
                return true;
            }

            private void destroyTexture(BaseDanmaku item) {
                if (item == null) {
                    return;
                }
                if (item.mGLTextureId != 0) {
                    //销毁之前的纹理
                    if (DEBUG) {
                        Log.d(TAG, "destroyTexture textid=" + item.mGLTextureId);
                    }
                    GLES20.glDeleteTextures(1, new int[]{item.mGLTextureId}, 0);
                    item.mGLTextureId = 0;
                }
            }

            private void buildFutureDanmaku() {
                long begin = mTimer.currMillisecond + mFutureBeginOffsetBegin;
                long end = mTimer.currMillisecond + mFutureBeginOffsetEnd;
                //拉取构建时间内的弹幕
                IDanmakus danmakus = subnew(begin, end);
                if (danmakus == null || danmakus.isEmpty()) {
                    return;
                }
                final AtomicInteger validBuildSize = new AtomicInteger(0);
                final AtomicInteger succeedBuildSize = new AtomicInteger(0);
                final int sizeInScreen = danmakus.size();
                danmakus.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                    int orderInScreen = 0;
                    int currScreenIndex = 0;

                    @Override
                    public int accept(BaseDanmaku item) {
                        if (mPause) {
                            return ACTION_BREAK;
                        }
                        if (!item.hasPassedFilter()) {
                            mContext.mDanmakuFilters.filter(item, orderInScreen, sizeInScreen, null, true, mContext);
                        }
                        if (item.priority == 0 && item.isFiltered()) {
                            return ACTION_CONTINUE;
                        }
                        if (item.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
                            // 同屏弹幕密度只对滚动弹幕有效
                            int screenIndex = (int) ((item.getActualTime() - mTimer.currMillisecond) / mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
                            if (currScreenIndex == screenIndex)
                                orderInScreen++;
                            else {
                                orderInScreen = 0;
                                currScreenIndex = screenIndex;
                            }
                        }
                        validBuildSize.incrementAndGet();
                        if (buildDanmakuCache(item, false)) {
                            succeedBuildSize.incrementAndGet();
                            mCachedDanmakus.addItem(item);
                            mDiplayer.getRenderer().getGLDanmakuHandler().addDanmaku(item);
                        }
                        return ACTION_CONTINUE;
                    }
                });
                if (DEBUG) {
                    Log.i(TAG, "buildFutureDanmaku validBuildSize = " + validBuildSize.get() + "\t succeedBuildSize = " + succeedBuildSize.get());
                }
            }

            private void clearAllCachedDanmakus(final boolean force) {
                if (DEBUG) {
                    Log.i(TAG, "clearAllCachedDanmakus force = " + force + "\t size = " + mCachedDanmakus.size());
                }
                mCachedDanmakus.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                    @Override
                    public int accept(BaseDanmaku item) {
                        boolean releaseTexture = item.isTimeOut() || force;
                        destroyCache(item, releaseTexture);
                        return releaseTexture ? ACTION_REMOVE : ACTION_CONTINUE;
                    }
                });
            }
        }
    }
}
