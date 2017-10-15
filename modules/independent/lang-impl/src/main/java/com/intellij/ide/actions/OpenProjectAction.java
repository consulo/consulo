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

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.annotations.RequiredDispatchThread;
import consulo.platform.Platform;
import consulo.project.ProjectOpenProcessors;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.NotNull;

public class OpenProjectAction extends AnAction implements DumbAware {
  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final FileChooserDescriptor descriptor = new OpenProjectFileChooserDescriptor(true);
    descriptor.setTitle(IdeBundle.message("title.open.project"));
    descriptor.setDescription(getFileChooserDescription());
    VirtualFile userHomeDir = null;
    if (SystemInfo.isUnix) {
      userHomeDir = VfsUtil.getUserHomeDir();
    }

    descriptor.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, Boolean.TRUE);

    final Project project = e.getData(CommonDataKeys.PROJECT);
    FileChooser.chooseFiles(descriptor, project, userHomeDir, files -> {
      if (files.size() == 1) {
        Platform.hacky(() -> ProjectUtil.open(files.get(0).getPath(), project, false), () -> ProjectUtil.openAsync(files.get(0).getPath(), project, false, UIAccess.get()));
      }
    });
  }

  public static String getFileChooserDescription() {
    ProjectOpenProcessor[] providers = ProjectOpenProcessors.getInstance().getProcessors();

    return IdeBundle.message("import.project.chooser.header", StringUtil.join(providers, ProjectOpenProcessor::getFileSample, ", <br>"));
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
    if (WelcomeFrame.isFromWelcomeFrame(e)) {
      e.getPresentation().setIcon(AllIcons.Welcome.OpenProject);
    }
  }
}