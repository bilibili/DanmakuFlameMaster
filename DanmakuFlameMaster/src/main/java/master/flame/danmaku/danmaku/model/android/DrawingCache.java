
package master.flame.danmaku.danmaku.model.android;

import master.flame.danmaku.danmaku.model.IDrawingCache;

public class DrawingCache implements IDrawingCache<DrawingCacheHolder> {

    private DrawingCacheHolder mHolder;

    private int mSize = 0;

    @Override
    public void build(int w, int h, int density) {
        DrawingCacheHolder holder = mHolder;
        if (holder == null || holder.width != w || holder.height != h) {
            holder = new DrawingCacheHolder(w, h, density);
        }
        mHolder = holder;
        mSize = mHolder.bitmap.getRowBytes() * mHolder.bitmap.getHeight();
    }

    @Override
    public DrawingCacheHolder get() {
        return mHolder;
    }

    @Override
    public void destroy() {
        if (mHolder != null) {
            mHolder.recycle();
            mHolder = null;
        }
    }

    @Override
    public int size() {
        if (mHolder != null) {
            return mSize;
        }
        return 0;
    }
}
