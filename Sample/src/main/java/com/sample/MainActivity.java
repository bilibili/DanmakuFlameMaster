
package com.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.VideoView;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.DanmakuGlobalConfig;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser;
import master.flame.danmaku.ui.widget.DanmakuSurfaceView;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements View.OnClickListener {

    private IDanmakuView mDanmakuView;

    private View mMediaController;

    public PopupWindow mPopupWindow;
    
    private Button mBtnRotate;

    private Button mBtnHideDanmaku;

    private Button mBtnShowDanmaku;

    private BaseDanmakuParser mParser;

    private Button mBtnPauseDanmaku;

    private Button mBtnResumeDanmaku;

    private Button mBtnSendDanmaku;

    private long mPausedPosition;

    private Button mBtnSendDanmakus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    private BaseDanmakuParser createParser(InputStream stream) {
        
        if(stream==null){
            return new BaseDanmakuParser() {
                
                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
            
        
        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

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

        mMediaController = findViewById(R.id.media_controller);
        mBtnRotate = (Button) findViewById(R.id.rotate);
        mBtnHideDanmaku = (Button) findViewById(R.id.btn_hide);
        mBtnShowDanmaku = (Button) findViewById(R.id.btn_show);
        mBtnPauseDanmaku = (Button) findViewById(R.id.btn_pause);
        mBtnResumeDanmaku = (Button) findViewById(R.id.btn_resume);
        mBtnSendDanmaku = (Button) findViewById(R.id.btn_send);
        mBtnSendDanmakus = (Button) findViewById(R.id.btn_send_danmakus);
        mBtnRotate.setOnClickListener(this);
        mBtnHideDanmaku.setOnClickListener(this);
        mMediaController.setOnClickListener(this);
        mBtnShowDanmaku.setOnClickListener(this);
        mBtnPauseDanmaku.setOnClickListener(this);
        mBtnResumeDanmaku.setOnClickListener(this);
        mBtnSendDanmaku.setOnClickListener(this);
        mBtnSendDanmakus.setOnClickListener(this);
        
        // VideoView
        VideoView mVideoView = (VideoView) findViewById(R.id.videoview);
        // DanmakuView
        mDanmakuView = (DanmakuSurfaceView) findViewById(R.id.sv_danmaku);
        DanmakuGlobalConfig.DEFAULT.setDanmakuStyle(DanmakuGlobalConfig.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false);
        if (mDanmakuView != null) {
            mParser = createParser(this.getResources().openRawResource(R.raw.comments));
            mDanmakuView.setCallback(new Callback() {

                @Override
                public void updateTimer(DanmakuTimer timer) {

                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
            mDanmakuView.prepare(mParser);

            mDanmakuView.showFPS(true);
            mDanmakuView.enableDanmakuDrawingCache(true);
            ((View) mDanmakuView).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    mMediaController.setVisibility(View.VISIBLE);
                }
            });
        }

        if (mVideoView != null) {
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            mVideoView.setVideoPath(Environment.getExternalStorageDirectory() + "/1.flv");
        }

    }

    @Override
    protected void onDestroy() {
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mMediaController) {
            mMediaController.setVisibility(View.GONE);
        }
        if (mDanmakuView == null || !mDanmakuView.isPrepared())
            return;
        if (v == mBtnRotate) {
            setRequestedOrientation(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (v == mBtnHideDanmaku) {
            mDanmakuView.hide();
            //mPausedPosition = mDanmakuView.hideAndPauseDrawTask();
        } else if (v == mBtnShowDanmaku) {
            mDanmakuView.show(); 
            //mDanmakuView.showAndResumeDrawTask(mPausedPosition); // sync to the video time in your practice
        } else if (v == mBtnPauseDanmaku) {
            mDanmakuView.pause();
        } else if (v == mBtnResumeDanmaku) {
            mDanmakuView.resume();
        } else if (v == mBtnSendDanmaku) {
            addDanmaku(false);
        } else if (v == mBtnSendDanmakus) {
            Boolean b = (Boolean) mBtnSendDanmakus.getTag();
            timer.cancel();
            if(b == null || !b) {
                mBtnSendDanmakus.setText(R.string.cancel_sending_danmakus);
                timer = new Timer();                
                timer.schedule(new AsyncAddTask(), 0, 1000);
                mBtnSendDanmakus.setTag(true);
            } else {
                mBtnSendDanmakus.setText(R.string.send_danmakus);
                mBtnSendDanmakus.setTag(false);
            }
        }
    }
    
    Timer timer = new Timer();
    class AsyncAddTask extends TimerTask {
        
        @Override
        public void run() {
            for(int i = 0;i<5;i++) {
                addDanmaku(true);
                SystemClock.sleep(20);
            }
        }
    };
    
    

    private void addDanmaku(boolean islive) {
        BaseDanmaku danmaku = DanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        //for(int i=0;i<100;i++){
        //}
        danmaku.text = "这是一条弹幕" + System.nanoTime();
        danmaku.padding = 5;
        danmaku.priority = 1;
        danmaku.isLive = islive;
        danmaku.time = mDanmakuView.getCurrentTime() + 1200;
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = Color.WHITE;
        //danmaku.underlineColor = Color.GREEN;
        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
    }

}
