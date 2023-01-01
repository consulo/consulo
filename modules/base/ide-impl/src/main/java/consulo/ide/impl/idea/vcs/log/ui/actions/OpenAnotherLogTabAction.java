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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogContentProvider;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogManager;
import consulo.ide.impl.idea.vcs.log.impl.VcsProjectLog;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;

public class OpenAnotherLogTabAction extends DumbAwareAction {
  public OpenAnotherLogTabAction() {
    super("Open Another Log Tab", "Open Another Log Tab", AllIcons.General.Add);
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || !Registry.is("vcs.log.open.another.log.visible", false)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    VcsProjectLog projectLog = VcsProjectLog.getInstance(project);
    VcsLogManager logManager = e.getData(VcsLogInternalDataKeys.LOG_MANAGER);
    e.getPresentation()
            .setEnabledAndVisible(logManager != null && projectLog.getLogManager() == logManager); // only for main log (it is a question, how and where we want to open tabs for external logs)
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    VcsLogContentProvider
            .openAnotherLogTab(e.getRequiredData(VcsLogInternalDataKeys.LOG_MANAGER), e.getRequiredData(CommonDataKeys.PROJECT));
  }
}
