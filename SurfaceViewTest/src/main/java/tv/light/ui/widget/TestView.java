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

import tv.light.controller.DrawHelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TestView extends View {

    private long startTime;

    private float cx;

    private float cy;

    private HandlerThread mDrawThread;

    private DrawHandler handler;

    private long avgDuration;

    private long maxDuration;

    public TestView(Context context) {
        super(context);
        init();
    }

    public TestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TestView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // if (event.getAction() == MotionEvent.ACTION_UP) {
        // quitDrawThread();
        // }

        updateCxCy(event.getX(), event.getY());

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTime(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            startDraw();
        }
    }

    private void updateCxCy(float x, float y) {
        cx = x;
        cy = y;
    }

    private void drawTime(Canvas canvas) {
        drawSomeThing(System.currentTimeMillis() + "ms", canvas);

    }

    void drawSomeThing(String text, Canvas canvas) {
        if (startTime <= 0) {
            startTime = System.currentTimeMillis();
        }
        long temp;
        Log.e("", "cycle:" + (temp = System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

        if (canvas != null) {

            // DrawHelper.clearCanvas(canvas);
            DrawHelper.drawDuration(
                    canvas,
                    String.valueOf(maxDuration = Math.max(temp, maxDuration)) + ":"
                            + String.valueOf(avgDuration = (temp + avgDuration) / 2) + ":"
                            + String.valueOf(temp));
            DrawHelper.drawText(canvas, text);
            DrawHelper.drawCircle(cx, cy, canvas);
            Log.e("draw Time", "draw time:" + (System.currentTimeMillis() - startTime));
        }
        this.invalidate();
    }

    private void startDraw() {
        mDrawThread = new HandlerThread("draw thread");
        mDrawThread.start();
        handler = new DrawHandler(mDrawThread.getLooper());
        handler.sendEmptyMessage(DrawHandler.START);
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
                        postInvalidate();
                        sendEmptyMessageDelayed(UPDATE, 0);
                    }
                    break;
            }
        }

    }
}
