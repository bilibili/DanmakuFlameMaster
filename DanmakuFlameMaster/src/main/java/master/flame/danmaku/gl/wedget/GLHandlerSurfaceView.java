
package master.flame.danmaku.gl.wedget;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLDebugHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import master.flame.danmaku.gl.Constants;

public class GLHandlerSurfaceView extends SurfaceView implements SurfaceHolder.Callback2, GLShareable {
    private final static String TAG = "GLHandlerSurfaceView";
    public int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private final static long NORMAL_WAIT_TIME = 20;
    private final static boolean DEBUG = Constants.DEBUG_GLHANDLERSURFACEVIEW;
    private final static boolean LOG_ATTACH_DETACH = false;
    private final static boolean LOG_THREADS = false;
    private final static boolean LOG_PAUSE_RESUME = false;
    private final static boolean LOG_SURFACE = false;
    private final static boolean LOG_RENDERER = false;
    private final static boolean LOG_RENDERER_DRAW_FRAME = false;
    private final static boolean LOG_EGL = false;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    public final static int DEFAULT_RENDERMODE = RENDERMODE_CONTINUOUSLY;


    public final static int DEBUG_CHECK_GL_ERROR = 1;
    public final static int DEBUG_LOG_GL_CALLS = 2;

    private static final int MSG_RUN = 1;
    private static final int MSG_SCHEDULE = 2;
    private final Object mMonitor = new Object();

    private GLHandler mHandler;
    private Looper mWorkLooper;
    private HandlerThread mHandlerThread;
    private int mThreadPriority = Thread.NORM_PRIORITY;
    private boolean mAllowMainThreadLooper = false;

    private final WeakReference<GLHandlerSurfaceView> mThisWeakRef =
            new WeakReference<>(this);
    private boolean mDetached;
    private Renderer mRenderer;
    private EGLConfigChooser mEGLConfigChooser;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
    private int mEGLContextClientVersion;
    private boolean mPreserveEGLContextOnPause;

    public GLHandlerSurfaceView(Context context) {
        super(context);
        init();
    }

    public GLHandlerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    protected void finalize() throws Throwable {
        if (DEBUG) {
            Log.i(TAG, "finalize");
        }
        try {
            exit();
        } finally {
            super.finalize();
        }
    }

