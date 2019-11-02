package master.flame.danmaku.gl.glview.view;

import android.content.Context;
import android.opengl.GLES20;
import android.support.annotation.NonNull;

import java.io.InputStream;

public final class GLShader {
    private String mVertexShaderProgram;
    private String mFragmentShaderProgram;

    private int mShaderProgram;
    private int mVertexShader;
    private int mFragmentShader;

    public GLShader(@NonNull String vertexShaderFileName, @NonNull String fragmentShaderFileName, Context context) {
        this(loadFromAssetsFile(vertexShaderFileName, context), loadFromAssetsFile(fragmentShaderFileName, context));
    }

    public GLShader(@NonNull String vertexShader, @NonNull String fragmentShader) {
        this.mVertexShaderProgram = vertexShader;
        this.mFragmentShaderProgram = fragmentShader;
    }

    public void create() {
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderProgram);
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderProgram);

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, mVertexShader);
            checkGlError("glAttachShader mVertexShader");
            GLES20.glAttachShader(program, mFragmentShader);
            checkGlError("glAttachShader mFragmentShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        mShaderProgram = program;
    }


    public void use() {
        GLES20.glUseProgram(mShaderProgram);
        checkGlError("glUseProgram " + mShaderProgram);
    }

    public void unuse() {
        GLES20.glUseProgram(0);
    }

    public int getProgram() {
        return mShaderProgram;
    }

    public int getAttributeLocation(String name) {
        return GLES20.glGetAttribLocation(mShaderProgram, name);
    }

    public int getUniformLocation(String name) {
        return GLES20.glGetUniformLocation(mShaderProgram, name);
    }

    public void onDestroy() {
        GLES20.glDeleteShader(mVertexShader);
        GLES20.glDeleteShader(mFragmentShader);
        GLES20.glDeleteProgram(mShaderProgram);
    }


    private void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            throw new RuntimeException(msg);
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private static String loadFromAssetsFile(String name, Context context) {
        StringBuilder result = new StringBuilder();
        try {
            InputStream is = context.getResources().getAssets().open(name);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
            is.close();
        } catch (Exception e) {
            return "";
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }
}
