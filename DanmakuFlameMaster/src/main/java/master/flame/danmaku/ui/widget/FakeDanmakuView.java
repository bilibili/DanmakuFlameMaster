package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

/**
 * Created by ch on 17/8/18.
 */

public class FakeDanmakuView extends DanmakuView implements DrawHandler.Callback {

    public interface OnFrameAvailableListener {
        void onFrameAvailable(long timeMills, Bitmap bitmap);

        void onFramesFinished(long timeMills);

        void onFailed(int errorCode, String msg);
    }

    private OnFrameAvailableListener mOnFrameAvailableListener;
    private int mWidth = 0;
    private int mHeight = 0;
    private float mScale = 1f;
    private DanmakuTimer mOuterTimer;
    private long mBeginTimeMills;
    private long mFrameIntervalMills = 16L;
    private long mEndTimeMills;
    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;

    private int mRetryCount = 0;
    private long mExpectBeginMills = 0;

    public FakeDanmakuView(Context context) {
        super(context);
    }

    public FakeDanmakuView(Context context, int width, int height, float scale) {
        super(context);
        mWidth = width;
        mHeight = height;
        mScale = scale;
        initBufferCanvas(width, height);
    }

    public void initBufferCanvas(int width, int height) {
        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }

    @Override
    public long drawDanmakus() {
        Canvas canvas = mBufferCanvas;
        if (canvas == null) {
            return 0;
        }
        mBufferBitmap.eraseColor(Color.TRANSPARENT);
        if (mClearFlag) {
            DrawHelper.clearCanvas(canvas);
            mClearFlag = false;
        } else {
            if (handler != null) {
                handler.draw(canvas);
            }
        }
        OnFrameAvailableListener mOnFrameAvailableListener = this.mOnFrameAvailableListener;
        if (mOnFrameAvailableListener != null) {
            long curr = mOuterTimer.currMillisecond;
            if (curr >= mExpectBeginMills - mFrameIntervalMills) {
                Bitmap bitmap = Bitmap.createScaledBitmap(mBufferBitmap, (int) (mWidth * mScale), (int) (mHeight * mScale), true);
                mOnFrameAvailableListener.onFrameAvailable(curr, bitmap);
                bitmap.recycle();
            }
            if (curr > mEndTimeMills) {
                mOnFrameAvailableListener.onFramesFinished(curr);
                release();
            }
        }
        mRequestRender = false;
        return 2;  // 固定频率
    }

    @Override
    public void release() {
        mOnFrameAvailableListener = null;
        Canvas canvas = mBufferCanvas;
        mBufferCanvas = null;
        if (canvas != null) {
            canvas.setBitmap(null);
        }
        Bitmap bmp = mBufferBitmap;
        mBufferCanvas = null;
        if (bmp != null) {
            bmp.recycle();
        }
        super.release();
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }

    @Override
    public boolean isShown() {
        return true;
    }

    @Override
    public boolean isViewReady() {
        return true;
    }

    @Override
    public int getViewWidth() {
        return mWidth;
    }

    @Override
    public int getViewHeight() {
        return mHeight;
    }

    @Override
    public void prepare(BaseDanmakuParser parser, DanmakuContext config) {
        config.updateMethod = 1;
        super.prepare(parser, config);
    }

    public void getFrameAtTime(final long beginMills, final long endMills, final int frameRate, final OnFrameAvailableListener onFrameAvailableListener) {
        if (mRetryCount++ > 5) {
            if (onFrameAvailableListener != null) {
                onFrameAvailableListener.onFailed(100, "not prepared");
            }
            release();
            return;
        }
        if (!isPrepared()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getFrameAtTime(beginMills, endMills, frameRate, onFrameAvailableListener);
                }
            }, 1500L);
            return;
        }
        mOnFrameAvailableListener = onFrameAvailableListener;
        mFrameIntervalMills = 1000 / frameRate;
        mExpectBeginMills = beginMills;
        mBeginTimeMills = Math.max(0, beginMills - getConfig().mDanmakuFactory.MAX_Duration_Scroll_Danmaku.value);
        mEndTimeMills = endMills;
        mOuterTimer = new DanmakuTimer(mBeginTimeMills);
        setCallback(this);
        start(mBeginTimeMills);
    }

    @Override
    public void prepared() {

    }

    @Override
    public void updateTimer(DanmakuTimer timer) {
        mOuterTimer.add(mFrameIntervalMills);
        timer.update(mOuterTimer.currMillisecond);
    }

    @Override
    public void danmakuShown(BaseDanmaku danmaku) {

    }

    @Override
    public void drawingFinished() {

    }
}
