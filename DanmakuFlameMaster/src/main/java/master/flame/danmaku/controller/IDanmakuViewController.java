package master.flame.danmaku.controller;

import android.content.Context;

/**
 * For internal control. DO NOT ACCESS this interface.
 */
public interface IDanmakuViewController {

    public boolean isViewReady();

    public int getViewWidth();

    public int getViewHeight();

    public Context getContext();

    public long drawDanmakus();

    public void clear();

    public boolean isHardwareAccelerated();

    public boolean isDanmakuDrawingCacheEnabled();

}
