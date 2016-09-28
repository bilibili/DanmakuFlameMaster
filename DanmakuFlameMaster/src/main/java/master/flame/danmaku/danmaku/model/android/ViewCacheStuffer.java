package master.flame.danmaku.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import master.flame.danmaku.danmaku.model.BaseDanmaku;

public abstract class ViewCacheStuffer<VH extends ViewCacheStuffer.ViewHolder> extends BaseCacheStuffer {

    public static abstract class ViewHolder {

        protected final View itemView;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            this.itemView.measure(widthMeasureSpec, heightMeasureSpec);
        }

        public int getMeasureWidth() {
            return this.itemView.getMeasuredWidth();
        }

        public int getMeasureHeight() {
            return this.itemView.getMeasuredHeight();
        }

        public void layout(int l, int t, int r, int b) {
            this.itemView.layout(l, t, r, b);
        }

        public void draw(Canvas canvas, AndroidDisplayer.DisplayerConfig displayerConfig) {
            this.itemView.draw(canvas);
            //TODO: apply displayerConfig
        }
    }

    public static final int INVALID_TYPE = -1;
    public static final int MEASURE_VIEW_TYPE = -2;
    public static final int DRAW_VIEW_TYPE = -3;
    public static final int CACHE_VIEW_TYPE = -3;

    private final int mMaximumWidthPixels;
    private final int mMaximumHeightPixels;
    private SparseArray<List<VH>> mViewHolderArray = new SparseArray();

    public abstract VH onCreateViewHolder(int viewType);

    public abstract void onBindViewHolder(int viewType, VH viewHolder, BaseDanmaku danmaku, AndroidDisplayer.DisplayerConfig displayerConfig, TextPaint paint);

    public int getItemViewType(int position, BaseDanmaku danmaku) {
        return 0;
    }

    public ViewCacheStuffer() {
        mMaximumWidthPixels = -1;  // FIXME: get maximum of canvas
        mMaximumHeightPixels = -1;
    }

    @Override
    public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
        int viewType = getItemViewType(danmaku.index, danmaku);
        List<VH> viewHolders = mViewHolderArray.get(viewType);
        if (viewHolders == null) {
            viewHolders = new ArrayList<>();
            viewHolders.add(onCreateViewHolder(viewType));
            viewHolders.add(onCreateViewHolder(viewType));
            viewHolders.add(onCreateViewHolder(viewType));
            mViewHolderArray.put(viewType, viewHolders);
        }
        VH viewHolder = viewHolders.get(0);
        // TODO displayerConfig or TextPaint
        onBindViewHolder(viewType, viewHolder, danmaku, null, paint);
        viewHolder.measure(View.MeasureSpec.makeMeasureSpec(mMaximumWidthPixels, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(mMaximumHeightPixels, View.MeasureSpec.AT_MOST));
        viewHolder.layout(0, 0, viewHolder.getMeasureWidth(), viewHolder.getMeasureHeight());
        danmaku.paintWidth = viewHolder.getMeasureWidth();
        danmaku.paintHeight = viewHolder.getMeasureHeight();
    }

    @Override
    public void clearCaches() {

    }

    @Override
    public void releaseResource(BaseDanmaku danmaku) {
        super.releaseResource(danmaku);
        danmaku.tag = null;
    }

    @Override
    public void drawDanmaku(BaseDanmaku danmaku, Canvas canvas, float left, float top, boolean fromWorkerThread, AndroidDisplayer.DisplayerConfig displayerConfig) {
        int viewType = getItemViewType(danmaku.index, danmaku);
        List<VH> viewHolders = mViewHolderArray.get(viewType);
        VH viewHolder = null;
        if (viewHolders != null) {
            viewHolder = viewHolders.get(fromWorkerThread ? 1 : 2);
        }
        if (viewHolder == null) {
            return;
        }
        //ignore danmaku.padding, apply it onBindViewHolder
        displayerConfig.definePaintParams(fromWorkerThread);
        TextPaint paint = displayerConfig.getPaint(danmaku, fromWorkerThread);
        displayerConfig.applyPaintConfig(danmaku, paint, false);

        onBindViewHolder(viewType, viewHolder, danmaku, displayerConfig, paint);
        viewHolder.measure(View.MeasureSpec.makeMeasureSpec(Math.round(danmaku.paintWidth), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(Math.round(danmaku.paintHeight), View.MeasureSpec.EXACTLY));
        boolean needRestore = false;
        if (!fromWorkerThread) {
            canvas.save();
            canvas.translate(left, top);
            needRestore = true;
        }
        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = displayerConfig.getUnderlinePaint(danmaku);
            float bottom = top + danmaku.paintHeight - displayerConfig.UNDERLINE_HEIGHT;
            canvas.drawLine(left, bottom, left + danmaku.paintWidth, bottom, linePaint);
        }
        //draw border
        if (danmaku.borderColor != 0) {
            Paint borderPaint = displayerConfig.getBorderPaint(danmaku);
            canvas.drawRect(left, top, left + danmaku.paintWidth, top + danmaku.paintHeight,
                    borderPaint);
        }
        //draw danmaku
        viewHolder.layout(0, 0, (int) danmaku.paintWidth, (int) danmaku.paintHeight);
        viewHolder.draw(canvas, displayerConfig); //FIXME: handle canvas.getMaximumBitmapWidth()
        //TODO: stroke handle displayerConfig
        if (needRestore) {
            canvas.restore();
        }
    }

}