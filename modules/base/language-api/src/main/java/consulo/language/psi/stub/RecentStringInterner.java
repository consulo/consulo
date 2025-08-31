/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.psi.stub;

import consulo.application.util.LowMemoryWatcher;
import consulo.disposer.Disposable;
import consulo.util.collection.SLRUCache;

import jakarta.annotation.Nonnull;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecentStringInterner {
  private final int myStripeMask;
  private final SLRUCache<String, String>[] myInterns;
  private final Lock[] myStripeLocks;

  RecentStringInterner(@Nonnull Disposable parentDisposable) {
    final int stripes = 16;
    //noinspection unchecked
    myInterns = new SLRUCache[stripes];
    myStripeLocks = new Lock[myInterns.length];
    int capacity = 8192;
    for (int i = 0; i < myInterns.length; ++i) {
      myInterns[i] = new SLRUCache<>(capacity / stripes, capacity / stripes) {
        @Nonnull
        @Override
        public String createValue(String key) {
          return key;
        }

        @Override
        protected void putToProtectedQueue(String key, @Nonnull String value) {
          super.putToProtectedQueue(value, value);
        }
      };
      myStripeLocks[i] = new ReentrantLock();
    }

    assert Integer.highestOneBit(stripes) == stripes;
    myStripeMask = stripes - 1;
    LowMemoryWatcher.register(this::clear, parentDisposable);
  }

  public String get(String s) {
    if (s == null) return null;
    int stripe = Math.abs(s.hashCode()) & myStripeMask;
    try {
      myStripeLocks[stripe].lock();
      return myInterns[stripe].get(s);
    }
    finally {
      myStripeLocks[stripe].unlock();
    }
  }

  public void clear() {
    for (int i = 0; i < myInterns.length; ++i) {
      try {
        myStripeLocks[i].lock();
        myInterns[i].clear();
      }
      finally {
        myStripeLocks[i].unlock();
      }
    }
  }
}
