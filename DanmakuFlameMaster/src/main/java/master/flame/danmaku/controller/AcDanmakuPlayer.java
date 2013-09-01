
package master.flame.danmaku.controller;

import java.io.IOException;

import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.AcFunDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.JSONSource;

import org.json.JSONException;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class AcDanmakuPlayer {

    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private IDataSource<?> mDataSource;
    private AcFunDanmakuParser mParser;
    private Danmakus mDanmakuList;
    private DanmakuTimer mTimer;
    private EventHandler mEventHandler;
    private HandlerThread mDrawThread;
    private DrawHandler mDrawHandler;
    public static final int ERROR_DATA_SOURCE = 100;
    public static final int EXTRA_DATA_NOT_FOUND = -5;

    private static final int DANMAKU_PREPARED = 1;
    private static final int DANMAKU_ERROR = 2;
    private static final String TAG = AcDanmakuPlayer.class.getSimpleName();
    private Context mContext;
    private DrawTask drawTask;
    private boolean mEnableDanmakuDrawingCache;
    private SurfaceHolder mSurfaceHolder;

    public AcDanmakuPlayer(Context context) {
        mContext = context;

        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        
    }
    private Uri mUri;
    public void setDataSource(Uri uri) {
        mUri = uri;
    }

    // TODO support headers
    // public void setDataSource(Context context, Uri uri, Map<String,String>
    // headers){
    // }
    public void setTimer(DanmakuTimer timer) {
        mTimer = timer;
    }

    public void prepareAsync() throws IllegalStateException {
        if (mUri == null || mParser == null || mTimer == null)
            throw new IllegalStateException("parser and timer hasn't initialized yet");
        new Thread() {

            public void run() {
                try {
                    mDataSource = new JSONSource(mUri);
                    mDanmakuList = mParser.load(mDataSource).setTimer(mTimer).parse();
                    if (mDanmakuList != null && mDanmakuList.size() > 0) {
                        mEventHandler.sendEmptyMessage(DANMAKU_PREPARED);
                    } else {
                        mEventHandler.obtainMessage(DANMAKU_ERROR, ERROR_DATA_SOURCE, 2).sendToTarget();
                    }
                } catch (IOException e) {
                    mEventHandler.obtainMessage(ERROR_DATA_SOURCE, ERROR_DATA_SOURCE, EXTRA_DATA_NOT_FOUND).sendToTarget();
                } catch (JSONException e) {
                    mEventHandler.obtainMessage(ERROR_DATA_SOURCE, ERROR_DATA_SOURCE, 1).sendToTarget();
                }
                
            }
        }.start();
    }
    public void seekTo(long pos){
        mDrawHandler.obtainMessage(DrawHandler.SEEK_TO, Long.valueOf(pos)).sendToTarget();
    }
    
    public void start() {
        if(mDrawHandler!= null && mDrawHandler.isStop()){
            mDrawHandler.sendEmptyMessage(DrawHandler.RESUME);
        }else{
            mDrawThread = new HandlerThread("draw thread");
            mDrawThread.start();
            mDrawHandler = new DrawHandler(mDrawThread.getLooper());
            mDrawHandler.sendEmptyMessage(DrawHandler.START);
        }
    }

    public boolean isPlaying() {
        boolean stop =  mDrawThread == null || mDrawHandler== null || mDrawHandler.isStop();
        return !stop;
    }

    public void stop() {
        if(isPlaying()){
            mDrawHandler.quit();
            drawTask.reset();
        }
    }

    public void pause() {
        if(isPlaying()){
            mDrawHandler.quit();
        }
    }

    public void release() {
        if(mDrawHandler != null){
            mDrawHandler.quit();
            mDrawHandler = null;
        }
        if(mDrawThread != null){
            mDrawThread.quit();
            mDrawThread = null;
        }
        if(drawTask != null){
            drawTask.quit();
            drawTask = null;
        }
    }

    private class DrawHandler extends Handler {

        static final int START = 1;
        static final int RESUME = 2;
        static final int SEEK_POS = 3;
        static final int UPDATE = 4;
        static final int SEEK_TO = 5;
        long pausedPostion;
        boolean quitFlag;
        long mTimeBase;

        public DrawHandler(Looper looper) {
            super(looper);
        }

        public void quit() {
            quitFlag = true;
        }

        public boolean isStop() {
            return quitFlag;
        }
        public long current(){
            return System.currentTimeMillis() - mTimeBase;
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case START:
                pausedPostion = 0;
            case RESUME:
                quitFlag = false;
                mTimeBase = System.currentTimeMillis() - pausedPostion;
                mTimer.update(pausedPostion);
                startDrawingWhenReady(new Runnable() {

                    @Override
                    public void run() {
                        sendEmptyMessage(UPDATE);
                    }
                });
                break;
            case SEEK_TO :
                Long pos = (Long)msg.obj;
                mTimeBase -= pos - current();
                sendEmptyMessage(UPDATE);
                break;
            case SEEK_POS:
                Long deltaMs = (Long) msg.obj;
                mTimeBase -= deltaMs;
            case UPDATE:
                long d = mTimer.update(current());
                if (d == 0) {
                    if (!quitFlag)
                        sendEmptyMessageDelayed(UPDATE, 10);
                    return;
                }
                if (d < 15) {
                    if (d < 10) {
                        try {
                            Thread.sleep(15 - d);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                drawDanmakus();
                if (!quitFlag)
                    sendEmptyMessage(UPDATE);
                else {
                    pausedPostion = current();
                    Log.i(TAG, "stop draw: current = " + pausedPostion);
                }
                break;
            }
        }

        private void startDrawingWhenReady(final Runnable runnable) {
            if (drawTask == null) {
                drawTask = createTask(mEnableDanmakuDrawingCache, mTimer, new IDrawTask.TaskListener() {

                    @Override
                    public void ready() {
                        Log.i(TAG, "start drawing multiThread enabled:" + mEnableDanmakuDrawingCache);
                        runnable.run();
                    }
                });
                drawTask.setDanmakus(mDanmakuList);

            } else {
                runnable.run();
            }
        }

    }

    private class EventHandler extends Handler {

        private AcDanmakuPlayer mPlayer;

        public EventHandler(AcDanmakuPlayer player, Looper looper) {
            super(looper);
            mPlayer = player;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case DANMAKU_PREPARED:
                if (mOnPreparedListener != null)
                    mOnPreparedListener.onPrepared(mPlayer);
                break;
            case DANMAKU_ERROR:
                Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(mPlayer, msg.arg1, msg.arg2);
                }
                break;
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener l) {
        mOnPreparedListener = l;
    }
    public void setSurfaceHolder(SurfaceHolder holder){
        mSurfaceHolder = holder;
    }
    public void drawDanmakus() {
        if(mSurfaceHolder == null)
            return;
        long stime = System.currentTimeMillis();
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {

            DrawHelper.clearCanvas(canvas);
            drawTask.draw(canvas);

            long dtime = System.currentTimeMillis() - stime;
            String fps = String.format("fps %.2f", 1000 / (float) dtime);
            DrawHelper.drawText(canvas, fps);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public int getHeight() {
        return mDisp == null? -1 : mDisp.height;
    }

    public int getWidth() {
        return mDisp == null? -1 : mDisp.width;
    }
    
    public int danmakusSize(){
        return mDanmakuList.size();
    }
    
    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public interface OnPreparedListener {

        void onPrepared(AcDanmakuPlayer player);
    }

    public interface OnErrorListener {

        void onError(AcDanmakuPlayer player, int what, int extra);
    }
    private AndroidDisplayer mDisp;
    public void setDisplay(AndroidDisplayer disp) {
        mDisp = disp;
        mParser = new AcFunDanmakuParser(mDisp);
    }

    public static int getMemoryClass(final Context context) {
        return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    private DrawTask createTask(boolean useDrwaingCache, DanmakuTimer timer, IDrawTask.TaskListener taskListener) {
        return useDrwaingCache && mMaxSize > 0? 
               new CacheManagingDrawTask(timer, mDisp, taskListener ,mMaxSize) : 
               new DrawTask(timer, mDisp, taskListener);
    }

    public void setEnableDrawingCache(boolean use) {
        mEnableDanmakuDrawingCache = use;
        if(use)
            mMaxSize = getMemoryClass(mContext) << 18;
    }

    private int mMaxSize;

    public void setDrawingCache(int maxSize) {
        mMaxSize = maxSize;
    }

    public Context getContext() {
        return mContext;
    }
}
