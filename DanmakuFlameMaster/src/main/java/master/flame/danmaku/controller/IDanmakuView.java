
package master.flame.danmaku.controller;

import android.content.Context;
import android.view.View;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDanmakuView {

    public boolean isPrepared();

    public boolean isViewReady();

    public long drawDanmakus();

    public void enableDanmakuDrawingCache(boolean enable);

    public boolean isDanmakuDrawingCacheEnabled();

    public void showFPS(boolean show);
    
    public void addDanmaku(BaseDanmaku item);
    
    public void setCallback(Callback callback);
    
    
    // ------------- Android View方法  --------------------
    
    public Context getContext();
    
    public View getView();

    public int getWidth();

    public int getHeight();
    
    public void setVisibility(int visibility);
    
    public boolean isShown();
    

    // ------------- 播放控制 -------------------
    
    public void prepare(BaseDanmakuParser parser);

    public void seekTo(Long ms);

    public void start();

    public void start(long postion);

    public void stop();

    public void pause();

    public void resume();

    public void release();
    
    public void toggle();
    
    public void show();
    
    public void hide();
    
    public void clear();

}
