// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides storage for locks returned from ClassLoader#getClassLoadingLock implementation. The returned locks are stored via weak references,
 * so when they become unreachable the corresponding entries are removed from the map, so class names won't waste the memory.
 */
class ClassLoadingLocks {
  private final ConcurrentMap<String, WeakLockReference> myMap = new ConcurrentHashMap<String, WeakLockReference>();
  private final ReferenceQueue<Object> myQueue = new ReferenceQueue<Object>();

  Object getOrCreateLock(String className) {
    WeakLockReference lockReference = myMap.get(className);
    if (lockReference != null) {
      Object lock = lockReference.get();
      if (lock != null) {
        return lock;
      }
    }

    Object newLock = new Object();
    WeakLockReference newRef = new WeakLockReference(className, newLock, myQueue);
    while (true) {
      processQueue();
      WeakLockReference oldRef = myMap.putIfAbsent(className, newRef);
      if (oldRef == null) return newLock;
      Object oldLock = oldRef.get();
      if (oldLock != null) {
        return oldLock;
      }
      else {
        if (myMap.replace(className, oldRef, newRef)) return newLock;
      }
    }
  }

  private void processQueue() {
    while (true) {
      WeakLockReference ref = (WeakLockReference)myQueue.poll();
      if (ref == null) break;
      myMap.remove(ref.myClassName, ref);
    }
  }

  private static class WeakLockReference extends WeakReference<Object> {
    final String myClassName;

    private WeakLockReference(String className, Object lock, ReferenceQueue<Object> q) {
      super(lock, q);
      myClassName = className;
    }
  }
}
