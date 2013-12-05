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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import master.flame.danmaku.controller.CacheManagingDrawTask;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.DrawTask;
import master.flame.danmaku.controller.IDrawTask;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public class DanmakuSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        View.OnClickListener {

    public static final String TAG = "DanmakuSurfaceView";

    private Callback mCallback;

    private SurfaceHolder mSurfaceHolder;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private DanmakuTimer timer;

    private IDrawTask drawTask;

    private long mTimeBase;

    private boolean isSurfaceCreated;

    private boolean mEnableDanmakuDrwaingCache;

    private OnClickListener mOnClickListener;
    private BaseDanmakuParser mParser;
    private boolean mShowFps;

    public DanmakuSurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        if (timer == null) {
            timer = new DanmakuTimer();
        }
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

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void release() {
        stop();
        if (drawTask != null) {
            drawTask.quit();
        }
    }

    public void stop() {
        stopDraw();
    }

    private void stopDraw() {
        if (handler != null) {
            handler.quit();
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

    public void prepare() {
        if (mDrawThread == null) {
            mDrawThread = new HandlerThread("draw thread");
            mDrawThread.start();
            handler = new DrawHandler(mDrawThread.getLooper());
            handler.sendEmptyMessage(DrawHandler.PREPARE);
        }
        
    }

    public void prepare(BaseDanmakuParser parser) {
    	prepare();
        mParser = parser;
    }

    public void showFPS(boolean show){
        mShowFps = show;
    }

    void drawDanmakus() {
        if (!isSurfaceCreated)
            return;
        if(!isShown())
            return;
        long stime = System.currentTimeMillis();
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            DrawHelper.clearCanvas(canvas);
            drawTask.draw(canvas);
            if(mShowFps){
                long dtime = System.currentTimeMillis() - stime;
                String fps = String.format("fps %.2f", 1000 / (float) dtime);
                DrawHelper.drawText(canvas, fps);
            }
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
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

    public void pause() {
        if (handler != null)
            handler.quit();
    }

    public void resume() {
        if (handler != null && mDrawThread != null && handler.isStop())
            handler.sendEmptyMessage(DrawHandler.RESUME);
        else {
            restart();
        }
    }

    public void restart() {
        stop();
        start();
    }

    public void start() {
        if (handler == null) {
            prepare();
        }
        handler.sendEmptyMessage(DrawHandler.START);
    }

    @Override
    public void onClick(View view) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(view);
        }
    }

    public void seekTo(Long ms) {
        seekBy(ms - timer.currMillisecond);
    }

    public void seekBy(Long deltaMs) {
        if (handler != null) {
            handler.obtainMessage(DrawHandler.SEEK_POS, deltaMs).sendToTarget();
        }
    }

    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDanmakuDrwaingCache = enable;
    }

    private IDrawTask createTask(boolean useDrwaingCache, DanmakuTimer timer, Context context,
            int width, int height, IDrawTask.TaskListener taskListener) {
        IDrawTask task = useDrwaingCache ? new CacheManagingDrawTask(timer, context, width, height,
                taskListener, 1024 * 1024 * getMemoryClass(getContext()) / 3) : new DrawTask(timer,
                context, width, height, taskListener);
        task.setParser(mParser);
        task.prepare();
        return task;
    }

    public static int getMemoryClass(final Context context) {
        return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();
    }

    public interface Callback {
        public void prepared();

        public void updateTimer(DanmakuTimer timer);
    }

    public class DrawHandler extends Handler {
        private static final int START = 1;

        private static final int UPDATE = 2;

        private static final int RESUME = 3;

        private static final int SEEK_POS = 4;

        private static final int PREPARE = 5;

        private long pausedPostion = 0;

        private boolean quitFlag;

        private boolean mReady;

        public DrawHandler(Looper looper) {
            super(looper);
        }

        public void quit() {
            quitFlag = true;
        }

        public boolean isStop() {
            return quitFlag;
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case PREPARE:
                	if(mParser==null || !isSurfaceCreated){
                		sendEmptyMessageDelayed(PREPARE,100);
                	}else{
	                    prepare(new Runnable() {
	                        @Override
	                        public void run() {
	                            mReady = true;
	                            if (mCallback != null) {
	                                mCallback.prepared();
	                            }
	                        }
	                    });
                	}
                    break;
                case START:
                    pausedPostion = 0;
                case RESUME:
                    quitFlag = false;
                    if (mReady) {
                        mTimeBase = System.currentTimeMillis() - pausedPostion;
                        timer.update(pausedPostion);
                        sendEmptyMessage(UPDATE);
                    } else {
                        sendEmptyMessageDelayed(RESUME, 100);
                    }
                    break;
                case SEEK_POS:
                    Long deltaMs = (Long) msg.obj;
                    mTimeBase -= deltaMs;
                    drawTask.seek(System.currentTimeMillis() - mTimeBase);
                case UPDATE:
                    long d = timer.update(System.currentTimeMillis() - mTimeBase);
                    if (mCallback != null) {
                        mCallback.updateTimer(timer);
                    }
                    // long d = timer.lastInterval();
                    if (d == 0) {
                        if (!quitFlag)
                            sendEmptyMessageDelayed(UPDATE, 10);
                        return;
                    }
                    if (d < 15) {
                        if (d < 10) {
                            try {
                                Thread.sleep(15 - d);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    drawDanmakus();
                    if (!quitFlag)
                        sendEmptyMessage(UPDATE);
                    else {
                        pausedPostion = timer.currMillisecond;
                        Log.i(TAG, "stop draw: current = " + pausedPostion);
                    }
                    break;
            }
        }

        private void prepare(final Runnable runnable) {
            if (drawTask == null) {
                drawTask = createTask(mEnableDanmakuDrwaingCache, timer, getContext(), getWidth(),
                        getHeight(), new IDrawTask.TaskListener() {
                            @Override
                            public void ready() {
                                Log.i(TAG, "start drawing multiThread enabled:"
                                        + mEnableDanmakuDrwaingCache);
                                runnable.run();
                            }
                        });

            } else {
                runnable.run();
            }
        }

    }

}
