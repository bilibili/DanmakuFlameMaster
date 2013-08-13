
package master.flame.danmaku.danmaku.util;

import android.util.Log;

public class AndroidCounter {

    private long counter = 0;

    public AndroidCounter() {
    }

    public AndroidCounter begin() {

        counter = System.currentTimeMillis();

        return this;
    }

    public AndroidCounter end() {

        counter = System.currentTimeMillis() - counter;

        return this;
    }

    public long getDuration() {
        return counter;
    }

    public void log(String title) {

        Log.e(title, Long.toString(counter) + " ms");
    }
}
