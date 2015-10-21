package master.flame.danmaku.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.lang.ref.SoftReference;

import master.flame.danmaku.danmaku.model.BaseDanmaku;

/**
 * Created by ch on 15-7-16.
 */
public class SpannedCacheStuffer extends SimpleTextCacheStuffer {

    @Override
    public void measure(BaseDanmaku danmaku, TextPaint paint) {
        if (danmaku.text instanceof Spanned) {
            CharSequence text = null;
            if (danmaku.text instanceof SpannableStringBuilder) {
                text = new SpannableStringBuilder(danmaku.text);
            } else if (danmaku.text instanceof Spannable) {
                text = Spannable.Factory.getInstance().newSpannable(danmaku.text);
            } else if (danmaku.text instanceof SpannedString) {
                text = new SpannedString(danmaku.text);
            }
            if (text != null) {
                //处理文本宽高 TextView 自动换行原理也是用StaticLayout；这里不换行
//                1.0f 相对行间距，相对字体大小，1.5f表示行间距为1.5倍的字体高度。
//                0.0f 在基础行距上添加多少
//                实际行间距等于这两者的和。
//                http://ithtw.com/875.html
                StaticLayout staticLayout = new StaticLayout(text, paint, (int) StaticLayout.getDesiredWidth(danmaku.text, paint), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
                danmaku.paintWidth = staticLayout.getWidth();
                danmaku.paintHeight = staticLayout.getHeight();
                danmaku.obj = new SoftReference<StaticLayout>(staticLayout);
                return;
            }
        }
        super.measure(danmaku, paint);
    }

    @Override
    public void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
        if (danmaku.obj == null) {
            super.drawStroke(danmaku, lineText, canvas, left, top, paint);
        }
    }

    @Override
    public void drawText(BaseDanmaku danmaku, String lineText, Canvas canvas, float left, float top, Paint paint) {
        if (danmaku.obj == null) {
            super.drawText(danmaku, lineText, canvas, left, top, paint);
            return;
        }
        SoftReference<StaticLayout> reference = (SoftReference<StaticLayout>) danmaku.obj;
        StaticLayout staticLayout = reference.get();
        if (staticLayout == null) {
            super.drawText(danmaku, lineText, canvas, left, top, paint);
            return;
        }
        boolean needRestore = false;
        if (left != 0 && top != 0) {
            canvas.save();
            canvas.translate(left, top + paint.ascent());
            needRestore = true;
        }
        staticLayout.draw(canvas);
        if (needRestore) {
            canvas.restore();
        }
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        System.gc();
    }
}
