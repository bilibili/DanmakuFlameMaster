/*
 * @author: zheng qian <xqq@0ginr.com>
 */
package master.flame.danmaku.ui.SkiaRedirector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.graphics.Canvas;
import android.os.Handler;

public class SkStupidRenderer implements SkStupidView.Renderer {
    
    private final SkStupidView mStupidView;
    private Handler mHandler = new Handler();
    private int mMSAASampleCount;
    private long mNativeHandle;
    private Canvas mCanvas = null;

    public SkStupidRenderer(SkStupidView view) {
        mStupidView = view;
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (gl instanceof GL11) {
            int value[] = new int[1];
            ((GL11)gl).glGetIntegerv(GL11.GL_SAMPLES, value, 0);
            if (value[0] == 1) {
                mMSAASampleCount = 0;
            } else {
                mMSAASampleCount = value[0];
            }
        }
        
        int width = mStupidView.getWidth();
        int height = mStupidView.getHeight();
        
        gl.glViewport(0, 0, width, height);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClearStencil(0);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);
        
        mNativeHandle = nativeInit(width, height, mMSAASampleCount);
        mStupidView.backendCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        nativeUpdateSize(mNativeHandle, width, height);
        mCanvas = null;
        mStupidView.backendChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mCanvas == null) {
            mCanvas = nativeLockCanvas(mNativeHandle);
        }
        //gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        mStupidView.onSkiaDraw(mCanvas);
        nativeUnlockCanvasAndPost(mNativeHandle, mCanvas);
    }

    protected void terminate() {
        nativeTerm(mNativeHandle);
        mStupidView.backendDestroyed();
    }
    
    protected static boolean isDeviceSupported() {
        return nativeIsSupported();
    }
    
    private static native boolean nativeIsSupported();
    private native long nativeInit(int width, int height, int msaaSampleCount);
    private native void nativeTerm(long nativeHandle);
    private native void nativeUpdateSize(long nativeHandle, int width, int height);
    private native Canvas nativeLockCanvas(long nativeHandle);
    private native void nativeUnlockCanvasAndPost(long nativeHandle, Canvas canvas);
}
