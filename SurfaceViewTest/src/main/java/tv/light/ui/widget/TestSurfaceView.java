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

package tv.light.ui.widget;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class TestSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mSurfaceHolder;

    private Paint paint;

    private HandlerThread mDrawThread;

    private DrawHandler handler;
    private long startTime;

    public TestSurfaceView(Context context) {
        super(context);
        init();
    }

    public TestSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TestSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(50);
        setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    void drawText(Canvas canvas, String text) {

        canvas.drawText(text, 50, 50, paint);

    }

    void drawCanvas(String text) {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            Log.e("", "cycle:" + (System.currentTimeMillis() - startTime));
            startTime = System.currentTimeMillis();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            drawText(canvas, text);
            Log.e("draw Time", "draw time:" + (System.currentTimeMillis() - startTime));
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startDraw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        // startDraw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // if (event.getAction() == MotionEvent.ACTION_UP) {
        quitDrawThread();
        // }
        return super.onTouchEvent(event);
    }

    private void startDraw() {
        mDrawThread = new HandlerThread("draw thread");
        mDrawThread.start();
        handler = new DrawHandler(mDrawThread.getLooper());
        handler.sendEmptyMessage(DrawHandler.START);
    }

    private void quitDrawThread() {
        if (handler != null) {
            handler.quit();// .sendEmptyMessage(DrawHandler.STOP);
            handler = null;
        }
        if (mDrawThread != null) {
            mDrawThread.quit();
            mDrawThread = null;
        }
    }

    private void drawTime() {
        drawCanvas(System.currentTimeMillis() + "ms");
    }

    public class DrawHandler extends Handler {

        private static final int START = 1;

        private static final int UPDATE = 2;

        private boolean quitFlag;

        public DrawHandler(Looper looper) {
            super(looper);
        }

        public void quit() {
            quitFlag = true;
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case START:
                    quitFlag = false;
                    sendEmptyMessage(UPDATE);
                    break;
                case UPDATE:
                    if (!quitFlag) {
                        drawTime();
                        sendEmptyMessageDelayed(UPDATE, 10);
                    }
                    break;
            }
        }

    }

}
