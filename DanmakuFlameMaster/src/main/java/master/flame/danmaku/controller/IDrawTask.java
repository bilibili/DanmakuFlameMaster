
package master.flame.danmaku.controller;

import android.graphics.Canvas;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDrawTask {

    public void draw(Canvas canvas);

    public void reset();

    public void seek(long mills);

    public void quit();

    public void prepare();

    public void setParser(BaseDanmakuParser parser);

    public interface TaskListener {
        public void ready();
    }

}
