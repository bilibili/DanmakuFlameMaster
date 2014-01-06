
package master.flame.danmaku.danmaku.model.android;

import java.util.ArrayList;
import java.util.List;

import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuFilters;
import master.flame.danmaku.danmaku.model.DanmakuFilters.IDanmakuFilter;
import android.graphics.Typeface;
import android.text.TextPaint;

public class DanmakuGlobalConfig {
    /*
     * TODO 选项：合并异色同字弹幕缓存
     *      选项：弹幕阴影样式
     */
    
    
    public static DanmakuGlobalConfig DEFAULT = new DanmakuGlobalConfig();

    /**
     * 默认字体
     */
    public Typeface mFont = null;

    /**
     * paint alpha:0-255
     */
    public int alpha = AlphaValue.MAX;
    
    public boolean isTranslucent = false;
    
    public float scaleTextSize = 1.0f;

    public TextPaint paint = new TextPaint();

    /**
     * 弹幕大小是否被缩放
     */
    private boolean isTextScaled = false;
       
    /**
     * 弹幕显示隐藏设置
     */
    public boolean FTDanmakuVisibility = true;
    public boolean FBDanmakuVisibility = true;
    public boolean L2RDanmakuVisibility = true;
    public boolean R2LDanmakuVisibility = true;
    public boolean SecialDanmakuVisibility = true;
    
    /**
     * 同屏弹幕数量 
     * -1 无限制
     *  0 按绘制效率自动调整
     *  n 同屏最大显示n个弹幕
     */
    public int maxNumsInScreen = -1;
    
    /**
     * 默认滚动速度系数
     */
    public float scrollSpeedFactor = 1.0f;
    
    public boolean isScrollSpeedChanged = false;
    
    /**
     * 绘制刷新率(毫秒)
     */
    public int refreshRateMS = 15;
    
    
    /**
     * 描边/阴影类型   
     *
     */
    public enum BorderType {
        NONE,SHADOW,STROKEN
    }
    
    public BorderType shadowType = BorderType.SHADOW;
    public int shadowRadius = 3;    
    

    /**
     * set typeface
     * 
     * @param font
     */
    public DanmakuGlobalConfig setTypeface(Typeface font) {
        if (mFont != font) {
            mFont = font;
            AndroidDisplayer.clearTextHeightCache();
            paint.setTypeface(mFont); // thread safe is not necessary
        }
        return this;
    }

    public DanmakuGlobalConfig setGlobalAlpha(float p) {
        int newAlpha = (int) (p * AlphaValue.MAX);
        if (newAlpha != alpha) {
            alpha = newAlpha;
            isTranslucent = (newAlpha != AlphaValue.MAX);
        }
        return this;
    }
    
    public DanmakuGlobalConfig setScaleTextSize(float p){
        if(scaleTextSize!=p){
            scaleTextSize = p;
        }
        isTextScaled = (scaleTextSize != 1f);
        return this;
    }

    /**
     * 
     * @return 是否显示顶部弹幕
     */
    public boolean getFTDanmakuVisibility() {
        return FTDanmakuVisibility;
    }

    List<Integer> mFilterTypes = new ArrayList<Integer>();
    
    /**
     * 设置是否显示顶部弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setFTDanmakuVisibility(boolean visible) {
        if(FTDanmakuVisibility != visible){
            FTDanmakuVisibility = visible;
            setVisible(visible,BaseDanmaku.TYPE_FIX_TOP);
            setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER,mFilterTypes);
        }
        return this;
    }

    private void setFilterData(String tag, Object data) {
        IDanmakuFilter filter = DanmakuFilters.getDefault().get(
                DanmakuFilters.TAG_TYPE_DANMAKU_FILTER);
        filter.setData(mFilterTypes);
    }

    private void setVisible(boolean visible, int type) {
        if (visible) {
            mFilterTypes.remove(Integer.valueOf(type));
        } else if (!mFilterTypes.contains(Integer.valueOf(type))) {
            mFilterTypes.add(Integer.valueOf(type));
        }
    }

    
    
    /**
     * 
     * @return 是否显示底部弹幕
     */
    public boolean getFBDanmakuVisibility() {
        return FBDanmakuVisibility;
    }

    /**
     * 设置是否显示底部弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setFBDanmakuVisibility(boolean visible) {
        if(FBDanmakuVisibility != visible){
            FBDanmakuVisibility = visible;
            setVisible(visible,BaseDanmaku.TYPE_FIX_BOTTOM);
            setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER,mFilterTypes);
        }
        return this;
    }

    /**
     * 
     * @return 是否显示左右滚动弹幕
     */
    public boolean getL2RDanmakuVisibility() {
        return L2RDanmakuVisibility;
    }

    /**
     * 设置是否显示左右滚动弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setL2RDanmakuVisibility(boolean visible) {
        if(L2RDanmakuVisibility != visible){
            L2RDanmakuVisibility = visible;
            setVisible(visible,BaseDanmaku.TYPE_SCROLL_LR);
            setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER,mFilterTypes);
        }
        return this;
    }

    /**
     * 
     * @return 是否显示右左滚动弹幕
     */
    public boolean getR2LDanmakuVisibility() {
        return R2LDanmakuVisibility;
    }

    /**
     * 设置是否显示右左滚动弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setR2LDanmakuVisibility(boolean visible) {       
        if(R2LDanmakuVisibility != visible){
            R2LDanmakuVisibility = visible;
            setVisible(visible,BaseDanmaku.TYPE_SCROLL_RL);
            setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER,mFilterTypes);
        }
        return this;
    }

    /**
     * 
     * @return 是否显示特殊弹幕
     */
    public boolean getSecialDanmakuVisibility() {
        return SecialDanmakuVisibility;
    }

    /**
     * 设置是否显示特殊弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setSecialDanmakuVisibility(boolean visible) {
        if(SecialDanmakuVisibility != visible){
            SecialDanmakuVisibility = visible;
            setVisible(visible,BaseDanmaku.TYPE_SPECIAL);
            setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER,mFilterTypes);
        }
        return this;
    }

   

}
