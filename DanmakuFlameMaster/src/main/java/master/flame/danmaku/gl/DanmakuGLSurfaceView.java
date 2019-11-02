package master.flame.danmaku.gl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.controller.IDanmakuViewController;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.SystemClock;
import master.flame.danmaku.gl.glview.controller.TextureGLSurfaceViewRenderer;
import master.flame.danmaku.gl.wedget.GLHandlerSurfaceView;
import master.flame.danmaku.ui.widget.DanmakuTouchHelper;

public class DanmakuGLSurfaceView extends GLHandlerSurfaceView implements IDanmakuView, IDanmakuViewController {
    private static final String TAG = "DanmakuGLSurfaceView";
    private static final boolean DEBUG = Constants.DEBUG_DANMAKUGLSURFACEVIEW;
    private static final boolean DEBUG_DRAW = Constants.DEBUG_DANMAKUGLSURFACEVIEW_DRAW_STATUS;

    private final TextureGLSurfaceViewRenderer mRenderer = new TextureGLSurfaceViewRenderer(this);
    private boolean mPause = true;

    public DanmakuGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public DanmakuGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        //配置2.0
        setEGLContextClientVersion(2);
        /*不能完全避免OpenglContent丢失，但可是避免绝大部分短暂的离开app再回到app时的Content重新构建过程
         https://stackoverflow.com/questions/11067881/android-when-is-opengl-context-destroyed
         */
        setPreserveEGLContextOnPause(true);
        //配置颜色配置、深度缓存、模板缓冲
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        //设置层级关系
        setZOrderMediaOverlay(true);
        setWillNotCacheDrawing(true);
        setDrawingCacheEnabled(false);
        setWillNotDraw(true);

