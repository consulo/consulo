/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.attach;

import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.platform.ProcessInfo;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link XAttachDebugger} allows to attach to process with specified {@link ProcessInfo}
 */
public interface XAttachDebugger {
  @Nonnull
  String getDebuggerDisplayName();

  /**
   * @return title for `Attach to process` module window, which will be shown when choosing this debugger
   */
  @Nullable
  default String getDebuggerSelectedTitle() {
    String title = getDebuggerDisplayName();
    title = StringUtil.shortenTextWithEllipsis(title, 50, 0);
    return XDebuggerLocalize.xdebuggerAttachPopupTitle(title).get();
  }

  /**
   * Attaches this debugger to the specified process. The debugger is guaranteed to be
   * returned by {@link XAttachDebuggerProvider#getAvailableDebuggers} for the specified process.
   *
   * @param hostInfo host (environment) on which process is being run
   * @param info     process to attach to
   * @throws ExecutionException if an error occurs during attach
   */
  void attachDebugSession(
    @Nonnull Project project,
    @Nonnull XAttachHost hostInfo,
    @Nonnull ProcessInfo info
  ) throws ExecutionException;
}
