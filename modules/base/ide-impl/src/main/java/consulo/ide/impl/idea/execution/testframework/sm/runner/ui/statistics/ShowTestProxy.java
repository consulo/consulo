/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.testframework.sm.runner.ui.statistics;

import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.sm.ui.statistic.StatisticsPanel;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author Roman Chernyatchik
 */
public class ShowTestProxy extends AnAction {
  public void actionPerformed(final AnActionEvent e) {
    final StatisticsPanel sender = e.getData(StatisticsPanel.SM_TEST_RUNNER_STATISTICS);
    if (sender == null) {
      return;
    }

    sender.showSelectedProxyInTestsTree();
  }

  @Override
  public void update(final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();

    // visible only in StatisticsTableView
    presentation.setVisible(e.getData(StatisticsPanel.SM_TEST_RUNNER_STATISTICS) != null);
    // enabled if some proxy is selected
    presentation.setEnabled(getSelectedTestProxy(e) != null);
  }

  @jakarta.annotation.Nullable
  private static Object getSelectedTestProxy(final AnActionEvent e) {
    return e.getData(AbstractTestProxy.KEY);
  }
}
