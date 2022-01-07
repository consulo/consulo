// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic;

import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  @Nonnull
  InstanceHolder ourInstance = new InstanceHolder();

  static boolean isEnabled() {
    return ourInstance.myIsEnabled;
  }

  @Nullable
  static EventWatcher getInstanceOrNull() {
    return null;
  }

  @RequiredUIAccess
  void runnableStarted(@Nonnull Runnable runnable, long startedAt);

  @RequiredUIAccess
  void runnableFinished(@Nonnull Runnable runnable, long finishedAt);

  @RequiredUIAccess
  void edtEventStarted(@Nonnull AWTEvent event, long startedAt);

  @RequiredUIAccess
  void edtEventFinished(@Nonnull AWTEvent event, long finishedAt);

  void reset();

  void logTimeMillis(@Nonnull String processId, long startedAt, @Nonnull Class<? extends Runnable> runnableClass);

  default void logTimeMillis(@Nonnull String processId, long startedAt) {
    logTimeMillis(processId, startedAt, Runnable.class);
  }
}
