
package master.flame.danmaku.controller;

import android.graphics.Canvas;

public interface IDrawTask {

    public void draw(Canvas canvas);

    public interface TaskListener {
        public void ready();
    }

}
