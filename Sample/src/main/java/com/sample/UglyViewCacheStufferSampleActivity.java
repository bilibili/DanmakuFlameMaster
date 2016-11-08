
package com.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.VideoView;

import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.model.android.ViewCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.IOUtils;
import master.flame.danmaku.danmaku.util.SystemClock;

public class UglyViewCacheStufferSampleActivity extends Activity implements View.OnClickListener {

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

    private Button mBtnSendDanmakuTextAndImage;

    private Button mBtnSendDanmakus;
    private DanmakuContext mContext;
    private BaseCacheStuffer.Proxy mCacheStufferAdapter = new BaseCacheStuffer.Proxy() {

        private Drawable mDrawable;

        @Override
        public void prepareDrawing(final BaseDanmaku danmaku, boolean fromWorkerThread) {
            if (danmaku.text instanceof Spanned) { // 根据你的条件检查是否需要需要更新弹幕
                // FIXME 这里只是简单启个线程来加载远程url图片，请使用你自己的异步线程池，最好加上你的缓存池
                new Thread() {

                    @Override
                    public void run() {
                        String url = "http://www.bilibili.com/favicon.ico";
                        InputStream inputStream = null;
                        Drawable drawable = mDrawable;
                        if (drawable == null) {
                            try {
                                URLConnection urlConnection = new URL(url).openConnection();
                                inputStream = urlConnection.getInputStream();
                                drawable = BitmapDrawable.createFromStream(inputStream, "bitmap");
                                mDrawable = drawable;
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                IOUtils.closeQuietly(inputStream);
                            }
                        }
                        if (drawable != null) {
                            drawable.setBounds(0, 0, 100, 100);
                            SpannableStringBuilder spannable = createSpannable(drawable);
                            danmaku.text = spannable;
                            if (mDanmakuView != null) {
                                mDanmakuView.invalidateDanmaku(danmaku, false);
                            }
                            return;
                        }
                    }
                }.start();
            }
        }

        @Override
        public void releaseResource(BaseDanmaku danmaku) {
            // TODO 重要:清理含有ImageSpan的text中的一些占用内存的资源 例如drawable
        }
    };
    private int mIconWidth;

    /**
     * 绘制背景(自定义弹幕样式)
     */
    private static class BackgroundCacheStuffer extends SpannedCacheStuffer {
        // 通过扩展SimpleTextCacheStuffer或SpannedCacheStuffer个性化你的弹幕样式
        final Paint paint = new Paint();

        @Override
        public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
            danmaku.padding = 10;  // 在背景绘制模式下增加padding
            super.measure(danmaku, paint, fromWorkerThread);
        }

