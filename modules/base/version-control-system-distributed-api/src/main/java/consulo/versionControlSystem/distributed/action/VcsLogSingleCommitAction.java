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
package consulo.versionControlSystem.distributed.action;

import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLog;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class VcsLogSingleCommitAction<Repo extends Repository> extends DumbAwareAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    VcsLog log = e.getRequiredData(VcsLog.KEY);

    CommitId commit = ContainerUtil.getFirstItem(log.getSelectedCommits());
    assert commit != null;
    Repo repository = getRepositoryForRoot(project, commit.getRoot());
    assert repository != null;

    actionPerformed(repository, commit.getHash());
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    VcsLog log = e.getData(VcsLog.KEY);
    if (project == null || log == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    List<CommitId> commits = log.getSelectedCommits();
    if (commits.isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    CommitId commit = ContainerUtil.getFirstItem(commits);
    assert commit != null;
    Repo repository = getRepositoryForRoot(project, commit.getRoot());

    if (repository == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    e.getPresentation().setVisible(isVisible(project, repository, commit.getHash()));
    e.getPresentation().setEnabled(commits.size() == 1 && isEnabled(repository, commit.getHash()));
  }

  protected abstract void actionPerformed(@Nonnull Repo repository, @Nonnull Hash commit);

  protected boolean isEnabled(@Nonnull Repo repository, @Nonnull Hash commit) {
    return true;
  }

  protected boolean isVisible(@Nonnull Project project, @Nonnull Repo repository, @Nonnull Hash hash) {
    return !getRepositoryManager(project).isExternal(repository);
  }

  @Nonnull
  protected abstract AbstractRepositoryManager<Repo> getRepositoryManager(@Nonnull Project project);

  @Nullable
  protected abstract Repo getRepositoryForRoot(@Nonnull Project project, @Nonnull VirtualFile root);
}
