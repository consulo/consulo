// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xdebugger.attach;

import com.intellij.execution.process.ProcessInfo;
import javax.annotation.Nonnull;

public interface XAttachProcessPresentationGroup extends XAttachPresentationGroup<ProcessInfo> {
  @Override
  default int compare(@Nonnull ProcessInfo a, @Nonnull ProcessInfo b) {
    return a.getPid() - b.getPid();
  }
}