        @Override
        public void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top) {
            paint.setColor(0x8125309b);
            canvas.drawRect(left + 2, top + 2, left + danmaku.paintWidth - 2, top + danmaku.paintHeight - 2, paint);
        }

        @Override
        public void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
            // 禁用描边绘制
        }
    }

    public class MyViewHolder extends ViewCacheStuffer.ViewHolder {

        private final ImageView mIcon;
        private final TextView mText;

        public MyViewHolder(View itemView) {
            super(itemView);
            mIcon = (ImageView) itemView.findViewById(R.id.icon);
            mText = (TextView) itemView.findViewById(R.id.text);
        }

    }

    public static class MyImageWare extends NonViewAware {

        private long start;
        private int id;
        private WeakReference<IDanmakuView> danmakuViewRef;
        private BaseDanmaku danmaku;
        private Bitmap bitmap;

        public MyImageWare(String imageUri, BaseDanmaku danmaku, int width, int height, IDanmakuView danmakuView) {
            this(imageUri, new ImageSize(width, height), ViewScaleType.FIT_INSIDE);
            if (danmaku == null) {
                throw new IllegalArgumentException("danmaku may not be null");
            }
            this.danmaku = danmaku;
            this.id = danmaku.hashCode();
            this.danmakuViewRef = new WeakReference<>(danmakuView);
            this.start = SystemClock.uptimeMillis();
        }

        @Override
        public int getId() {
            return this.id;
        }

        public String getImageUri() {
            return this.imageUri;
        }

        private MyImageWare(ImageSize imageSize, ViewScaleType scaleType) {
            super(imageSize, scaleType);
        }

        private MyImageWare(String imageUri, ImageSize imageSize, ViewScaleType scaleType) {
            super(imageUri, imageSize, scaleType);
        }

        @Override
        public boolean setImageDrawable(Drawable drawable) {
            return super.setImageDrawable(drawable);
        }

        @Override
        public boolean setImageBitmap(Bitmap bitmap) {
//            if (this.danmaku.isTimeOut() || this.danmaku.isFiltered()) {
//                return true;
//            }
            if (this.danmaku.text.toString().contains("textview")) {
                Log.e("DFM", (SystemClock.uptimeMillis() - this.start) + "ms=====> inside" + danmaku.tag + ":" + danmaku.getActualTime() + ",url: bitmap" + (bitmap == null));
            }
            this.bitmap = bitmap;
            IDanmakuView danmakuView = danmakuViewRef.get();
            if (danmakuView != null) {
                danmakuView.invalidateDanmaku(danmaku, true);
            }
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .memoryCacheSizePercentage(13).build(); // default
        ImageLoader.getInstance().init(config);
        setContentView(R.layout.activity_main);
        findViews();
    }

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

    private void findViews() {

        mMediaController = findViewById(R.id.media_controller);
        mBtnRotate = (Button) findViewById(R.id.rotate);
        mBtnHideDanmaku = (Button) findViewById(R.id.btn_hide);
        mBtnShowDanmaku = (Button) findViewById(R.id.btn_show);
        mBtnPauseDanmaku = (Button) findViewById(R.id.btn_pause);
        mBtnResumeDanmaku = (Button) findViewById(R.id.btn_resume);
        mBtnSendDanmaku = (Button) findViewById(R.id.btn_send);
        mBtnSendDanmakuTextAndImage = (Button) findViewById(R.id.btn_send_image_text);
        mBtnSendDanmakus = (Button) findViewById(R.id.btn_send_danmakus);
        mBtnRotate.setOnClickListener(this);
        mBtnHideDanmaku.setOnClickListener(this);
        mMediaController.setOnClickListener(this);
        mBtnShowDanmaku.setOnClickListener(this);
        mBtnPauseDanmaku.setOnClickListener(this);
        mBtnResumeDanmaku.setOnClickListener(this);
        mBtnSendDanmaku.setOnClickListener(this);
        mBtnSendDanmakuTextAndImage.setOnClickListener(this);
        mBtnSendDanmakus.setOnClickListener(this);

        // VideoView
        VideoView mVideoView = (VideoView) findViewById(R.id.videoview);
        // DanmakuView

        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        mDanmakuView = (IDanmakuView) findViewById(R.id.sv_danmaku);
        mContext = DanmakuContext.create();

        mIconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, getResources().getDisplayMetrics());
        mContext.setDanmakuBold(true);
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.2f)
                .setCacheStuffer(new ViewCacheStuffer<MyViewHolder>() {

                    @Override
                    public MyViewHolder onCreateViewHolder(int viewType) {
                        Log.e("DFM", "onCreateViewHolder:" + viewType);
                        return new MyViewHolder(View.inflate(getApplicationContext(), R.layout.layout_view_cache, null));
                    }

                    @Override
                    public void onBindViewHolder(int viewType, MyViewHolder viewHolder, BaseDanmaku danmaku, AndroidDisplayer.DisplayerConfig displayerConfig, TextPaint paint) {
                        if (paint != null)
                            viewHolder.mText.getPaint().set(paint);
                        viewHolder.mText.setText(danmaku.text);
                        viewHolder.mText.setTextColor(danmaku.textColor);
                        viewHolder.mText.setTextSize(TypedValue.COMPLEX_UNIT_PX, danmaku.textSize);
                        Bitmap bitmap = null;
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare != null) {
                            bitmap = imageWare.bitmap;
                            if (danmaku.text.toString().contains("textview")) {
                                Log.e("DFM", "onBindViewHolder======> bitmap:" + (bitmap == null) + "  " + danmaku.tag + "url:" + imageWare.getImageUri());
                            }
                        }
                        if (bitmap != null) {
                            viewHolder.mIcon.setImageBitmap(bitmap);
                            if (danmaku.text.toString().contains("textview")) {
                                Log.e("DFM", "onBindViewHolder======>" + danmaku.tag + "url:" + imageWare.getImageUri());
                            }
                        } else {
                            viewHolder.mIcon.setImageResource(R.drawable.ic_launcher);
                        }
                    }

                    @Override
                    public void releaseResource(BaseDanmaku danmaku) {
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare != null) {
                            ImageLoader.getInstance().cancelDisplayTask(imageWare);
                        }
                        danmaku.setTag(null);
                        Log.e("DFM", "releaseResource url:" + danmaku.text);
                    }


                    String[] avatars = { "http://i0.hdslb.com/bfs/face/e13fcb94342c325debb2d3a1d9e503ac4f083514.jpg@45w_45h.webp",
                            "http://i0.hdslb.com/bfs/bangumi/2558e1341d2e934a7e06bb7d92551fef5c82c172.jpg@72w_72h.webp", "http://i0.hdslb.com/bfs/face/128edefeef7ce9cfc443a2489d8a1c7d44d88b80.jpg@72w_72h.webp"};
                    @Override
                    public void prepare(BaseDanmaku danmaku, boolean fromWorkerThread) {
                        if (danmaku.isTimeOut()) {
                            return;
                        }
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare == null) {
                            String avatar = avatars[danmaku.index % avatars.length];
                            imageWare = new MyImageWare(avatar, danmaku, mIconWidth, mIconWidth, mDanmakuView);
                            danmaku.setTag(imageWare);
                        }
                        if (danmaku.text.toString().contains("textview")) {
                            Log.e("DFM", "onAsyncLoadResource======>" + danmaku.tag + "url:" + imageWare.getImageUri());
                        }
                        ImageLoader.getInstance().displayImage(imageWare.getImageUri(), imageWare);
                    }

                }, null) // 图文混排使用SpannedCacheStuffer
