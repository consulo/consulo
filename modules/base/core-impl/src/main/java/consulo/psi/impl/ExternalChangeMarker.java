/*
 * Copyright 2013-2019 consulo.io
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
package consulo.psi.impl;

import com.intellij.util.BitUtil;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-02
 */
public final class ExternalChangeMarker {
  public static final int IgnorePsiEventsMarker = 1 << 1;
  public static final int ExternalChangeAction = 1 << 2 | IgnorePsiEventsMarker;
  public static final int ExternalDocumentChange = 1 << 3 | ExternalChangeAction;

  private static ThreadLocal<Deque<Integer>> ourIgnorePsiEventsMarker = ThreadLocal.withInitial(ConcurrentLinkedDeque::new);

  public static void mark(Runnable subRunnable, int flags) {
    mark(() -> {
      subRunnable.run();
      return null;
    }, flags);
  }

  public static <T> T mark(Supplier<T> subRunnable, int mask) {
    Deque<Integer> queue = ourIgnorePsiEventsMarker.get();
    int flags = BitUtil.set(0, mask, true);
    try {
      queue.addLast(flags);
      return subRunnable.get();
    }
    finally {
      Integer last = queue.removeLast();
      assert last == flags;
    }
  }

  public static boolean isMarked(int mask) {
    Deque<Integer> flags = ourIgnorePsiEventsMarker.get();
    for (Integer flag : flags) {
      if(BitUtil.isSet(flag, mask)) {
        return true;
      }
    }
    return false;
  }
}