/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.roots.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.actions.MarkRootAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 22:46/31.10.13
 */
public class ExcludedContentFolderTypeProvider extends ContentFolderTypeProvider {
  private static final Color EXCLUDED_COLOR = new JBColor(new Color(0x992E00), DarculaColors.RED);

  @NotNull
  public static ExcludedContentFolderTypeProvider getInstance() {
    return EP_NAME.findExtension(ExcludedContentFolderTypeProvider.class);
  }

  public ExcludedContentFolderTypeProvider() {
    super("EXCLUDED");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @NotNull
  public String getMarkActionDescription() {
    return ProjectBundle.message("module.toggle.0.action.description", getName());
  }

  @NotNull
  @Override
  public AnAction createMarkAction() {
    return new MarkRootAction(getName(), getMarkActionDescription(), getIcon(), this) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        VirtualFile[] vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        String message = vFiles.length == 1 ? FileUtil.toSystemDependentName(vFiles[0].getPath()) : vFiles.length + " selected files";
        final int rc = Messages
          .showOkCancelDialog(e.getData(CommonDataKeys.PROJECT), ActionsBundle.message("action.mark.as.excluded.confirm.message", message),
                              getMarkActionDescription(), Messages.getQuestionIcon());
        if (rc != 0) {
          return;
        }
        super.actionPerformed(e);
      }

    };
  }

  @Override
  public Icon getChildDirectoryIcon() {
    return AllIcons.Modules.ExcludeRoot;
  }

  @NotNull
  @Override
  public String getName() {
    return ProjectBundle.message("module.toggle.excluded.action");
  }

  @NotNull
  @Override
  public Color getGroupColor() {
    return EXCLUDED_COLOR;
  }
}
