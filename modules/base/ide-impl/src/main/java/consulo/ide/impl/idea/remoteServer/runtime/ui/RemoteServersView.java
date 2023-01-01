/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remoteServer.runtime.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.ide.impl.idea.remoteServer.runtime.ServerConnection;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class RemoteServersView {
  @Nonnull
  public static RemoteServersView getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, RemoteServersView.class);
  }

  public abstract void showServerConnection(@Nonnull ServerConnection<?> connection);

  public abstract void showDeployment(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName);
}
