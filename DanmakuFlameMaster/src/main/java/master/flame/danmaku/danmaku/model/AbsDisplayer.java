package master.flame.danmaku.danmaku.model;

public abstract class AbsDisplayer<T> implements IDisplayer {
    
    public abstract T getExtraData();
    
    public abstract void setExtraData(T data);

}
