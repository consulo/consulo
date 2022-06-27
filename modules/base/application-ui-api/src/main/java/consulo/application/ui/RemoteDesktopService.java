/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.application.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RemoteDesktopService {
  public static RemoteDesktopService getInstance() {
    return Application.get().getInstance(RemoteDesktopService.class);
  }

  public static boolean isRemoteSession() {
    return getInstance().isRemoteDesktopConnected();
  }

  public abstract boolean isRemoteDesktopConnected();
}
