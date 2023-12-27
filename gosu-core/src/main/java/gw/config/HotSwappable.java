package gw.config;

public class HotSwappable<T> {
  private final ObjectInitializer<T> _initializer;
  private volatile T _value;

  public HotSwappable(ObjectInitializer<T> initializer) {
    _initializer = initializer;
    _value = _initializer.init();
  }

  public T get() {
    return _value;
  }

  public synchronized final void reset() {
    _value = _initializer.init();
  }

  @FunctionalInterface
  public interface ObjectInitializer<T> {
    T init();
  }
}
