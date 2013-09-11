
package master.flame.danmaku.danmaku.model;

public class SpecialDanmaku extends BaseDanmaku {

    public float beginX, beginY;

    public float endX, endY;

    public float deltaX, deltaY;

    public long translationDuration;

    public long translationStartDelay;

    public int beginAlpha;

    public int endAlpha;

    public int deltaAlpha;

    public long alphaDuration;

    public float rotateX, rotateZ;

    public float pivotX, pivotY;

    private float[] currStateValues = new float[4]; // currX,currY,currAlpha;

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        getRectAtTime(displayer, mTimer.currMillisecond);
    }

    @Override
    public float[] getRectAtTime(IDisplayer displayer, long currTime) {

        if (!isMeasured())
            return null;

        long deltaTime = currTime - time;

        // caculate alpha
        alpha = beginAlpha;
        if (alphaDuration > 0 && deltaAlpha != 0) {
            float alphaProgress = deltaTime / (float) alphaDuration;
            int vectorAlpha = (int) (deltaAlpha * alphaProgress);
            alpha = beginAlpha + vectorAlpha;
        }

        // caculate x y
        float currX = beginX;
        float currY = beginY;
        long dtime = deltaTime - translationStartDelay;
        if (translationDuration > 0 && dtime >= 0 && dtime <= translationDuration) {
            float tranalationProgress = dtime / (float) translationDuration;
            if (deltaX != 0) {
                float vectorX = deltaX * tranalationProgress;
                currX = beginX + vectorX;
            }
            if (deltaY != 0) {
                float vectorY = deltaY * tranalationProgress;
                currY = beginY + vectorY;
            }
        }

        currStateValues[0] = currX;
        currStateValues[1] = currY;
        currStateValues[2] = currX + paintWidth;
        currStateValues[3] = currY + paintHeight;

        this.visibility = isOutside() ? INVISIBLE : VISIBLE;

        return currStateValues;
    }

    @Override
    public float getLeft() {
        return currStateValues[0];
    }

    @Override
    public float getTop() {
        return currStateValues[1];
    }

    @Override
    public float getRight() {
        return currStateValues[2];
    }

    @Override
    public float getBottom() {
        return currStateValues[3];
    }

    @Override
    public int getType() {
        return TYPE_SPECIAL;
    }

    public void setTranslationData(float beginX, float beginY, float endX, float endY,
            long translationDuration, long translationStartDelay) {
        this.beginX = beginX;
        this.beginY = beginY;
        this.endX = endX;
        this.endY = endY;
        this.deltaX = endX - beginX;
        this.deltaY = endY - beginY;
        this.translationDuration = translationDuration;
        this.translationStartDelay = translationStartDelay;
    }

    public void setAlphaData(int beginAlpha, int endAlpha, long alphaDuration) {
        this.beginAlpha = beginAlpha;
        this.endAlpha = endAlpha;
        this.deltaAlpha = endAlpha - beginAlpha;
        this.alphaDuration = alphaDuration;
    }

}
