package master.flame.danmaku.gl.glview.controller;

import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

import master.flame.danmaku.gl.wedget.GLHandlerSurfaceView;
import master.flame.danmaku.gl.wedget.GLShareable;


public class TextureGLSurfaceViewRenderer implements GLHandlerSurfaceView.Renderer, GLShareable {
    private static final String TAG = "SurfaceViewRenderer";
    private GLHandlerSurfaceView mGLSurfaceView;
    private GLDanmakuHandler mGLDanmakuHandler;
    private boolean mCreated = false;
    private boolean mPaused = true;
    private boolean mDrawFinished = false;
    private boolean mHide = true;
    private boolean mClearFlag = false;

    public TextureGLSurfaceViewRenderer(GLHandlerSurfaceView surfaceView) {
        mGLSurfaceView = surfaceView;
        mGLDanmakuHandler = new GLDanmakuHandler(surfaceView);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCreated = true;
        mGLDanmakuHandler.onSurfaceCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mGLDanmakuHandler.onSurfaceSizeChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (!mPaused && !mHide && !mClearFlag) {
            mGLDanmakuHandler.onGLDrawFrame();
        }
        mClearFlag = false;
        synchronized (this) {
            mDrawFinished = true;
            notifyAll();
        }
    }

    public void onResume() {
        mPaused = false;
        mGLDanmakuHandler.onResume();
    }

    public void onPause() {
        mPaused = true;
        mGLDanmakuHandler.onPause();
    }

    public void hide() {
        mHide = true;
        if (mCreated) {
            mGLSurfaceView.requestRender();
        }
    }

    public void show() {
        mHide = false;
        if (mCreated) {
            mGLSurfaceView.requestRender();
        }
    }

    public void clearNextFrame() {
        mClearFlag = true;
        if (mCreated) {
            mGLSurfaceView.requestRender();
        }
    }

    public void requestRender() {
        if (mCreated) {
            mGLDanmakuHandler.prepareRender();
            mGLSurfaceView.requestRender();
        }
    }

    public void requestRenderSync() {
        if (mCreated) {
            mGLDanmakuHandler.prepareRender();
            mGLSurfaceView.requestRender();
            synchronized (this) {
                while (!mDrawFinished && !mPaused) {
                    try {
                        wait(200);
                    } catch (InterruptedException ignore) {
                    }
                }
                mDrawFinished = false;
            }
        }
    }

    public GLDanmakuHandler getGLDanmakuHandler() {
        return mGLDanmakuHandler;
    }

    @Override
    public EGLContext getSharedContext() {
        return mGLSurfaceView.getSharedContext();
    }

    @Override
    public EGLConfig getSharedConfig() {
        return mGLSurfaceView.getSharedConfig();
    }

    @Override
    public int getEGLContextClientVersion() {
        return mGLSurfaceView.getEGLContextClientVersion();
    }

    @Override
    public int getSurfaceWidth() {
        return mGLSurfaceView.getSurfaceWidth();
    }

    @Override
    public int getSurfaceHeight() {
        return mGLSurfaceView.getSurfaceWidth();
    }
}
