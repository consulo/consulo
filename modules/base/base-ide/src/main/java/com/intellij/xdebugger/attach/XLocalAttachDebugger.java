// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xdebugger.attach;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

public interface XLocalAttachDebugger extends XAttachDebugger {
  @Override
  @Nonnull
  String getDebuggerDisplayName();

  void attachDebugSession(@Nonnull Project project, @Nonnull ProcessInfo info) throws ExecutionException;

  @Override
  default void attachDebugSession(@Nonnull Project project, @Nonnull XAttachHost hostInfo, @Nonnull ProcessInfo info) throws ExecutionException {
    attachDebugSession(project, info);
  }
}
