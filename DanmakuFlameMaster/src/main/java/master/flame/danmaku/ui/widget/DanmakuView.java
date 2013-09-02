package master.flame.danmaku.ui.widget;

import master.flame.danmaku.controller.AcDanmakuPlayer;
import master.flame.danmaku.controller.AcDanmakuPlayer.OnErrorListener;
import master.flame.danmaku.controller.AcDanmakuPlayer.OnPreparedListener;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;


/**
 * @author Yrom
 *
 */
public class DanmakuView extends SurfaceView implements Callback {
    
    private SurfaceHolder mSurfaceHolder;
    private DanmakuTimer mTimer;
    private Uri mUri;
//    private int mSeekWhenPrepared; // TODO 

    public DanmakuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mSurfaceHolder = getHolder();
        setZOrderOnTop(true);
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mTimer = new DanmakuTimer();
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DanmakuView(Context context) {
        this(context, null, 0);
    }
    
    public void setDanmakuPath(String uri){
        setDanmakuURI(Uri.parse(uri));
    }
    
    public void setDanmakuURI(Uri uri){
        mUri = uri;
//        mSeekWhenPrepared = 0;
        requestLayout();
        invalidate();
        loadDanmakus();
        
    }
    private void loadDanmakus() {
        if (mUri == null) {
            return;
        }
        try{
            mPlayer = new AcDanmakuPlayer(getContext());
            mPlayer.setOnPreparedListener(mOnPreparedListener);
            mPlayer.setOnErrorListener(mOnErrorListener);
            mPlayer.setDisplay(getDisplayer());
            mPlayer.setSurfaceHolder(mSurfaceHolder);
            mPlayer.setEnableDrawingCache(true);
            mPlayer.setTimer(mTimer);
            mPlayer.setDataSource(mUri);
            mPlayer.prepareAsync();
        }catch(Exception e){
            if(mOnErrorListener != null)
                mOnErrorListener.onError(mPlayer, 1, 1);
        }
    }
    public void seekTo(long pos){
        if(mPlayer != null){
            mPlayer.seekTo(pos);
        }
    }
    public void start(){
        if(mPlayer != null)
            mPlayer.start();
        else
            loadDanmakus();
    }
    public void stop(){
        if(mPlayer != null)
            mPlayer.stop();
    }
    public void pause(){
        if(mPlayer != null)
            mPlayer.pause();
    }
    private AndroidDisplayer getDisplayer(){
        AndroidDisplayer disp = new AndroidDisplayer();
        disp.width = getWidth();
        disp.height = getHeight();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        if(disp.width == 0)
            disp.width = displayMetrics.heightPixels;
        if(disp.height == 0)
            disp.height = displayMetrics.widthPixels;
        disp.density = displayMetrics.density;
        disp.densityDpi = displayMetrics.densityDpi;
        disp.scaledDensity = displayMetrics.scaledDensity;
        return disp;
    }
    
    OnPreparedListener mOnPreparedListener;
    OnErrorListener mOnErrorListener;
    private AcDanmakuPlayer mPlayer;
    public void setOnPreparedListener(OnPreparedListener l){
        mOnPreparedListener = l;
    }
    public void setOnErrorListener(OnErrorListener l){
        mOnErrorListener = l;
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }
    public void release(){
        if(mPlayer != null){
            mPlayer.release();
            mPlayer = null;
        }
    }
}
