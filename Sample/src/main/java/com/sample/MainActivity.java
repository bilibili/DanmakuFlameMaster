
package com.sample;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.PopupWindow;
import android.widget.VideoView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.AcFunDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView.Callback;

import com.sample.R;

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
    
	private BaseDanmakuParser createParser(InputStream stream) {
		ILoader loader = DanmakuLoaderFactory
				.create(DanmakuLoaderFactory.TAG_BILI);

		try {
			loader.load(stream);
		} catch (IllegalDataException e) {
			e.printStackTrace();
		}
		BaseDanmakuParser parser = new BiliDanmukuParser();
		IDataSource<?> dataSource = loader.getDataSource();
		parser.load(dataSource);
		return parser;

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

                if (mVideoView != null) {
                    mVideoView.start();
                }
            }
        });

        // VideoView
        mVideoView = (VideoView) findViewById(R.id.videoview);
        // DanmakuView
        mDanmakuView = (DanmakuSurfaceView) findViewById(R.id.sv_danmaku);
        if (mDanmakuView != null) {

			BaseDanmakuParser parser = createParser(this.getResources()
					.openRawResource(R.raw.comments));
			mDanmakuView.prepare(parser);

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
                    } else{
                        mPopupWindow.showAtLocation(mDanmakuView, Gravity.NO_GRAVITY, 0, 0);
                    }

                    if (mVideoView != null) {
                        mVideoView.pause();
                    }
                }
            });
            

        }


        if (mVideoView != null) {
            mVideoView.setVideoPath(Environment.getExternalStorageDirectory() + "/1.flv");
            //mVideoView.setVideoPath("http://edge.v.iask.com/44027740.hlv?KID=sina,viask&Expires=1380384000&ssig=d3Xzxbv1fI");
        }

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mDanmakuView.start();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
