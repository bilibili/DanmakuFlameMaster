package com.sample;

import java.io.InputStream;

import master.flame.danmaku.controller.DMSiteType;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends Activity {

    private DanmakuSurfaceView mDanmakuView;

    private VideoView mVideoView;

    private View mMediaController;

    public PopupWindow mPopupWindow;
    
    DMSiteType mType = DMSiteType.BILI;
    String mVideoPath = "http://f.youku.com/player/getFlvPath/sid/1387355677398_00/st/mp4/fileid/030008010052AF8EC8DF6B13A1F5D931F954B8-E615-896C-F13C-ADC840C07D71?K=302bf9e26b1554a02411a421,k2:1bc34fddf78cbd103";

//    String mVideoPath = "/sdcard/Download/0.mp4";
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mVideoPath == null) {
            Toast.makeText(this, "Please edit MainActivity sample, and set mVideoPath variable to your media file URL/path", Toast.LENGTH_LONG).show();
            return;
        }
        findViews();
        loadDanmakus("http://comment.bilibili.tv/1269904.xml");
    }
	private void loadDanmakus(final String url) {
	    
		final ILoader loader = mType.getLoader();
		new Thread(){
		    public void run() {
		        try {
		            loader.load(url);
		            runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BaseDanmakuParser parser = mType.getParser();
                            IDataSource<?> dataSource = loader.getDataSource();
                            parser.load(dataSource);
                            mDanmakuView.prepare(parser);
                        }
                    });
		        } catch (IllegalDataException e) {
		            e.printStackTrace();
		        }
		    }
		}.start();
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
            mDanmakuView.showFPS(true);
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
        	 mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                 @Override
                 public void onPrepared(MediaPlayer mediaPlayer) {
                     mediaPlayer.start();
                     mDanmakuView.start();
                 }
             });
            mVideoView.setVideoPath(mVideoPath);
        }


    }

    @Override
    protected void onDestroy() {
        if(mDanmakuView!=null){
            mDanmakuView.release();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
