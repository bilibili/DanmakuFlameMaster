package master.flame.danmaku.gl.glview.view.provider;

import android.graphics.Bitmap;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.gl.glview.MatrixInfo;
import master.flame.danmaku.gl.glview.view.GLTextureImgProvider;
import master.flame.danmaku.gl.glview.view.GLView;

/**
 * 创建人:yangzhiqian
 * 创建时间:2018/7/11 15:57
 * 备注:
 */
public class GLDanmakuProvider extends GLTextureImgProvider<BaseDanmaku> {

    @Override
    public void setGLView(GLView glView) {
        super.setGLView(glView);
    }

    @Override
    protected Bitmap getTextureBitmap() {
        //不需要上层创建纹理
        return null;
    }

    @Override
    protected float[] resolveModelMatrix(MatrixInfo matrixInfo) {
        // TODO: 2018/8/11 rotate
        float sx = matrixInfo.mScaleX * getTextureWidth();
        float sy = matrixInfo.mScaleY * getTextureHeight();
        float sz = 1;
        float tx = getTextureWidth() / 2 + matrixInfo.mTransX - mDisplayWidth / 2;
        float ty = -getTextureHeight() / 2 - matrixInfo.mTransY + mDisplayHeight / 2;
        float tz = matrixInfo.mTransZ;
        float[] resultM = new float[16];
        /**
         *              平移矩阵                            缩放矩阵
         *       1     0     0    tx                   sx    0    0    0
         *       0     1     0    ty                   0     sy   0    0
         *       0     0     1    tz                   0     0    sz   0
         *       0     0     0    1                    0     0    0    1
         *
         *
         *        缩放+平移
         *         sx   0   0   tx
         *         0    sy  0   ty
         *         0    0   sz  tz
         *         0    0   0   1
         */
        resultM[0] = sx;
        resultM[1] = 0;
        resultM[2] = 0;
        resultM[3] = 0;

        resultM[4] = 0;
        resultM[5] = sy;
        resultM[6] = 0;
        resultM[7] = 0;

        resultM[8] = 0;
        resultM[9] = 0;
        resultM[10] = sz;
        resultM[11] = 0;

        resultM[12] = tx;
        resultM[13] = ty;
        resultM[14] = tz;
        resultM[15] = 1;

        return resultM;

    }

    @Override
    protected MatrixInfo getMatrixInfo() {
        MatrixInfo matrixInfo = new MatrixInfo();
        BaseDanmaku data = getData();
        matrixInfo.mTransX = data.getLeft();
        matrixInfo.mTransY = data.getTop();
        return matrixInfo;
    }

    @Override
    public boolean isVisibable() {
        return getData() != null && getData().isShown();
    }

    @Override
    public boolean isRecyclered() {
        return getData() == null || getData().isTimeOut();
    }

    @Override
    public int getTextureId() {
        BaseDanmaku data = getData();
        if (data == null || data.mGLTextureId == 0) {
            return super.getTextureId();
        }
        return data.mGLTextureId;
    }

    @Override
    public float getTextureWidth() {
        BaseDanmaku data = getData();
        return data == null ? 0 : data.mTextureWidth;
    }

    @Override
    public float getTextureHeight() {
        BaseDanmaku data = getData();
        return data == null ? 0 : data.mTextureHeight;
    }

    @Override
    protected int createTexture() {
        return 0;
    }

    @Override
    protected void destroyTexture() {
    }

    @Override
    protected boolean isTextureOutOfData() {
        return false;
    }
}
