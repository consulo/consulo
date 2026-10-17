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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.commited.CommittedChangesAdapter;
import consulo.versionControlSystem.change.commited.CommittedChangesListener;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import org.jspecify.annotations.Nullable;
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

  private final Project myProject;
  private final CommittedChangesCache myCache;

  @Inject
  public IncomingChangesIndicator(Project project, CommittedChangesCache cache) {
    myProject = project;
    myCache = cache;

    if (project.isDefault()) {
      return;
    }

    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(CommittedChangesListener.class, new CommittedChangesAdapter() {
      @Override
      public void incomingChangesUpdated(@Nullable List<CommittedChangeList> receivedChanges) {
        updateWidgetLater();
      }
    });
    VcsListener listener = this::updateWidgetLater;
    connection.subscribe(VcsMappingListener.class, listener);
    connection.subscribe(PluginVcsMappingListener.class, listener);
  }

  private void updateWidgetLater() {
    UIAccess uiAccess = myProject.getUIAccess();
    if (!uiAccess.isValid()) {
      return;
    }

    uiAccess.give(() -> {
      if (!myProject.isDisposed()) {
        StatusBarWidgetsManager.getInstance(myProject).updateWidget(IncomingChangesWidgetFactory.class, uiAccess);
      }
    });
  }

  public boolean needIndicator() {
    AbstractVcs[] vcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
    for (AbstractVcs vcs : vcses) {
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
