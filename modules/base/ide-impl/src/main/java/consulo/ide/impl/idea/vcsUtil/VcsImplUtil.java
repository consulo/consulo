/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcsUtil;

import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.WaitForProgressToShow;
import javax.annotation.Nonnull;

import java.io.File;

/**
 * <p>{@link VcsUtil} extension that needs access to the <code>vcs-impl</code> module.</p>
 *
 * @author Kirill Likhodedov
 */
public class VcsImplUtil {
  /**
   * Shows error message with specified message text and title.
   * The parent component is the root frame.
   *
   * @param project Current project component
   * @param message information message
   * @param title   Dialog title
   */
  public static void showErrorMessage(final Project project, final String message, final String title) {
    Runnable task = new Runnable() {
      public void run() {
        Messages.showErrorDialog(project, message, title);
      }
    };
    WaitForProgressToShow.runOrInvokeLaterAboveProgress(task, null, project);
  }

  @Nonnull
  public static String getShortVcsRootName(@Nonnull Project project, @Nonnull VirtualFile root) {
    VirtualFile projectDir = project.getBaseDir();

    String repositoryPath = root.getPresentableUrl();
    if (projectDir != null) {
      String relativePath = VfsUtilCore.getRelativePath(root, projectDir, File.separatorChar);
      if (relativePath != null) {
        repositoryPath = relativePath;
      }
    }

    return repositoryPath.isEmpty() ? root.getName() : repositoryPath;
  }
}
