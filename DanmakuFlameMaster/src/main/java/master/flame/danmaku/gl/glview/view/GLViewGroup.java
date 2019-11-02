package master.flame.danmaku.gl.glview.view;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.gl.Constants;
import master.flame.danmaku.gl.glview.GLProgram;
import master.flame.danmaku.gl.glview.view.provider.GLDanmakuProvider;
import master.flame.danmaku.gl.utils.SpeedsMeasurement;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;

public class GLViewGroup {
    private static final String TAG = "GLViewGroup";
    private static final boolean DEBUG = Constants.DEBUG_GLVIEWGROUP;
    private static final boolean DEBUG_DRAW = Constants.DEBUG_GLVIEWGROUP_DRAW;
    private static final boolean DEBUG_INIT = Constants.DEBUG_GLVIEWGROUP_INIT;
    private static final boolean DEBUG_RELEASE = Constants.DEBUG_GLVIEWGROUP_RELEASE;
    private static final boolean DEBUG_ADD = Constants.DEBUG_GLVIEWGROUP_ADD;
    private SpeedsMeasurement mCreateSpeeds = new SpeedsMeasurement("mCreateSpeeds");
    private SpeedsMeasurement mDrawSpeeds = new SpeedsMeasurement("mDrawSpeeds");
    private SpeedsMeasurement mReleaseSpeeds = new SpeedsMeasurement("mReleaseSpeeds");

    //纹理映射
    private static final float[] sPosition = {
            -0.5f, 0.5f,
            -0.5f, -0.5f,
            0.5f, 0.5f,
            0.5f, -0.5f
    };

    private static final float[] sCoordinate = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private static final FloatBuffer sPositionBuffer;
    private static final FloatBuffer sCoordBuffer;

