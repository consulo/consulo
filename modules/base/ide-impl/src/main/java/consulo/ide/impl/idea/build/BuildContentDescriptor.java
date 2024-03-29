// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.event.BuildEventsNls;
import consulo.process.ProcessHandler;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class BuildContentDescriptor extends RunContentDescriptor {
  private boolean activateToolWindowWhenFailed = true;

  public BuildContentDescriptor(@Nullable ExecutionConsole executionConsole,
                                @Nullable ProcessHandler processHandler,
                                @Nonnull JComponent component,
                                @BuildEventsNls.Title String displayName) {
    super(executionConsole, processHandler, component, displayName);
  }

  public boolean isActivateToolWindowWhenFailed() {
    return activateToolWindowWhenFailed;
  }

  public void setActivateToolWindowWhenFailed(boolean activateToolWindowWhenFailed) {
    this.activateToolWindowWhenFailed = activateToolWindowWhenFailed;
  }
}
