package master.flame.danmaku.ui.widget;

import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

/**
 * Created by kmfish on 2015/1/25.
 */
public class DanmakuTouchHelper {

    private IDanmakuView danmakuView;
    private RectF mDanmakuBounds;
    private List<BaseDanmaku> hitDanmakuList;

    private DanmakuTouchHelper(IDanmakuView danmakuView) {
        this.danmakuView = danmakuView;
        this.mDanmakuBounds = new RectF();
        this.hitDanmakuList = new ArrayList<BaseDanmaku>();
    }

    public static synchronized DanmakuTouchHelper instance(IDanmakuView danmakuView) {
        return new DanmakuTouchHelper(danmakuView);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                BaseDanmaku clickDanmaku = touchHitDanmaku(event.getX(), event.getY());
                if (null != clickDanmaku) {
                    performClick(clickDanmaku);
                }
                break;
            default:
                break;
        }

        return false;
    }

    private void performClick(BaseDanmaku danmaku) {
        if (danmakuView.getOnDanmakuClickListener() != null) {
            danmakuView.getOnDanmakuClickListener().onDanmakuClick(danmaku);
        }
    }

    private BaseDanmaku touchHitDanmaku(float x, float y) {
        hitDanmakuList.clear();
        mDanmakuBounds.setEmpty();

        List<BaseDanmaku> danmakus = danmakuView.getCurrentVisibleDanmakus();

        if (null != danmakus && !danmakus.isEmpty()) {
            Iterator<BaseDanmaku> iterator = danmakus.iterator();
            while (iterator.hasNext()) {
                BaseDanmaku danmaku = iterator.next();
                mDanmakuBounds.set(danmaku.getLeft(), danmaku.getTop(), danmaku.getRight(), danmaku.getBottom());
                if (mDanmakuBounds.contains(x, y)) {
                    hitDanmakuList.add(danmaku);
                }
            }

            if (!hitDanmakuList.isEmpty()) {
                final int size = hitDanmakuList.size();
                BaseDanmaku newestDanmaku = null;
                for (int i = 0; i < size; i++) {
                    BaseDanmaku hitDanmaku = hitDanmakuList.get(i);
                    if (null == newestDanmaku
                            || DanmakuUtils.compare(hitDanmaku, newestDanmaku) > 0) {
                        newestDanmaku = hitDanmaku;
                    }
                }

                return newestDanmaku;
            }
        }

        return null;
    }

}
