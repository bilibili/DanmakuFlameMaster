
package master.flame.danmaku.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.widget.VideoView;
import master.flame.danmaku.activity.R;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;

public class MainActivity extends Activity {

    private DanmakuSurfaceView mDanmakuView;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    private void findViews() {
        mDanmakuView = (DanmakuSurfaceView) findViewById(R.id.sv_danmaku);
        if (mDanmakuView != null) {
            mDanmakuView.enableDanmakuDrawingCache(true);
        }

        mVideoView = (VideoView) findViewById(R.id.videoview);
        if (mVideoView != null) {
            mVideoView.setVideoPath(Environment.getExternalStorageDirectory() + "/1.flv");
            mVideoView.requestFocus();
            mVideoView.start();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
