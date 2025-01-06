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

package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.IgnoredSettingsDialog;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.change.ChangeListManager;

/**
 * @author yole
 */
public class IgnoredSettingsAction extends AnAction implements DumbAware {
  public IgnoredSettingsAction() {
    super(
      "Configure Ignored Files...",
      "Specify file paths and masks which are ignored",
       PlatformIconGroup.filetypesIgnored()
    );
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;
    IgnoredSettingsDialog.configure(project);
  }
}
