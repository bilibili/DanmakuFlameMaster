
package master.flame.danmaku.danmaku.model.android;

import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.objectpool.Poolable;

public class DrawingCache implements IDrawingCache<DrawingCacheHolder>, Poolable<DrawingCache> {

    private DrawingCacheHolder mHolder;

    private int mSize = 0;

    private DrawingCache mNextElement;

    private boolean mIsPooled;

    public DrawingCache() {
        mHolder = new DrawingCacheHolder();
    }

    @Override
    public void build(int w, int h, int density) {
        DrawingCacheHolder holder = mHolder;
        if (holder == null) {
            holder = new DrawingCacheHolder(w, h, density);
        } else {
            holder.buildCache(w, h, density);
        }
        mHolder = holder;
        mSize = mHolder.bitmap.getRowBytes() * mHolder.bitmap.getHeight();
    }

    @Override
    public DrawingCacheHolder get() {
        if (mHolder == null || mHolder.bitmap == null) {
            return null;
        }
        return mHolder;
    }

    @Override
    public void destroy() {
        if (mHolder != null) {
            mHolder.recycle();
        }
        mSize = 0;
    }

    @Override
    public int size() {
        if (mHolder != null) {
            return mSize;
        }
        return 0;
    }

    @Override
    public void setNextPoolable(DrawingCache element) {
        mNextElement = element;
    }

    @Override
    public DrawingCache getNextPoolable() {
        return mNextElement;
    }

    @Override
    public boolean isPooled() {
        return mIsPooled;
    }

    @Override
    public void setPooled(boolean isPooled) {
        mIsPooled = isPooled;
    }
}
