
package master.flame.danmaku.danmaku.model.android;

import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.objectpool.Poolable;

public class DrawingCache implements IDrawingCache<DrawingCacheHolder>, Poolable<DrawingCache> {

    private DrawingCacheHolder mHolder;

    private int mSize = 0;

    private DrawingCache mNextElement;

    private boolean mIsPooled;
    
    private int referenceCount = 0;

    public DrawingCache() {
        mHolder = new DrawingCacheHolder();
    }

    @Override
    public void build(int w, int h, int density, boolean checkSizeEquals) {
        DrawingCacheHolder holder = mHolder;
        if (holder == null) {
            holder = new DrawingCacheHolder(w, h, density);
        } else {
            holder.buildCache(w, h, density, checkSizeEquals);
        }
        mHolder = holder;
        mSize = mHolder.bitmap.getRowBytes() * mHolder.bitmap.getHeight();
    }
    
    @Override
    public void erase() {
        final DrawingCacheHolder holder = mHolder;
        if (holder == null) {
            return;
        }
        holder.erase();
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
        referenceCount = 0;
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

    @Override
    public boolean hasReferences() {
        return referenceCount > 0;
    }

    @Override
    public void increaseReference() {
        referenceCount++;
    }

    @Override
    public void decreaseReference() {
        referenceCount--;
    }

    @Override
    public int width() {
        if (mHolder != null) {
            return mHolder.width;
        }
        return 0;
    }

    @Override
    public int height() {
        if (mHolder != null) {
            return mHolder.height;
        }
        return 0;
    }

}
