package master.flame.danmaku.gl.glview.view;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import master.flame.danmaku.gl.Constants;
import master.flame.danmaku.gl.glview.GLUtils;
import master.flame.danmaku.gl.glview.MatrixInfo;


public abstract class GLTextureImgProvider<D> {
    private static final String TAG = "GLTextureImgProvider";
    private static final boolean DEBUG_CREATE_TEXTURE = Constants.DEBUG_GLVIEW_CREATE_TEXTURE;
    private static final boolean DEBUG_RELEASE_TEXTURE = Constants.DEBUG_GLVIEW_RELEASE_TEXTURE;
    protected GLView mGlView;
    private D mData;
    /**
     * 是否获取到纹理图片，该值可以控制是否需要刷新content
     */
    protected boolean mHaveTextureImgFetched = false;
    private AtomicBoolean mImgFetching = new AtomicBoolean(false);

    /**
     * 是否自动更新view的位置、大小和转转变化，也就对应着opengl中模型矩阵
     */
    private boolean mAutoFreshMatrix = true;

    /**
     * 自动更新view的位置、大小和转转变化的频率，只有{@link #mAutoFreshMatrix}为true才有效<br/>
     * 如果模型矩阵计算比较复杂，则可能会影响帧率，这个参数控制刷新模型矩阵的频率<br/>
     * 自动刷新模型矩阵时两次刷新之间的间隔
     */
    private long mFreshMatrixGapTime = 0;
    /**
     * 当前状态是否需要刷新模型矩阵，只有{@link #mAutoFreshMatrix}为true才有效
     */
    private boolean mNeedFreshMatrix = true;
    /**
     * 上次刷新模型矩阵的时间
     */
    private long mLashFreshMatrixTime;

    /**
     * 获取到纹理数据后该值存在，映射到纹理后该值为null
     * 如果该值不为null，则需要加到到纹理
     */
    private Bitmap mViewBitmap;
    /**
     * 加载到纹理后的纹理id，没有则为0
     */
    private int mViewTextureId;

    //空间参数
    private float mTextureWidth, mTextureHeight;
    protected float mDisplayWidth = -1, mDisplayHeight = -1;
    /**
     * view的矩阵信息
     */
    private MatrixInfo mMatrixInfo = new MatrixInfo();
    /**
     * 模型矩阵
     */
    private float[] mModelMatrix = new float[16];

    public GLTextureImgProvider() {
        this(null);
    }

    public GLTextureImgProvider(D data) {
        setData(data);
        Matrix.setIdentityM(mModelMatrix, 0);
    }

    void onDisplaySizeChanged(int displayWidth, int displayHeight) {
        if (displayWidth == mDisplayWidth && displayHeight == mDisplayHeight) {
            return;
        }
        mDisplayWidth = displayWidth;
        mDisplayHeight = displayHeight;
        //显示的区域大小变化了，需要重新构建模型矩阵
        mModelMatrix = resolveModelMatrix(getCachedMatrixInfo());
    }

    public void onGLDrawFrame() {
        if (isTextureOutOfData()) {
            freshTextureContent();
        }
    }

    protected float[] resolveModelMatrix(MatrixInfo matrixInfo) {
        if (matrixInfo == null) {
            return mModelMatrix;
        }
        //此时正在坐标原点
        //缩放
        float[] scaleM = new float[16];
        Matrix.setIdentityM(scaleM, 0);
        Matrix.scaleM(scaleM, 0, matrixInfo.mScaleX * mTextureWidth, matrixInfo.mScaleY * mTextureHeight, 1);

        //旋转+镜像旋转
        float[] rotateM = new float[16];
        Matrix.setIdentityM(rotateM, 0);
        Matrix.rotateM(rotateM, 0, matrixInfo.mRotateX, 1, 0, 0);
        Matrix.rotateM(rotateM, 0, matrixInfo.mRotateY, 0, 1, 0);
        Matrix.rotateM(rotateM, 0, matrixInfo.mRotateZ, 0, 0, 1);

        //位移+镜像位移
        float[] transT = new float[16];
        Matrix.setIdentityM(transT, 0);
        float transX = mTextureWidth / 2 + matrixInfo.mTransX - mDisplayWidth / 2;
        float transY = -mTextureHeight / 2 - matrixInfo.mTransY + mDisplayHeight / 2;
        Matrix.translateM(transT, 0, transX, transY, matrixInfo.mTransZ);

        //混合变换
        float[] resultM = new float[16];
        Matrix.setIdentityM(resultM, 0);
        Matrix.multiplyMM(resultM, 0, scaleM, 0, resultM, 0);
        Matrix.multiplyMM(resultM, 0, rotateM, 0, resultM, 0);
        Matrix.multiplyMM(resultM, 0, transT, 0, resultM, 0);
        return resultM;
    }

