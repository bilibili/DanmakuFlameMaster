
package master.flame.danmaku.danmaku.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DanmakuFilters {

    public static interface IDanmakuFilter {
        /*
         * 是否过滤
         */
        public boolean filter(BaseDanmaku danmakus);

        public void setData(Object data);
        
    }

    /**
     * 根据弹幕类型过滤
     * 
     * @author ch
     */
    public static class TypeDanmakuFilter implements IDanmakuFilter {

        List<Integer> mFilterTypes = Collections.synchronizedList(new ArrayList<Integer>());

        public void enableType(Integer type) {
            if (!mFilterTypes.contains(type))
                mFilterTypes.add(type);
        }

        public void disableType(Integer type) {
            if (mFilterTypes.contains(type))
                mFilterTypes.remove(type);
        }

        @Override
        public boolean filter(BaseDanmaku danmaku) {
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

    }

    public final static String TAG_TYPE_DANMAKU_FILTER = "0001_Filter";

    private static DanmakuFilters instance = null;

    public final Exception filterException = new Exception("not suuport this filter tag");
    
    public boolean filter(BaseDanmaku danmaku){
        Iterator<IDanmakuFilter> fit = filters.values().iterator();
        while (fit.hasNext()) {
            if (fit.next().filter(danmaku)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据注册的过滤器过滤弹幕
     * @param danmakus
     * @return 过滤掉的数量
     */
    public int filter(IDanmakus danmakus) {
        if(filters.isEmpty()){
            return 0;
        }
        int count = 0;
        IDanmakuIterator it = danmakus.iterator();
        while (it.hasNext()) {
            BaseDanmaku danmaku = it.next();
            synchronized (this) {
                Iterator<IDanmakuFilter> fit = filters.values().iterator();
                while (fit.hasNext()) {
                    if (fit.next().filter(danmaku)) {
                        //it.remove();
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    private final static Map<String, IDanmakuFilter> filters = Collections.synchronizedSortedMap(new TreeMap<String, IDanmakuFilter>());
    
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
        filters.remove(tag);
    }

    public void reset() {
        filters.clear();
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
