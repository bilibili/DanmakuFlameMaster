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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Danmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;

public class Danmakus implements IDanmakus {

    public Collection<BaseDanmaku> items;

    private Danmakus subItems;

    private BaseDanmaku startItem, endItem;

    private BaseDanmaku endSubItem;

    private BaseDanmaku startSubItem;

    private volatile AtomicInteger mSize = new AtomicInteger(0);

    private int mSortType = ST_BY_TIME;

    private BaseComparator mComparator;

    private boolean mDuplicateMergingEnabled;
    private Object mLockObject = new Object();

    public Danmakus() {
        this(ST_BY_TIME, false);
    }

    public Danmakus(int sortType) {
        this(sortType, false);
    }

    public Danmakus(int sortType, boolean duplicateMergingEnabled) {
        this(sortType, duplicateMergingEnabled, null);
    }

    public Danmakus(int sortType, boolean duplicateMergingEnabled, BaseComparator baseComparator) {
        BaseComparator comparator = null;
        if (sortType == ST_BY_TIME) {
            comparator = baseComparator == null ? new TimeComparator(duplicateMergingEnabled) : baseComparator;
        } else if (sortType == ST_BY_YPOS) {
            comparator = new YPosComparator(duplicateMergingEnabled);
        } else if (sortType == ST_BY_YPOS_DESC) {
            comparator = new YPosDescComparator(duplicateMergingEnabled);
        }
        if (sortType == ST_BY_LIST) {
            items = new LinkedList<>();
        } else {
            mDuplicateMergingEnabled = duplicateMergingEnabled;
            comparator.setDuplicateMergingEnabled(duplicateMergingEnabled);
            items = new TreeSet<>(comparator);
            mComparator = comparator;
        }
        mSortType = sortType;
        mSize.set(0);
    }

    public Danmakus(Collection<BaseDanmaku> items) {
        setItems(items);
    }

    public Danmakus(boolean duplicateMergingEnabled) {
        this(ST_BY_TIME, duplicateMergingEnabled);
    }

    public void setItems(Collection<BaseDanmaku> items) {
        if (mDuplicateMergingEnabled && mSortType != ST_BY_LIST) {
            synchronized (this.mLockObject) {
                this.items.clear();
                this.items.addAll(items);
                items = this.items;
            }
        } else {
            this.items = items;
        }
        if (items instanceof List) {
            mSortType = ST_BY_LIST;
        }
        mSize.set(items == null ? 0 : items.size());
    }

