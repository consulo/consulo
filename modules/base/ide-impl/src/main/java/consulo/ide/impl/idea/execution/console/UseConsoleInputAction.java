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
package consulo.ide.impl.idea.execution.console;

import consulo.application.AllIcons;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.execution.ui.console.ConsoleExecuteAction;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public final class UseConsoleInputAction extends ToggleAction implements DumbAware {
  private final String processInputStateKey;
  private boolean useProcessStdIn;

  public UseConsoleInputAction(@Nonnull String processInputStateKey) {
    super("Use Console Input", null, AllIcons.Debugger.CommandLine);

    this.processInputStateKey = processInputStateKey;
    useProcessStdIn = ApplicationPropertiesComponent.getInstance().getBoolean(processInputStateKey);
  }

  @Override
  public boolean isSelected(@Nullable AnActionEvent event) {
    return !useProcessStdIn;
  }

  @Override
  public void setSelected(AnActionEvent event, boolean state) {
    useProcessStdIn = !state;

    LanguageConsoleView consoleView = (LanguageConsoleView)event.getData(ConsoleView.KEY);
    assert consoleView != null;
    DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(consoleView.getProject());
    PsiFile file = consoleView.getFile();
    daemonCodeAnalyzer.setHighlightingEnabled(file, state);
    daemonCodeAnalyzer.restart(file);
    ApplicationPropertiesComponent.getInstance().setValue(processInputStateKey, useProcessStdIn);

    List<AnAction> actions = ActionUtil.getActions(consoleView.getConsoleEditor().getComponent());
    ConsoleExecuteAction action = ContainerUtil.findInstance(actions, ConsoleExecuteAction.class);
    action.getExecuteActionHandler().setUseProcessStdIn(!state);
  }
}