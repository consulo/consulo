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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.checkout.PreCheckoutListener;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;

@ExtensionImpl(order = "last")
public class ProjectImporterCheckoutListener implements PreCheckoutListener {
  @Override
  public boolean processCheckedOutDirectory(Project project, File directory) {
    final File[] files = directory.listFiles();
    if (files != null) {
      final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
      for (File file : files) {
        if (file.isDirectory()) continue;
        final VirtualFile virtualFile = localFileSystem.findFileByIoFile(file);
        if (virtualFile != null) {
          final ProjectOpenProcessor openProcessor = ProjectOpenProcessors.getInstance().findProcessor(file);
          if (openProcessor != null) {
            int rc = Messages.showYesNoDialog(project, VcsBundle .message("checkout.open.project.prompt", files[0].getPath()),
                                              VcsBundle.message("checkout.title"), Messages.getQuestionIcon());
            if (rc == Messages.YES) {
              ProjectImplUtil.openAsync(virtualFile.getPath(), project, false, UIAccess.current());
            }
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public void processOpenedProject(Project lastOpenedProject) {
  }
}