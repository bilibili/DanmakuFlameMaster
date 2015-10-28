
package master.flame.danmaku.danmaku.util;

import android.app.ActivityManager;
import android.content.Context;

public class AndroidUtils {
/*    Return the approximate per-application memory class of the current device.
    This gives you an idea of how hard a memory limit you should impose on your application to let the overall system work best.
    The returned value is in megabytes; the baseline Android memory class is 16 (which happens to be the Java heap limit of those devices);
    some device with more memory may return 24 or even higher numbers.*/

    public static int getMemoryClass(final Context context) {
        return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
}
