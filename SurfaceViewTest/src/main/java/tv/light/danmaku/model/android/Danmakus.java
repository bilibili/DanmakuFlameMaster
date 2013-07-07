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

package tv.light.danmaku.model.android;

import tv.light.danmaku.model.Danmaku;
import tv.light.danmaku.model.DanmakuBase;
import tv.light.danmaku.model.IDanmakus;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Danmakus implements IDanmakus {

    public Set<DanmakuBase> items;

    private Danmakus subItems;

    private DanmakuBase startItem, endItem;

    public Danmakus() {
        items = new TreeSet<DanmakuBase>(new DanmakusCompartor());
    }

    public Danmakus(Set<DanmakuBase> items) {
        setItems(items);
    }

    public void setItems(Set<DanmakuBase> items) {
        this.items = items;
    }

    @Override
    public void addItem(DanmakuBase item) {
        items.add(item);
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
        if (startItem != null && endItem != null) {
            startItem.time = startTime;
            endItem.time = endTime;
            subItems.setItems(((SortedSet<DanmakuBase>) items).subSet(startItem, endItem));
            return subItems;
        }
        return null;
    }

    private DanmakuBase createItem(String text) {
        return new Danmaku(text);
    }

    private class DanmakusCompartor implements Comparator<DanmakuBase> {

        @Override
        public int compare(DanmakuBase obj1, DanmakuBase obj2) {
            long val = obj1.time - obj2.time;
            if (val > 0) {
                return -1;
            } else if (val < 0) {
                return 1;
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
}
