package com.sample.gl;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.sample.BiliDanmukuParser;
import com.sample.R;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.gl.AndroidGLDisplayer;
import master.flame.danmaku.gl.utils.SpeedsMeasurement;

public class DanmakuActivity extends Activity {
    public static final String TYPE = "danmaku_type";
    public static final int TYPE_DANMAKU_VIEW = 1;
    public static final int TYPE_DANMAKU_GL_VIEW = 2;
    private static final String XML_PARSE_LOAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><i><maxlimit>800</maxlimit></i>";

    protected IDanmakuView mNormalDanmakuView;
    private DanmakuContext mNormalDanmakuContext;
    private SpannedCacheStuffer mSpannedCacheStuffer = new SpannedCacheStuffer();
    private BaseDanmakuParser mParser;
    int mDanmakuType;
    EditText time;
    FrequencyDanmakuSender frequencyDanmakuSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDanmakuType = getIntent().getIntExtra(TYPE, TYPE_DANMAKU_GL_VIEW);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.layout_danmaku);

        if (mDanmakuType == TYPE_DANMAKU_GL_VIEW) {
            mNormalDanmakuView = (IDanmakuView) findViewById(R.id.danmaku_gl);
        } else {
            mNormalDanmakuView = (IDanmakuView) findViewById(R.id.danmaku_view);
        }
        ((View) mNormalDanmakuView).setVisibility(View.VISIBLE);
        time = (EditText) findViewById(R.id.et_time);
        frequencyDanmakuSender = new FrequencyDanmakuSender();
        frequencyDanmakuSender.init(mNormalDanmakuView, time);
        initNormalDanmuView();

    }


    @Override
    public void onResume() {
        super.onResume();
        mNormalDanmakuView.resume();
        frequencyDanmakuSender.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNormalDanmakuView.pause();
        frequencyDanmakuSender.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNormalDanmakuView.release();
    }


    private void initNormalDanmuView() {
        mNormalDanmakuContext = DanmakuContext.create();
        mNormalDanmakuContext.cachingPolicy.mAllowDelayInCacheModel = true;
        if (mDanmakuType == TYPE_DANMAKU_GL_VIEW) {
            mNormalDanmakuContext.mDisplayer = new AndroidGLDisplayer(mNormalDanmakuContext);
        }
        mNormalDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_SHADOW, 10)
                .setMaximumVisibleSizeInScreen(200)
                // 图文混排使用SpannedCacheStuffer;
                .setCacheStuffer(mSpannedCacheStuffer, null);

        mNormalDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
            @Override
            public void updateTimer(DanmakuTimer timer) {
            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {
            }

            @Override
            public void drawingFinished() {
            }

            @Override
            public void prepared() {
                mNormalDanmakuView.start();
            }
        });

        mNormalDanmakuView.enableDanmakuDrawingCache(true);

        try {
            mParser = createParser(string2InputStream(XML_PARSE_LOAD));
        } catch (Exception e) {
        }

        if (mParser != null) {
            mNormalDanmakuView.prepare(mParser, mNormalDanmakuContext);
        }
        mNormalDanmakuView.show();
    }

    ///Danmaku test
    private BaseDanmakuParser createParser(InputStream stream) {
        if (stream == null) {
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

    public static InputStream string2InputStream(String in) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes("UTF-8"));
        return is;
    }
    private static class FrequencyDanmakuSender implements TextWatcher, Runnable {
        private int mSpeed = 0;
        private boolean mRunning = false;
        private Handler mHandler = new Handler();
        protected IDanmakuView mNormalDanmakuView;
        private Random rand = new Random();
        private Duration mDuration = new Duration(6000);
        private HandlerThread handlerThread;
        private SpeedsMeasurement speedsMeasurement = new SpeedsMeasurement("MainSenderSpeed");

        FrequencyDanmakuSender() {
            handlerThread = new HandlerThread("sender");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }

        public void init(IDanmakuView danmakuView, TextView textView) {

            this.mNormalDanmakuView = danmakuView;
            textView.addTextChangedListener(this);
            try {
                mSpeed = Integer.parseInt(textView.getText().toString());
                mHandler.removeCallbacksAndMessages(null);
                if (mRunning && mSpeed > 0) {
                    mHandler.post(this);
                }
            } catch (Exception ignore) {
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            try {
                mSpeed = Integer.parseInt(s.toString());
                mHandler.removeCallbacksAndMessages(null);
                if (mRunning && mSpeed > 0) {
                    mHandler.post(this);
                }
            } catch (Exception ignore) {
            }
        }

        @Override
        public void run() {
            if (!mNormalDanmakuView.isPaused()) {
                // 根据position创建弹幕展示的类型
                int danmaType = BaseDanmaku.TYPE_SCROLL_RL;
                String position = String.valueOf((1 + rand.nextInt(4)));
                if ("2".equals(position)) {
                    danmaType = BaseDanmaku.TYPE_FIX_TOP;
                } else if ("3".equals(position)) {
                    danmaType = BaseDanmaku.TYPE_FIX_BOTTOM;
                } else if ("4".equals(position)) {
                    danmaType = BaseDanmaku.TYPE_SCROLL_LR;
                }
                BaseDanmaku danmaku = mNormalDanmakuView.getConfig().mDanmakuFactory.createDanmaku(danmaType);
                if (danmaku == null) {
                    return;
                }

                danmaku.isLive = false;

                danmaku.text = getRandomString(Math.abs(rand.nextInt() % 20) + 5);

                danmaku.priority = 1;
                danmaku.padding = 5;
                danmaku.setTime(mNormalDanmakuView.getCurrentTime() + 20 + rand.nextInt(20));
                danmaku.textSize = 30 + rand.nextInt(30);
                danmaku.textColor = rand.nextInt() | 0xff000000;
                danmaku.setDuration(mDuration);
                mNormalDanmakuView.addDanmaku(danmaku);
                speedsMeasurement.dot();
            }
            if (mRunning && mSpeed > 0) {
                mHandler.postDelayed(this, 1000 / mSpeed);
            }
        }

        public void resume() {
            mHandler.removeCallbacksAndMessages(null);
            mRunning = true;
            if (mSpeed > 0) {
                mHandler.post(this);
            }
        }

        public void stop() {
            mHandler.removeCallbacksAndMessages(null);
            mRunning = false;
        }

        public static String getRandomString(int length) {
            String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?$$$%%%###@@@!!!&&&***^^^???";
            Random random = new Random();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < length; i++) {
                int number = random.nextInt(str.length());
                sb.append(str.charAt(number));
            }
            return sb.toString();
        }
    }
}
