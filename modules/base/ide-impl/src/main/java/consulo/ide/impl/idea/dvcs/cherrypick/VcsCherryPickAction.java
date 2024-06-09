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
package consulo.ide.impl.idea.dvcs.cherrypick;

import consulo.ui.ex.action.AnActionEvent;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.distributed.VcsCherryPicker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogDataKeys;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.ide.impl.idea.dvcs.DvcsImplIcons;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class VcsCherryPickAction extends DumbAwareAction {
  private static final String NAME = "Cherry-Pick";
  private static final String SEVERAL_VCS_DESCRIPTION = "Selected commits are tracked by different vcses";

  public VcsCherryPickAction() {
    super(NAME, null, DvcsImplIcons.CherryPick);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    FileDocumentManager.getInstance().saveAllDocuments();

    Project project = e.getRequiredData(Project.KEY);
    VcsLog log = e.getRequiredData(VcsLogDataKeys.VCS_LOG);

    VcsCherryPickManager.getInstance(project).cherryPick(log);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setVisible(true);

    final VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
    Project project = e.getData(Project.KEY);
    if (project == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    VcsCherryPickManager cherryPickManager = VcsCherryPickManager.getInstance(project);

    List<VcsCherryPicker> cherryPickers = getActiveCherryPickersForProject(project);
    if (log == null || cherryPickers.isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    List<CommitId> commits = VcsLogUtil.collectFirstPack(log.getSelectedCommits(), VcsLogUtil.MAX_SELECTED_COMMITS);
    if (commits.isEmpty() || cherryPickManager.isCherryPickAlreadyStartedFor(commits)) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final Map<VirtualFile, List<Hash>> groupedByRoot = groupByRoot(commits);
    VcsCherryPicker activeCherryPicker = getActiveCherryPicker(cherryPickers, groupedByRoot.keySet());
    String description = activeCherryPicker != null ? activeCherryPicker.getInfo(log, groupedByRoot) : SEVERAL_VCS_DESCRIPTION;
    e.getPresentation().setEnabled(description == null);
    e.getPresentation()
            .setText(activeCherryPicker == null ? concatActionNamesForAllAvailable(cherryPickers) : activeCherryPicker.getActionTitle());
    e.getPresentation().setDescription(description == null ? "" : description);
  }

  @Nullable
  private static VcsCherryPicker getActiveCherryPicker(@Nonnull List<VcsCherryPicker> cherryPickers,
                                                       @Nonnull Collection<VirtualFile> roots) {
    return ContainerUtil.find(cherryPickers, picker -> picker.canHandleForRoots(roots));
  }

  @Nonnull
  private static Map<VirtualFile, List<Hash>> groupByRoot(@Nonnull List<CommitId> details) {
    Map<VirtualFile, List<Hash>> result = ContainerUtil.newHashMap();
    for (CommitId commit : details) {
      List<Hash> hashes = result.get(commit.getRoot());
      if (hashes == null) {
        hashes = ContainerUtil.newArrayList();
        result.put(commit.getRoot(), hashes);
      }
      hashes.add(commit.getHash());
    }
    return result;
  }

  @Nonnull
  private static String concatActionNamesForAllAvailable(@Nonnull final List<VcsCherryPicker> pickers) {
    return StringUtil.join(pickers, VcsCherryPicker::getActionTitle, "/");
  }

  @Nonnull
  private static List<VcsCherryPicker> getActiveCherryPickersForProject(@Nullable final Project project) {
    if (project != null) {
      final ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
      AbstractVcs[] vcss = projectLevelVcsManager.getAllActiveVcss();
      return ContainerUtil.mapNotNull(vcss, vcs -> vcs != null ? VcsCherryPickManager.getInstance(project)
              .getCherryPickerFor(vcs.getKeyInstanceMethod()) : null);
    }
    return ContainerUtil.emptyList();
  }


}
