
package master.flame.danmaku.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.Danmakus;

public class DanmakuFilters {

    public static interface IDanmakuFilter {
        /*
         * 是否过滤
         */
        public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
                DanmakuTimer timer);

        public void setData(Object data);

        public void reset();

    }

    /**
     * 根据弹幕类型过滤
     * 
     * @author ch
     */
    public static class TypeDanmakuFilter implements IDanmakuFilter {

        final List<Integer> mFilterTypes = Collections.synchronizedList(new ArrayList<Integer>());

        public void enableType(Integer type) {
            if (!mFilterTypes.contains(type))
                mFilterTypes.add(type);
        }

        public void disableType(Integer type) {
            if (mFilterTypes.contains(type))
                mFilterTypes.remove(type);
        }

        @Override
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, int totalsizeInScreen,
                DanmakuTimer timer) {
            if (danmaku != null && mFilterTypes.contains(danmaku.getType()))
                return true;
            return false;
        }

        @Override
        public void setData(Object data) {
            if (data == null || data instanceof List<?>) {
                mFilterTypes.clear();
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    List<Integer> list = (List<Integer>) data;
                    for (Integer i : list) {
                        enableType(i);
                    }
                }
            }
        }

        @Override
        public void reset() {
            mFilterTypes.clear();
        }

    }

    /**
     * 根据同屏数量过滤弹幕
     * 
     * @author ch
     */
    public static class QuantityDanmakuFilter implements IDanmakuFilter {

        protected int mMaximumSize = -1;

        protected final IDanmakus danmakus = new Danmakus();

        protected BaseDanmaku mLastSkipped = null;

        @Override
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, int totalsizeInScreen,
                DanmakuTimer timer) {
            BaseDanmaku last = danmakus.last();
            if (last != null && last.isTimeOut()) {
                reset();
                last = null;
            }

            if (mMaximumSize <= 0 || danmaku.getType() != BaseDanmaku.TYPE_SCROLL_RL) {
                return false;
            }

            if (danmakus.contains(danmaku)) {
                return true;
            }

            if (totalsizeInScreen < mMaximumSize || danmaku.isShown()
                    || (mLastSkipped != null && (danmaku.time - mLastSkipped.time > 500))) {
                mLastSkipped = danmaku;
                return false;
            }

            if (orderInScreen > mMaximumSize && !danmaku.isTimeOut()) {
                danmakus.addItem(danmaku);
                return true;
            }
            mLastSkipped = danmaku;
            return false;
        }

        @Override
        public void setData(Object data) {
            if (data instanceof Integer) {
                Integer maximumSize = (Integer) data;
                if (maximumSize != mMaximumSize) {
                    mMaximumSize = maximumSize;
                    danmakus.clear();
                }
            }
        }

        @Override
        public void reset() {
            danmakus.clear();
        }
    }

    /**
     * 根据绘制耗时过滤弹幕
     * 
     * @author ch
     */
    public static class ElapsedTimeFilter implements IDanmakuFilter {

        long mMaxTime = 20; // 绘制超过20ms就跳过 ，默认保持接近50fps

        protected final IDanmakus danmakus = new Danmakus();

        @Override
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, int totalsizeInScreen,
                DanmakuTimer timer) {

            if (danmakus.last() != null && danmakus.last().isTimeOut()) {
                reset();
            }

            if (danmakus.contains(danmaku)) {
                return true;
            }

            if (timer == null || !danmaku.isOutside()) {
                return false;
            }

            long elapsedTime = System.currentTimeMillis() - timer.currMillisecond;
            if (elapsedTime >= mMaxTime) {
                danmakus.addItem(danmaku);
                return true;
            }
            return false;
        }

        @Override
        public void setData(Object data) {

        }

        @Override
        public void reset() {
            danmakus.clear();
        }

    }

    public final static String TAG_TYPE_DANMAKU_FILTER = "1010_Filter";

    public final static String TAG_QUANTITY_DANMAKU_FILTER = "1011_Filter";

    public final static String TAG_ELAPSED_TIME_FILTER = "1012_Filter";

    private static DanmakuFilters instance = null;

    public final Exception filterException = new Exception("not suuport this filter tag");

    public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
            DanmakuTimer timer) {
        for (IDanmakuFilter f : mFilterArray) {
            if (f != null && f.filter(danmaku, index, totalsizeInScreen, timer)) {
                return true;
            }
        }
        return false;
    }

    private final static Map<String, IDanmakuFilter> filters = Collections
            .synchronizedSortedMap(new TreeMap<String, IDanmakuFilter>());

    public IDanmakuFilter get(String tag) {
        IDanmakuFilter f = filters.get(tag);
        if (f == null) {
            f = registerFilter(tag, null);
        }
        return f;
    }

    IDanmakuFilter[] mFilterArray = new IDanmakuFilter[0];

    public IDanmakuFilter registerFilter(String tag, Object data) {
        if (tag == null) {
            throwFilterException();
            return null;
        }
        IDanmakuFilter filter = filters.get(tag);
        if (filter == null) {
            if (TAG_TYPE_DANMAKU_FILTER.equals(tag)) {
                filter = new TypeDanmakuFilter();
            } else if (TAG_QUANTITY_DANMAKU_FILTER.equals(tag)) {
                filter = new QuantityDanmakuFilter();
            } else if (TAG_ELAPSED_TIME_FILTER.equals(tag)) {
                filter = new ElapsedTimeFilter();
            }
            // add more filter
        }
        if (filter == null) {
            throwFilterException();
            return null;
        }
        filter.setData(data);
        filters.put(tag, filter);
        mFilterArray = filters.values().toArray(mFilterArray);
        return filter;
    }

    public void unregisterFilter(String tag) {
        IDanmakuFilter f = filters.remove(tag);
        if (f != null) {
            f.reset();
            f = null;
            mFilterArray = filters.values().toArray(mFilterArray);
        }
    }

    public void clear() {
        filters.clear();
        mFilterArray = new IDanmakuFilter[0];
    }

    public void reset() {
        for (IDanmakuFilter f : mFilterArray) {
            if (f != null)
                f.reset();
        }
    }

    private void throwFilterException() {
        try {
            throw filterException;
        } catch (Exception e) {
        }
    }

    public static DanmakuFilters getDefault() {
        if (instance == null) {
            instance = new DanmakuFilters();
        }
        return instance;
    }

}
