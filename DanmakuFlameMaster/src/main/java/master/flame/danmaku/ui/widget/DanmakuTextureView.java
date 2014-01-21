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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

/**
 * DanmakuTextureView需要开启GPU加速才能显示弹幕
 * 很遗憾...经过测试TextureView没有提升绘制速度,也许哪里用的不对
 * @author ch
 *
 */
public class DanmakuTextureView extends TextureView implements IDanmakuView,
        TextureView.SurfaceTextureListener, View.OnClickListener {

    public static final String TAG = "DanmakuSurfaceView";

    private Callback mCallback;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache;

    private OnClickListener mOnClickListener;

    private boolean mShowFps;

    public DanmakuTextureView(Context context) {
        super(context);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        setOpaque(false);
        setSurfaceTextureListener(this);
        setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (l != this) {
            mOnClickListener = l;
        } else
            super.setOnClickListener(l);
    }

    public DanmakuTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) {
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
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        isSurfaceCreated = true;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        isSurfaceCreated = false;
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        isSurfaceCreated = true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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
            if (handler.drawTask != null) {
                handler.drawTask.quit();
            }
            handler.quit();
            handler.getLooper().quit();
            handler = null;
        }
        if (mDrawThread != null) {
            mDrawThread.quit();
            try {
                mDrawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrawThread = null;
        }
    }

    private void prepare() {
        if (mDrawThread == null) {
            mDrawThread = new HandlerThread("draw thread");
            mDrawThread.start();
            handler = new DrawHandler(mDrawThread.getLooper(), this);
        }
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
    public void showFPS(boolean show) {
        mShowFps = show;
    }

    @Override
    public long drawDanmakus() {
        if (!isSurfaceCreated)
            return 0;
        if (!isShown())
            return 0;
        long stime = System.currentTimeMillis();
        long dtime = 0;
        Canvas canvas = lockCanvas();
        if (canvas != null) {
            handler.drawTask.draw(canvas);
            dtime = System.currentTimeMillis() - stime;
            if (mShowFps) {
                String fps = String.format("%02d MS, fps %.2f", dtime, 1000 / (float) dtime);
                DrawHelper.drawText(canvas, fps);
            }
            if (isSurfaceCreated)
                unlockCanvasAndPost(canvas);
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
            handler.quit();
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
        } else {
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
        if (handler != null) {
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

}
