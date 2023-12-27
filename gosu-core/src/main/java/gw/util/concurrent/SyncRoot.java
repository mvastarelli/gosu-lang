package gw.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface SyncRoot {
  public interface ReaderWriter {
    ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock _readLock = _lock.readLock();
    ReentrantReadWriteLock.WriteLock _writeLock = _lock.writeLock();

    default void acquireWrite( LockAction action ) {
      _writeLock.lock();
      try {
        action.act();
      }
      finally {
        _writeLock.unlock();
      }
    }

    default <T> T acquireWrite( LockFunc<T> func ) {
      _writeLock.lock();
      try {
        return func.func();
      }
      finally {
        _writeLock.unlock();
      }
    }

    default void acquireRead( LockAction action ) {
      _readLock.lock();
      try {
        action.act();
      }
      finally {
        _readLock.unlock();
      }
    }

    default <T> T acquireRead( LockFunc<T> func ) {
      _readLock.lock();
      try {
        return func.func();
      }
      finally {
        _readLock.unlock();
      }
    }
  }

  public interface Mutex {
    Object _sync = new Object();

    default void acquire(LockAction action ) {
      synchronized(_sync) {
        action.act();
      }
    }

    default <T> T acquire( LockFunc<T> func ) {
      synchronized(_sync) {
        return func.func();
      }
    }
  }

  @FunctionalInterface
  public interface LockAction {
    void act();
  }

  @FunctionalInterface
  public interface LockFunc<T> {
    T func();
  }
}
