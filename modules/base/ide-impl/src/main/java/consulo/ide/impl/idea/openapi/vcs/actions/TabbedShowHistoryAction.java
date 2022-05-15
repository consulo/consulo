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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.UpdateInBackground;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.vcs.AbstractVcs;
import consulo.ide.impl.idea.openapi.vcs.AbstractVcsHelper;
import consulo.ide.impl.idea.openapi.vcs.FilePath;
import consulo.ide.impl.idea.openapi.vcs.ProjectLevelVcsManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangesUtil;
import consulo.ide.impl.idea.openapi.vcs.history.VcsHistoryProvider;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.vcsUtil.VcsUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

import static consulo.ide.impl.idea.util.ObjectUtils.assertNotNull;
import static consulo.ide.impl.idea.vcsUtil.VcsUtil.getIfSingle;


public class TabbedShowHistoryAction extends AbstractVcsAction implements UpdateInBackground {
  @Override
  protected void update(@Nonnull VcsContext context, @Nonnull Presentation presentation) {
    Project project = context.getProject();

    presentation.setEnabled(isEnabled(context));
    presentation.setVisible(project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss());
  }

  protected boolean isEnabled(@Nonnull VcsContext context) {
    boolean result = false;
    Project project = context.getProject();

    if (project != null) {
      Pair<FilePath, VirtualFile> pair = getPathAndParentFile(context);

      if (pair.first != null && pair.second != null) {
        result = isEnabled(project, pair.first, pair.second);
      }
    }

    return result;
  }

  private static boolean isEnabled(@Nonnull Project project, @Nonnull FilePath path, @Nonnull VirtualFile fileOrParent) {
    boolean result = false;
    AbstractVcs vcs = ChangesUtil.getVcsForFile(fileOrParent, project);

    if (vcs != null) {
      VcsHistoryProvider provider = vcs.getVcsHistoryProvider();

      result = provider != null &&
               (provider.supportsHistoryForDirectories() || !path.isDirectory()) &&
               AbstractVcs.fileInVcsByFileStatus(project, fileOrParent) &&
               provider.canShowHistoryFor(fileOrParent);
    }

    return result;
  }

  @Nonnull
  private static Pair<FilePath, VirtualFile> getPathAndParentFile(@Nonnull VcsContext context) {
    if (context.getSelectedFilesStream().findAny().isPresent()) {
      VirtualFile file = getIfSingle(context.getSelectedFilesStream());
      return file != null ? Pair.create(VcsUtil.getFilePath(file), file) : Pair.empty();
    }

    File[] ioFiles = context.getSelectedIOFiles();
    if (ioFiles != null && ioFiles.length > 0) {
      for (File ioFile : ioFiles) {
        VirtualFile parent = getParentVirtualFile(ioFile);
        if (parent != null) return Pair.create(VcsUtil.getFilePath(parent, ioFile.getName()), parent);
      }
    }

    return Pair.empty();
  }

  @Nullable
  private static VirtualFile getParentVirtualFile(@Nonnull File ioFile) {
    File parentIoFile = ioFile.getParentFile();
    return parentIoFile != null ? LocalFileSystem.getInstance().findFileByIoFile(parentIoFile) : null;
  }

  @Override
  protected void actionPerformed(@Nonnull VcsContext context) {
    Project project = context.getProject();
    Pair<FilePath, VirtualFile> pair = getPathAndParentFile(context);
    FilePath path = assertNotNull(pair.first);
    VirtualFile fileOrParent = assertNotNull(pair.second);
    AbstractVcs vcs = assertNotNull(ChangesUtil.getVcsForFile(fileOrParent, project));
    VcsHistoryProvider provider = assertNotNull(vcs.getVcsHistoryProvider());

    AbstractVcsHelper.getInstance(project).showFileHistory(provider, vcs.getAnnotationProvider(), path, null, vcs);
  }

  @Override
  protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
    return true;
  }
}
