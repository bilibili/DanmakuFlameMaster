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
import master.flame.danmaku.danmaku.model.IDanmakuIterator;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

import java.util.*;

public class Danmakus implements IDanmakus {

    public static final int ST_BY_TIME = 0;

    public static final int ST_BY_YPOS = 1;

    public static final int ST_BY_YPOS_DESC = 2;

    public Set<BaseDanmaku> items;

    private Danmakus subItems;

    private BaseDanmaku startItem, endItem;

    private BaseDanmaku endSubItem;

    private BaseDanmaku startSubItem;
    
    private DanmakuIterator iterator;

    private int mSize = 0;

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
            comparator = new YPosDescComparator();
        }
        items = new TreeSet<BaseDanmaku>(comparator);
        mSize = 0;
        iterator = new DanmakuIterator(items);
    }

    public Danmakus(Set<BaseDanmaku> items) {
        setItems(items);
    }

    public void setItems(Set<BaseDanmaku> items) {        
        this.items = items;
        mSize = (items == null ? 0 : items.size());
        iterator.setDatas(items);
    }

    public IDanmakuIterator iterator() {
        iterator.reset();
        return iterator;
    }

    @Override
    public void addItem(BaseDanmaku item) {
        if (items != null) {
            if (items.add(item))
                mSize++;
        }
    }

    @Override
    public void removeItem(BaseDanmaku item) {
        if (item == null) {
            return;
        }
        if (item.isOutside()) {
            item.setVisibility(false);
        }
        if (items.remove(item))
            mSize--;
    }

    public Set<BaseDanmaku> subset(long startTime, long endTime) {
        if (items == null || items.size() == 0) {
            return null;
        }
        if (subItems == null) {
            subItems = new Danmakus();
        }
        if (startSubItem == null) {
            startSubItem = createItem("start");
        }
        if (endSubItem == null) {
            endSubItem = createItem("end");
        }

        startSubItem.time = startTime;
        endSubItem.time = endTime;
        return ((SortedSet<BaseDanmaku>) items).subSet(startSubItem, endSubItem);
    }

    @Override
    public IDanmakus sub(long startTime, long endTime) {
        if (items == null || items.size() == 0) {
            return null;
        }
        if (subItems == null) {
            subItems = new Danmakus();
        }
        if (startItem == null) {
            startItem = createItem("start");
        }
        if (endItem == null) {
            endItem = createItem("end");
        }

        if (subItems != null) {
            long dtime = startTime - startItem.time;
            if (dtime >= 0 && endTime <= endItem.time) {
                return subItems;
            }
        }

        startItem.time = startTime;
        endItem.time = endTime + 1000; // +1000减少subSet次数
        subItems.setItems(((SortedSet<BaseDanmaku>) items).subSet(startItem, endItem));
        return subItems;
    }

    private BaseDanmaku createItem(String text) {
        return new Danmaku(text);
    }

    public int size() {
        return mSize;
    }

    @Override
    public void clear() {
        if (items != null){
            items.clear();
            mSize = 0;
        }
        if (subItems != null) {
            subItems.clear();
        }
    }

    @Override
    public BaseDanmaku first() {
        if (items != null && !items.isEmpty()) {
            return ((SortedSet<BaseDanmaku>) items).first();
        }
        return null;
    }

    @Override
    public BaseDanmaku last() {
        if (items != null && !items.isEmpty()) {
            return ((SortedSet<BaseDanmaku>) items).last();
        }
        return null;
    }
    
    private class DanmakuIterator implements IDanmakuIterator{
        
        private Set<BaseDanmaku> mData;
        private Iterator<BaseDanmaku> it;

        public DanmakuIterator(Set<BaseDanmaku> datas){
            setDatas(datas);
        }
        
        public synchronized void reset(){
            if(mData!=null || mSize > 0){
                it = mData != null ? mData.iterator() : null;
            }else{
                it = null;
            }
        }

        public synchronized void setDatas(Set<BaseDanmaku> datas){
            mData = datas;
        }

        @Override
        public synchronized BaseDanmaku next() {
            return it != null ? it.next() : null;
        }

        @Override
        public synchronized boolean hasNext() {
            return it != null && it.hasNext();
        }

        @Override
        public synchronized void remove() {
            if(it!=null){
                it.remove();
            }
        }

    }

    private class TimeComparator implements Comparator<BaseDanmaku> {
        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {

            return DanmakuUtils.compare(obj1, obj2);
        }
    }

    private class YPosComparator implements Comparator<BaseDanmaku> {
        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            int result = Float.compare(obj1.getTop(), obj2.getTop());
            if (result != 0) {
                return result;
            }
            return DanmakuUtils.compare(obj1, obj2);
        }
    }

    private class YPosDescComparator implements Comparator<BaseDanmaku> {
        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {

            int result = Float.compare(obj2.getTop(), obj1.getTop());
            if (result != 0) {
                return result;
            }
            return DanmakuUtils.compare(obj1, obj2);
        }
    }

    @Override
    public boolean contains(BaseDanmaku item) {
        return this.items != null && this.items.contains(item);
    }

    @Override
    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

}
