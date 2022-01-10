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
package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import consulo.platform.base.localize.IdeLocalize;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesCompactEmptyMiddlePackagesAction extends FavoritesToolbarButtonAction {
  public FavoritesCompactEmptyMiddlePackagesAction(Project project, FavoritesViewTreeBuilder builder) {
    super(project, builder, IdeLocalize.actionCompactEmptyMiddlePackages(), AllIcons.ObjectBrowser.CompactEmptyPackages);
  }

  @Override
  public void updateButton(AnActionEvent e) {
    super.updateButton(e);
    Presentation presentation = e.getPresentation();
    if (getViewSettings().isFlattenPackages()) {
      presentation.setTextValue(IdeLocalize.actionHideEmptyMiddlePackages());
      presentation.setDescriptionValue(IdeLocalize.actionShowHideEmptyMiddlePackages());
    }
    else {
      presentation.setTextValue(IdeLocalize.actionCompactEmptyMiddlePackages());
      presentation.setDescriptionValue(IdeLocalize.actionShowCompactEmptyMiddlePackages());
    }
  }

  @Override
  public boolean isOptionEnabled() {
    return getViewSettings().isHideEmptyMiddlePackages();
  }

  @Override
  public void setOption(boolean hide) {
    getViewSettings().setHideEmptyMiddlePackages(hide);
  }
}
