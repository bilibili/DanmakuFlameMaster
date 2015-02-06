/*
 * @author: zheng qian <xqq@0ginr.com>
 */
package master.flame.danmaku.ui.SkiaRedirector;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

public class SkStupidView extends GLSurfaceView {

    public static final String TAG = "SkStupidView";
    private final SkStupidRenderer mSkiaRenderer;
    private int mRequestedMSAASampleCount;
    private Callback mCallback;
    
    static {
        try {
            System.loadLibrary("DFMACC");
        } catch (UnsatisfiedLinkError e) {
            throw e;
        }
    }
    
    public SkStupidView(Context context, int msaaSampleCount) {
        super(context);
        
        mSkiaRenderer = new SkStupidRenderer(this);
        mRequestedMSAASampleCount = msaaSampleCount;
        
        setEGLContextClientVersion(2);
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setEGLConfigChooser(8, 8, 8, 8, 0, 8);
        //} else {
        //    setEGLConfigChooser(new SkStupidViewEGLConfigChooser());
        //}
        
        setRenderer(mSkiaRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Implement this to do your drawing.
     * You cannot directly call this method or it may cause undefined exception.
     * @param canvas the canvas to be drawn with HW-accelerated Skia backend.
     */
    protected void onSkiaDraw(Canvas canvas) {
    }
    
    @Override
    public boolean isHardwareAccelerated() {
        return true;
    }
    
    public void terminate() {
        queueEvent(new Runnable() {            
            @Override
            public void run() {
                mSkiaRenderer.terminate();
            }
        });
    }
    
    public interface Callback {
        // Will be called immediately after Skia backend created.
        public void onBackendCreated();
        // Will be called immediately after rebuilt render-target for new size.
        public void onBackendChanged(int width, int height);
        // Will be called immediately after Skia backend teardowned.
        public void onBackendDestroyed();
    }
    
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    protected void backendCreated() {
        if (mCallback != null) {
            mCallback.onBackendCreated();
        }
    }
    
    protected void backendChanged(int width, int height) {
        if (mCallback != null) {
            mCallback.onBackendChanged(width, height);
        }
    }
    
    protected void backendDestroyed() {
        if (mCallback != null) {
            mCallback.onBackendDestroyed();
        }
    }
    
    private class SkStupidViewEGLConfigChooser implements GLSurfaceView.EGLConfigChooser {
        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int numConfigs = 0;
            int[] configSpec = null;
            int[] value = new int[1];
            
            int[] validAPIs = new int[] {
                EGL14.EGL_OPENGL_API,
                EGL14.EGL_OPENGL_ES_API
            };
            int initialAPI = 1;
            
            for (int i = initialAPI; i < validAPIs.length && numConfigs == 0; i++) {
                int currentAPI = validAPIs[i];
                EGL14.eglBindAPI(currentAPI);

                // setup the renderableType which will only be included in the
                // spec if we are attempting to get access to the OpenGL APIs.
                int renderableType = EGL14.EGL_OPENGL_BIT;
                if (currentAPI == EGL14.EGL_OPENGL_API) {
                    renderableType = EGL14.EGL_OPENGL_ES2_BIT;
                }

                if (mRequestedMSAASampleCount > 0) {
                    configSpec = new int[] {
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 0,
                        EGL10.EGL_STENCIL_SIZE, 8,
                        EGL10.EGL_SAMPLE_BUFFERS, 1,
                        EGL10.EGL_SAMPLES, mRequestedMSAASampleCount,
                        EGL10.EGL_NONE, renderableType,
                        EGL10.EGL_NONE
                    };

                    Log.i("Skia", "spec: " + configSpec);
                    
                    if (!egl.eglChooseConfig(display, configSpec, null, 0, value)) {
                        Log.i("Skia", "Could not get MSAA context count: " + mRequestedMSAASampleCount);
                    }

                    numConfigs = value[0];
                }

                if (numConfigs <= 0) {
                    // Try without multisampling.
                    configSpec = new int[] {
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 0,
                        EGL10.EGL_STENCIL_SIZE, 8,
                        EGL10.EGL_NONE, renderableType,
                        EGL10.EGL_NONE
                    };
                    
                    Log.i("Skia", "spec: " + configSpec);

                    if (!egl.eglChooseConfig(display, configSpec, null, 0, value)) {
                        Log.i("Skia", "Could not get non-MSAA context count");
                    }
                    numConfigs = value[0];
                }
            }

            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            // Get all matching configurations.
            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, value)) {
                throw new IllegalArgumentException("Could not get config data");
            }

            for (int i = 0; i < configs.length; ++i) {
                EGLConfig config = configs[i];
                if (findConfigAttrib(egl, display, config , EGL10.EGL_RED_SIZE, 0) == 8 &&
                        findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0) == 8 &&
                        findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0) == 8 &&
                        findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0) == 8 &&
                        findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0) == 8) {
                    return config;
                }
            }

            throw new IllegalArgumentException("Could not find suitable EGL config");
        }
        
        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
            int[] value = new int[1];
            if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                return value[0];
            }
            return defaultValue;
        }
        
    }
    
}
