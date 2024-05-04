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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class IncomingChangesIndicator {
  private static final Logger LOG = Logger.getInstance(IncomingChangesIndicator.class);

  private final Application myApplication;
  private final Project myProject;
  private final CommittedChangesCache myCache;

  @Inject
  public IncomingChangesIndicator(Application application, Project project, CommittedChangesCache cache) {
    myApplication = application;
    myProject = project;
    myCache = cache;

    if (project.isDefault()) {
      return;
    }

    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(CommittedChangesListener.class, new CommittedChangesAdapter() {
      @Override
      public void incomingChangesUpdated(@Nullable final List<CommittedChangeList> receivedChanges) {
        application.invokeLater(() -> updateWidget());
      }
    });
    final VcsListener listener = () -> application.invokeLater(this::updateWidget);
    connection.subscribe(VcsMappingListener.class, listener);
    connection.subscribe(PluginVcsMappingListener.class, listener);
  }

  private void updateWidget() {
    StatusBarWidgetsManager manager = StatusBarWidgetsManager.getInstance(myProject);

    manager.updateWidget(IncomingChangesWidgetFactory.class, myApplication.getLastUIAccess());
  }

  public boolean needIndicator() {
    final AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
    for (AbstractVcs vcs : vcss) {
      CachingCommittedChangesProvider provider = vcs.getCachingCommittedChangesProvider();
      if (provider != null && provider.supportsIncomingChanges()) {
        return true;
      }
    }
    return false;
  }

  public CommittedChangesCache getCache() {
    return myCache;
  }
}
