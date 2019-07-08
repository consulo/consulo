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

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-02
 */
public final class ExternalChangeMarker {
  public static final int IgnorePsiEventsMarker = 1;
  public static final int ExternalChangeAction = 2;
  public static final int ExternalDocumentChange = 3;

  private static final int IgnorePsiEventsMarker_mask = 1 << 1;
  private static final int ExternalChangeAction_mask = 1 << 2;
  private static final int ExternalDocumentChange_mask = 1 << 3;

  private static ThreadLocal<Integer> ourIgnorePsiEventsMarker = ThreadLocal.withInitial(() -> 0);

  public static void mark(Runnable subRunnable, int flags) {
    mark(() -> {
      subRunnable.run();
      return null;
    }, flags);
  }

  public static <T> T mark(Supplier<T> subRunnable, int markerId) {
    try {
      ourIgnorePsiEventsMarker.set(setOrRemoveMask(markerId, ourIgnorePsiEventsMarker.get(), true));

      return subRunnable.get();
    }
    finally {
      ourIgnorePsiEventsMarker.set(setOrRemoveMask(markerId, ourIgnorePsiEventsMarker.get(), false));
    }
  }

  private static int setOrRemoveMask(int markerId, int oldValue, boolean value) {
    int newValue = oldValue;
    switch (markerId) {
      case IgnorePsiEventsMarker:
        newValue = BitUtil.set(newValue, IgnorePsiEventsMarker_mask, value);
      case ExternalChangeAction:
        newValue = BitUtil.set(newValue, ExternalChangeAction_mask, value);
      case ExternalDocumentChange:
        newValue = BitUtil.set(newValue, ExternalDocumentChange_mask, value);
    }
    return newValue;
  }

  public static boolean isMarked(int markerId) {
    switch (markerId) {
      case IgnorePsiEventsMarker:
        return BitUtil.isSet(ourIgnorePsiEventsMarker.get(), IgnorePsiEventsMarker_mask);
      case ExternalChangeAction:
        return BitUtil.isSet(ourIgnorePsiEventsMarker.get(), IgnorePsiEventsMarker_mask) || BitUtil.isSet(ourIgnorePsiEventsMarker.get(), ExternalChangeAction_mask);
      case ExternalDocumentChange:
        return BitUtil.isSet(ourIgnorePsiEventsMarker.get(), IgnorePsiEventsMarker_mask) || BitUtil.isSet(ourIgnorePsiEventsMarker.get(), ExternalChangeAction_mask) ||
               BitUtil.isSet(ourIgnorePsiEventsMarker.get(), ExternalDocumentChange_mask);
      default:
        throw new IllegalArgumentException(String.valueOf(markerId));
    }
  }
}