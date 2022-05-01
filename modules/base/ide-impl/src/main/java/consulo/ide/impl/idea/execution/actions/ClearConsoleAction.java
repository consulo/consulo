// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.actions;

import consulo.application.AllIcons;
import consulo.execution.ExecutionBundle;
import consulo.execution.ExecutionDataKeys;
import consulo.execution.ui.console.ConsoleView;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

import javax.annotation.Nonnull;

public class ClearConsoleAction extends DumbAwareAction {
  public ClearConsoleAction() {
    super(ExecutionBundle.message("clear.all.from.console.action.name"), "Clear the contents of the console", AllIcons.Actions.GC);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ConsoleView data = e.getData(ExecutionDataKeys.CONSOLE_VIEW);
    boolean enabled = data != null && data.getContentSize() > 0;
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final ConsoleView consoleView = e.getData(ExecutionDataKeys.CONSOLE_VIEW);
    if (consoleView != null) {
      consoleView.clear();
    }
  }
}
