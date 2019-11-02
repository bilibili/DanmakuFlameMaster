package master.flame.danmaku.gl.glview.view;

import android.opengl.Matrix;

public class GLViewGroupMatrixProvider {
    /**
     * 透视矩阵中的视野角度
     */
    private static float DEFAULT_VIEW_FOVY = 45;
    private static float DEFAULT_NEAR_DISTANCE = 0.1f;
    private static float DEFAULT_FAR_DISTANCE = (float) (360f / Math.tan(Math.PI * DEFAULT_VIEW_FOVY / 360));


    private int mDisplayWidth, mDisplayHeight;
    //镜像操作
    private boolean mReverseHorizontal, mReverseVertical;

    /**
     * 观察矩阵
     */
    private float[] mViewMatrix = new float[16];
    /**
     * 投影矩阵
     */
    private float[] mProjectMatrix = new float[16];
    /**
     * 透视投影的参数
     */
    private float mViewFOVY = DEFAULT_VIEW_FOVY;
    private float mViewDistance = DEFAULT_FAR_DISTANCE;
    private float mViewNearDistance = DEFAULT_NEAR_DISTANCE;


    public GLViewGroupMatrixProvider() {
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mProjectMatrix, 0);
    }


    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        mViewDistance = (float) mDisplayHeight / (2f * (float) Math.tan(Math.PI * mViewFOVY / 360));
        mViewMatrix = resolveViewMatrix();
        mProjectMatrix = resolveProjectMatrix();
    }

    private float[] resolveViewMatrix() {
        //设置相机位置,放在中间
        // TODO: 2018/8/10 翻转操作
        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, mViewDistance, 0, 0, 0f, 0f, 1.0f, 0.0f);
        return viewMatrix;
    }

    private float[] resolveProjectMatrix() {
        //投影矩阵
        float[] projectMatrix = new float[16];
        Matrix.setIdentityM(projectMatrix, 0);
        Matrix.perspectiveM(projectMatrix, 0, mViewFOVY, (float) mDisplayWidth / (float) mDisplayHeight, mViewNearDistance, mViewDistance * 1.01f);
        return projectMatrix;
    }

    public float[] getViewMatrix() {
        return mViewMatrix;
    }

    public float[] getProjectMatrix() {
        return mProjectMatrix;
    }

    public void setViewsReverseState(boolean reverseHorizontal, boolean reverseVertical) {
        if (reverseHorizontal == mReverseHorizontal && reverseVertical == mReverseVertical) {
            return;
        }
        mReverseHorizontal = reverseHorizontal;
        mReverseVertical = reverseVertical;
        mViewMatrix = resolveViewMatrix();
    }

    public boolean isReverseHorizontal() {
        return mReverseHorizontal;
    }

    public boolean isReverseVertical() {
        return mReverseVertical;
    }
}
