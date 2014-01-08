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

package master.flame.danmaku.controller;

import android.graphics.Canvas;

import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDrawTask {

    public void addDanmaku(BaseDanmaku item);

    public void draw(Canvas canvas);

    public void reset();

    public void seek(long mills);

    public void start();

    public void quit();

    public void prepare() throws IllegalDataException;

    public void setParser(BaseDanmakuParser parser);

    public interface TaskListener {
        public void ready();
    }

}
