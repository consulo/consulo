/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.annotations.RequiredDispatchThread;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OpenProjectFileChooserDescriptor extends FileChooserDescriptor {
  public OpenProjectFileChooserDescriptor(final boolean chooseFiles) {
    super(chooseFiles, true, chooseFiles, chooseFiles, false, false);
  }

  @RequiredDispatchThread
  @Override
  public boolean isFileSelectable(final VirtualFile file) {
    if (file == null) return false;
    return isProjectDirectory(file) || canOpen(file);
  }

  @Override
  public Icon getIcon(final VirtualFile file) {
    if (isProjectDirectory(file)) {
      return dressIcon(file, SandboxUtil.getAppIcon());
    }
    final Icon icon = getProcessorIcon(file);
    if (icon != null) {
      return dressIcon(file, icon);
    }
    return super.getIcon(file);
  }

  @Nullable
  private static Icon getProcessorIcon(final VirtualFile virtualFile) {
    final ProjectOpenProcessor provider = ProjectOpenProcessor.findProcessor(virtualFile);
    if (provider != null) {
      return provider.getIcon(virtualFile);
    }
    return null;
  }

  @Override
  public boolean isFileVisible(final VirtualFile file, final boolean showHiddenFiles) {
    if (!showHiddenFiles && FileElement.isFileHidden(file)) return false;
    return canOpen(file) || super.isFileVisible(file, showHiddenFiles) && file.isDirectory();
  }

  public static boolean canOpen(final VirtualFile file) {
    return ProjectOpenProcessor.findProcessor(file) != null;
  }

  private static boolean isProjectDirectory(final VirtualFile virtualFile) {
    // the root directory of any drive is never an Consulo project
    if (virtualFile.getParent() == null) return false;
    // NOTE: For performance reasons, it's very important not to iterate through all of the children here.
    if (virtualFile.isDirectory() && virtualFile.isValid() && virtualFile.findChild(Project.DIRECTORY_STORE_FOLDER) != null) return true;
    return false;
  }
}