    public final MatrixInfo getCachedMatrixInfo() {
        return mMatrixInfo;
    }

    public float[] getModelMatrix() {
        long currentTimeMillis = SystemClock.uptimeMillis();
        if (mNeedFreshMatrix || (isAutoFreshMatrix() && currentTimeMillis - mLashFreshMatrixTime >= mFreshMatrixGapTime)) {
            mMatrixInfo = getMatrixInfo();
            mNeedFreshMatrix = false;
            mLashFreshMatrixTime = currentTimeMillis;
            mModelMatrix = resolveModelMatrix(mMatrixInfo);
        }
        return mModelMatrix;
    }

    public boolean isAutoFreshMatrix() {
        return mAutoFreshMatrix;
    }

    public void setIsAutoFreshMatrix(boolean autoFresh) {
        mAutoFreshMatrix = autoFresh;
    }

    /**
     * 设置模型矩阵刷新的频率
     * <=0表示没错都刷新
     * 默认为0
     *
     * @param fps 刷新的帧率
     */
    public void setFreshMatrixhFps(int fps) {
        if (fps <= 0) {
            mFreshMatrixGapTime = 0;
        } else {
            mFreshMatrixGapTime = 1000 / fps;
        }
    }

    public void setGLView(GLView glView) {
        this.mGlView = glView;
    }

    public void setData(D data) {
        this.mData = data;
    }

    public D getData() {
        return this.mData;
    }

    public void freshTextureContent() {
        if (mImgFetching.compareAndSet(false, true)) {
            try {
                Bitmap textureBitmap = getTextureBitmap();
                if (textureBitmap == null || textureBitmap.isRecycled()) {
                    return;
                }
                //纹理显示的大小就是位图的大小
                // TODO: 2018/8/11 纹理压缩提高性能
                mTextureWidth = textureBitmap.getWidth();
                mTextureHeight = textureBitmap.getHeight();
                mViewBitmap = textureBitmap;
                mViewTextureId = createTexture();
                mNeedFreshMatrix = true;
                mHaveTextureImgFetched = true;
            } finally {
                mImgFetching.set(false);
            }
        }
    }

    public int getTextureId() {
        return mViewTextureId;
    }

    protected int createTexture() {
        int textureId = 0;
        final Bitmap bitmap = mViewBitmap;
        if (bitmap != null && !bitmap.isRecycled()) {
            //删除之前的texture
            onDestroy();
            long start = System.nanoTime();
            //位图有效
            mViewBitmap = null;
            try {
                textureId = GLUtils.createBitmapTexture2D(bitmap);
            } catch (Exception ignore) {
                return 0;
            }
            if (DEBUG_CREATE_TEXTURE) {
                Log.i(TAG, "createViewTexture texid=" + mViewTextureId + "\t time=" + (System.nanoTime() - start));
            }
        }
        return textureId;
    }

    protected void destroyTexture() {
        if (mViewTextureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{mViewTextureId}, 0);
            if (DEBUG_RELEASE_TEXTURE) {
                Log.i(TAG, "destroyTexture texid=" + mViewTextureId);
            }
            mViewTextureId = 0;
        }
    }

    public void onDestroy() {
        destroyTexture();
    }

    public float getViewElevation() {
        return getMatrixInfo().mTransZ;
    }

    public boolean isVisibable() {
        return true;
    }

    public boolean isRecyclered() {
        return false;
    }

    protected boolean isTextureOutOfData() {
        return !mHaveTextureImgFetched;
    }

    protected void onGLContextLost() {
        mHaveTextureImgFetched = false;
    }

    public float getTextureWidth() {
        return mTextureWidth;
    }

    public float getTextureHeight() {
        return mTextureHeight;
    }

    protected abstract Bitmap getTextureBitmap();

    protected abstract MatrixInfo getMatrixInfo();

}
