package master.flame.danmaku.danmaku.model;

public abstract class AbsDanmakuSync {

    public static final int SYNC_STATE_HALT = 1;

    public static final int SYNC_STATE_PLAYING = 2;

    /**
     * Get the uptime of timer synchronization
     *
     * @return
     */
    public abstract long getUptimeMillis();

    /**
     * Get the state of timer synchronization
     *
     * @return SYNC_STATE_HALT or SYNC_STATE_PLAYING
     */
    public abstract int getSyncState();

    /**
     * Get the threshold-time of timer synchronization
     * This value should be greater than or equal to 1000L
     *
     * @return
     */
    public long getThresholdTimeMills() {
        return 1500L;
    }

    /**
     * synchronize pause/resume state with outside playback
     * @return
     */
    public boolean isSyncPlayingState() {
        return false;
    }

}
