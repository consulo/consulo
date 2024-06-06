/*
 * Copyright 2013-2020 consulo.io
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
package consulo.remoteServer.impl.internal.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServerListener;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ServersToolWindowManager {
  public static ServersToolWindowManager getInstance(@Nonnull Project project) {
    return project.getInstance(ServersToolWindowManager.class);
  }

  public static final String ID = "Application Servers";

  @Nonnull
  private final Project myProject;

  @Nonnull
  private final RemoteServersManager myRemoteServersManager;
  @Nonnull
  private final ToolWindowManager myToolWindowManager;

  @Inject
  ServersToolWindowManager(@Nonnull Project project, @Nonnull RemoteServersManager remoteServersManager, @Nonnull ToolWindowManager toolWindowManager) {
    myProject = project;
    myRemoteServersManager = remoteServersManager;
    myToolWindowManager = toolWindowManager;

    for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensionList()) {
      contributor.setupAvailabilityListener(project, () -> updateWindowAvailable(true));
    }

    myProject.getMessageBus().connect().subscribe(RemoteServerListener.class, new RemoteServerListener() {
      @Override
      public void serverAdded(@Nonnull RemoteServer<?> server) {
        updateWindowAvailable(true);
      }

      @Override
      public void serverRemoved(@Nonnull RemoteServer<?> server) {
        updateWindowAvailable(false);
      }
    });
  }

  private void updateWindowAvailable(boolean showIfAvailable) {
    ToolWindow toolWindow = myToolWindowManager.getToolWindow(ID);

    boolean available = isAvailable();
    boolean doShow = !toolWindow.isAvailable() && available;
    if (toolWindow.isAvailable() && !available) {
      toolWindow.hide(null);
    }
    toolWindow.setAvailable(available, null);
    if (showIfAvailable && doShow) {
      toolWindow.show(null);
    }
  }

  public boolean isAvailable() {
    if (!myRemoteServersManager.getServers().isEmpty()) {
      return true;
    }
    for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensionList()) {
      if (contributor.canContribute(myProject)) {
        return true;
      }
    }
    return false;
  }
}
