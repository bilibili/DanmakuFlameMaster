
package master.flame.danmaku.danmaku.model.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import master.flame.danmaku.controller.DanmakuFilters;
import master.flame.danmaku.controller.DanmakuFilters.IDanmakuFilter;
import master.flame.danmaku.danmaku.model.AlphaValue;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.GlobalFlagValues;
import master.flame.danmaku.danmaku.parser.DanmakuFactory;

import android.graphics.Typeface;

public class DanmakuGlobalConfig {

    public enum DanmakuConfigTag {
        FT_DANMAKU_VISIBILITY, FB_DANMAKU_VISIBILITY, L2R_DANMAKU_VISIBILITY, R2L_DANMAKU_VISIBILIY, SPECIAL_DANMAKU_VISIBILITY, TYPEFACE, TRANSPARENCY, SCALE_TEXTSIZE, MAXIMUM_NUMS_IN_SCREEN, DANMAKU_STYLE, DANMAKU_BOLD, COLOR_VALUE_WHITE_LIST, USER_ID_BLACK_LIST, USER_HASH_BLACK_LIST, SCROLL_SPEED_FACTOR, BLOCK_GUEST_DANMAKU, DUPLICATE_MERGING_ENABLED;

        public boolean isVisibilityRelatedTag() {
            return this.equals(FT_DANMAKU_VISIBILITY) || this.equals(FB_DANMAKU_VISIBILITY)
                    || this.equals(L2R_DANMAKU_VISIBILITY) || this.equals(R2L_DANMAKU_VISIBILIY)
                    || this.equals(SPECIAL_DANMAKU_VISIBILITY) || this.equals(COLOR_VALUE_WHITE_LIST)
                    || this.equals(USER_ID_BLACK_LIST);
        }
    }

    public static DanmakuGlobalConfig DEFAULT = new DanmakuGlobalConfig();

    /**
     * 默认字体
     */
    public Typeface mFont = null;

    /**
     * paint alpha:0-255
     */
    public int transparency = AlphaValue.MAX;

    public boolean isTranslucent = false;

    public float scaleTextSize = 1.0f;

    /**
     * 弹幕大小是否被缩放
     */
    public boolean isTextScaled = false;

    /**
     * 弹幕显示隐藏设置
     */
    public boolean FTDanmakuVisibility = true;

    public boolean FBDanmakuVisibility = true;

    public boolean L2RDanmakuVisibility = true;

    public boolean R2LDanmakuVisibility = true;

    public boolean SecialDanmakuVisibility = true;
    
    List<Integer> mFilterTypes = new ArrayList<Integer>();

    /**
     * 同屏弹幕数量 -1 按绘制效率自动调整 0 无限制 n 同屏最大显示n个弹幕
     */
    public int maximumNumsInScreen = -1;

    /**
     * 默认滚动速度系数
     */
    public float scrollSpeedFactor = 1.0f;


    /**
     * 绘制刷新率(毫秒)
     */
    public int refreshRateMS = 15;

    /**
     * 描边/阴影类型
     */
    public enum BorderType {
        NONE, SHADOW, STROKEN
    }
    
    public final static int DANMAKU_STYLE_DEFAULT = -1; // 自动
    public final static int DANMAKU_STYLE_NONE = 0; // 无
    public final static int DANMAKU_STYLE_SHADOW = 1; // 阴影
    public final static int DANMAKU_STYLE_STROKEN = 2; // 描边
    public final static int DANMAKU_STYLE_PROJECTION = 3; // 投影

    public BorderType shadowType = BorderType.SHADOW;

    public int shadowRadius = 3;

    List<Integer> mColorValueWhiteList = new ArrayList<Integer>();
    
    List<Integer> mUserIdBlackList = new ArrayList<Integer>(); 
    
    List<String> mUserHashBlackList = new ArrayList<String>();

    private ArrayList<ConfigChangedCallback> mCallbackList;

    private boolean mBlockGuestDanmaku = false;

    private boolean mDuplicateMergingEnable = false;

    /**
     * set typeface
     * 
     * @param font
     */
    public DanmakuGlobalConfig setTypeface(Typeface font) {
        if (mFont != font) {
            mFont = font;
            AndroidDisplayer.clearTextHeightCache();
            AndroidDisplayer.setTypeFace(font);
            notifyConfigureChanged(DanmakuConfigTag.TYPEFACE);
        }
        return this;
    }

