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

package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public class DanmakuSurfaceView extends SurfaceView implements IDanmakuView, SurfaceHolder.Callback,
        View.OnClickListener {

    public static final String TAG = "DanmakuSurfaceView";

    private Callback mCallback;

    private SurfaceHolder mSurfaceHolder;

    private HandlerThread mDrawThread;

    private DrawHandler handler;
    
    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache = true;

    private OnClickListener mOnClickListener;
    
    private boolean mShowFps;

    private boolean mDanmakuVisibile = true;

    public DanmakuSurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setZOrderMediaOverlay(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (l != this) {
            mOnClickListener = l;
        } else
            super.setOnClickListener(l);
    }

    public DanmakuSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void addDanmaku(BaseDanmaku item) {
        if(handler != null){
            handler.addDanmaku(item);
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
        if (handler != null) {
            handler.setCallback(callback);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	isSurfaceCreated = true;
    	Canvas canvas = surfaceHolder.lockCanvas();
    	if(canvas!=null){
    	    DrawHelper.clearCanvas(canvas);
    	    surfaceHolder.unlockCanvasAndPost(canvas);
    	}
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isSurfaceCreated = false;
    }

    @Override
    public void release() {
        stop();
    }

    @Override
    public void stop() {
        stopDraw();
    }

    private void stopDraw() {
        if (handler != null) {
            handler.quit();
            handler = null;
        }
        if (mDrawThread != null) {
            try {
                mDrawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrawThread.quit();
            mDrawThread = null;
        }
    }

    private void prepare() {
        if (mDrawThread == null) {
            mDrawThread = new HandlerThread("draw thread");
            mDrawThread.start();
        }
        if (handler == null)
            handler = new DrawHandler(mDrawThread.getLooper(), this, mDanmakuVisibile);
    }

    @Override
    public void prepare(BaseDanmakuParser parser) {
    	prepare();
        handler.setParser(parser);
        handler.setCallback(mCallback);
        handler.prepare();
    }

    @Override
    public boolean isPrepared() {
        return handler != null && handler.isPrepared();
    }

    @Override
    public void showFPS(boolean show){
        mShowFps = show;
    }

    @Override
    public long drawDanmakus() {
        if (!isSurfaceCreated)
            return 0;
        if (!isShown())
            return -1;
        long stime = System.currentTimeMillis();
        long dtime = 0;
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            handler.drawTask.draw(canvas);
            dtime = System.currentTimeMillis() - stime;
            if (mShowFps) {
                String fps = String.format("%02d MS, fps %.2f", dtime, 1000 / (float) dtime);
                DrawHelper.drawFPS(canvas, fps);
            }
            if (isSurfaceCreated)
                mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
        return dtime;
    }

    public void toggle() {
        if (isSurfaceCreated) {
            if (handler == null)
                start();
            else if (handler.isStop()) {
                resume();
            } else
                pause();
        }
    }

    @Override
    public void pause() {
        if (handler != null)
            handler.pause();
    }

    @Override
    public void resume() {
        if (handler != null && mDrawThread != null && handler.isPrepared())
            handler.resume();
        else {
            restart();
        }
    }

    public void restart() {
        stop();
        start();
    }

    @Override
    public void start() {
        start(0);
    }

    @Override
    public void start(long postion) {
        if (handler == null) {
            prepare();
        }else{
            handler.removeCallbacksAndMessages(null);
        }
        handler.obtainMessage(DrawHandler.START, postion).sendToTarget();
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(view);
        }
    }

    public void seekTo(Long ms) {
        if(handler != null){
            handler.seekTo(ms);
        }
    }


    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDanmakuDrwaingCache = enable;
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        return mEnableDanmakuDrwaingCache;
    }

    @Override
    public boolean isViewReady() {
        return isSurfaceCreated;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {
        mDanmakuVisibile = true; 
        if (handler == null) {
            return;
        }
        handler.showDanmakus();
    }

    @Override
    public void hide() {
        mDanmakuVisibile = false;
        if (handler == null) {
            return;
        }
        handler.hideDanmakus();
    }

    @Override
    public void clear() {
        if (!isViewReady()) {
            return;
        }
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            DrawHelper.clearCanvas(canvas);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean isShown() {
        if (handler == null || !isViewReady()) {
            return false;
        }
        return handler.getVisibility();
    }


}
