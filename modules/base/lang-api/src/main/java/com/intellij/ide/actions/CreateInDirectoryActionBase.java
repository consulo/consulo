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

package com.intellij.ide.actions;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.migration.SwingImageRef;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * The base abstract class for actions which create new file elements in IDE view
 *
 * @since 15.1
 */
public abstract class CreateInDirectoryActionBase extends AnAction {
  protected CreateInDirectoryActionBase() {
  }

  @Deprecated
  protected CreateInDirectoryActionBase(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  protected CreateInDirectoryActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected CreateInDirectoryActionBase(@Nullable String text, @Nullable String description, @Nullable SwingImageRef icon) {
    super(text, description, icon);
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent e) {
    if (!e.getPresentation().isVisible()) {
      return;
    }

    final DataContext dataContext = e.getDataContext();
    final Presentation presentation = e.getPresentation();

    final boolean enabled = isAvailable(dataContext);

    presentation.setVisible(enabled);
    presentation.setEnabled(enabled);
  }

  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  protected boolean isAvailable(final DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return false;
    }

    if (DumbService.getInstance(project).isDumb() && !isDumbAware()) {
      return false;
    }

    final IdeView view = dataContext.getData(LangDataKeys.IDE_VIEW);
    if (view == null || view.getDirectories().length == 0) {
      return false;
    }

    return true;
  }
}
