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
package com.intellij.openapi.vcs.checkout;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.project.ProjectOpenProcessors;
import consulo.ui.UIAccess;

import java.io.File;

public class ProjectImporterCheckoutListener implements CheckoutListener {
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
              ProjectUtil.openAsync(virtualFile.getPath(), project, false, UIAccess.current());
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