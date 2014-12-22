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

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import master.flame.danmaku.danmaku.model.android.BitmapHolder;
import master.flame.danmaku.danmaku.model.android.GLESCanvas;
import master.flame.danmaku.danmaku.model.android.SimplePaint;
import tv.cjump.gl.glrenderer.BasicTexture;
import tv.cjump.gl.glrenderer.BitmapTexture;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 * DanmakuGLSurfaceView
 * 
 * @author ch
 */
public class GLSurfaceViewTest extends GLSurfaceView {

    public GLSurfaceViewTest(Context context) {
        super(context);
        init();
    }

    public GLSurfaceViewTest(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(new MyRenderer());
    }

    private class MyRenderer implements Renderer {

        // Random number generator used to set the background color
        private Random r = new Random();
        private Bitmap bitmap;
        private BitmapTexture texture;
        private BitmapHolder holder;
        
        public MyRenderer() {
            //bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.);

        }
        
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // This method will be called EVERY TIME our application resumes.
            // When the app pauses, the surfaces and all of the OpenGL resources
            // are freed (textures, etc). Since we don't use anything special in
            // this sample, we don't need to do anything.
            
            
           
           //canvas.setAlpha(0.9f);
           BasicTexture.invalidateAllTextures();
           paint.setColor(Color.GREEN);
           paint.setStyle(Style.FILL);
           paint.setTextSize(50);
           paint.setStrokeCap(Paint.Cap.BUTT);
           paint.setStrokeWidth(6);
           paint.setStrokeJoin(Paint.Join.MITER);
           paint.setStrokeMiter(2.5f);
           //paint.setStrokeJoin(Paint.Join.ROUND);
           
           bitmap = Bitmap.createBitmap(256, 256, Config.ARGB_8888);
           Canvas canvas = new Canvas(bitmap);
           canvas.drawColor(Color.RED);
           canvas.drawText(text, 0, 30, paint);
           holder = new BitmapHolder(bitmap);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // Called when the surface change size or right after it's created.
            // Generally, you will set Viewport and camera options here.
            if(canvas == null || canvas.getWidth() != width || canvas.getHeight()!= height){
                canvas = new GLESCanvas(2,(GL11)gl,width,height);
            }
        }

        GLESCanvas canvas = null;
        SimplePaint paint = new SimplePaint();
        float[] color = new float[]{1.0f,1.0f,0.0f,0.5f};
        String text = "GLES中文Texture绘制";
        
        @Override
        public void onDrawFrame(GL10 gl) {
            // This is the drawing of every app frame. Nothing really special here,
            // We just set the clearColor to random values and clear the screen using
            // that color.
            canvas.clear();
            canvas.drawRect(100, 0, 299, 400, paint);
            
//            canvas.clear(120, 0, 299, 400);
//            paint.setColor(Color.RED);
            //canvas.drawText(text, 500, 400, paint);
            canvas.drawBitmap(holder, 300, 0, paint);
            canvas.drawText("ssss" + System.currentTimeMillis(), 10, 200, paint);
            //canvas.clearBuffer(color);
            //texture.draw(canvas, 100, 100);
        }

    }
}
