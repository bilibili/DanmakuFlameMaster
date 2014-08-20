package com.sample;

import android.app.Activity;
import android.os.Bundle;

import master.flame.danmaku.ui.widget.DanmakuGLSurfaceView;
import master.flame.danmaku.ui.widget.GLSurfaceViewTest;

public class GLMainActivity extends Activity {
    
    private GLSurfaceViewTest mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceViewTest(this);
        this.setContentView(mGLSurfaceView);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }
    
    

}
