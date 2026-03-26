// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection.impl.set;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;

/**
 * Weak hash set.
 * Null keys are NOT allowed
 */
public final class WeakHashSet<T> extends AbstractSet<T> {
  private final Set<MyRef<T>> set = new HashSet<>();
  private final ReferenceQueue<T> queue = new ReferenceQueue<>();

  private static class MyRef<T> extends WeakReference<T> {
    private final int myHashCode;

    MyRef(T referent, @Nullable ReferenceQueue<? super T> q) {
      super(referent, q);
      myHashCode = referent.hashCode();
    }

    @Override
    public int hashCode() {
      return myHashCode; // has to be stable even in presence of GC
    }

    // must compare to HardRef by its get().equals() for add()/remove()/contains() to put in correct bucket
    // must compare to another MyRef by identity to be able for remove() to remove GCed value from processQueue()
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MyRef)) return false;
      MyRef<?> otherRef = (MyRef<?>)obj;
      if (this instanceof HardRef || otherRef instanceof HardRef) {
        return Comparing.equal(otherRef.get(), get());
      }
      return this == obj;
    }
  }

  private static class HardRef<T> extends MyRef<T> {
    HardRef(T referent) {
      super(referent, null);
    }
  }

  @Override
  @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", comment = "Remove explicit casts")
  public Iterator<T> iterator() {
    return ContainerUtil.filterIterator(
      ContainerUtil.mapIterator(set.iterator(), (Function<MyRef<T>, @Nullable T>) Reference::get),
      Objects::nonNull
    );
  }

  @Override
  public int size() {
    return set.size();
  }

  @Override
  public boolean add(T t) {
    processQueue();
    MyRef<T> ref = new MyRef<>(t, queue);
    return set.add(ref);
  }

  @Override
  public boolean remove(Object o) {
    processQueue();
    //noinspection unchecked
    return set.remove(new HardRef<>((T)o));
  }

  @Override
  public boolean contains(Object o) {
    processQueue();
    //noinspection unchecked
    return set.contains(new HardRef<>((T)o));
  }

  @Override
  public void clear() {
    set.clear();
  }

  private void processQueue() {
    MyRef<T> ref;
    //noinspection unchecked
    while ((ref = (MyRef<T>)queue.poll()) != null) {
      set.remove(ref);
    }
  }
}
