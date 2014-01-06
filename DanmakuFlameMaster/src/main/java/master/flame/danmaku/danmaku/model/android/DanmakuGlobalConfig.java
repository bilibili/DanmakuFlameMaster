
package master.flame.danmaku.danmaku.model.android;

import master.flame.danmaku.danmaku.model.AlphaValue;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

public class DanmakuGlobalConfig {
    
    public static DanmakuGlobalConfig DEFAULT = new DanmakuGlobalConfig();

    /**
     * 默认字体
     */
    public Typeface mFont = Typeface.DEFAULT;

    /**
     * paint alpha:0-255
     */
    public int alpha = AlphaValue.MAX;
    
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
     * 同屏弹幕数量  -1无限制
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
        if (font == null)
            mFont = Typeface.DEFAULT;
        else
            mFont = font;
        paint.setTypeface(mFont);
        return this;
    }

    public DanmakuGlobalConfig setGlobalAlpha(float p) {
        int newAlpha = (int) (p * AlphaValue.MAX);
        if (newAlpha != alpha) {
            alpha = newAlpha;
            paint.setAlpha(alpha);
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

    /**
     * 设置是否显示顶部弹幕
     * @param visible
     */
    public DanmakuGlobalConfig setFTDanmakuVisibility(boolean visible) {
        FTDanmakuVisibility = visible;
        return this;
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
        FBDanmakuVisibility = visible;
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
        L2RDanmakuVisibility = visible;
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
        R2LDanmakuVisibility = visible;
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
        SecialDanmakuVisibility = visible;
        return this;
    }

   

}
