
package com.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import tv.cjump.gl.glrenderer.BasicTexture;
import tv.cjump.gl.glrenderer.BitmapTexture;
import tv.cjump.gl.glrenderer.GLCanvas;
import tv.cjump.gl.glrenderer.GLES20Canvas;
import tv.cjump.gl.glrenderer.GLPaint;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SampleGLView extends GLSurfaceView {

    public SampleGLView(Context context) {
        super(context);
        init();
    }

    public SampleGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("NewApi")
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

        public MyRenderer() {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            texture = new BitmapTexture(bitmap);
            texture.setOpaque(false);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // This method will be called EVERY TIME our application resumes.
            // When the app pauses, the surfaces and all of the OpenGL resources
            // are freed (textures, etc). Since we don't use anything special in
            // this sample, we don't need to do anything.

            canvas = new GLES20Canvas();
            // canvas.setAlpha(0.9f);
            BasicTexture.invalidateAllTextures();

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // Called when the surface change size or right after it's created.
            // Generally, you will set Viewport and camera options here.
            canvas.setSize(width, height);
        }

        GLCanvas canvas = null;

        GLPaint paint = new GLPaint();

        float[] color = new float[] {
                1.0f, 1.0f, 0.0f, 0.5f
        };

        @Override
        public void onDrawFrame(GL10 gl) {
            // This is the drawing of every app frame. Nothing really special
            // here,
            // We just set the clearColor to random values and clear the screen
            // using
            // that color.

            canvas.clearBuffer(color);
            texture.draw(canvas, 100, 100);
        }

    }

}
