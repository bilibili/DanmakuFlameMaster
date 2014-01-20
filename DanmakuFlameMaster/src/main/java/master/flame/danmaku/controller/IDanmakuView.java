package master.flame.danmaku.controller;

import android.content.Context;

import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDanmakuView {

    public void prepare(BaseDanmakuParser parser);
    
    public boolean isPrepared();
    
    public boolean isViewReady();
    
    public void seekTo(Long ms);
    
    public long drawDanmakus();
    
    public void enableDanmakuDrawingCache(boolean enable);
    
    public boolean isDanmakuDrawingCacheEnabled();
    
    public Context getContext();
    
    public int getWidth();
    
    public int getHeight();
    
}
