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
package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.ide.localize.IdeLocalize;

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
