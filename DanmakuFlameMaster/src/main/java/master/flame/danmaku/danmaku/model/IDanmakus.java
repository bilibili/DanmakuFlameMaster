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

package master.flame.danmaku.danmaku.model;

import java.util.Collection;
import java.util.Comparator;

import master.flame.danmaku.danmaku.util.DanmakuUtils;

public interface IDanmakus {

    abstract class Consumer<Progress, Result> {

        public static final int ACTION_CONTINUE = 0;
        public static final int ACTION_BREAK = 1;
        public static final int ACTION_REMOVE = 2;
        public static final int ACTION_REMOVE_AND_BREAK = 3;

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @return next action of the loop
         *
         * @see #ACTION_CONTINUE
         * @see #ACTION_BREAK
         * @see #ACTION_REMOVE
         */
        public abstract int accept(Progress t);

        public void before() {

        }

        public void after() {

        }

        public Result result() {
            return null;
        }
    }

    abstract class DefaultConsumer<Progress> extends Consumer<Progress, Void> {

    }

    int ST_BY_TIME = 0;

    int ST_BY_YPOS = 1;

    int ST_BY_YPOS_DESC = 2;

    /**
     * this type is used to iterate/remove/insert elements, not support sub/subnew
     */
    int ST_BY_LIST = 4;


    boolean addItem(BaseDanmaku item);

    boolean removeItem(BaseDanmaku item);
    
    IDanmakus subnew(long startTime, long endTime);

    IDanmakus sub(long startTime, long endTime);

    int size();

    void clear();
    
    BaseDanmaku first();
    
    BaseDanmaku last();
    
    boolean contains(BaseDanmaku item);

    boolean isEmpty();
    
    void setSubItemsDuplicateMergingEnabled(boolean enable);

    Collection<BaseDanmaku> getCollection();

    void forEachSync(Consumer<? super BaseDanmaku, ?> consumer);

    void forEach(Consumer<? super BaseDanmaku, ?> consumer);

    Object obtainSynchronizer();

    class BaseComparator implements Comparator<BaseDanmaku> {

        protected boolean mDuplicateMergingEnable;

        public BaseComparator(boolean duplicateMergingEnabled) {
            setDuplicateMergingEnabled(duplicateMergingEnabled);
        }

        public void setDuplicateMergingEnabled(boolean enable) {
            mDuplicateMergingEnable = enable;
        }

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            if (mDuplicateMergingEnable && DanmakuUtils.isDuplicate(obj1, obj2)) {
                return 0;
            }
            return DanmakuUtils.compare(obj1, obj2);
        }

    }

    class TimeComparator extends BaseComparator {

        public TimeComparator(boolean duplicateMergingEnabled) {
            super(duplicateMergingEnabled);
        }

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            return super.compare(obj1, obj2);
        }
    }

    class YPosComparator extends BaseComparator {

        public YPosComparator(boolean duplicateMergingEnabled) {
            super(duplicateMergingEnabled);
        }

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            if (mDuplicateMergingEnable && DanmakuUtils.isDuplicate(obj1, obj2)) {
                return 0;
            }
            return Float.compare(obj1.getTop(), obj2.getTop());
        }
    }

    class YPosDescComparator extends BaseComparator {

        public YPosDescComparator(boolean duplicateMergingEnabled) {
            super(duplicateMergingEnabled);
        }

        @Override
        public int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
            if (mDuplicateMergingEnable && DanmakuUtils.isDuplicate(obj1, obj2)) {
                return 0;
            }
            return Float.compare(obj2.getTop(), obj1.getTop());
        }
    }

}
