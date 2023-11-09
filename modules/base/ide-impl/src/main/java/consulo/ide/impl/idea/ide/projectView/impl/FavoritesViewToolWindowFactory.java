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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesPanel;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowEx;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class FavoritesViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Nonnull
  @Override
  public String getId() {
    return ToolWindowId.BOOKMARKS;
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, ToolWindow toolWindow) {
    final ContentManager contentManager = toolWindow.getContentManager();
    final FavoritesTreeViewPanel panel = new FavoritesPanel(project).getPanel();
    panel.setupToolWindow((ToolWindowEx)toolWindow);
    final Content content = contentManager.getFactory().createContent(panel, null, false);
    contentManager.addContent(content);
  }

  @Override
  public boolean isSecondary() {
    return true;
  }

  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.LEFT;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.toolwindowsToolwindowbookmarks();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Bookmarks");
  }
}
