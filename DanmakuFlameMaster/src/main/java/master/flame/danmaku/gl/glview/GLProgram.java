package master.flame.danmaku.gl.glview;

import android.content.Context;

import master.flame.danmaku.gl.glview.view.GLShader;

public class GLProgram {
    private Context mAppContext;
    public GLShader mShader;
    public int glHPosition;
    public int glHCoordinate;
    public int glHTexture;
    public int glHProject;
    public int glHView;
    public int glHModel;
    public int glHRotate;
    public int glAlpha;

    public GLProgram(Context context) {
        mAppContext = context.getApplicationContext();
    }

    /**
     * gl线程
     */
    public void load() {
        mShader = new GLShader("gl/glview.vert", "gl/glview.frag", mAppContext);
        mShader.create();

        glHPosition = mShader.getAttributeLocation("vPosition");
        glHCoordinate = mShader.getAttributeLocation("vCoordinate");
        glHTexture = mShader.getUniformLocation("vTexture");
        glHProject = mShader.getUniformLocation("vProject");
        glHView = mShader.getUniformLocation("vView");
        glHModel = mShader.getUniformLocation("vModel");
        glHRotate = mShader.getUniformLocation("vRotate");
        glAlpha = mShader.getUniformLocation("alpha");
    }

    /**
     * gl线程
     */
    public void unload() {
        mShader.onDestroy();
    }

    /**
     * gl线程
     */
    public void use() {
        mShader.use();
    }

    /**
     * gl线程
     */
    public void unuse() {
        mShader.unuse();
    }
}