    public DanmakuGlobalConfig setDanmakuTransparency(float p) {
        int newTransparency = (int) (p * AlphaValue.MAX);
        if (newTransparency != transparency) {
            transparency = newTransparency;
            isTranslucent = (newTransparency != AlphaValue.MAX);
            notifyConfigureChanged(DanmakuConfigTag.TRANSPARENCY, p);
        }
        return this;
    }

    public DanmakuGlobalConfig setScaleTextSize(float p) {
        if (scaleTextSize != p) {
            scaleTextSize = p;
            AndroidDisplayer.clearTextHeightCache();
            GlobalFlagValues.updateMeasureFlag();
            GlobalFlagValues.updateVisibleFlag();
            notifyConfigureChanged(DanmakuConfigTag.SCALE_TEXTSIZE, p);
        }
        isTextScaled = (scaleTextSize != 1f);
        return this;
    }

    /**
     * @return 是否显示顶部弹幕
     */
    public boolean getFTDanmakuVisibility() {
        return FTDanmakuVisibility;
    }

    /**
     * 设置是否显示顶部弹幕
     * 
     * @param visible
     */
    public DanmakuGlobalConfig setFTDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_FIX_TOP);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        if (FTDanmakuVisibility != visible) {
            FTDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.FT_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    private <T> void setFilterData(String tag, T data) {
        @SuppressWarnings("unchecked")
        IDanmakuFilter<T> filter = (IDanmakuFilter<T>) DanmakuFilters.getDefault().get(tag);
        filter.setData(data);
    }

    private void setDanmakuVisible(boolean visible, int type) {
        if (visible) {
            mFilterTypes.remove(Integer.valueOf(type));
        } else if (!mFilterTypes.contains(Integer.valueOf(type))) {
            mFilterTypes.add(type);
        }
    }

    /**
     * @return 是否显示底部弹幕
     */
    public boolean getFBDanmakuVisibility() {
        return FBDanmakuVisibility;
    }

    /**
     * 设置是否显示底部弹幕
     * 
     * @param visible
     */
    public DanmakuGlobalConfig setFBDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_FIX_BOTTOM);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        if (FBDanmakuVisibility != visible) {
            FBDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.FB_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * @return 是否显示左右滚动弹幕
     */
    public boolean getL2RDanmakuVisibility() {
        return L2RDanmakuVisibility;
    }

    /**
     * 设置是否显示左右滚动弹幕
     * 
     * @param visible
     */
    public DanmakuGlobalConfig setL2RDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SCROLL_LR);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        if(L2RDanmakuVisibility != visible){
            L2RDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.L2R_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * @return 是否显示右左滚动弹幕
     */
    public boolean getR2LDanmakuVisibility() {
        return R2LDanmakuVisibility;
    }

    /**
     * 设置是否显示右左滚动弹幕
     * 
     * @param visible
     */
    public DanmakuGlobalConfig setR2LDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SCROLL_RL);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        if (R2LDanmakuVisibility != visible) {
            R2LDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.R2L_DANMAKU_VISIBILIY, visible);
        }
        return this;
    }

    /**
     * @return 是否显示特殊弹幕
     */
    public boolean getSecialDanmakuVisibility() {
        return SecialDanmakuVisibility;
    }

    /**
     * 设置是否显示特殊弹幕
     * 
     * @param visible
     */
    public DanmakuGlobalConfig setSpecialDanmakuVisibility(boolean visible) {
        setDanmakuVisible(visible, BaseDanmaku.TYPE_SPECIAL);
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes);
        if (SecialDanmakuVisibility != visible) {
            SecialDanmakuVisibility = visible;
            notifyConfigureChanged(DanmakuConfigTag.SPECIAL_DANMAKU_VISIBILITY, visible);
        }
        return this;
    }

    /**
     * 设置同屏弹幕密度 -1自动 0无限制
     * 
     * @param maxSize
     * @return
     */
    public DanmakuGlobalConfig setMaximumVisibleSizeInScreen(int maxSize) {
        maximumNumsInScreen = maxSize;
        // 无限制
        if (maxSize == 0) {
            DanmakuFilters.getDefault()
                    .unregisterFilter(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER);
            DanmakuFilters.getDefault().unregisterFilter(DanmakuFilters.TAG_ELAPSED_TIME_FILTER);
            notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
            return this;
        }
        // 自动调整
        if (maxSize == -1) {
            DanmakuFilters.getDefault()
                    .unregisterFilter(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER);
            DanmakuFilters.getDefault()
                    .registerFilter(DanmakuFilters.TAG_ELAPSED_TIME_FILTER);
            notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
            return this;
        }
        setFilterData(DanmakuFilters.TAG_QUANTITY_DANMAKU_FILTER, maxSize);
        notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_NUMS_IN_SCREEN, maxSize);
        return this;
    }

    /**
     * 设置描边样式
     * 
     * @param type DANMAKU_STYLE_NONE DANMAKU_STYLE_SHADOW or
     *            DANMAKU_STYLE_STROKEN or DANMAKU_STYLE_PROJECTION
     * @param values
     *        DANMAKU_STYLE_SHADOW 阴影模式下，values传入阴影半径
     *        DANMAKU_STYLE_STROKEN 描边模式下，values传入描边宽度
     *        DANMAKU_STYLE_PROJECTION
     *            投影模式下，values传入offsetX, offsetY, alpha
     *                offsetX/offsetY: x/y 方向上的偏移量
     *                alpha: 投影透明度 [0...255]
     * @return
     */
    public DanmakuGlobalConfig setDanmakuStyle(int style, float... values) {
        switch (style) {
            case DANMAKU_STYLE_NONE:
                AndroidDisplayer.CONFIG_HAS_SHADOW = false;
                AndroidDisplayer.CONFIG_HAS_STROKE = false;
                AndroidDisplayer.CONFIG_HAS_PROJECTION = false;
                break;
            case DANMAKU_STYLE_SHADOW:
                AndroidDisplayer.CONFIG_HAS_SHADOW = true;
                AndroidDisplayer.CONFIG_HAS_STROKE = false;
                AndroidDisplayer.CONFIG_HAS_PROJECTION = false;
                AndroidDisplayer.setShadowRadius(values[0]);
                break;
            case DANMAKU_STYLE_DEFAULT:
            case DANMAKU_STYLE_STROKEN:
                AndroidDisplayer.CONFIG_HAS_SHADOW = false;
                AndroidDisplayer.CONFIG_HAS_STROKE = true;
                AndroidDisplayer.CONFIG_HAS_PROJECTION = false;
                AndroidDisplayer.setPaintStorkeWidth(values[0]);
                break;
            case DANMAKU_STYLE_PROJECTION:
                AndroidDisplayer.CONFIG_HAS_SHADOW = false;
                AndroidDisplayer.CONFIG_HAS_STROKE = false;
                AndroidDisplayer.CONFIG_HAS_PROJECTION = true;
                AndroidDisplayer.setProjectionConfig(values[0], values[1], (int)values[2]);
                break;
        }
        notifyConfigureChanged(DanmakuConfigTag.DANMAKU_STYLE, style, values[0]);
        return this;
    }

    /**
     * 设置是否粗体显示,对某些字体无效
     * 
     * @param bold
     * @return
     */
    public DanmakuGlobalConfig setDanmakuBold(boolean bold) {
        AndroidDisplayer.setFakeBoldText(bold);
        notifyConfigureChanged(DanmakuConfigTag.DANMAKU_BOLD, bold);
        return this;
    }
    
    /**
     * 设置色彩过滤弹幕白名单
     * @param colors
     * @return
     */
    public DanmakuGlobalConfig setColorValueWhiteList(Integer... colors) {
        mColorValueWhiteList.clear();
        if (colors == null || colors.length == 0) {
            DanmakuFilters.getDefault().unregisterFilter(
                    DanmakuFilters.TAG_TEXT_COLOR_DANMAKU_FILTER);
        } else {
            Collections.addAll(mColorValueWhiteList, colors);
            setFilterData(DanmakuFilters.TAG_TEXT_COLOR_DANMAKU_FILTER, mColorValueWhiteList);
        }
        notifyConfigureChanged(DanmakuConfigTag.COLOR_VALUE_WHITE_LIST, mColorValueWhiteList);
        return this;
    }
    
    public List<Integer> getColorValueWhiteList(){
        return mColorValueWhiteList;
    }
    
    /**
     * 设置屏蔽弹幕用户hash
     * @param hashes 
     * @return
     */
    public DanmakuGlobalConfig setUserHashBlackList(String... hashes) {
        mUserHashBlackList.clear();
        if (hashes == null || hashes.length == 0) {
            DanmakuFilters.getDefault().unregisterFilter(
                    DanmakuFilters.TAG_USER_HASH_FILTER);
        } else {
            Collections.addAll(mUserHashBlackList, hashes);
            setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        }
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    public DanmakuGlobalConfig removeUserHashBlackList(String... hashes){
        if(hashes == null || hashes.length == 0) {
            return this;
        }
        for (String hash : hashes) {
            mUserHashBlackList.remove(hash);
        }
        setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    /**
     * 添加屏蔽用户
     * @param hashes
     * @return
     */
    public DanmakuGlobalConfig addUserHashBlackList(String... hashes){
        if(hashes == null || hashes.length == 0) {
            return this;
        }
        Collections.addAll(mUserHashBlackList, hashes);
        setFilterData(DanmakuFilters.TAG_USER_HASH_FILTER, mUserHashBlackList);
        notifyConfigureChanged(DanmakuConfigTag.USER_HASH_BLACK_LIST, mUserHashBlackList);
        return this;
    }
    
    public List<String> getUserHashBlackList(){
        return mUserHashBlackList;
    }
    
    
    /**
     * 设置屏蔽弹幕用户id , 0 表示游客弹幕
     * @param ids 
     * @return
     */
    public DanmakuGlobalConfig setUserIdBlackList(Integer... ids) {
        mUserIdBlackList.clear();
        if (ids == null || ids.length == 0) {
            DanmakuFilters.getDefault().unregisterFilter(
                    DanmakuFilters.TAG_USER_ID_FILTER);
        } else {
            Collections.addAll(mUserIdBlackList, ids);
            setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        }
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    public DanmakuGlobalConfig removeUserIdBlackList(Integer... ids){
        if(ids == null || ids.length == 0) {
            return this;
        }
        for (Integer id : ids) {
            mUserIdBlackList.remove(id);
        }
        setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    /**
     * 添加屏蔽用户
     * @param ids
     * @return
     */
    public DanmakuGlobalConfig addUserIdBlackList(Integer... ids){
        if(ids == null || ids.length == 0) {
            return this;
        }
        Collections.addAll(mUserIdBlackList, ids);
        setFilterData(DanmakuFilters.TAG_USER_ID_FILTER, mUserIdBlackList);
        notifyConfigureChanged(DanmakuConfigTag.USER_ID_BLACK_LIST, mUserIdBlackList);
        return this;
    }
    
    public List<Integer> getUserIdBlackList(){
        return mUserIdBlackList;
    }
    
    /**
     * 设置是否屏蔽游客弹幕
     * @param block true屏蔽，false不屏蔽
     * @return
     */
    public DanmakuGlobalConfig blockGuestDanmaku(boolean block) {
        if (mBlockGuestDanmaku != block) {
            mBlockGuestDanmaku = block;
            if (block) {
                setFilterData(DanmakuFilters.TAG_GUEST_FILTER, block);
            } else {
                DanmakuFilters.getDefault().unregisterFilter(DanmakuFilters.TAG_GUEST_FILTER);
            }
            notifyConfigureChanged(DanmakuConfigTag.BLOCK_GUEST_DANMAKU, block);
        }
        return this;
    }
    
    /**
     * 设置弹幕滚动速度系数,只对滚动弹幕有效
     * @param p
     * @return
     */
    public DanmakuGlobalConfig setScrollSpeedFactor(float p){
        if (scrollSpeedFactor != p) {
            scrollSpeedFactor = p;
            DanmakuFactory.updateDurationFactor(p);
            GlobalFlagValues.updateMeasureFlag();
            GlobalFlagValues.updateVisibleFlag();
            notifyConfigureChanged(DanmakuConfigTag.SCROLL_SPEED_FACTOR, p);
        }
        return this;
    }
    
    /**
     * 设置是否启用合并重复弹幕
     * @param enable
     * @return
     */
    public DanmakuGlobalConfig setDuplicateMergingEnabled(boolean enable) {
        if (mDuplicateMergingEnable != enable) {
            mDuplicateMergingEnable = enable;
            notifyConfigureChanged(DanmakuConfigTag.DUPLICATE_MERGING_ENABLED, enable);
        }
        return this;
    }
    
    public boolean isDuplicateMergingEnabled() {
        return mDuplicateMergingEnable;
    }
    
    
    public interface ConfigChangedCallback {
        public boolean onDanmakuConfigChanged(DanmakuGlobalConfig config, DanmakuConfigTag tag,
                Object... value);
    }

    public void registerConfigChangedCallback(ConfigChangedCallback listener) {
        if (mCallbackList == null) {
            mCallbackList = new ArrayList<ConfigChangedCallback>();
        }
        mCallbackList.add(listener);
    }

    public void unregisterConfigChangedCallback(ConfigChangedCallback listener) {
        if (mCallbackList == null)
            return;
        mCallbackList.remove(listener);
    }

    private void notifyConfigureChanged(DanmakuConfigTag tag, Object... values) {
        if (mCallbackList != null) {
            for (ConfigChangedCallback cb : mCallbackList) {
                cb.onDanmakuConfigChanged(this, tag, values);
            }
        }
    }

}
