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
import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
@Singleton
public class IncomingChangesIndicator {
  private static final Logger LOG = Logger.getInstance(IncomingChangesIndicator.class);

  private final Project myProject;
  private final CommittedChangesCache myCache;
  private IndicatorComponent myIndicatorComponent;

  @Inject
  public IncomingChangesIndicator(Application application, Project project, CommittedChangesCache cache) {
    myProject = project;
    myCache = cache;

    if(project.isDefault()) {
      return;
    }

    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(CommittedChangesListener.class, new CommittedChangesAdapter() {
      @Override
      public void incomingChangesUpdated(@Nullable final List<CommittedChangeList> receivedChanges) {
        application.invokeLater(() -> refreshIndicator());
      }
    });
    final VcsListener listener = () -> UIUtil.invokeLaterIfNeeded(this::updateIndicatorVisibility);
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, listener);
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED_IN_PLUGIN, listener);
  }

  private void updateIndicatorVisibility() {
    final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (needIndicator()) {
      if (myIndicatorComponent == null) {
        myIndicatorComponent = new IndicatorComponent();
        statusBar.addWidget(myIndicatorComponent, myProject);
        refreshIndicator();
      }
    }
    else {
      if (myIndicatorComponent != null) {
        statusBar.removeWidget(myIndicatorComponent.ID());
        myIndicatorComponent = null;
      }
    }
  }

  private boolean needIndicator() {
    final AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
    for (AbstractVcs vcs : vcss) {
      CachingCommittedChangesProvider provider = vcs.getCachingCommittedChangesProvider();
      if (provider != null && provider.supportsIncomingChanges()) {
        return true;
      }
    }
    return false;
  }

  private void refreshIndicator() {
    if (myIndicatorComponent == null) {
      return;
    }
    final List<CommittedChangeList> list = myCache.getCachedIncomingChanges();
    if (list == null || list.isEmpty()) {
      debug("Refreshing indicator: no changes");
      myIndicatorComponent.clear();
    }
    else {
      debug("Refreshing indicator: " + list.size() + " changes");
      myIndicatorComponent.setChangesAvailable(VcsBundle.message("incoming.changes.indicator.tooltip", list.size()));
    }
  }

  private static void debug(@NonNls final String message) {
    LOG.debug(message);
  }

  private static class IndicatorComponent implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private StatusBar myStatusBar;

    private consulo.ui.image.Image myCurrentIcon = AllIcons.Ide.IncomingChangesOff;
    private String myToolTipText;

    private IndicatorComponent() {
    }

    void clear() {
      update(AllIcons.Ide.IncomingChangesOff, "No incoming changelists available");
    }

    void setChangesAvailable(@Nonnull final String toolTipText) {
      update(AllIcons.Ide.IncomingChangesOn, toolTipText);
    }

    private void update(@Nonnull final consulo.ui.image.Image icon, @Nullable final String toolTipText) {
      myCurrentIcon = icon;
      myToolTipText = toolTipText;
      if (myStatusBar != null) myStatusBar.updateWidget(ID());
    }

    @Override
    @Nonnull
    public consulo.ui.image.Image getIcon() {
      return myCurrentIcon;
    }

    @Override
    public String getTooltipText() {
      return myToolTipText;
    }

    @Override
    public Consumer<MouseEvent> getClickConsumer() {
      return mouseEvent -> {
        if (myStatusBar != null) {
        DataContext dataContext = DataManager.getInstance().getDataContext((Component) myStatusBar);
          final Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project != null) {
          ToolWindow changesView = ToolWindowManager.getInstance(project).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID);
          changesView.show(new Runnable() {
            @Override
            public void run() {
              ChangesViewContentManager.getInstance(project).selectContent("Incoming");
            }
          });
        }
        }
      };
    }

    @Override
    @Nonnull
    public String ID() {
      return "IncomingChanges";
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
}
