
package master.flame.danmaku.danmaku.model;

public interface IDrawingCache<T> {

    public void build(int w, int h, int density);

    public T get();

    public void destroy();

    public int size();

}
