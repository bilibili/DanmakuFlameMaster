package master.flame.danmaku.gl.glview.controller;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.gl.Constants;
import master.flame.danmaku.gl.glview.view.GLViewGroup;
import master.flame.danmaku.gl.wedget.GLHandlerSurfaceView;

/**
 * 创建人:yangzhiqian
 * 创建时间:2018/7/12 14:12
 * 备注: 负责传递弹幕信息和控制gl线程空闲时的任务调度
 */
public class GLDanmakuHandler implements Runnable {
    private static final String TAG = "GLDanmakuHandler";
    private static final boolean DEBUG = Constants.DEBUG_GLDANMAKUHANDLER;
    private GLHandlerSurfaceView mGLSurfaceView;
    private final GLViewGroup mGLViewGroup;
    private boolean mPaused = true;
    private volatile boolean mPrepareRender = false;
    private final AtomicBoolean mPostRunning = new AtomicBoolean(false);
    private boolean mHasGLContext = false;

    public GLDanmakuHandler(GLHandlerSurfaceView surfaceView) {
        if (surfaceView == null) {
            throw new RuntimeException("surfaceView 不能为空");
        }
        this.mGLSurfaceView = surfaceView;
        this.mGLViewGroup = new GLViewGroup(surfaceView.getContext());
        mGLViewGroup.setViewsReverseState(false, false);
    }

    /**
     * gl线程
     */
    public void onSurfaceCreate() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreate");
        }
        mHasGLContext = true;
        mGLViewGroup.onGLSurfaceViewCreate();
        postRun();
    }

    /**
     * gl线程
     */
    public void onSurfaceSizeChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceSizeChanged width=" + width + "\theight=" + height);
        }
        mHasGLContext = true;
        mGLViewGroup.onDisplaySizeChanged(width, height);
    }

    /**
     * gl线程
     */
    public void onGLDrawFrame() {
        if (DEBUG) {
            Log.d(TAG, "onGLDrawFrame");
        }
        mHasGLContext = true;
        if (!mPaused) {
            mGLViewGroup.onGLDrawFrame();
        }
        //渲染完成
        mPrepareRender = false;
        //此时渲染线程空闲，重新开启辅助任务
        postRun();
    }

    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume");
        }
        mPaused = false;
    }

    public void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onResume");
        }
        //GLSurface在onPause时会造成GLContext丢失，即使调用了setPreserveEGLContextOnPause也存在丢失的情况
        //此处在onPause是默认GLContext丢失，当opengl生命周期启动时GLContext一定存在
        mHasGLContext = false;
        mPaused = true;
    }

    public void prepareRender() {
        if (DEBUG) {
            Log.d(TAG, "prepareRender");
        }
        //上层调用让GLSurfaceView渲染，此时对于空闲时执行的任务可以暂时停止，优先渲染
        mPrepareRender = true;
    }

    public void addDanmaku(BaseDanmaku danmaku) {
        if (DEBUG) {
            Log.d(TAG, "addDanmaku");
        }
        if (danmaku == null) {
            return;
        }
        mGLViewGroup.addDanmu(danmaku);
        postRun();
    }

    public void removeDamaku(BaseDanmaku danmaku) {
        if (DEBUG) {
            Log.d(TAG, "removeDamaku");
        }
        if (danmaku == null) {
            return;
        }
        mGLViewGroup.removeView(danmaku);
        postRun();
    }

    public void removeAllDanmaku() {
        if (DEBUG) {
            Log.d(TAG, "removeAllDanmaku");
        }
        mGLViewGroup.removeAll();
        postRun();
    }

    public void setAlpha(float alpha) {
        mGLViewGroup.setAlpha(alpha);
    }

    public float getAlpha() {
        return mGLViewGroup.getAlpha();
    }

    protected boolean postRun() {
        //在弹幕密度比较高的情况下，由于queueEvent方法需要进行同步，该处锁机制同步消耗较大，所以暂时去除该辅助任务
        //具体代价可以在高密度弹幕情况下对mGLSurfaceView.queueEvent(this);进行耗时检测
//        if (mPostRunning.compareAndSet(false, true)) {
//            mGLSurfaceView.queueEvent(this);
//            if (DEBUG) {
//                Log.d(TAG, "postRun succeed");
//            }
//            return true;
//        }
        return false;
    }

    /**
     * gl线程
     */
    @Override
    public void run() {
        if (DEBUG) {
            Log.d(TAG, "postRun start");
        }
        int size = 0;
        while (mHasGLContext &&
                !mPrepareRender &&
                (mGLViewGroup.initFirst() ||
                        mGLViewGroup.releaseFirst() ||
                        mPostRunning.compareAndSet(true, false))
                ) {
            size++;
        }
        //此处忽略线程安全（辅助任务)
        mPostRunning.set(false);
        if (DEBUG) {
            Log.d(TAG, "postRun end ,run task size " + size);
        }
    }
}