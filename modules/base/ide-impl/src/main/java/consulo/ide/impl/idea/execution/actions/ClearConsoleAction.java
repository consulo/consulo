// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.actions;

import consulo.application.AllIcons;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.console.ConsoleView;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public class ClearConsoleAction extends DumbAwareAction {
  public ClearConsoleAction() {
    super(
      ExecutionLocalize.clearAllFromConsoleActionName(),
      LocalizeValue.localizeTODO("Clear the contents of the console"),
      AllIcons.Actions.GC
    );
  }

  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent e) {
    ConsoleView data = e.getData(ConsoleView.KEY);
    boolean enabled = data != null && data.getContentSize() > 0;
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final ConsoleView consoleView = e.getData(ConsoleView.KEY);
    if (consoleView != null) {
      consoleView.clear();
    }
  }
}