    public void exit() {
        boolean exitSucceed = false;
        long start = System.nanoTime();
        if (mHandler != null) {
            mHandler.requestExitAndWait();
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mWorkLooper = null;
            }
            mHandler = null;
            exitSucceed = true;
        }
        if (DEBUG) {
            Log.i(TAG, "exit state=" + exitSucceed + "\t time=" + (System.nanoTime() - start));
        }
    }

    public void setGLWrapper(GLWrapper glWrapper) {
        mGLWrapper = glWrapper;
    }

    public void setDebugFlags(int debugFlags) {
        mDebugFlags = debugFlags;
    }

    public int getDebugFlags() {
        return mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
    }

    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
    }

    public void setAllowMainThreadLooper(boolean allow) {
        this.mAllowMainThreadLooper = allow;
    }

    public boolean isAllowMainThreadLooper() {
        return this.mAllowMainThreadLooper;
    }

    public void setThreadPriority(int priority) {
        checkRenderThreadState();
        this.mThreadPriority = priority;
    }

    public int getThreadPriority() {
        return this.mThreadPriority;
    }

    public void setRenderer(Renderer renderer) {
        setRenderer(renderer, null);
    }

    public void setRenderer(Renderer renderer, Looper workLooper) {
        checkRenderThreadState();
        if (DEBUG) {
            Log.i(TAG, "setRenderer workLooper = " + workLooper);
        }
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (mEGLContextFactory == null) {
            mEGLContextFactory = new DefaultContextFactory();
        }
        if (mEGLWindowSurfaceFactory == null) {
            mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        mRenderer = renderer;
        if (workLooper == null) {
            mHandlerThread = new HandlerThread(getClass().getSimpleName(), mThreadPriority);
            mHandlerThread.start();
            workLooper = mHandlerThread.getLooper();
        }
        if (!mAllowMainThreadLooper && workLooper == Looper.getMainLooper()) {
            throw new IllegalArgumentException("main looper is not allowed to be gl thread");
        }
        mWorkLooper = workLooper;
        mHandler = new GLHandler(mWorkLooper, mThisWeakRef);
        mHandler.obtainMessage(MSG_RUN).sendToTarget();
    }

    public void setEGLContextFactory(EGLContextFactory factory) {
        checkRenderThreadState();
        mEGLContextFactory = factory;
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        checkRenderThreadState();
        mEGLWindowSurfaceFactory = factory;
    }

    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        checkRenderThreadState();
        mEGLConfigChooser = configChooser;
    }

    public void setEGLConfigChooser(boolean needDepth) {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize));
    }

    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode) {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer before call setRenderMode");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "setRenderMode renderMode = " + renderMode);
        }
        mHandler.setRenderMode(renderMode);
    }

    public int getRenderMode() {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer before call getRenderMode");
            return DEFAULT_RENDERMODE;
        }
        return mHandler.getRenderMode();
    }

    public void requestRender() {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer before call requestRender");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "requestRender");
        }
        mHandler.requestRender();
    }

    private void checkRenderThreadState() {
        if (mHandler != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mHandler == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "surfaceCreated");
        }
        mHandler.surfaceCreated();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mHandler == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "surfaceDestroyed");
        }
        mHandler.surfaceDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHandler == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "surfaceChanged");
        }
        mHandler.onWindowResize(w, h);
    }

    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        if (mHandler == null) {
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "surfaceRedrawNeededAsync");
        }
        mHandler.requestRenderAndNotify(finishDrawing);
    }

    @Deprecated
    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {

    }

    public void onPause() {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "onPause");
        }
        mHandler.onPause();
    }

    public void onResume() {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "onResume");
        }
        mHandler.onResume();
    }

    public void queueEvent(Runnable r) {
        if (mHandler == null) {
            Log.w(TAG, "did you have called the setRenderer before call queueEvent");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "queueEvent");
        }
        mHandler.queueEvent(r);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + mDetached);
        }

        if (mDetached && (mRenderer != null)) {
            int renderMode = DEFAULT_RENDERMODE;
            if (mHandler != null) {
                renderMode = mHandler.getRenderMode();
            }
            setRenderer(mRenderer, mWorkLooper);
            setRenderMode(renderMode);
        }
        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow");
        }
        exit();
        mDetached = true;
        super.onDetachedFromWindow();
    }

    @Override
    public EGLContext getSharedContext() {
        GLHandler handler = mHandler;
        if (handler == null) {
            return null;
        }
        return handler.mEglHelper.mEglContext;
    }

    @Override
    public EGLConfig getSharedConfig() {
        GLHandler handler = mHandler;
        if (handler == null) {
            return null;
        }
        return handler.mEglHelper.mEglConfig;
    }

    @Override
    public int getEGLContextClientVersion() {
        return mEGLContextClientVersion;
    }

    @Override
    public int getSurfaceWidth() {
        return getWidth();
    }

    @Override
    public int getSurfaceHeight() {
        return getHeight();
    }

    //==================================inner classes or ininterfaces==========================================================

    static class GLHandler extends Handler {
        WeakReference<GLHandlerSurfaceView> mWeakReference;
        private EglHelper mEglHelper;

        private boolean mShouldExit;
        private boolean mExited;
        private boolean mRequestPaused;
        private boolean mPaused;
        private boolean mHasSurface;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mFinishedCreatingEglSurface;
        private boolean mShouldReleaseEglContext;
        private int mWidth;
        private int mHeight;
        private int mRenderMode;
        private boolean mRequestRender;
        private boolean mWantRenderNotification;
        private boolean mRenderComplete;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable = null;

        GL10 gl = null;
        boolean createEglContext = false;
        boolean createEglSurface = false;
        boolean createGlInterface = false;
        boolean lostEglContext = false;
        boolean sizeChanged = false;
        boolean wantRenderNotification = false;
        boolean doRenderNotification = false;
        boolean askedToReleaseEglContext = false;
        int w = 0;
        int h = 0;
        Runnable finishDrawingRunnable = null;

        private long mLastRun = 0;

        GLHandler(Looper looper, WeakReference<GLHandlerSurfaceView> glSurfaceViewWeakRef) {
            super(looper);
            mWeakReference = glSurfaceViewWeakRef;
            mEglHelper = new EglHelper(mWeakReference);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            if (glHandlerSurfaceView == null) {
                //view 被销毁了
                exit();
                return;
            }

            try {
                while (true) {
                    synchronized (glHandlerSurfaceView.mMonitor) {
                        while (true) {
                            if (mShouldExit) {
                                exit();
                                return;
                            }

                            if (!mEventQueue.isEmpty()) {
                                Runnable remove = mEventQueue.remove(0);
                                if (remove != null) {
                                    remove.run();
                                }
                                continue;
                            }

                            // Update the pause state.
                            boolean pausing = false;
                            if (mPaused != mRequestPaused) {
                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                notifyMonitor();
                                if (LOG_PAUSE_RESUME) {
                                    Log.i("GLThread", "mPaused is now " + mPaused + " tid=" + Thread.currentThread().getId());
                                }
                            }

                            // Do we need to give up the EGL context?
                            if (mShouldReleaseEglContext) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL context because asked to tid=" + Thread.currentThread().getId());
                                }
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mShouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            // Have we lost the EGL context?
                            if (lostEglContext) {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                lostEglContext = false;
                            }

                            // When pausing, release the EGL surface:
                            if (pausing && mHaveEglSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL surface because paused tid=" + Thread.currentThread().getId());
                                }
                                stopEglSurfaceLocked();
                            }

                            // When pausing, optionally release the EGL Context:
                            if (pausing && mHaveEglContext) {
                                if (!glHandlerSurfaceView.mPreserveEGLContextOnPause) {
                                    stopEglContextLocked();
                                    if (LOG_SURFACE) {
                                        Log.i("GLThread", "releasing EGL context because paused tid=" + Thread.currentThread().getId());
                                    }
                                }
                            }

                            // Have we lost the SurfaceView surface?
                            if ((!mHasSurface) && (!mWaitingForSurface)) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed surfaceView surface lost tid=" + Thread.currentThread().getId());
                                }
                                if (mHaveEglSurface) {
                                    stopEglSurfaceLocked();
                                }
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                notifyMonitor();
                            }

                            // Have we acquired the surface view surface?
                            if (mHasSurface && mWaitingForSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed surfaceView surface acquired tid=" + Thread.currentThread().getId());
                                }
                                mWaitingForSurface = false;
                                notifyMonitor();
                            }

                            if (doRenderNotification) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "sending render notification tid=" + Thread.currentThread().getId());
                                }
                                mWantRenderNotification = false;
                                doRenderNotification = false;
                                mRenderComplete = true;
                                notifyMonitor();
                            }

                            if (mFinishDrawingRunnable != null) {
                                finishDrawingRunnable = mFinishDrawingRunnable;
                                mFinishDrawingRunnable = null;
                            }

                            // Ready to draw?
                            if (readyToDraw()) {

                                // If we don't have an EGL context, try to acquire one.
                                if (!mHaveEglContext) {
                                    if (askedToReleaseEglContext) {
                                        askedToReleaseEglContext = false;
                                    } else {
                                        try {
                                            mEglHelper.start();
                                        } catch (RuntimeException t) {
                                            notifyMonitor();
                                            throw t;
                                        }
                                        mHaveEglContext = true;
                                        createEglContext = true;

                                        notifyMonitor();
                                    }
                                }

                                if (mHaveEglContext && !mHaveEglSurface) {
                                    mHaveEglSurface = true;
                                    createEglSurface = true;
                                    createGlInterface = true;
                                    sizeChanged = true;
                                }

                                if (mHaveEglSurface) {
                                    if (mSizeChanged) {
                                        sizeChanged = true;
                                        w = mWidth;
                                        h = mHeight;
                                        mWantRenderNotification = true;
                                        if (LOG_SURFACE) {
                                            Log.i("GLThread",
                                                    "noticing that we want render notification tid="
                                                            + Thread.currentThread().getId());
                                        }

                                        // Destroy and recreate the EGL surface.
                                        createEglSurface = true;

                                        mSizeChanged = false;
                                    }
                                    mRequestRender = false;
                                    notifyMonitor();
                                    if (mWantRenderNotification) {
                                        wantRenderNotification = true;
                                    }
                                    scheduleNextRun();
                                    break;
                                }
                            } else {
                                if (finishDrawingRunnable != null) {
                                    Log.w(TAG, "Warning, !readyToDraw() but waiting for " +
                                            "draw finished! Early reporting draw finished.");
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            }
                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_THREADS) {
                                Log.i("GLThread", "waiting tid=" + Thread.currentThread().getId()
                                        + " mHaveEglContext: " + mHaveEglContext
                                        + " mHaveEglSurface: " + mHaveEglSurface
                                        + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                        + " mPaused: " + mPaused
                                        + " mHasSurface: " + mHasSurface
                                        + " mSurfaceIsBad: " + mSurfaceIsBad
                                        + " mWaitingForSurface: " + mWaitingForSurface
                                        + " mWidth: " + mWidth
                                        + " mHeight: " + mHeight
                                        + " mRequestRender: " + mRequestRender
                                        + " mRenderMode: " + mRenderMode);
                            }
                            return;
                        }
                    } // end of synchronized(sGLThreadManager)


                    if (createEglSurface) {
                        if (LOG_SURFACE) {
                            Log.w("GLThread", "egl createSurface");
                        }
                        if (mEglHelper.createSurface()) {
                            synchronized (glHandlerSurfaceView.mMonitor) {
                                mFinishedCreatingEglSurface = true;
                                glHandlerSurfaceView.mMonitor.notifyAll();
                            }
                        } else {
                            synchronized (glHandlerSurfaceView.mMonitor) {
                                mFinishedCreatingEglSurface = true;
                                mSurfaceIsBad = true;
                                glHandlerSurfaceView.mMonitor.notifyAll();
                            }
                            continue;
                        }
                        createEglSurface = false;
                    }

                    if (createGlInterface) {
                        gl = (GL10) mEglHelper.createGL();

                        createGlInterface = false;
                    }

                    if (createEglContext) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceCreated");
                        }
                        glHandlerSurfaceView.mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        glHandlerSurfaceView.mRenderer.onSurfaceChanged(gl, w, h);
                        sizeChanged = false;
                    }

                    if (LOG_RENDERER_DRAW_FRAME) {
                        Log.w("GLThread", "onDrawFrame tid=" + Thread.currentThread().getId());
                    }
                    glHandlerSurfaceView.mRenderer.onDrawFrame(gl);
                    if (finishDrawingRunnable != null) {
                        finishDrawingRunnable.run();
                        finishDrawingRunnable = null;
                    }
                    int swapError = mEglHelper.swap();
                    switch (swapError) {
                        case EGL10.EGL_SUCCESS:
                            break;
                        case EGL11.EGL_CONTEXT_LOST:
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + Thread.currentThread().getId());
                            }
                            lostEglContext = true;
                            break;
                        default:
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);
                            synchronized (glHandlerSurfaceView.mMonitor) {
                                mSurfaceIsBad = true;
                                glHandlerSurfaceView.mMonitor.notifyAll();
                            }
                            break;
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                        wantRenderNotification = false;
                    }
                    return;
                }
            } catch (Throwable throwable) {
                exit();
                throw throwable;
            } finally {
                synchronized (glHandlerSurfaceView.mMonitor) {
                    glHandlerSurfaceView.mMonitor.notifyAll();
                }
            }
        }

        private void stopEglSurfaceLocked() {
            if (mHaveEglSurface) {
                mHaveEglSurface = false;
                mEglHelper.destroySurface();
            }
        }

        private void stopEglContextLocked() {
            if (mHaveEglContext) {
                mEglHelper.finish();
                mHaveEglContext = false;
                notifyMonitor();
            }
        }

        private void exit() {
            try {
                stopEglSurfaceLocked();
                stopEglContextLocked();
            } finally {
                mExited = true;
            }
        }

        private void scheduleNextRun() {
            if (mRenderMode == GLHandlerSurfaceView.RENDERMODE_CONTINUOUSLY) {
                long curr = SystemClock.uptimeMillis();
                long gap = curr - mLastRun;
                if (gap > 16) {
                    gap = 0;
                } else {
                    gap = 16 - gap;
                }
                mLastRun = curr;
                removeMessages(MSG_SCHEDULE);
                sendEmptyMessageDelayed(MSG_SCHEDULE, gap);
            }
        }

        private void notifyMonitor() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            if (glHandlerSurfaceView != null) {
                synchronized (glHandlerSurfaceView.mMonitor) {
                    glHandlerSurfaceView.mMonitor.notifyAll();
                }
            }
        }

        private boolean isLooperThread() {
            return Looper.myLooper() == getLooper();
        }

        public boolean ableToDraw() {
            return mHaveEglContext && mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                    && (mWidth > 0) && (mHeight > 0)
                    && (mRequestRender || (mRenderMode == RENDERMODE_CONTINUOUSLY));
        }

        public void setRenderMode(int renderMode) {
            if (!((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTINUOUSLY))) {
                throw new IllegalArgumentException("renderMode");
            }
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mRenderMode = renderMode;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                }
            }
        }

        public int getRenderMode() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mRequestRender = true;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                }
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mWantRenderNotification = true;
                mRequestRender = true;
                mRenderComplete = false;
                mFinishDrawingRunnable = finishDrawing;

                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                }
            }
        }

        public void surfaceCreated() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated tid=" + getLooper().getThread().getId());
                }
                mHasSurface = true;
                mFinishedCreatingEglSurface = false;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while (mWaitingForSurface
                            && !mFinishedCreatingEglSurface
                            && !mExited) {
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed tid=" + getLooper().getThread().getId());
                }
                mHasSurface = false;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while ((!mWaitingForSurface) && (!mExited)) {
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
        }

        public void onPause() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause tid=" + getLooper().getThread().getId());
                }
                mRequestPaused = true;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while ((!mExited) && (!mPaused)) {
                        if (LOG_PAUSE_RESUME) {
                            Log.i("Main thread", "onPause waiting for mPaused.");
                        }
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
        }

        public void onResume() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume tid=" + getLooper().getThread().getId());
                }
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while ((!mExited) && mPaused && (!mRenderComplete)) {
                        if (LOG_PAUSE_RESUME) {
                            Log.i("Main thread", "onResume waiting for !mPaused.");
                        }
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                mRequestRender = true;
                mRenderComplete = false;

                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while (!mExited && !mPaused && !mRenderComplete
                            && ableToDraw()) {
                        if (LOG_SURFACE) {
                            Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getLooper().getThread().getId());
                        }
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }

            }
        }

        public void requestExitAndWait() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mShouldExit = true;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                    while (!mExited) {
                        try {
                            glHandlerSurfaceView.mMonitor.wait(NORMAL_WAIT_TIME);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mShouldReleaseEglContext = true;
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                }
            }
        }

        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            GLHandlerSurfaceView glHandlerSurfaceView = mWeakReference.get();
            synchronized (glHandlerSurfaceView.mMonitor) {
                mEventQueue.add(r);
                removeMessages(MSG_RUN);
                if (isLooperThread()) {
                    handleMessage(obtainMessage(MSG_RUN));
                } else {
                    obtainMessage(MSG_RUN).sendToTarget();
                }
            }
        }
    }

    public interface GLWrapper {
        GL wrap(GL gl);
    }

    public interface Renderer {
        void onSurfaceCreated(GL10 gl, EGLConfig config);

        void onSurfaceChanged(GL10 gl, int width, int height);

        void onDrawFrame(GL10 gl);
    }

    public interface EGLContextFactory {
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

        void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
    }

    private class DefaultContextFactory implements EGLContextFactory {

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                    EGL10.EGL_NONE};

            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                    mEGLContextClientVersion != 0 ? attrib_list : null);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display,
                                   EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                if (LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
                }
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    public interface EGLWindowSurfaceFactory {

        EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
                                       Object nativeWindow);

        void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
    }

    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
                                              EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "eglCreateWindowSurface", e);
            }
            return result;
        }

        public void destroySurface(EGL10 egl, EGLDisplay display,
                                   EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    public interface EGLConfigChooser {
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
    }

    private abstract class BaseConfigChooser
            implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                        EGLConfig[] configs);

        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                return configSpec;
            }
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
            if (mEGLContextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len + 1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    private static class EglHelper {
        private WeakReference<GLHandlerSurfaceView> mGLSurfaceViewWeakRef;
        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;

        public EglHelper(WeakReference<GLHandlerSurfaceView> glSurfaceViewWeakRef) {
            mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        public void start() {
            if (LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
            }

            mEgl = (EGL10) EGLContext.getEGL();
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }

            int[] version = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            GLHandlerSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view == null) {
                mEglConfig = null;
                mEglContext = null;
            } else {
                mEglConfig = view.mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
                mEglContext = view.mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            }
            if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
                mEglContext = null;
                throwEglException("createContext");
            }
            if (LOG_EGL) {
                Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().getId());
            }

            mEglSurface = null;
        }

        public boolean createSurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            if (mEgl == null) {
                throw new RuntimeException("egl not initialized");
            }
            if (mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            }

            destroySurfaceImp();

            GLHandlerSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view != null) {
                mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                        mEglDisplay, mEglConfig, view.getHolder());
            } else {
                mEglSurface = null;
            }

            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
                int error = mEgl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
                return false;
            }

            return true;
        }

        GL createGL() {
            GL gl = mEglContext.getGL();
            GLHandlerSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view != null) {
                if (view.mGLWrapper != null) {
                    gl = view.mGLWrapper.wrap(gl);
                }

                if ((view.mDebugFlags & (DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS)) != 0) {
                    int configFlags = 0;
                    Writer log = null;
                    if ((view.mDebugFlags & DEBUG_CHECK_GL_ERROR) != 0) {
                        configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                    }
                    if ((view.mDebugFlags & DEBUG_LOG_GL_CALLS) != 0) {
                        log = new LogWriter();
                    }
                    gl = GLDebugHelper.wrap(gl, configFlags, log);
                }
            }
            return gl;
        }

        public int swap() {
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                return mEgl.eglGetError();
            }
            return EGL10.EGL_SUCCESS;
        }

        public void destroySurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                GLHandlerSurfaceView view = mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
                }
                mEglSurface = null;
            }
        }

        public void finish() {
            if (LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (mEglContext != null) {
                GLHandlerSurfaceView view = mGLSurfaceViewWeakRef.get();
                if (view != null) {
                    view.mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
                }
                mEglContext = null;
            }
            if (mEglDisplay != null) {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            String message = formatEglError(function, error);
            if (LOG_THREADS) {
                Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " "
                        + message);
            }
            throw new RuntimeException(message);
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            return function + " failed: " + "0x" + Integer.toHexString(error);
        }

    }

    static class LogWriter extends Writer {

        @Override
        public void close() {
            flushBuilder();
        }

        @Override
        public void flush() {
            flushBuilder();
        }

        @Override
        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (mBuilder.length() > 0) {
                Log.v("GLSurfaceView", mBuilder.toString());
                mBuilder.delete(0, mBuilder.length());
            }
        }

        private StringBuilder mBuilder = new StringBuilder();
    }
}
