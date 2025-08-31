/*
 * Copyright 2013-2022 consulo.io
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
package consulo.execution.unscramble;

import consulo.execution.ui.console.ConsoleView;
import consulo.execution.unscramble.awt.AWTThreadDumpPanel;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.action.DefaultActionGroup;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 04/11/2022
 */
public interface ThreadDumpPanel {
  @Nonnull
  static ThreadDumpPanel create(Project project, ConsoleView consoleView, DefaultActionGroup toolbarActions, List<ThreadState> threadDump) {
    return new AWTThreadDumpPanel(project, consoleView, toolbarActions, threadDump);
  }

  @Nonnull
  JComponent getComponent();

  @Nonnull
  Component getUIComponent();

  void selectStackFrame(int index);
}
