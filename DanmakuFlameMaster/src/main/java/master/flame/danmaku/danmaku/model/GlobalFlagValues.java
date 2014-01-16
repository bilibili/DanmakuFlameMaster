package master.flame.danmaku.danmaku.model;

public class GlobalFlagValues {

    public static int VISIBLE_RESET_FLAG = 0;
    
    public static void resetAll(){
        VISIBLE_RESET_FLAG = 0;
    }
    
    public static void updateVisibleFlag(){
        VISIBLE_RESET_FLAG++;
    }
    
}
