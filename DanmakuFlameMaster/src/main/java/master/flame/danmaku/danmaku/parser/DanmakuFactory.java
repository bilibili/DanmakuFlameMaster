
package master.flame.danmaku.danmaku.parser;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.FBDanmaku;
import master.flame.danmaku.danmaku.model.FTDanmaku;
import master.flame.danmaku.danmaku.model.R2LDanmaku;

public class DanmakuFactory {

    public static float BILI_PLAYER_WIDTH       = 539;

    public static float BILI_PLAYER_HEIGHT      = 385;

    public static long  COMMON_DANMAKU_DURATION = 3500; // B站原始分辨率下弹幕存活时间

    public static long  REAL_DANMAKU_DURATION   = -1;

    public static BaseDanmaku createDanmaku(int type, int dispWidth) {
        if (REAL_DANMAKU_DURATION == -1)
            REAL_DANMAKU_DURATION = (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH));
        return createDanmaku(type, type == 1 ? REAL_DANMAKU_DURATION : COMMON_DANMAKU_DURATION);
    }

    public static BaseDanmaku createDanmaku(int type, long duration) {
        BaseDanmaku instance = null;
        switch (type) {
        case BaseDanmaku.TYPE_SCROLL_RL: // 从右往左滚动
            instance = new R2LDanmaku(duration);
            break;
        case BaseDanmaku.TYPE_FIX_BOTTOM: // 底端固定
            instance = new FBDanmaku(duration);
            break;
        case BaseDanmaku.TYPE_FIX_TOP: // 顶端固定
            instance = new FTDanmaku(duration);
            break;
         // TODO: more Danmaku type
        }
        return instance;
    }
    
    public static void updateDanmakuDuration(BaseDanmaku danmaku, float dispWidth) {
        danmaku.duration = (long) (COMMON_DANMAKU_DURATION * (dispWidth / BILI_PLAYER_WIDTH));
    }
}
