package master.flame.danmaku.gl;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.renderer.IRenderer;
import master.flame.danmaku.danmaku.util.DanmakuUtils;
import master.flame.danmaku.gl.glview.controller.TextureGLSurfaceViewRenderer;

/**
 * 创建人:yangzhiqian
 * 创建时间:2018/7/11 15:12
 * 备注:
 */
public class AndroidGLDisplayer extends AndroidDisplayer {

    private TextureGLSurfaceViewRenderer mRenderer;

    public AndroidGLDisplayer(DanmakuContext context) {
        super(context);
    }

    @Override
    public int draw(BaseDanmaku danmaku) {
        if (danmaku == null || danmaku.isTimeOut()) {
            return IRenderer.NOTHING_RENDERING;
        }
        if (danmaku.mGLTextureId != 0) {
            //有纹理id表示一定添加过
            return IRenderer.CACHE_RENDERING;
        }
        //没有cache，有两种情况:
        // bitmpa还没准备好
        // 已经有bitmap但没有加载到纹理中
        if (!DanmakuUtils.isCacheOk(danmaku)) {
            //其实没有绘制，但返回TEXT_RENDERING会通知GLDrawTask构建bitmap
            //暂时不添加到gl，等待bitmap创建成功后再添加
            return IRenderer.TEXT_RENDERING;
        } else {
            //添加到gl
//            mRenderer.getGLDanmakuHandler().addDanmaku(danmaku);
            return IRenderer.CACHE_RENDERING;
        }
    }

    public void setRenderer(TextureGLSurfaceViewRenderer mRenderer) {
        this.mRenderer = mRenderer;
    }

    public TextureGLSurfaceViewRenderer getRenderer() {
        return mRenderer;
    }
}
