package master.flame.danmaku.ui.widget;

import android.graphics.RectF;
import android.nfc.Tag;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.Danmakus;

/**
 * Created by kmfish on 2015/1/25.
 */
public class DanmakuTouchHelper {
    private final GestureDetector mTouchDelegate;
    private IDanmakuView danmakuView;
    private RectF mDanmakuBounds;
    private float mXOff;
    private float mYOff;

    private final android.view.GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent event) {
            if (danmakuView != null) {
                IDanmakuView.OnDanmakuClickListener onDanmakuClickListener = danmakuView.getOnDanmakuClickListener();
                if (onDanmakuClickListener != null) {
                    mXOff = danmakuView.getXOff();
                    mYOff = danmakuView.getYOff();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            IDanmakus clickDanmakus = touchHitDanmaku(event.getX(), event.getY());
            boolean isEventConsumed = false;
            if (null != clickDanmakus && !clickDanmakus.isEmpty()) {
                isEventConsumed = performDanmakuClick(clickDanmakus, false);
            }
            if (!isEventConsumed) {
                isEventConsumed = performViewClick();
            }
            return isEventConsumed;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            IDanmakuView.OnDanmakuClickListener onDanmakuClickListener = danmakuView.getOnDanmakuClickListener();
            if (onDanmakuClickListener == null) {
                return;
            }
            mXOff = danmakuView.getXOff();
            mYOff = danmakuView.getYOff();
            IDanmakus clickDanmakus = touchHitDanmaku(event.getX(), event.getY());
            if (null != clickDanmakus && !clickDanmakus.isEmpty()) {
                performDanmakuClick(clickDanmakus, true);
            }
        }
    };

    private DanmakuTouchHelper(IDanmakuView danmakuView) {
        this.danmakuView = danmakuView;
        this.mDanmakuBounds = new RectF();
        this.mTouchDelegate = new GestureDetector(((View) danmakuView).getContext(), mOnGestureListener);
    }

    public static synchronized DanmakuTouchHelper instance(IDanmakuView danmakuView) {
        return new DanmakuTouchHelper(danmakuView);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mTouchDelegate.onTouchEvent(event);
    }

    private boolean performDanmakuClick(IDanmakus danmakus, boolean isLongClick) {
        IDanmakuView.OnDanmakuClickListener onDanmakuClickListener = danmakuView.getOnDanmakuClickListener();
        if (onDanmakuClickListener != null) {
            if (isLongClick) {
                return onDanmakuClickListener.onDanmakuLongClick(danmakus);
            } else {
                return onDanmakuClickListener.onDanmakuClick(danmakus);
            }
        }
        return false;
    }

    private boolean performViewClick() {
        IDanmakuView.OnDanmakuClickListener onDanmakuClickListener = danmakuView.getOnDanmakuClickListener();
        if (onDanmakuClickListener != null) {
            return onDanmakuClickListener.onViewClick(danmakuView);
        }
        return false;
    }

    private IDanmakus touchHitDanmaku(final float x, final float y) {
        final IDanmakus hitDanmakus = new Danmakus();
        mDanmakuBounds.setEmpty();

        IDanmakus danmakus = danmakuView.getCurrentVisibleDanmakus();
        if (null != danmakus && !danmakus.isEmpty()) {
            danmakus.forEachSync(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    if (null != danmaku) {
                        mDanmakuBounds.set(danmaku.getLeft(), danmaku.getTop(), danmaku.getRight(), danmaku.getBottom());
                        if (mDanmakuBounds.intersect(x - mXOff, y - mYOff, x + mXOff, y + mYOff)) {
                            hitDanmakus.addItem(danmaku);
                        }
                    }
                    return ACTION_CONTINUE;
                }
            });
        }

        return hitDanmakus;
    }
}
