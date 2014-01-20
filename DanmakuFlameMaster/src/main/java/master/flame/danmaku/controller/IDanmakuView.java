
package master.flame.danmaku.controller;

import android.content.Context;

import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDanmakuView {

    public boolean isPrepared();

    public boolean isViewReady();

    public long drawDanmakus();

    public void enableDanmakuDrawingCache(boolean enable);

    public boolean isDanmakuDrawingCacheEnabled();

    public Context getContext();

    public int getWidth();

    public int getHeight();

    public void showFPS(boolean show);

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

}
