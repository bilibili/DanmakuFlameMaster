
package master.flame.danmaku.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.*;
import android.widget.PopupWindow;
import android.widget.VideoView;
import master.flame.danmaku.activity.R;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;

public class MainActivity extends Activity {

    private DanmakuSurfaceView mDanmakuView;

    private VideoView mVideoView;

    private View mMediaController;

    public PopupWindow mPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    private void findViews() {
        LayoutInflater mLayoutInflater = getLayoutInflater();
        mMediaController = mLayoutInflater.inflate(R.layout.media_controller, null);
        mMediaController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }
                if (mDanmakuView != null) {
                    mDanmakuView.resume();
                }
            }
        });

        mDanmakuView = (DanmakuSurfaceView) findViewById(R.id.sv_danmaku);
        if (mDanmakuView != null) {
            mDanmakuView.enableDanmakuDrawingCache(true);
            mDanmakuView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (mPopupWindow == null) {
                        mPopupWindow = new PopupWindow(mMediaController,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);
                    }

                    if (mPopupWindow.isShowing()) {
                        mPopupWindow.dismiss();
                    } else
                        mPopupWindow.showAtLocation(mDanmakuView, Gravity.NO_GRAVITY, 0, 0);
                }
            });
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
