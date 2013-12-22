
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

    /**
     * log output
     * 
     * @param tag
     */
    public void end(String tag) {
        end();
        Log.d(tag, Long.toString(counter) + " ms");
    }

    public AndroidCounter end() {

        counter = System.currentTimeMillis() - counter;

        return this;
    }
}
