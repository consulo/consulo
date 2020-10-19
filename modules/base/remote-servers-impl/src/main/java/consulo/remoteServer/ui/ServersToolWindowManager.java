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
package consulo.remoteServer.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServerListener;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.impl.runtime.ui.RemoteServersViewContributor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
@Singleton
public class ServersToolWindowManager {
  public static ServersToolWindowManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ServersToolWindowManager.class);
  }

  public static final String ID = "Application Servers";

  @Nonnull
  private final Project myProject;

  @Nonnull
  private final RemoteServersManager myRemoteServersManager;
  @Nonnull
  private final ToolWindowManager myToolWindowManager;

  @Inject
  private ServersToolWindowManager(@Nonnull Project project, @Nonnull RemoteServersManager remoteServersManager, @Nonnull ToolWindowManager toolWindowManager) {
    myProject = project;
    myRemoteServersManager = remoteServersManager;
    myToolWindowManager = toolWindowManager;

    for (RemoteServersViewContributor contributor : RemoteServersViewContributor.EP_NAME.getExtensionList()) {
      contributor.setupAvailabilityListener(project, () -> updateWindowAvailable(true));
    }

    myProject.getMessageBus().connect().subscribe(RemoteServerListener.TOPIC, new RemoteServerListener() {
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
