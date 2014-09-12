
package master.flame.danmaku.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.Danmakus;

public class DanmakuFilters {

    public static interface IDanmakuFilter<T> {
        /*
         * 是否过滤
         */
        public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
                DanmakuTimer timer);

        public void setData(T data);

        public void reset();

    }

    /**
     * 根据弹幕类型过滤
     * 
     * @author ch
     */
    public static class TypeDanmakuFilter implements IDanmakuFilter<List<Integer>> {

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
        public void setData(List<Integer> data) {
            reset();
            if (data != null) {
                List<Integer> list = data;
                for (Integer i : list) {
                    enableType(i);
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
    public static class QuantityDanmakuFilter implements IDanmakuFilter<Integer> {

        protected int mMaximumSize = -1;

        protected final IDanmakus danmakus = new Danmakus();

        protected BaseDanmaku mLastSkipped = null;

        @Override
        public synchronized boolean filter(BaseDanmaku danmaku, int orderInScreen, int totalsizeInScreen,
                DanmakuTimer timer) {
            BaseDanmaku last = danmakus.last();
            if (last != null && last.isTimeOut()) {
                danmakus.clear();
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
        public void setData(Integer data) {
            reset();
            if(data == null) return;
            Integer maximumSize = data;
            if (maximumSize != mMaximumSize) {
                mMaximumSize = maximumSize;
            }
        }

        @Override
        public synchronized void reset() {
            danmakus.clear();
        }
    }

    /**
     * 根据绘制耗时过滤弹幕
     * 
     * @author ch
     */
    public static class ElapsedTimeFilter implements IDanmakuFilter<Object> {

        long mMaxTime = 20; // 绘制超过20ms就跳过 ，默认保持接近50fps

        protected final IDanmakus danmakus = new Danmakus();

        @Override
        public synchronized boolean filter(BaseDanmaku danmaku, int orderInScreen, int totalsizeInScreen,
                DanmakuTimer timer) {

            if (danmakus.last() != null && danmakus.last().isTimeOut()) {
                danmakus.clear();
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
            reset();
        }

        @Override
        public synchronized void reset() {
            danmakus.clear();
        }

    }
    
    /**
     * 根据文本颜色白名单过滤
     * @author ch
     *
     */
    public static class TextColorFilter  implements IDanmakuFilter<List<Integer>> {
        
        public List<Integer> mWhiteList = new ArrayList<Integer>(); 
        
        private void addToWhiteList(Integer color){
            if(!mWhiteList.contains(color)){
                mWhiteList.add(color);
            }
        }
        
        @Override
        public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
                DanmakuTimer timer) {
            if (danmaku != null && !mWhiteList.contains(danmaku.textColor))
                return true;
            return false;
        }

        @Override
        public void setData(List<Integer> data) {
            reset();
            if (data != null) {
                List<Integer> list = data;
                for (Integer i : list) {
                    addToWhiteList(i);
                }
            }
        }

        @Override
        public void reset() {
            mWhiteList.clear();
        }
        
    }
    
    /**
     * 根据用户Id黑名单过滤
     * @author ch
     *
     */
    public static class UserIdFilter  implements IDanmakuFilter<List<Integer>> {
        
        public List<Integer> mBlackList = new ArrayList<Integer>(); 
        
        private void addToBlackList(Integer id){
            if(!mBlackList.contains(id)){
                mBlackList.add(id);
            }
        }
        
        @Override
        public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
                DanmakuTimer timer) {
            if (danmaku != null && mBlackList.contains(danmaku.userId))
                return true;
            return false;
        }

        @Override
        public void setData(List<Integer> data) {
            reset();
            if (data != null) {
                List<Integer> list = data;
                for (Integer i : list) {
                    addToBlackList(i);
                }
            }
        }

        @Override
        public void reset() {
            mBlackList.clear();
        }
        
    }

    public final static String TAG_TYPE_DANMAKU_FILTER = "1010_Filter";

    public final static String TAG_QUANTITY_DANMAKU_FILTER = "1011_Filter";

    public final static String TAG_ELAPSED_TIME_FILTER = "1012_Filter";
    
    public final static String TAG_TEXT_COLOR_DANMAKU_FILTER = "1013_Filter";
    
    public final static String TAG_USER_ID_FILTER = "1014_Filter";

    private static DanmakuFilters instance = null;

    public final Exception filterException = new Exception("not suuport this filter tag");

    public boolean filter(BaseDanmaku danmaku, int index, int totalsizeInScreen,
            DanmakuTimer timer) {
        for (IDanmakuFilter<?> f : mFilterArray) {
            if (f != null && f.filter(danmaku, index, totalsizeInScreen, timer)) {
                return true;
            }
        }
        return false;
    }

    private final static Map<String, IDanmakuFilter<?>> filters = Collections
            .synchronizedSortedMap(new TreeMap<String, IDanmakuFilter<?>>());

    public IDanmakuFilter<?> get(String tag) {
        IDanmakuFilter<?> f = filters.get(tag);
        if (f == null) {
            f = registerFilter(tag);
        }
        return f;
    }

    IDanmakuFilter<?>[] mFilterArray = new IDanmakuFilter[0];

    public IDanmakuFilter<?> registerFilter(String tag) {
        if (tag == null) {
            throwFilterException();
            return null;
        }
        IDanmakuFilter<?> filter = filters.get(tag);
        if (filter == null) {
            if (TAG_TYPE_DANMAKU_FILTER.equals(tag)) {
                filter = new TypeDanmakuFilter();
            } else if (TAG_QUANTITY_DANMAKU_FILTER.equals(tag)) {
                filter = new QuantityDanmakuFilter();
            } else if (TAG_ELAPSED_TIME_FILTER.equals(tag)) {
                filter = new ElapsedTimeFilter();
            } else if (TAG_TEXT_COLOR_DANMAKU_FILTER.equals(tag)) {
                filter = new TextColorFilter();
            } else if (TAG_USER_ID_FILTER.equals(tag)) {
                filter = new UserIdFilter();
            }
            // add more filter
        }
        if (filter == null) {
            throwFilterException();
            return null;
        }
        filter.setData(null);
        filters.put(tag, filter);
        mFilterArray = filters.values().toArray(mFilterArray);
        return filter;
    }

    public void unregisterFilter(String tag) {
        IDanmakuFilter<?> f = filters.remove(tag);
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
        for (IDanmakuFilter<?> f : mFilterArray) {
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