    static {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(sPosition.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        sPositionBuffer = byteBuffer.asFloatBuffer();
        sPositionBuffer.put(sPosition);
        sPositionBuffer.position(0);

        byteBuffer = ByteBuffer.allocateDirect(sCoordinate.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        sCoordBuffer = byteBuffer.asFloatBuffer();
        sCoordBuffer.put(sCoordinate);
        sCoordBuffer.position(0);
    }

    /**
     * 观察矩阵和投影矩阵等整体变换
     */
    private GLViewGroupMatrixProvider mMatrixProvider;
    /**
     * glsl管理
     */
    private GLProgram mGlProgram;

    private float mAlpha = 1f;
    private int mDisplayWidth, mDisplayHeight;
    /**
     * 如果不开启深度测试，则需要通过弹幕排序来实现层级关系
     */
    private Comparator<GLView> zComparator = new Comparator<GLView>() {
        @Override
        public int compare(GLView o1, GLView o2) {
            if (o1.getImgProvider().getData() == o2.getImgProvider().getData()) {
                //同一个弹幕,此代码快不应该出现，否则逻辑可能有问题Override
                Log.w(TAG, "zComparator compare the same damaku override");
                return 0;
            }
            return o1.getViewElevation() >= o2.getViewElevation() ? 1 : -1;
        }
    };

    /**
     * BaseDanmaku  add->mNewDanmaku  initFirst->  mRunningViews  timeout->mRemovingViews  releaseFirst->mRemovedViews
     * 刚添加的弹幕集合，集合中的弹幕经过变换成gl中的纹理后会放入到mRunningViews集合中显示
     * 此处特意没有使用{@link master.flame.danmaku.danmaku.model.Danmaku}管理
     */
    private final Queue<BaseDanmaku> mNewDanmaku = new ConcurrentLinkedQueue<>();
    /**
     * 需要渲染的弹幕库，按照zComparator的排序绘制
     */
    private final Collection<GLView> mRunningViews = new TreeSet<>(zComparator);
    /**
     * 已经被删除了的弹幕库，绝大部分因为弹幕库超过了屏幕区域
     * 在此集合中的弹幕库的纹理还没有被销毁，需要在随后被销毁调，否则有内存溢出的可能
     */
    private final Queue<GLView> mRemovingViews = new ConcurrentLinkedQueue<>();
    /**
     * 已经被删除了的弹幕库，mRemovingViews中的弹幕库在纹理被销毁后放入次集合，后面再重复利用放入到mRunningViews集合中显示
     */
    private final Queue<GLView> mRemovedViews = new ConcurrentLinkedQueue<>();

    public GLViewGroup(Context context) {
        mGlProgram = new GLProgram(context);
        mMatrixProvider = new GLViewGroupMatrixProvider();
    }

    /**
     * gl线程
     * Called when the surface is created or recreated.
     * Called when the rendering thread starts and whenever the EGL context is lost. The EGL context will typically be lost when the Android device awakes after going to sleep.
     */
    public void onGLSurfaceViewCreate() {
        if (DEBUG) {
            Log.i(TAG, "onGLSurfaceViewCreate");
        }
        //opengl  create 或者 recreate 时 glcontext 很容易丢失，
        //重新加载一次glsl
        mGlProgram.load();
        for (GLView glView : mRunningViews) {
            glView.onGLCreate();
        }
    }

    /**
     * gl线程
     */
    public void onDisplaySizeChanged(int width, int height) {
        if (DEBUG) {
            Log.i(TAG, "onDisplaySizeChanged width=" + width + "\t height=" + height);
        }
        GLES20.glViewport(0, 0, width, height);
        mDisplayWidth = width;
        mDisplayHeight = height;
        mMatrixProvider.onDisplaySizeChanged(width, height);
        for (GLView glView : mRunningViews) {
            glView.onDisplaySizeChanged(width, height);
        }
    }

    /**
     * gl线程
     */
    public void onGLDrawFrame() {
        if (DEBUG_DRAW) {
            //新增
            mCreateSpeeds.taskStart();
            int addSize = initNew();
            mCreateSpeeds.taskEnd();
            //绘制
            mDrawSpeeds.taskStart();
            int drawSize = drawRunning();
            mDrawSpeeds.taskEnd();
            //移除
            mReleaseSpeeds.taskStart();
            int removeSize = releaseRemoved();
            mReleaseSpeeds.taskEnd();

            Log.i(TAG, "onGLDrawFrame  " +
                    "new=" + mNewDanmaku.size() + "  " +
                    "run=" + mRunningViews.size() + "  " +
                    "rmv=" + mRemovingViews.size() + "  " +
                    "as=" + addSize + "  " +
                    "ds=" + drawSize + "  " +
                    "rs=" + removeSize + "  ");
        } else {
            initNew();
            drawRunning();
            releaseRemoved();
        }
    }

    /**
     * gl线程
     *
     * @return 添加的个数
     */
    private int initNew() {
        int addSize = 0;
        while (initFirst()) {
            addSize++;
        }
        return addSize;
    }

    /**
     * gl线程
     *
     * @return 是否成功
     */
    public boolean initFirst() {
        int loopTime = 0;
        boolean reuseGlView = true;
        BaseDanmaku danmaku;
        while ((danmaku = mNewDanmaku.poll()) != null && danmaku.isTimeOut()) {
            loopTime++;
        }
        if (danmaku == null) {
            return false;
        }
        GLView recyclerView = mRemovedViews.poll();
        if (recyclerView == null) {
            reuseGlView = false;
            recyclerView = new GLView(this);
        }
        recyclerView.setRecyclered(false);
        GLTextureImgProvider imgProvider = recyclerView.getImgProvider();
        if (imgProvider == null || !(imgProvider instanceof GLDanmakuProvider)) {
            imgProvider = new GLDanmakuProvider();
        }
        imgProvider.setData(danmaku);
        imgProvider.setGLView(recyclerView);
        recyclerView.setImgProvider(imgProvider);

        //重新走生命周期
        recyclerView.onGLCreate();
        recyclerView.onDisplaySizeChanged(mDisplayWidth, mDisplayHeight);
        //添加到需要渲染的弹幕的集合中，等待下一次渲染
        mRunningViews.add(recyclerView);

        if (DEBUG_INIT) {
            Log.i(TAG, "init texture id=" + danmaku.id + "\tloopTime=" + loopTime + "\t reuseGlView=" + reuseGlView);
        }
        return true;
    }

    /**
     * gl线程
     *
     * @return 绘制的个数
     */
    private int drawRunning() {
        if (mRunningViews.isEmpty()) {
            return 0;
        }
        GLES20.glEnable(GL_BLEND);
        GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GL_CULL_FACE);
        int drawSize = 0;
        Iterator<GLView> iterator = mRunningViews.iterator();
        mGlProgram.use();
        boolean isFirst = true;
        while (iterator.hasNext()) {
            GLView next = iterator.next();
            if (next.isRecyclered()) {
                //已经标记为移除
                mRemovingViews.add(next);
                iterator.remove();
                continue;
            }
            if (!next.isVisibale()) {
                continue;
            }
            if (isFirst) {
                isFirst = false;
                GLES20.glUniformMatrix4fv(mGlProgram.glHProject, 1, false, mMatrixProvider.getProjectMatrix(), 0);
                GLES20.glUniformMatrix4fv(mGlProgram.glHView, 1, false, mMatrixProvider.getViewMatrix(), 0);
                GLES20.glUniform1f(mGlProgram.glAlpha, mAlpha);
                GLES20.glEnableVertexAttribArray(mGlProgram.glHPosition);
                GLES20.glEnableVertexAttribArray(mGlProgram.glHCoordinate);
                GLES20.glVertexAttribPointer(mGlProgram.glHPosition, 2, GLES20.GL_FLOAT, false, 0, sPositionBuffer);
                GLES20.glVertexAttribPointer(mGlProgram.glHCoordinate, 2, GLES20.GL_FLOAT, false, 0, sCoordBuffer);
                GLES20.glActiveTexture(GL_TEXTURE0);
                GLES20.glUniform1i(mGlProgram.glHTexture, 0);
            }
            next.onDrawFrame(mGlProgram.glHModel);
            drawSize++;
        }
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        GLES20.glDisable(GL_BLEND);
        GLES20.glDisable(GL_CULL_FACE);
        mGlProgram.unuse();
        return drawSize;
    }

    /**
     * gl线程
     *
     * @return 释放的个数
     */
    private int releaseRemoved() {
        int removeSize = 0;
        while (releaseFirst()) {
            removeSize++;
        }
        return removeSize;
    }

    /**
     * gl线程
     *
     * @return 成功移除了第一个
     */
    public boolean releaseFirst() {
        GLView first = mRemovingViews.poll();
        if (first == null) {
            return false;
        }
        first.onDestroy();
        if (DEBUG_RELEASE) {
            BaseDanmaku data = (BaseDanmaku) first.getImgProvider().getData();
            Log.i(TAG, "release id=" + data.id);
        }
        mRemovedViews.offer(first);
        return true;
    }

    public boolean isEmpty() {
        return mRunningViews.isEmpty() && mRemovingViews.isEmpty() && mNewDanmaku.isEmpty();
    }

    public void addDanmu(BaseDanmaku danmaku) {
        if (danmaku == null) {
            return;
        }
        if (DEBUG_ADD) {
            Log.i(TAG, "addDanmu id=" + danmaku.id);
        }
        mNewDanmaku.offer(danmaku);
    }

    public void removeView(Object view) {
        if (view == null) {
            return;
        }

        if (view instanceof GLView) {
            //gl线程
            if (DEBUG) {
                BaseDanmaku data = (BaseDanmaku) ((GLView) view).getImgProvider().getData();
                Log.i(TAG, "removeView id=" + data.id);
            }
            //设置回收状态，在下一次绘制时会被移除
            ((GLView) view).setRecyclered(true);
        }
        if (view instanceof BaseDanmaku) {
            if (DEBUG) {
                Log.i(TAG, "removeView id=" + ((BaseDanmaku) view).id);
            }
            if (!mNewDanmaku.remove(view)) {
                for (GLView glView : new ArrayList<>(mRunningViews)) {
                    if (glView.getImgProvider().getData() == view) {
                        glView.setRecyclered(true);
                        return;
                    }
                }
            }
        }
    }

    public void removeAll() {
        if (DEBUG) {
            Log.i(TAG, "removeAll");
        }
        mNewDanmaku.clear();
        for (GLView glView : new ArrayList<>(mRunningViews)) {
            glView.setRecyclered(true);
        }
    }

    public void freshView(Object view) {
        if (view == null) {
            return;
        }
        if (view instanceof GLView) {
            //gl线程
            ((GLView) view).freshView();
            return;
        }

        for (GLView next : new ArrayList<>(mRunningViews)) {
            if (next.getImgProvider().getData() == view) {
                next.freshView();
                return;
            }
        }
    }

    public void setViewsReverseState(boolean reverseHorizontal, boolean reverseVertical) {
        mMatrixProvider.setViewsReverseState(reverseHorizontal, reverseVertical);
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public float getDisplayWidth() {
        return mDisplayWidth;
    }

    public float getDisplayHeight() {
        return mDisplayHeight;
    }
}
