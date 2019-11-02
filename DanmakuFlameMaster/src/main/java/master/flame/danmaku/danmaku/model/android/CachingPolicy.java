package master.flame.danmaku.danmaku.model.android;

/**
 * Created by ch on 17/4/28. <br/>
 * The cacing policy apply to {@link master.flame.danmaku.controller.CacheManagingDrawTask}
 * 提供缓存相关的策略设置: <br/>
 * 1.缓存格式  ARGB_4444  ARGB_8888 <br/>
 * 2.缓存池总容量大小百分比系数(0.0~1.0) <br/>
 * 3.过期缓存回收频率 <br/>
 * 4.缓存回收条件内存占比阈值 <br/>
 * 5.可复用缓存尺寸调节
 */

public class CachingPolicy {

    public final static int BMP_BPP_ARGB_4444 = 16;
    public final static int BMP_BPP_ARGB_8888 = 32;
    public final static int CACHE_PERIOD_AUTO = 0;
    public final static int CACHE_PERIOD_NOT_RECYCLE = -1;

    public final static CachingPolicy POLICY_LAZY = new CachingPolicy(BMP_BPP_ARGB_4444, 0.3f, CACHE_PERIOD_AUTO, 50, 0.01f);
    public final static CachingPolicy POLICY_GREEDY = new CachingPolicy(BMP_BPP_ARGB_4444, 0.5f, CACHE_PERIOD_NOT_RECYCLE, 50, 0.005f);
    public final static CachingPolicy POLICY_DEFAULT = POLICY_LAZY;


    public CachingPolicy(int bitsPerPixelOfCache, float maxCachePoolSizeFactorPercentage, long periodOfRecycle, int reusableOffsetPixel, float forceRecyleThreshold) {
        this.bitsPerPixelOfCache = bitsPerPixelOfCache;
        /* Note: as of {@link android.os.Build.VERSION_CODES#KITKAT},
        * any bitmap created with this configuration will be created
        * using {@link #ARGB_8888} instead.*/
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            this.bitsPerPixelOfCache = BMP_BPP_ARGB_8888;
        }
        this.maxCachePoolSizeFactorPercentage = maxCachePoolSizeFactorPercentage;
        this.periodOfRecycle = periodOfRecycle;
        this.reusableOffsetPixel = reusableOffsetPixel;
        this.forceRecyleThreshold = forceRecyleThreshold;
    }

    /**
     * 缓存bitmap的格式, ARGB_4444 = 16  ARGB_8888 = 32
     * use BMP_BPP_ARGB_4444 or BMP_BPP_ARGB_8888
     *
     * Note: as of {@link android.os.Build.VERSION_CODES#KITKAT},
     * any bitmap created with this configuration will be created
     * using {@link #ARGB_8888} instead.
     */
    public int bitsPerPixelOfCache = BMP_BPP_ARGB_4444;

    /**
     * 0.0 ~ 1.0, 超过0.5的话有OOM风险
     */
    public float maxCachePoolSizeFactorPercentage = 0.3f;

    /**
     * 回收周期
     *
     * @see CACHE_PERIOD_AUTO 0: 默认
     * @see CACHE_PERIOD_NOT_RECYCLE -1： 不回收
     */
    public long periodOfRecycle = CACHE_PERIOD_AUTO;

//    public DanmakuTimer recyleTimer = new DanmakuTimer(SystemClock.uptimeMillis());

    /**
     * 内存占用大小超过总容量一定比例值(forceRecyleThreshold值)的缓存,在回收时进行主动回收,忽略CACHE_PERIOD_NOT_RECYCLE
     */
    public float forceRecyleThreshold = 0.01f;

    /**
     * @see master.flame.danmaku.controller.CacheManagingDrawTask.CacheManager#findReusableCache
     */
    public int reusableOffsetPixel = 0;

    public int maxTimesOfStrictReusableFinds = 20;

    public int maxTimesOfReusableFinds = 150;


    /**
     * 是否开启了弹幕缓存模式
     */
    public boolean mCacheDrawEnabled = false;
    /**
     * 是否允许弹幕在使用缓存的状态下延迟显示
     * 对于使用缓存的弹幕，直接绘制弹幕缓存的速度比绘制弹幕速度快得多
     * 在某些极端的情况下(弹幕密度大或则预留创建缓存的时间短)，弹幕缓存无法及时提供，所以此标识表明是否允许在没有
     * 准备好弹幕缓存的情况下延迟弹幕显示，直到弹幕缓存创建好。
     * 该变量在绘制{@link AndroidDisplayer}和
     * {@link master.flame.danmaku.controller.CacheManagingDrawTask}中使用到
     * 默认不延迟。
     * 该值只有在{@link #mCacheDrawEnabled}状态为true才有效
     */
    public boolean mAllowDelayInCacheModel = false;
}
