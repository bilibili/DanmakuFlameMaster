package master.flame.danmaku.gl.wedget;

import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * 创建人:yangzhiqian
 * 创建时间:2018/8/16 15:06
 */
public interface GLShareable {
    EGLContext getSharedContext();

    EGLConfig getSharedConfig();

    int getEGLContextClientVersion();

    int getSurfaceWidth();

    int getSurfaceHeight();

    final class GLShareHelper {
        private EGL10 mEGL10;
        private EGLContext mSharedContext;
        private EGLConfig mSharedConfig;
        private EGLDisplay mCurrentDisplay;
        private EGLContext mCurrentContext;
        private EGLSurface mCurrentSurface;

        private static final ThreadLocal<GLShareHelper> sLocalGLShareHelper = new ThreadLocal<>();

        private GLShareHelper() {
        }

        public static GLShareHelper makeSharedGlContext(final GLShareable shareable) {
            EGL10 egl = (EGL10) EGLContext.getEGL();
            EGLContext currentEglContext = egl.eglGetCurrentContext();
            if (sLocalGLShareHelper.get() != null && currentEglContext != null && currentEglContext != EGL10.EGL_NO_CONTEXT) {
                //已经调用过makeSharedGlContext了
                return sLocalGLShareHelper.get();
            }
            if (currentEglContext != null && currentEglContext != EGL10.EGL_NO_CONTEXT) {
                //当前线程已经存在glcontext
                Log.w("GLShareHelper", "当前线程已经存在gl环境，请不要在此线程再次初始化opengl环境");
                return null;
            }
            if (shareable == null) {
                return null;
            }
            EGLContext sharedContext = shareable.getSharedContext();
            if (sharedContext == null || sharedContext == EGL10.EGL_NO_CONTEXT) {
                return null;
            }
            EGLConfig sharedConfig = shareable.getSharedConfig();
            if (sharedConfig == null) {
                return null;
            }
            int eglContextClientVersion = shareable.getEGLContextClientVersion();
            int[] attrib_list = {0x3098, eglContextClientVersion,
                    EGL10.EGL_NONE};

            final EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (display == EGL10.EGL_NO_DISPLAY) {
                return null;
            }
            currentEglContext = egl.eglCreateContext(display, sharedConfig, sharedContext,
                    eglContextClientVersion != 0 ? attrib_list : null);
            if (currentEglContext == EGL10.EGL_NO_CONTEXT) {
                return null;
            }

            int[] surfaceAttribList = {
                    EGL10.EGL_WIDTH, shareable.getSurfaceWidth(),
                    EGL10.EGL_HEIGHT, shareable.getSurfaceHeight(),
                    EGL10.EGL_NONE
            };
            EGLSurface eglSurface = egl.eglCreatePbufferSurface(display, sharedConfig, surfaceAttribList);
            if (eglSurface == EGL10.EGL_NO_SURFACE) {
                return null;
            }
            if (!egl.eglMakeCurrent(display, eglSurface, eglSurface, currentEglContext)) {
                egl.eglDestroySurface(display, eglSurface);
                return null;
            }
            GLES20.glFlush();
            GLShareHelper shareHelper = new GLShareHelper();
            shareHelper.mEGL10 = egl;
            shareHelper.mSharedContext = sharedContext;
            shareHelper.mSharedConfig = sharedConfig;
            shareHelper.mCurrentContext = currentEglContext;
            shareHelper.mCurrentDisplay = display;
            shareHelper.mCurrentSurface = eglSurface;
            sLocalGLShareHelper.set(shareHelper);
            return shareHelper;
        }

        public static void release() {
            GLShareHelper shareHelper = sLocalGLShareHelper.get();
            if (shareHelper == null) {
                return;
            }
            shareHelper.mEGL10.eglDestroySurface(shareHelper.mCurrentDisplay, shareHelper.mCurrentSurface);
            if (!shareHelper.mEGL10.eglDestroyContext(shareHelper.mCurrentDisplay, shareHelper.mCurrentContext)) {
                Log.e("GLShareHelper", "display:" + shareHelper.mCurrentDisplay + " context: " + shareHelper.mCurrentContext);
            }
            //todo releae mCurrentDisplay
//            shareHelper.mEGL10.eglTerminate(shareHelper.mCurrentDisplay);
            sLocalGLShareHelper.set(null);
        }
    }
}