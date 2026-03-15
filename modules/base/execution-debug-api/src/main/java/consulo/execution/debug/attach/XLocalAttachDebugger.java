// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.attach;

import consulo.platform.ProcessInfo;
import consulo.process.ExecutionException;
import consulo.project.Project;

public interface XLocalAttachDebugger extends XAttachDebugger {
  @Override
  
  String getDebuggerDisplayName();

  void attachDebugSession(Project project, ProcessInfo info) throws ExecutionException;

  @Override
  default void attachDebugSession(Project project, XAttachHost hostInfo, ProcessInfo info) throws ExecutionException {
    attachDebugSession(project, info);
  }
}
