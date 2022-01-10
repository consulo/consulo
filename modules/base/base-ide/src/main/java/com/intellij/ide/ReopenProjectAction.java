/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.BitUtil;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.module.extension.impl.ModuleExtensionProviders;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;

/**
 * @author yole
 */
public class ReopenProjectAction extends AnAction implements DumbAware {
  private final String myProjectPath;
  private final String myProjectName;
  private List<String> myExtensions;
  private boolean myIsRemoved;

  public ReopenProjectAction(final String projectPath, final String projectName, final String displayName, @Nonnull List<String> extensions) {
    myProjectPath = projectPath;
    myProjectName = projectName;
    myExtensions = extensions;

    final Presentation presentation = getTemplatePresentation();
    String text = projectPath.equals(displayName) ? FileUtil.getLocationRelativeToUserHome(projectPath) : displayName;
    presentation.setText(text, false);
    presentation.setDescription(projectPath);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    //Force move focus to IdeFrame
    IdeEventQueue.getInstance().getPopupManager().closeAllPopups();

    final int modifiers = e.getModifiers();
    final boolean forceOpenInNewFrame = BitUtil.isSet(modifiers, InputEvent.CTRL_MASK) || BitUtil.isSet(modifiers, InputEvent.SHIFT_MASK) || e.getPlace() == ActionPlaces.WELCOME_SCREEN;

    Project project = e.getData(CommonDataKeys.PROJECT);
    if (!new File(myProjectPath).exists()) {
      if (Messages.showDialog(project, "The path " +
                                       FileUtil.toSystemDependentName(myProjectPath) +
                                       " does not exist.\n" +
                                       "If it is on a removable or network drive, please make sure that the drive is connected.", "Reopen Project", new String[]{"OK", "&Remove From List"}, 0,
                              Messages.getErrorIcon()) == 1) {
        myIsRemoved = true;
        RecentProjectsManager.getInstance().removePath(myProjectPath);
      }
      return;
    }

    ProjectUtil.openAsync(myProjectPath, project, forceOpenInNewFrame, UIAccess.current());
  }

  public boolean isRemoved() {
    return myIsRemoved;
  }

  @Nonnull
  public Image getExtensionIcon() {
    List<String> extensions = getExtensions();
    Image moduleMainIcon = Image.empty(16);
    if (!extensions.isEmpty()) {
      for (String extensionId : extensions) {
        ModuleExtensionProviderEP provider = ModuleExtensionProviders.findProvider(extensionId);
        if (provider != null) {
          moduleMainIcon = provider.getIcon();
          break;
        }
      }
    }
    return moduleMainIcon;
  }

  @Nonnull
  public List<String> getExtensions() {
    return myExtensions;
  }

  public String getProjectPath() {
    return myProjectPath;
  }

  public String getProjectName() {
    return myProjectName;
  }
}
