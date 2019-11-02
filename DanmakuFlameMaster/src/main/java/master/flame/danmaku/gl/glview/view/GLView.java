package master.flame.danmaku.gl.glview.view;

import android.opengl.GLES20;
import android.util.Log;

import master.flame.danmaku.gl.Constants;

import static android.opengl.GLES20.GL_TEXTURE_2D;

public class GLView {

    private static final String TAG = "GLView";
    private static final boolean DEBUG = Constants.DEBUG_GLVIEW;
    private static final boolean DEBUG_DRAW = Constants.DEBUG_GLVIEW_DRAW;

    public GLViewGroup mGLViewGroup;

    /**
     * 提供模型矩阵和纹理数据
     */
    private GLTextureImgProvider mImgProvider;

    /**
     * 标记当前view是否已经被回收
     */
    private boolean mRecyclered = false;

    public GLView(GLViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("GLViewGroup 不能为空");
        }
        this.mGLViewGroup = viewGroup;
    }

    /**
     * gl线程
     */
    public void onGLCreate() {
        if (DEBUG) {
            Log.i(TAG, "onGLCreate");
        }
        //GLSurfaceView在pause后，大概率会造成GLContext丢失，此时需要重新初始化链接glsl并创建glview纹理
        //先销毁原先所有的gl数据
        onDestroy();
        mImgProvider.onGLContextLost();
    }

    /**
     * 显示器的大小，GLSurfaceView全屏模式下,数值为手机的分辨率
     *
     * @param w 宽度
     * @param h 高度
     */
    public void onDisplaySizeChanged(int w, int h) {
        if (DEBUG) {
            Log.i(TAG, "onDisplaySizeChanged widht=" + w + "\t height=" + h);
        }
        mImgProvider.onDisplaySizeChanged(w, h);
        freshView();
    }

    /**
     * 刷新view的内容，该方法会让view的内容提供者刷新位图<br/>
     * 该方法一般比较耗时<br/>
     */
    public void freshView() {
        mImgProvider.freshTextureContent();
    }

    /**
     * 每次渲染时实时获取view的变化情况<br/>
     * 该方法并不会造成耗时影响<br/>
     *
     * @param auto true表示会正常刷新view的位置和变换，默认为true
     */
    public void freshViewMatrixAuto(boolean auto) {
        mImgProvider.setIsAutoFreshMatrix(auto);
    }

    /**
     * 是否自动刷新view变换
     *
     * @return true表示自动刷新
     * @see #freshViewMatrixAuto(boolean)
     */
    public boolean isFreshViewMatrixAuto() {
        return mImgProvider.isAutoFreshMatrix();
    }

    /**
     * view的海拔，也是view的z轴值
     *
     * @return iew的海拔
     */
    public float getViewElevation() {
        return mImgProvider.getViewElevation();
    }

    /**
     * 获取纹理位图提供者
     *
     * @return GLTextureImgProvider
     */
    public GLTextureImgProvider getImgProvider() {
        return this.mImgProvider;
    }

    public void setImgProvider(GLTextureImgProvider provider) {
        this.mImgProvider = provider;
    }

    /**
     * 绘制view
     * gl线程
     */
    public void onDrawFrame(int glHModel) {
        long start = System.nanoTime();
        boolean succeed = false;
        mImgProvider.onGLDrawFrame();
        int textureId = mImgProvider.getTextureId();
        if (GLES20.glIsTexture(textureId)) {
            succeed = true;
            GLES20.glUniformMatrix4fv(glHModel, 1, false, mImgProvider.getModelMatrix(), 0);
            GLES20.glBindTexture(GL_TEXTURE_2D, textureId);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }
        if (DEBUG_DRAW) {
            Log.i(TAG, "onDrawFrame succeed = " + succeed + "\t time=" + (System.nanoTime() - start));
        }
    }

    public boolean isVisibale() {
        return mImgProvider.isVisibable();
    }

    public void setRecyclered(boolean recyclered) {
        this.mRecyclered = recyclered;
    }

    public boolean isRecyclered() {
        return mRecyclered || mImgProvider.isRecyclered();
    }

    public void onDestroy() {
        mImgProvider.onDestroy();
    }

    public float getViewWidth() {
        return mImgProvider.getTextureWidth();
    }

    public float getViewHeight() {
        return mImgProvider.getTextureHeight();
    }
}
