
package master.flame.danmaku.danmaku.model;

public class Duration {

    private long mInitialDuration;

    private float factor = 1.0f;

    public long value;

    public Duration(long initialDuration) {
        mInitialDuration = initialDuration;
        value = initialDuration;
    }

    public void setFactor(float f) {
        if (factor != f) {
            factor = f;
            value = (long) (mInitialDuration * f);
        }
    }

}
