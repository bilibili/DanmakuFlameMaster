
package master.flame.danmaku.danmaku.model;

public abstract class RingBuffer<T> {

    T[] mBuffer;

    int mPutIndex = 0; /* 环形缓冲区的当前放入位置 */

    int mGetIndex = 0; /* 缓冲区的当前取出位置 */

    int n = 0; /* 环形缓冲区中的元素总数量 */

    int mCapacity; /* buffer容量 */

    public RingBuffer(int capacity) {
        mCapacity = capacity;
        initBuffer(mCapacity);
    }

    private void initBuffer(int capacity) {
        mBuffer = createBuffer(capacity);
    }

    protected abstract T[] createBuffer(int capacity);

    public boolean put(T obj) {
        if (n < mCapacity) {
            mBuffer[mPutIndex] = obj;
            mPutIndex = addring(mPutIndex);
            n++;
            return true;
        }
        return false;
    }

    private int addring(int i) {
        return (i + 1) == mCapacity ? 0 : i + 1;
    }

    public T get() {
        int pos;
        if (n > 0) {
            pos = mGetIndex;
            mGetIndex = addring(mGetIndex);
            n--;
            return mBuffer[pos];
        } else {
            return null;
        }
    }

    public int size() {
        return n;
    }

    public boolean isFull() {
        return n == mCapacity;
    }

    public boolean isEmpty() {
        return n == 0;
    }

}
