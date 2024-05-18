// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.dashboard;

import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.ui.ex.content.Content;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
public interface RunDashboardNode {
  default @Nullable RunContentDescriptor getDescriptor() {
    return null;
  }

  default @Nullable Content getContent() {
    return null;
  }

  Project getProject();

  default @Nullable Runnable getRemover() {
    return null;
  }
}