//        .setCacheStuffer(new BackgroundCacheStuffer())  // 绘制背景使用BackgroundCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        if (mDanmakuView != null) {
            mParser = createParser(this.getResources().openRawResource(R.raw.comments));
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
//                    Log.d("DFM", "danmakuShown(): text=" + danmaku.text);
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
            mDanmakuView.setOnDanmakuClickListener(new IDanmakuView.OnDanmakuClickListener() {

                @Override
                public boolean onDanmakuClick(IDanmakus danmakus) {
                    Log.d("DFM", "onDanmakuClick: danmakus size:" + danmakus.size());
                    BaseDanmaku latest = danmakus.last();
                    if (null != latest) {
                        Log.d("DFM", "onDanmakuClick: text of latest danmaku:" + latest.text);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onViewClick(IDanmakuView view) {
                    mMediaController.setVisibility(View.VISIBLE);
                    return false;
                }
            });
            mDanmakuView.prepare(mParser, mContext);
            mDanmakuView.showFPS(true);
            mDanmakuView.enableDanmakuDrawingCache(true);
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
    protected void onPause() {
        super.onPause();
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
        ImageLoader.getInstance().destroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDanmakuView != null) {
            // dont forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }
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
            setRequestedOrientation(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (v == mBtnHideDanmaku) {
            mDanmakuView.hide();
            // mPausedPosition = mDanmakuView.hideAndPauseDrawTask();
        } else if (v == mBtnShowDanmaku) {
            mDanmakuView.show();
            // mDanmakuView.showAndResumeDrawTask(mPausedPosition); // sync to the video time in your practice
        } else if (v == mBtnPauseDanmaku) {
            mDanmakuView.pause();
        } else if (v == mBtnResumeDanmaku) {
            mDanmakuView.resume();
        } else if (v == mBtnSendDanmaku) {
            addDanmaku(false);
        } else if (v == mBtnSendDanmakuTextAndImage) {
            addDanmaKuShowTextAndImage(false);
        } else if (v == mBtnSendDanmakus) {
            Boolean b = (Boolean) mBtnSendDanmakus.getTag();
            timer.cancel();
            if (b == null || !b) {
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
            for (int i = 0; i < 20; i++) {
                addDanmaku(true);
                SystemClock.sleep(20);
            }
        }
    }

    ;

    private void addDanmaku(boolean islive) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        // for(int i=0;i<100;i++){
        // }
        danmaku.text = "这是一条弹幕" + System.nanoTime();
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = Color.WHITE;
        // danmaku.underlineColor = Color.GREEN;
        danmaku.borderColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);

    }

    private void addDanmaKuShowTextAndImage(boolean islive) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_launcher);
        drawable.setBounds(0, 0, 100, 100);
        //SpannableStringBuilder spannable = createSpannable(drawable);
        danmaku.text = "这是文本，图片在textview左侧";
        danmaku.setTag(new MyImageWare("http://i0.hdslb.com/bfs/face/084bd13eb5dc51a64674085bb28e958ecd5addd0.jpg@180w_180h.webp", danmaku, mIconWidth, mIconWidth, mDanmakuView));
        danmaku.padding = 5;
        danmaku.priority = 1;  // 一定会显示, 一般用于本机发送的弹幕
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.RED;
        danmaku.textShadowColor = 0; // 重要：如果有图文混排，最好不要设置描边(设textShadowColor=0)，否则会进行两次复杂的绘制导致运行效率降低
        danmaku.underlineColor = Color.GREEN;
        mDanmakuView.addDanmaku(danmaku);
    }

    private SpannableStringBuilder createSpannable(Drawable drawable) {
        String text = "bitmap";
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        ImageSpan span = new ImageSpan(drawable);//ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.append("图文混排");
        spannableStringBuilder.setSpan(new BackgroundColorSpan(Color.parseColor("#8A2233B1")), 0, spannableStringBuilder.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableStringBuilder;
    }

}
