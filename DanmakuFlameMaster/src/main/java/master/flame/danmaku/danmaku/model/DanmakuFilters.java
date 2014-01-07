
package master.flame.danmaku.danmaku.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import master.flame.danmaku.danmaku.model.android.Danmakus;

public class DanmakuFilters {

    public static interface IDanmakuFilter {
        /*
         * 是否过滤
         */
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, Long drawingStartTime);

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
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, Long drawingStartTime) {
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

        @Override
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, Long drawingStartTime) {
            
            if(danmakus.last()!=null && danmakus.last().isTimeOut()){
                reset();
            }
            
            if (mMaximumSize <= 0 || danmaku.getType() != BaseDanmaku.TYPE_SCROLL_RL) {
                return false;
            }

            if (danmakus.contains(danmaku)) {
                return true;
            }

            if (orderInScreen > mMaximumSize && !danmaku.isTimeOut()) {
                danmakus.addItem(danmaku);
                return true;
            }
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

        long mMaxTime = 40; // 绘制超过40ms就跳过 ，保持接近25fps

        protected final IDanmakus danmakus = new Danmakus();

        @Override
        public boolean filter(BaseDanmaku danmaku, int orderInScreen, Long drawingStartTime) {
            
            if(danmakus.last()!=null && danmakus.last().isTimeOut()){
                reset();
            }
            
            long elapsedTime = System.currentTimeMillis() - drawingStartTime.longValue();
            if (danmaku.isTimeOut() || !danmaku.isOutside()) {
                return false;
            }
            if (danmakus.contains(danmaku)) {
                return true;
            }
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

    public boolean filter(BaseDanmaku danmaku, int index, Long drawingStartTime) {
        Iterator<IDanmakuFilter> fit = filters.values().iterator();
        while (fit.hasNext()) {
            if (fit.next().filter(danmaku, index, drawingStartTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据注册的过滤器过滤弹幕
     * 
     * @param danmakus
     * @return 过滤掉的数量
     */
    public int filter(IDanmakus danmakus, int orderInScreen, Long startTime) {
        if (filters.isEmpty()) {
            return 0;
        }
        int count = 0;
        IDanmakuIterator it = danmakus.iterator();
        while (it.hasNext()) {
            BaseDanmaku danmaku = it.next();
            synchronized (this) {
                Iterator<IDanmakuFilter> fit = filters.values().iterator();
                while (fit.hasNext()) {
                    if (fit.next().filter(danmaku, orderInScreen, startTime)) {
                        // it.remove();
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
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
        return filter;
    }

    public void unregisterFilter(String tag) {
        IDanmakuFilter f = filters.remove(tag);
        if (f != null)
            f.reset();
        f = null;
    }

    public void clear() {
        filters.clear();
    }
    
    public void reset() {
        for (IDanmakuFilter f : filters.values()) {
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
