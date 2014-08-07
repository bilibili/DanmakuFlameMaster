
package master.flame.danmaku.danmaku.model.android;

import android.text.TextPaint;

import master.flame.danmaku.danmaku.model.ICanvas.IPaint;

public class SimplePaint extends TextPaint implements IPaint<TextPaint> {

    @Override
    public TextPaint data() {
        return this;
    }

}
