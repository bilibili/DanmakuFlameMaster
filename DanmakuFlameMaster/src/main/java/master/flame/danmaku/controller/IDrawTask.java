
package master.flame.danmaku.controller;

import android.graphics.Canvas;

public interface IDrawTask {

    public void draw(Canvas canvas);

    public void reset();

    public void seek(long mills);

    public interface TaskListener {
        public void ready();
    }

}
