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
package consulo.desktop.awt.wm.impl.dock;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.ide.impl.idea.openapi.wm.impl.SystemDock;
import consulo.desktop.awt.uiOld.win.WinDockDelegate;
import consulo.disposer.Disposable;
import consulo.platform.Platform;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-05-11
 */
@Singleton
@ServiceImpl
public class DesktopSystemDockImpl extends SystemDock implements Disposable {
  public interface Delegate {
    void updateRecentProjectsMenu();

    default void dispose() {
    }
  }

  @Nonnull
  private final Delegate myDelegate;

  @Inject
  public DesktopSystemDockImpl(@Nonnull Application application) {
    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.MENU)) {
      myDelegate = new Java9DockDelegateImpl();
    }
    else if (Platform.current().os().isWindows()) {
      myDelegate = new WinDockDelegate(application);
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
