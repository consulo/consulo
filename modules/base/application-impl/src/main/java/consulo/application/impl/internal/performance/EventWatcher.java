// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.performance;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public interface EventWatcher {

  final class InstanceHolder {
    private
    @Nullable
    EventWatcher myInstance = null;
    private final boolean myIsEnabled = Boolean.getBoolean("idea.event.queue.dispatch.listen");

    private InstanceHolder() {
    }
  }

  
  InstanceHolder ourInstance = new InstanceHolder();

  static boolean isEnabled() {
    return ourInstance.myIsEnabled;
  }

  @Nullable
  static EventWatcher getInstanceOrNull() {
    return null;
  }

  @RequiredUIAccess
  void runnableStarted(Runnable runnable, long startedAt);

  @RequiredUIAccess
  void runnableFinished(Runnable runnable, long finishedAt);

  @RequiredUIAccess
  void edtEventStarted(AWTEvent event, long startedAt);

  @RequiredUIAccess
  void edtEventFinished(AWTEvent event, long finishedAt);

  void reset();

  void logTimeMillis(String processId, long startedAt, Class<? extends Runnable> runnableClass);

  default void logTimeMillis(String processId, long startedAt) {
    logTimeMillis(processId, startedAt, Runnable.class);
  }
}
