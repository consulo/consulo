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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.mac.MacDockDelegate;
import com.intellij.ui.win.WinDockDelegate;
import org.consulo.ide.eap.EarlyAccessProgramDescriptor;
import org.consulo.ide.eap.EarlyAccessProgramManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Fokin
 */
public class SystemDock {
  public static class WinJumpList implements EarlyAccessProgramDescriptor {

    @NotNull
    @Override
    public String getName() {
      return "Windows 7+ JumpList";
    }

    @Override
    public boolean getDefaultState() {
      return false;
    }

    @NotNull
    @Override
    public String getDescription() {
      return "JumpList support for Windows 7+";
    }
  }

  private static Delegate delegate;

  static {
    if (SystemInfo.isMac) {
      delegate = MacDockDelegate.getInstance();
    } else if (SystemInfo.isWin7OrNewer && EarlyAccessProgramManager.getInstance().getState(WinJumpList.class)) {
      delegate = WinDockDelegate.getInstance();
    }
  }

  public static void updateMenu () {
    if (delegate == null) return;
    delegate.updateRecentProjectsMenu();
  }

  public interface Delegate {
    void updateRecentProjectsMenu();
  }
}
