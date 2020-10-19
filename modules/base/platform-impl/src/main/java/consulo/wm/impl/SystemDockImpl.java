/*
 * Copyright 2013-2019 consulo.io
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
package consulo.wm.impl;

import consulo.disposer.Disposable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.ui.mac.MacDockDelegate;
import com.intellij.ui.win.WinDockDelegate;
import consulo.platform.Platform;
import consulo.ui.taskbar.Java9DockDelegateImpl;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2019-05-11
 */
@Singleton
public class SystemDockImpl extends SystemDock implements Disposable {
  public interface Delegate {
    void updateRecentProjectsMenu();

    default void dispose() {
    }
  }

  @Nonnull
  private final Delegate myDelegate;

  public SystemDockImpl() {
    if (Platform.current().isWebService()) {
      myDelegate = () -> {
      };
    }
    else if (SystemInfo.isMac) {
      if (SystemInfo.isJavaVersionAtLeast(9, 0, 0)) {
        myDelegate = new Java9DockDelegateImpl();
      }
      else {
        myDelegate = new MacDockDelegate();
      }
    }
    else if (SystemInfo.isWin7OrNewer) {
      myDelegate = new WinDockDelegate();
    }
    else {
      myDelegate = () -> {
      };
    }
  }

  @Override
  public void updateMenu() {
    myDelegate.updateRecentProjectsMenu();
  }

  @Override
  public void dispose() {
    myDelegate.dispose();
  }
}
