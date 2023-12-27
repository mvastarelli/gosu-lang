/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class LockingLazyVar<T>
{
  private boolean _hasValue = false;
  private T _value = null;

  private final ReentrantReadWriteLock _lock;

  public LockingLazyVar() {
    this(new ReentrantReadWriteLock() );
  }

  public LockingLazyVar(ReentrantReadWriteLock lock) {
    _lock = lock;
  }

  /**
   * @return the value of this lazy var, created if necessary
   */
  public final T get() {
    _lock.readLock().lock();

    try {
      if (_hasValue) {
        return _value;
      }
    } finally {
      _lock.readLock().unlock();
    }

    _lock.writeLock().lock();

    try {
      if (_hasValue) {
        return _value;
      } else {
        _value = init();
        _hasValue = true;
        return _value;
      }
    } finally {
      _lock.writeLock().unlock();
    }
  }

  protected abstract T init();

  /**
   * Clears the variable, forcing the next call to {@link #get()} to re-calculate
   * the value.
   */
  public final T clear()
  {
    _lock.writeLock().lock();

    try {
      var result = _value;
      _value = null;
      _hasValue = false;
      return result;
    } finally {
      _lock.writeLock().unlock();
    }
  }

  public final void clearNoLock()
  {
    _value = null;
    _hasValue = false;
  }

  public boolean isLoaded() {
    _lock.readLock().lock();

    try {
      return _hasValue;
    } finally {
      _lock.readLock().unlock();
    }
  }

  /**
   * A simple init interface to make LockingLazyVar's easier to construct
   * from gosu.
   */
  public interface LazyVarInit<Q> {
    Q init();
  }

  /**
   * Creates a new LockingLazyVar based on the type of the LazyVarInit passed in.
   * This method is intended to be called with blocks from Gosu.
   */
  public static <Q> LockingLazyVar<Q> make( final LazyVarInit<Q> init ) {
    return new LockingLazyVar<Q>(){
      protected Q init()
      {
        return init.init();
      }
    };
  }
}
