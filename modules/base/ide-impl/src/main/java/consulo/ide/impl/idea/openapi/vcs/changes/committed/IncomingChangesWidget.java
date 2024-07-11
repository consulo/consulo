/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

class IncomingChangesWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
  private StatusBar myStatusBar;

  private Image myCurrentIcon = AllIcons.Ide.IncomingChangesOff;
  private String myToolTipText;
  private final StatusBarWidgetFactory myFactory;
  private final IncomingChangesIndicator myIncomingChangesIndicator;

  IncomingChangesWidget(StatusBarWidgetFactory factory, IncomingChangesIndicator incomingChangesIndicator) {
    myFactory = factory;
    myIncomingChangesIndicator = incomingChangesIndicator;
  }

   void refreshIndicator() {
    final List<CommittedChangeList> list = myIncomingChangesIndicator.getCache().getCachedIncomingChanges();
    if (list == null || list.isEmpty()) {
      clear();
    }
    else {
      setChangesAvailable(VcsLocalize.incomingChangesIndicatorTooltip(list.size()).get());
    }
  }

  void clear() {
    update(AllIcons.Ide.IncomingChangesOff, "No incoming changelists available");
  }

  void setChangesAvailable(@Nonnull final String toolTipText) {
    update(AllIcons.Ide.IncomingChangesOn, toolTipText);
  }

  private void update(@Nonnull final Image icon, @Nullable final String toolTipText) {
    myCurrentIcon = icon;
    myToolTipText = toolTipText;
    if (myStatusBar != null) myStatusBar.updateWidget(getId());
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return myCurrentIcon;
  }

  @Override
  public String getTooltipText() {
    return myToolTipText;
  }

  @Override
  @RequiredUIAccess
  public Consumer<MouseEvent> getClickConsumer() {
    return mouseEvent -> {
      if (myStatusBar != null) {
        DataContext dataContext = DataManager.getInstance().getDataContext((Component)myStatusBar);
        final Project project = dataContext.getData(Project.KEY);
        if (project != null) {
          ToolWindow changesView = ToolWindowManager.getInstance(project).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID);
          changesView.show(() -> ChangesViewContentManager.getInstance(project).selectContent("Incoming"));
        }
      }
    };
  }

  @Override
  @Nonnull
  public String getId() {
    return myFactory.getId();
  }

  @Override
  public WidgetPresentation getPresentation() {
    return this;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    myStatusBar = statusBar;
  }

  @Override
  public void dispose() {
    myStatusBar = null;
  }
}