    @Override
    public boolean addItem(BaseDanmaku item) {
        synchronized (this.mLockObject) {
            if (items != null) {
                try {
                    if (items.add(item)) {
                        mSize.incrementAndGet();
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeItem(BaseDanmaku item) {
        if (item == null) {
            return false;
        }
        if (item.isOutside()) {
            item.setVisibility(false);
        }
        synchronized (this.mLockObject) {
            if (items.remove(item)) {
                mSize.decrementAndGet();
                return true;
            }
        }
        return false;
    }

    private Collection<BaseDanmaku> subset(long startTime, long endTime) {
        if (mSortType == ST_BY_LIST || items == null || items.size() == 0) {
            return null;
        }
        if (subItems == null) {
            subItems = new Danmakus(mDuplicateMergingEnabled);
            subItems.mLockObject = this.mLockObject;
        }
        if (startSubItem == null) {
            startSubItem = createItem("start");
        }
        if (endSubItem == null) {
            endSubItem = createItem("end");
        }

        startSubItem.setTime(startTime);
        endSubItem.setTime(endTime);
        return ((SortedSet<BaseDanmaku>) items).subSet(startSubItem, endSubItem);
    }

    @Override
    public IDanmakus subnew(long startTime, long endTime) {
        Collection<BaseDanmaku> subset = subset(startTime, endTime);
        if (subset == null || subset.isEmpty()) {
            return null;
        }
        LinkedList<BaseDanmaku> newSet = new LinkedList<BaseDanmaku>(subset);
        return new Danmakus(newSet);
    }

    @Override
    public IDanmakus sub(long startTime, long endTime) {
        if (items == null || items.size() == 0) {
            return null;
        }
        if (subItems == null) {
            if(mSortType == ST_BY_LIST) {
                subItems = new Danmakus(Danmakus.ST_BY_LIST);
                subItems.mLockObject = this.mLockObject;
                synchronized (this.mLockObject) {
                    subItems.setItems(items);
                }
            } else {
                subItems = new Danmakus(mDuplicateMergingEnabled);
                subItems.mLockObject = this.mLockObject;
            }
        }
        if (mSortType == ST_BY_LIST) {
            return subItems;
        }
        if (startItem == null) {
            startItem = createItem("start");
        }
        if (endItem == null) {
            endItem = createItem("end");
        }

        if (subItems != null) {
            long dtime = startTime - startItem.getActualTime();
            if (dtime >= 0 && endTime <= endItem.getActualTime()) {
                return subItems;
            }
        }

        startItem.setTime(startTime);
        endItem.setTime(endTime);
        synchronized (this.mLockObject) {
            subItems.setItems(((SortedSet<BaseDanmaku>) items).subSet(startItem, endItem));
        }
        return subItems;
    }

    private BaseDanmaku createItem(String text) {
        return new Danmaku(text);
    }

    public int size() {
        return mSize.get();
    }

    @Override
    public void clear() {
        synchronized (this.mLockObject) {
            if (items != null) {
                items.clear();
                mSize.set(0);
            }
        }
        if (subItems != null) {
            subItems = null;
            startItem = createItem("start");
            endItem = createItem("end");
        }
    }

    @Override
    public BaseDanmaku first() {
        if (items != null && !items.isEmpty()) {
            if (mSortType == ST_BY_LIST) {
                return ((LinkedList<BaseDanmaku>) items).peek();
            }
            return ((SortedSet<BaseDanmaku>) items).first();
        }
        return null;
    }

    @Override
    public BaseDanmaku last() {
        if (items != null && !items.isEmpty()) {
            if (mSortType == ST_BY_LIST) {
                return ((LinkedList<BaseDanmaku>) items).peekLast();
            }
            return ((SortedSet<BaseDanmaku>) items).last();
        }
        return null;
    }

    @Override
    public boolean contains(BaseDanmaku item) {
        return this.items != null && this.items.contains(item);
    }

    @Override
    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    private void setDuplicateMergingEnabled(boolean enable) {
        mComparator.setDuplicateMergingEnabled(enable);
        mDuplicateMergingEnabled = enable;
    }

    @Override
    public void setSubItemsDuplicateMergingEnabled(boolean enable) {
        mDuplicateMergingEnabled = enable;
        startItem = endItem = null;
        if (subItems == null) {
            subItems = new Danmakus(enable);
            subItems.mLockObject = this.mLockObject;
        }
        subItems.setDuplicateMergingEnabled(enable);
    }

    @Override
    public Collection<BaseDanmaku> getCollection() {
        return this.items;
    }

    @Override
    public void forEachSync(Consumer<? super BaseDanmaku, ?> consumer) {
        synchronized (this.mLockObject) {
            forEach(consumer);
        }
    }

    @Override
    public void forEach(Consumer<? super BaseDanmaku, ?> consumer) {
        consumer.before();
        Iterator<BaseDanmaku> it = items.iterator();
        while (it.hasNext()) {
            BaseDanmaku next = it.next();
            if (next == null) {
                continue;
            }
            int action = consumer.accept(next);
            if (action == DefaultConsumer.ACTION_BREAK) {
                break;
            } else if (action == DefaultConsumer.ACTION_REMOVE) {
                it.remove();
                mSize.decrementAndGet();
            } else if (action == DefaultConsumer.ACTION_REMOVE_AND_BREAK) {
                it.remove();
                mSize.decrementAndGet();
                break;
            }
        }
        consumer.after();
    }

    @Override
    public Object obtainSynchronizer() {
        return mLockObject;
    }

}