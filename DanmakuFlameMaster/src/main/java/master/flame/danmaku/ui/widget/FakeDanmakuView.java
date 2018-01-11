package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.SpecialDanmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

/**
 * Created by ch on 17/8/18.
 */

public class FakeDanmakuView extends DanmakuView implements DrawHandler.Callback {

    private class CustomParser extends BaseDanmakuParser {
        private final BaseDanmakuParser mBaseParser;
        private final long stTime;
        private final long edTime;
        private float mDispScaleX, mDispScaleY;
        private int mViewWidth;

        public CustomParser(BaseDanmakuParser baseParser, long stTime, long edTime) {
            this.mBaseParser = baseParser;
            this.stTime = stTime;
            this.edTime = edTime;
        }

        @Override
        protected IDanmakus parse() {
            final IDanmakus danmakus = new Danmakus();
            IDanmakus subnew;
            try {
                subnew = this.mBaseParser.getDanmakus().subnew(this.stTime, this.edTime);
            } catch (Exception e) {
                subnew = this.mBaseParser.getDanmakus();
            }
            if (subnew == null) {
                return danmakus;
            }
            subnew.forEach(new IDanmakus.Consumer<BaseDanmaku, Object>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    long time = danmaku.getTime();
                    if (time < stTime) {
                        return IDanmakus.Consumer.ACTION_CONTINUE;
                    } else if (time > edTime) {
                        return IDanmakus.Consumer.ACTION_BREAK;
                    }
                    BaseDanmaku item = mContext.mDanmakuFactory.createDanmaku(danmaku.getType(), mContext);
                    if (item != null) {
                        item.setTime(danmaku.getTime());
                        DanmakuUtils.fillText(item, danmaku.text);
                        item.textSize = danmaku.textSize;
                        item.textColor = danmaku.textColor;
                        item.textShadowColor = danmaku.textShadowColor;

                        if (danmaku instanceof SpecialDanmaku) {
                            SpecialDanmaku sdanmaku = (SpecialDanmaku) danmaku;
                            item.index = danmaku.index;
                            item.duration = new Duration(sdanmaku.getDuration());
                            item.rotationZ = sdanmaku.rotateZ;
                            item.rotationY = sdanmaku.rotationY;
                            ((SpecialDanmaku) item).isQuadraticEaseOut = sdanmaku.isQuadraticEaseOut;

                            mContext.mDanmakuFactory.fillTranslationData(item, sdanmaku.beginX,
                                    sdanmaku.beginY, sdanmaku.endX, sdanmaku.endY, sdanmaku.translationDuration, sdanmaku.translationStartDelay, mDispScaleX, mDispScaleY);
                            mContext.mDanmakuFactory.fillAlphaData(item, sdanmaku.beginAlpha, sdanmaku.endAlpha, item.getDuration());

//                            mContext.mDanmakuFactory.fillLinePathData(item, points, mDispScaleX,
//                                    mDispScaleY);  // FIXME
                            return 0;  // FIXME skip special danmakus
                        }

                        item.setTimer(mTimer);
                        item.mFilterParam = danmaku.mFilterParam;
                        item.filterResetFlag = danmaku.filterResetFlag;
                        item.flags = mContext.mGlobalFlagValues;
                        Object lock = danmakus.obtainSynchronizer();
                        synchronized (lock) {
                            danmakus.addItem(item);
                        }
                    }
                    return 0;
                }
            });
            return danmakus;
        }

        @Override
        public BaseDanmakuParser setDisplayer(IDisplayer disp) {
            super.setDisplayer(disp);
            if (mBaseParser == null || mBaseParser.getDisplayer() == null) {
                return this;
            }
            mDispScaleX = mDispWidth / (float) mBaseParser.getDisplayer().getWidth();
            mDispScaleY = mDispHeight / (float) mBaseParser.getDisplayer().getHeight();
            if (mViewWidth <= 1) {
                mViewWidth = disp.getWidth();
            }
            return this;
        }

        @Override
        protected float getViewportSizeFactor() {
            float scale = DanmakuFactory.COMMON_DANMAKU_DURATION * mViewWidth / DanmakuFactory.BILI_PLAYER_WIDTH;
            float factor = 1.1f;
            return mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * factor / scale;
        }
    }

    private DanmakuTimer mTimer;

    public interface OnFrameAvailableListener {

        void onConfig(DanmakuContext config);

        void onFrameAvailable(long timeMills, Bitmap bitmap);

        void onFramesFinished(long timeMills);

        void onFailed(int errorCode, String msg);
    }

    private boolean mIsRelease;
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
        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }

    @Override
    public long drawDanmakus() {
        if (mIsRelease) {
            return 0;
        }
        Canvas canvas = mBufferCanvas;
        if (canvas == null) {
            return 0;
        }
        Bitmap bufferBitmap = this.mBufferBitmap;
        if (bufferBitmap == null || bufferBitmap.isRecycled()) {
            return 0;
        }
        bufferBitmap.eraseColor(Color.TRANSPARENT);
        if (mClearFlag) {
            DrawHelper.clearCanvas(canvas);
            mClearFlag = false;
        } else {
            if (handler != null) {
                handler.draw(canvas);
            }
        }
        OnFrameAvailableListener onFrameAvailableListener = this.mOnFrameAvailableListener;
        if (onFrameAvailableListener != null) {
            long curr = mOuterTimer.currMillisecond;
            try {
                if (curr >= mExpectBeginMills - mFrameIntervalMills) {
                    Bitmap bitmap;
                    boolean recycle = false;
                    if (mScale == 1f) {
                        bitmap = bufferBitmap;
                    } else {
                        bitmap = Bitmap.createScaledBitmap(bufferBitmap, (int) (mWidth * mScale), (int) (mHeight * mScale), true);
                        recycle = true;
                    }
                    onFrameAvailableListener.onFrameAvailable(curr, bitmap);
                    if (recycle) {
                        bitmap.recycle();
                    }
                }
            } catch (Exception e) {
                release();
                onFrameAvailableListener.onFailed(101, e.getMessage());
            } finally {
                if (curr >= mEndTimeMills) {
                    release();
                    if (mTimer != null) {
                        mTimer.update(mEndTimeMills);
                    }
                    onFrameAvailableListener.onFramesFinished(curr);
                }
            }
        }
        mRequestRender = false;
        return 2;  // 固定频率
    }

    @Override
    public void release() {
        mIsRelease = true;
        super.release();
        mBufferBitmap = null;
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
        CustomParser newParser = new CustomParser(parser, mBeginTimeMills, mEndTimeMills);
        DanmakuContext configCopy;
        try {
            configCopy = (DanmakuContext) config.clone();
            configCopy.resetContext();
            configCopy.transparency = AlphaValue.MAX;
            configCopy.setDanmakuTransparency(config.transparency / (float) AlphaValue.MAX);
            configCopy.mGlobalFlagValues.FILTER_RESET_FLAG = config.mGlobalFlagValues.FILTER_RESET_FLAG;
            configCopy.setDanmakuSync(null);
            configCopy.unregisterAllConfigChangedCallbacks();
            configCopy.mGlobalFlagValues.updateAll();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            configCopy = config;
        }
        configCopy.updateMethod = 1;
        if (mOnFrameAvailableListener != null) {
            mOnFrameAvailableListener.onConfig(configCopy);
        }
        super.prepare(newParser, configCopy);
        handler.setIdleSleep(false);
        handler.enableNonBlockMode(true);
    }

    public void setTimeRange(final long beginMills, final long endMills) {
        mExpectBeginMills = beginMills;
        mBeginTimeMills = Math.max(0, beginMills - 30000L); // FIXME: 17/8/23 magic code 30000L
        mEndTimeMills = endMills;
    }

    public void setOnFrameAvailableListener(OnFrameAvailableListener onFrameAvailableListener) {
        mOnFrameAvailableListener = onFrameAvailableListener;
    }

    public void getFrameAtTime(final int frameRate) {
        if (mRetryCount++ > 5) {
            release();
            if (mOnFrameAvailableListener != null) {
                mOnFrameAvailableListener.onFailed(100, "not prepared");
            }
            return;
        }
        if (!isPrepared()) {
            DrawHandler handler = this.handler;
            if (handler == null) {
                return;
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getFrameAtTime(frameRate);
                }
            }, 1000L);
            return;
        }
        mFrameIntervalMills = 1000 / frameRate;
        setCallback(this);
        long beginMills = Math.max(0, mExpectBeginMills - getConfig().mDanmakuFactory.MAX_DANMAKU_DURATION * 3 / 2);
        mOuterTimer = new DanmakuTimer(beginMills);
        start(beginMills);
    }

    @Override
    public void prepared() {

    }

    @Override
    public void updateTimer(DanmakuTimer timer) {
        mTimer = timer;
        timer.update(mOuterTimer.currMillisecond);
        mOuterTimer.add(mFrameIntervalMills);
        timer.add(mFrameIntervalMills);
    }

    @Override
    public void danmakuShown(BaseDanmaku danmaku) {

    }

    @Override
    public void drawingFinished() {

    }
}
