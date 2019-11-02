package com.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.sample.gl.DanmakuActivity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_entrance);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_enter_glsurfaceview:
                Intent intent = new Intent();
                intent.setClass(this, DanmakuActivity.class);
                intent.putExtra(DanmakuActivity.TYPE, DanmakuActivity.TYPE_DANMAKU_GL_VIEW);
                startActivity(intent);
                break;
            case R.id.btn_enter_view:
                intent = new Intent();
                intent.setClass(this, DanmakuActivity.class);
                intent.putExtra(DanmakuActivity.TYPE, DanmakuActivity.TYPE_DANMAKU_VIEW);
                startActivity(intent);
                break;
            case R.id.btn_bili_enter_view:
                intent = new Intent();
                intent.setClass(this, BiliMainActivity.class);
                startActivity(intent);
                break;
        }
    }
}
