package master.flame.danmaku.gl.utils;

import android.util.Log;

public class SpeedsMeasurement {
    private String mTag;
    private int index = 0;
    private int lastIndex = 0;
    private long lastTime;
    private int seconds = 0;

    public SpeedsMeasurement(String tag) {
        mTag = tag;
    }

    public void dot() {
        long curr = System.currentTimeMillis();
        if (curr - lastTime > 1000) {
            lastTime = curr;
            int speed = index - lastIndex;
            lastIndex = index;
            Log.d(mTag,
                    "sec=" + seconds +
                            "\tspeed=" + speed +
                            "\t avg=" + index / (seconds == 0 ? 1 : seconds) +
                            "\tindex=" + index);
            seconds++;
        }
        index++;
    }

    private static final int MAX_RECORDER = 100;
    private final long[] mTimes = new long[MAX_RECORDER];
    private int mTaskIndex = 0;
    private long mLastStart = 0;

    public void taskStart() {
        mLastStart = System.nanoTime();
    }

    public void taskEnd() {
        long current = System.nanoTime();
        long spends = current - mLastStart;
        mTimes[mTaskIndex % MAX_RECORDER] = spends;
        mTaskIndex++;
        if (mTaskIndex % MAX_RECORDER == 0) {
            long totalTimes = 0;
            for (long mTime : mTimes) {
                totalTimes += mTime;
            }
            Log.d(mTag, "avg=" + totalTimes / MAX_RECORDER +
                    "\ttotalIndex=" + mTaskIndex);
        }
    }
}