        //设置透明模式，否则底部的播放器会被遮盖
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mUiThreadId = Thread.currentThread().getId();
        setBackgroundColor(Color.TRANSPARENT);
        setDrawingCacheBackgroundColor(Color.TRANSPARENT);
        //因为绘制mCanvas上不会被显示，所以暂不配置，否则可能影响到其他的弹幕控件(b站弹幕库问题)
//        DrawHelper.useDrawColorToClearCanvas(true, false);
        mTouchHelper = DanmakuTouchHelper.instance(this);
    }

    @Override
    public void onPause() {
        if (!mPause) {
            super.onPause();
            mRenderer.onPause();
        }
        mPause = true;
    }

    @Override
    public void onResume() {
        if (mPause) {
            super.onResume();
            mRenderer.onResume();
        }
        mPause = false;
    }

    @Override
    public void setAlpha(float alpha) {
        mRenderer.getGLDanmakuHandler().setAlpha(alpha);
    }

    @Override
    public float getAlpha() {
        return mRenderer.getGLDanmakuHandler().getAlpha();
    }

    //==================================以下和弹幕库相关======================================
    private DrawHandler.Callback mCallback;

    private HandlerThread mHandlerThread;

    protected volatile DrawHandler handler;

    private boolean isSurfaceCreated;

    private OnDanmakuClickListener mOnDanmakuClickListener;

    private float mXOff;

    private float mYOff;

    private View.OnClickListener mOnClickListener;

    private DanmakuTouchHelper mTouchHelper;

    private boolean mShowFps;

    private boolean mDanmakuVisible = true;

    protected int mDrawingThreadType = THREAD_TYPE_NORMAL_PRIORITY;

    protected boolean mRequestRender = false;

    private long mUiThreadId;

    private static final int MAX_RECORD_SIZE = 50;
    private static final int ONE_SECOND = 1000;
    private LinkedList<Long> mDrawTimes;


    /**
     * 该mBitmap，mCanvas用来作为原先绘制方式的画布位图，大小等于view的大小
     * 因为DrawTask需要操作一个Canvas，所以构造了一个和原来相同的画布
     * 绘制内容并不会显示
     */
    private Bitmap mBitmap;
    private Canvas mCanvas;

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) {
            handler.addDanmaku(item);
        }
    }

    @Override
    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        if (handler != null) {
            handler.invalidateDanmaku(item, remeasure);
        }
    }

    @Override
    public void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        if (handler != null) {
            handler.removeAllDanmakus(isClearDanmakusOnScreen);
        }
    }

    @Override
    public void removeAllLiveDanmakus() {
        if (handler != null) {
            handler.removeAllLiveDanmakus();
        }
    }

    @Override
    public IDanmakus getCurrentVisibleDanmakus() {
        if (handler != null) {
            return handler.getCurrentVisibleDanmakus();
        }
        return null;
    }

    @Override
    public void setCallback(DrawHandler.Callback callback) {
        mCallback = callback;
        if (handler != null) {
            handler.setCallback(callback);
        }
    }

    @Override
    public void release() {
        if (DEBUG) {
            Log.d(TAG, "release");
        }
        stop();
        if (mDrawTimes != null) {
            mDrawTimes.clear();
        }
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
    }

    @Override
    public void stop() {
        if (DEBUG) {
            Log.d(TAG, "stopDraw start");
        }
        stopDraw();
        if (DEBUG) {
            Log.d(TAG, "stopDraw finish");
        }
    }

    private synchronized void stopDraw() {
        if (this.handler == null) {
            return;
        }
        DrawHandler handler = this.handler;
        this.handler = null;
        //opengl退出
        super.exit();
        if (handler != null) {
            handler.quit();
        }
        //退出handle线程
        HandlerThread handlerThread = this.mHandlerThread;
        mHandlerThread = null;
        if (handlerThread != null) {
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handlerThread.quit();
        }
    }

    protected synchronized Looper getLooper(int type) {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        int priority;
        switch (type) {
            case THREAD_TYPE_MAIN_THREAD:
                return Looper.getMainLooper();
            case THREAD_TYPE_HIGH_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY;
                break;
            case THREAD_TYPE_LOW_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_LOWEST;
                break;
            case THREAD_TYPE_NORMAL_PRIORITY:
            default:
                priority = android.os.Process.THREAD_PRIORITY_DEFAULT;
                break;
        }
        String threadName = "DFM Handler Thread #" + priority;
        mHandlerThread = new HandlerThread(threadName, priority);
        mHandlerThread.start();
        if (DEBUG) {
            Log.d(TAG, "start a new looper,type=" + type);
        }
        return mHandlerThread.getLooper();
    }

    private void prepare() {
        if (handler == null) {
            Looper looper = getLooper(mDrawingThreadType);
            setRenderer(mRenderer, looper);
            setRenderMode(GLHandlerSurfaceView.RENDERMODE_WHEN_DIRTY);
            handler = new DrawHandler(looper, this, mDanmakuVisible);
            if (DEBUG) {
                Log.d(TAG, "prepare");
            }
        }
    }

    @Override
    public void prepare(BaseDanmakuParser parser, DanmakuContext config) {
        //此处mDisplayer必须是AndroidGLDisplayer
        ((AndroidGLDisplayer) config.mDisplayer).setRenderer(mRenderer);
        prepare();
        handler.setConfig(config);
        handler.setParser(parser);
        handler.setCallback(mCallback);
        handler.prepare();
    }

    @Override
    public boolean isPrepared() {
        return handler != null && handler.isPrepared();
    }

    @Override
    public DanmakuContext getConfig() {
        if (handler == null) {
            return null;
        }
        return handler.getConfig();
    }

    @Override
    public void showFPS(boolean show) {
        mShowFps = show;
    }

    private float fps() {
        if (mDrawTimes == null) {
            mDrawTimes = new LinkedList<>();
        }
        long lastTime = SystemClock.uptimeMillis();
        mDrawTimes.addLast(lastTime);
        Long first = mDrawTimes.peekFirst();
        if (first == null) {
            return 0.0f;
        }
        float dtime = lastTime - first;
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes.removeFirst();
        }
        return dtime > 0 ? mDrawTimes.size() * ONE_SECOND / dtime : 0.0f;
    }

    @Override
    public long drawDanmakus() {
        if (DEBUG_DRAW) {
            Log.d(TAG, "drawDanmakus");
        }
        if (!isSurfaceCreated)
            return 0;
        if (!isShown())
            return -1;
        long stime = SystemClock.uptimeMillis();
        if (mCanvas == null) {
            initTempCanvas(getWidth(), getHeight());
        }
        if (mCanvas != null) {
            handler.draw(mCanvas);
        }
        mRenderer.requestRender();
        if (mShowFps) {
            //todo 暂时fps，暂时没有想到解决方案，但可以通过打印log方式
            Log.i(TAG, String.format("danmuku fps = %.2f", fps()));
        }
        return SystemClock.uptimeMillis() - stime;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (DEBUG) {
            Log.d(TAG, "onLayout");
        }
        initTempCanvas(right - left, bottom - top);
        if (handler != null) {
            handler.notifyDispSizeChanged(right - left, bottom - top);
        }
        isSurfaceCreated = true;
    }

    private void initTempCanvas(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (mBitmap == null || mBitmap.getWidth() != width || mBitmap.getHeight() != height || mBitmap.isRecycled()) {
            if (mBitmap != null && !mBitmap.isRecycled()) {
                mBitmap.recycle();
            }
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
            if (mCanvas == null) {
                mCanvas = new Canvas();
            }
            mCanvas.setBitmap(mBitmap);
            if (DEBUG) {
                Log.d(TAG, "create a new temp bitmap,width=" + width + ",height=" + height);
            }
        }
    }

    @Override
    public void toggle() {
        if (isSurfaceCreated) {
            if (handler == null)
                start();
            else if (handler.isStop()) {
                resume();
            } else {
                pause();
            }
        }
    }

    @Override
    public void pause() {
        if (DEBUG) {
            Log.d(TAG, "pause");
        }
        onPause();
        if (handler != null) {
            handler.removeCallbacks(mResumeRunnable);
            handler.pause();
        }
    }

    private int mResumeTryCount = 0;

    private Runnable mResumeRunnable = new Runnable() {
        @Override
        public void run() {
            DrawHandler drawHandler = handler;
            if (drawHandler == null) {
                return;
            }
            mResumeTryCount++;
            if (mResumeTryCount > 4 || DanmakuGLSurfaceView.this.isShown()) {
                drawHandler.resume();
            } else {
                drawHandler.postDelayed(this, 100 * mResumeTryCount);
            }
        }
    };

    @Override
    public void resume() {
        if (DEBUG) {
            Log.d(TAG, "resume");
        }
        onResume();
        if (handler != null && handler.isPrepared()) {
            mResumeTryCount = 0;
            handler.post(mResumeRunnable);
        } else if (handler == null) {
            restart();
        }
    }

    @Override
    public boolean isPaused() {
        if (handler != null) {
            return handler.isStop();
        }
        return false;
    }

    public void restart() {
        if (DEBUG) {
            Log.d(TAG, "restart");
        }
        stop();
        start();
    }

    @Override
    public void start() {
        if (DEBUG) {
            Log.d(TAG, "start");
        }
        start(0);
    }

    @Override
    public void start(long position) {
        if (DEBUG) {
            Log.d(TAG, "start position=" + position);
        }
        if (handler == null) {
            prepare();
        } else {
            handler.removeCallbacksAndMessages(null);
        }
        handler.obtainMessage(DrawHandler.START, position).sendToTarget();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mTouchHelper.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void seekTo(Long ms) {
        if (DEBUG) {
            Log.d(TAG, "seekTo " + ms);
        }
        if (handler != null) {
            handler.seekTo(ms);
        }
    }


    @Override
    public void enableDanmakuDrawingCache(boolean enable) {
        //opengl必须使用缓存，然后放到opengl中
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        //此处必须为true
        return true;
    }

    @Override
    public boolean isViewReady() {
        return isSurfaceCreated;
    }

    @Override
    public int getViewWidth() {
        return super.getWidth();
    }

    @Override
    public int getViewHeight() {
        return super.getHeight();
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {
        if (DEBUG) {
            Log.d(TAG, "show ");
        }
        showAndResumeDrawTask(null);
    }

    @Override
    public void showAndResumeDrawTask(Long position) {
        if (DEBUG) {
            Log.d(TAG, "show position = " + position);
        }
        mDanmakuVisible = true;
        mRenderer.show();
        if (handler == null) {
            return;
        }
        handler.showDanmakus(position);
    }

    @Override
    public void hide() {
        if (DEBUG) {
            Log.d(TAG, "hide ");
        }
        mDanmakuVisible = false;
        mRenderer.hide();
        if (handler == null) {
            return;
        }
        handler.hideDanmakus(false);
    }

    @Override
    public long hideAndPauseDrawTask() {
        mDanmakuVisible = false;
        mRenderer.hide();
        if (handler == null) {
            return 0;
        }
        return handler.hideDanmakus(true);
    }

    @Override
    public void clear() {
        if (DEBUG) {
            Log.d(TAG, "clear ");
        }
        if (!isViewReady()) {
            return;
        }
        mRenderer.clearNextFrame();
    }

    @Override
    public boolean isShown() {
        return mDanmakuVisible && super.isShown();
    }

    @Override
    public void setDrawingThreadType(int type) {
        mDrawingThreadType = type;
    }

    @Override
    public long getCurrentTime() {
        if (handler != null) {
            return handler.getCurrentTime();
        }
        return 0;
    }

    @Override
    @SuppressLint("NewApi")
    public boolean isHardwareAccelerated() {
        //isHardwareAccelerated为true会触发cache分片，opengl实现不需要
        return false;
    }

    @Override
    public void clearDanmakusOnScreen() {
        if (DEBUG) {
            Log.d(TAG, "clearDanmakusOnScreen ");
        }
        if (handler != null) {
            handler.clearDanmakusOnScreen();
        }
    }

    @Override
    public void setOnDanmakuClickListener(OnDanmakuClickListener listener) {
        mOnDanmakuClickListener = listener;
    }

    @Override
    public void setOnDanmakuClickListener(OnDanmakuClickListener listener, float xOff,
                                          float yOff) {
        mOnDanmakuClickListener = listener;
        mXOff = xOff;
        mYOff = yOff;
    }

    @Override
    public OnDanmakuClickListener getOnDanmakuClickListener() {
        return mOnDanmakuClickListener;
    }

    @Override
    public float getXOff() {
        return mXOff;
    }

    @Override
    public float getYOff() {
        return mYOff;
    }

    @Override
    public void forceRender() {
        if (DEBUG) {
            Log.d(TAG, "forceRender ");
        }
        mRequestRender = true;
        handler.forceRender();
        mRenderer.requestRender();
    }
}
