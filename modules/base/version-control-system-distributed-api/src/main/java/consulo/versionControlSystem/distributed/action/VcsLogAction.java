/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.action;

import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLog;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class VcsLogAction<Repo extends Repository> extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    VcsLog log = e.getRequiredData(VcsLog.KEY);
    List<VcsFullCommitDetails> details = log.getSelectedDetails();
    MultiMap<Repo, VcsFullCommitDetails> grouped = groupByRootWithCheck(project, details);
    assert grouped != null;
    actionPerformed(project, grouped);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    VcsLog log = e.getData(VcsLog.KEY);
    if (project == null || log == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    List<VcsFullCommitDetails> details = log.getSelectedDetails();
    MultiMap<Repo, VcsFullCommitDetails> grouped = groupByRootWithCheck(project, details);
    if (grouped == null) {
      e.getPresentation().setEnabledAndVisible(false);
    }
    else {
      e.getPresentation().setVisible(isVisible(project, grouped));
      e.getPresentation().setEnabled(!grouped.isEmpty() && isEnabled(grouped));
    }
  }

  protected abstract void actionPerformed(@Nonnull Project project, @Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped);

  protected abstract boolean isEnabled(@Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped);

  protected boolean isVisible(@Nonnull final Project project, @Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped) {
    return ContainerUtil.and(grouped.keySet(), repo -> {
      RepositoryManager<Repo> manager = getRepositoryManager(project);
      return !manager.isExternal(repo);
    });
  }

  @Nonnull
  protected abstract AbstractRepositoryManager<Repo> getRepositoryManager(@Nonnull Project project);

  @Nullable
  protected abstract Repo getRepositoryForRoot(@Nonnull Project project, @Nonnull VirtualFile root);

  @Nullable
  private MultiMap<Repo, VcsFullCommitDetails> groupByRootWithCheck(@Nonnull Project project, @Nonnull List<VcsFullCommitDetails> commits) {
    MultiMap<Repo, VcsFullCommitDetails> map = MultiMap.create();
    for (VcsFullCommitDetails commit : commits) {
      Repo root = getRepositoryForRoot(project, commit.getRoot());
      if (root == null) { // commit from some other VCS
        return null;
      }
      map.putValue(root, commit);
    }
    return map;
  }

}
