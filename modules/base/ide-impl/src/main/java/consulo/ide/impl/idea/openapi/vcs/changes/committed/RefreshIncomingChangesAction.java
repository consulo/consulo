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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;

/**
 * @author yole
 */
public class RefreshIncomingChangesAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project != null) {
      doRefresh(project);
    }
  }

  public static void doRefresh(final Project project) {
    final CommittedChangesCache cache = CommittedChangesCache.getInstance(project);
    cache.hasCachesForAnyRoot(notEmpty -> {
      if ((!notEmpty) && (!CacheSettingsDialog.showSettingsDialog(project))) {
        return;
      }
      cache.refreshAllCachesAsync(true, false);
      cache.refreshIncomingChangesAsync();
    });
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    e.getPresentation().setEnabled(project != null && !CommittedChangesCache.getInstance(project).isRefreshingIncomingChanges());
  }
}