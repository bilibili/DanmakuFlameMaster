package master.flame.danmaku.ui.widget;

import android.graphics.RectF;
import android.view.MotionEvent;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

/**
 * Created by kmfish on 2015/1/25.
 */
public class DanmakuTouchHelper {

    private IDanmakuView danmakuView;
    private RectF mDanmakuBounds;
    private IDanmakus hitDanmakus;

    private DanmakuTouchHelper(IDanmakuView danmakuView) {
        this.danmakuView = danmakuView;
        this.mDanmakuBounds = new RectF();
        this.hitDanmakus = new Danmakus();
    }

    public static synchronized DanmakuTouchHelper instance(IDanmakuView danmakuView) {
        return new DanmakuTouchHelper(danmakuView);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                IDanmakus clickDanmakus = touchHitDanmaku(event.getX(), event.getY());
                BaseDanmaku newestDanmaku = null;
                if (null != clickDanmakus && !clickDanmakus.isEmpty()) {
                    performClick(clickDanmakus);
                    newestDanmaku = fetchLatestOne(clickDanmakus);
                }

                if (null != newestDanmaku) {
                    performClickWithlatest(newestDanmaku);
                }
                break;
            default:
                break;
        }

        return false;
    }

    private void performClickWithlatest(BaseDanmaku newest) {
        if (danmakuView.getOnDanmakuClickListener() != null) {
            danmakuView.getOnDanmakuClickListener().onDanmakuClick(newest);
        }
    }

    private void performClick(IDanmakus danmakus) {
        if (danmakuView.getOnDanmakuClickListener() != null) {
            danmakuView.getOnDanmakuClickListener().onDanmakuClick(danmakus);
        }
    }

    private IDanmakus touchHitDanmaku(float x, float y) {
        hitDanmakus.clear();
        mDanmakuBounds.setEmpty();

        IDanmakus danmakus = danmakuView.getCurrentVisibleDanmakus();
        if (null != danmakus && !danmakus.isEmpty()) {
            IDanmakuIterator iterator = danmakus.iterator();
            while (iterator.hasNext()) {
                BaseDanmaku danmaku = iterator.next();
                mDanmakuBounds.set(danmaku.getLeft(), danmaku.getTop(), danmaku.getRight(), danmaku.getBottom());
                if (mDanmakuBounds.contains(x, y)) {
                    hitDanmakus.addItem(danmaku);
                }
            }


        }

        return hitDanmakus;
    }

    private BaseDanmaku fetchLatestOne(IDanmakus danmakus) {
        if (danmakus.isEmpty()) {
            return null;
        }

        BaseDanmaku newestDanmaku = null;
        IDanmakuIterator hitIterator = danmakus.iterator();
        while (hitIterator.hasNext()) {
            BaseDanmaku hitDanmaku = hitIterator.next();
            if (null == newestDanmaku
                    || DanmakuUtils.compare(hitDanmaku, newestDanmaku) > 0) {
                newestDanmaku = hitDanmaku;
            }
        }

        return newestDanmaku;

    }

}
