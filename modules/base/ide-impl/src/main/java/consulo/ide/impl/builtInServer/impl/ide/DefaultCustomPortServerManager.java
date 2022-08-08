/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.builtInServer.impl.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.ide.impl.builtInServer.custom.CustomPortServerManagerBase;
import consulo.ide.impl.builtInServer.impl.BuiltInServerManagerImpl;
import consulo.project.ui.notification.NotificationType;

@ExtensionImpl
public final class DefaultCustomPortServerManager extends CustomPortServerManagerBase {
  @Override
  public void cannotBind(Exception e, int port) {
    BuiltInServerManagerImpl.NOTIFICATION_GROUP.createNotification("Cannot start built-in HTTP server on custom port " +
                                                                   port +
                                                                   ". " +
                                                                   "Please ensure that port is free (or check your firewall settings) and restart " +
                                                                   ApplicationNamesInfo.getInstance().getFullProductName(), NotificationType.ERROR).notify(null);
  }

  @Override
  public int getPort() {
    int port = BuiltInServerOptions.getInstance().builtInServerPort;
    return port == BuiltInServerOptions.DEFAULT_PORT ? -1 : port;
  }

  @Override
  public boolean isAvailableExternally() {
    return BuiltInServerOptions.getInstance().builtInServerAvailableExternally;
  }
}
