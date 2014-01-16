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

package master.flame.danmaku.danmaku.parser;

import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;

/**
 *
 */
public abstract class BaseDanmakuParser {
	
	protected IDataSource<?> mDataSource;
	
	protected DanmakuTimer mTimer;
	protected int mDispWidth;
	protected int mDispHeight;
	protected float mDispDensity;
	protected float mScaledDensity;
	
	protected int danmakuCount = -1;
	
	public BaseDanmakuParser setDisp(IDisplayer disp) {
		mDispWidth = disp.getWidth();
		mDispHeight = disp.getHeight();
		mDispDensity = disp.getDensity();
		mScaledDensity = disp.getScaledDensity();
		return this;
	}
	
	public BaseDanmakuParser load(IDataSource<?> source) {
		mDataSource = source;
		return this;
	}
	
	public BaseDanmakuParser setTimer(DanmakuTimer timer) {
		mTimer = timer;
		return this;
	}
	
	public int getDanmakuCount() {
		return danmakuCount;
	}
	
	public abstract Danmakus parse() throws IllegalDataException;	
}
