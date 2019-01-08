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

import com.intellij.openapi.application.Application;
import com.intellij.util.BitUtil;
import consulo.annotations.RequiredWriteAction;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-01-02
 */
public class ExternalChangeMarker {
  public static final int IgnorePsiEventsMarker = 1 << 1;
  public static final int ExternalChangeAction = 1 << 2 | IgnorePsiEventsMarker;
  public static final int ExternalDocumentChange = 1 << 3 | ExternalChangeAction;
  public static final int DocumentRunnable = 1 << 4;

  private static ThreadLocal<Integer> ourIgnorePsiEventsMarker = ThreadLocal.withInitial(() -> 0);

  @RequiredWriteAction
  public static void mark(Runnable subRunnable, int flags) {
    mark(() -> {
      subRunnable.run();
      return null;
    }, flags);
  }

  @RequiredWriteAction
  public static <T> T mark(Supplier<T> subRunnable, int flags) {
    Application.get().assertWriteAccessAllowed();
    try {
      Integer oldValue = ourIgnorePsiEventsMarker.get();
      ourIgnorePsiEventsMarker.set(BitUtil.set(oldValue, flags, true));

      return subRunnable.get();
    }
    finally {
      Integer oldValue = ourIgnorePsiEventsMarker.get();
      ourIgnorePsiEventsMarker.set(BitUtil.set(oldValue, flags, false));
    }
  }

  @RequiredWriteAction
  public static boolean isMarked(int mask) {
    Application.get().assertWriteAccessAllowed();
    return BitUtil.isSet(ourIgnorePsiEventsMarker.get(), mask);
  }
}
