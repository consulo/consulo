// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.action;

import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.console.ConsoleView;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

public class ClearConsoleAction extends DumbAwareAction {
  public ClearConsoleAction() {
    super(
      ExecutionLocalize.clearAllFromConsoleActionName(),
      LocalizeValue.localizeTODO("Clear the contents of the console"),
        PlatformIconGroup.actionsGc()
    );
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ConsoleView data = e.getData(ConsoleView.KEY);
    boolean enabled = data != null && data.getContentSize() > 0;
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ConsoleView consoleView = e.getData(ConsoleView.KEY);
    if (consoleView != null) {
      consoleView.clear();
    }
  }
}
