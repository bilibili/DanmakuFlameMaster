/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.danmaku.model.android;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Danmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;

import java.util.*;

public class Danmakus implements IDanmakus {

    public static final int ST_BY_TIME = 0;

    public static final int ST_BY_YPOS = 1;

    public static final int ST_BY_YPOS_DESC = 2;

   public Set<BaseDanmaku> items;

    private Danmakus subItems;

    private BaseDanmaku startItem, endItem;

    public Danmakus() {
        this(ST_BY_TIME);
    }

    public Danmakus(int sortType) {
        Comparator<BaseDanmaku> comparator = null;
        if (sortType == ST_BY_TIME) {
            comparator = new TimeComparator();
        } else if (sortType == ST_BY_YPOS) {
            comparator = new YPosComparator();
        } else if (sortType == ST_BY_YPOS_DESC) {
            comparator = new YposDescComparator();
      }
        items = new TreeSet<BaseDanmaku>(comparator);
    }

    public Danmakus(Set<BaseDanmaku> items) {
        setItems(items);
    }

    public void setItems(Set<BaseDanmaku> items) {
        if (this.items != null) {
            Iterator<BaseDanmaku> it = this.items.iterator();
            while (it.hasNext()) {
                BaseDanmaku item = it.next();
                if (item.isOutside()) {
                    item.setVisibility(false);
                    if (item.hasDrawingCache()) item.cache.destroy();
                } else {
                    break;
                }
            }
        }
        this.items = items;
    }

    public Iterator<BaseDanmaku> iterator() {
        if (items != null) {
            return items.iterator();
        }
        return null;
    }

    @Override
    public void addItem(BaseDanmaku item) {
        if (items != null)
            items.add(item);
    }

    @Override
    public void removeItem(BaseDanmaku item) {
        if (item.isOutside()) {
            item.setVisibility(false);
        }
        if (items != null) {
            items.remove(item);
        }
    }

    @Override
    public IDanmakus sub(long startTime, long endTime) {
        if (subItems == null) {
            subItems = new Danmakus();
        }
        if (startItem == null) {
            startItem = createItem("start");
        }
        if (endItem == null) {
            endItem = createItem("end");
        }

        if (startTime >= startItem.time && endTime <= endItem.time && subItems != null) {
            return subItems;
        }

        startItem.time = startTime;
        endItem.time = endTime;
        subItems.setItems(((SortedSet<BaseDanmaku>) items).subSet(startItem, endItem));
        return subItems;
    }

    private BaseDanmaku createItem(String text) {
        return new Danmaku(text);
    }

    public int size() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }

    @Override
    public void clear() {
        if (items != null)
            items.clear();
        if (subItems != null) {
            Iterator<BaseDanmaku> it = subItems.iterator();
            while (it.hasNext()) {
                BaseDanmaku item = it.next();
                item.setVisibility(false);
            }
            subItems.clear();
        }
    }

    private class TimeComparator implements Comparator<BaseDanmaku> {

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            long val = obj1.time - obj2.time;
            if (val > 0) {
                return 1;
            } else if (val < 0) {
                return -1;
            }

            Integer t1 = obj1.getType();
            Integer t2 = obj2.getType();
            int result = t1.compareTo(t2);
            if (result != 0) {
                return result;
            }

            if (obj1.text == obj2.text) {
                return 0;
            }
            if (obj1.text == null) {
                return -1;
            }
            if (obj2.text == null) {
                return 1;
            }
            return obj1.text.compareTo(obj2.text);
        }
    }

    private class YPosComparator implements Comparator<BaseDanmaku> {

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            int result = Float.compare(obj1.getTop(), obj2.getTop());
            if (result != 0) {
                return result;
            }
            long val = obj1.time - obj2.time;
            if (val > 0) {
                result = 1;
            } else if (val < 0) {
                result = -1;
            }
            return result;
        }
    }


    private class YposDescComparator implements Comparator<BaseDanmaku> {
        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {

            int result = Float.compare(obj2.getTop(), obj1.getTop());
            if (result != 0) {
                return result;
            }
            long val = obj1.time - obj2.time;
            if (val > 0) {
                result = 1;
            } else if (val < 0) {
                result = -1;
            }
            return result;
        }
    }
}
