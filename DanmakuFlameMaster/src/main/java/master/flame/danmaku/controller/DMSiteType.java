package master.flame.danmaku.controller;

import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.AcFunDanmakuParser;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser;

/**
 * @author Yrom
 *
 */
public enum DMSiteType {
    ACFUN(AcFunDanmakuParser.class), BILI(BiliDanmukuParser.class);
    private Class<? extends BaseDanmakuParser> mParserClass;
    private DMSiteType(Class<? extends BaseDanmakuParser> parserClass){
        mParserClass = parserClass;
    }
    /**
     * 
     * @return
     */
    public ILoader getLoader(){
        return DanmakuLoaderFactory.create(name().toLowerCase());
    }
    
    public BaseDanmakuParser getParser(){
        try {
            return mParserClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
