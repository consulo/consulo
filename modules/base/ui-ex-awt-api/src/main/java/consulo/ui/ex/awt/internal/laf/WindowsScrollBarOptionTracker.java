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
package consulo.ui.ex.awt.internal.laf;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import consulo.util.jna.JnaLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Jun-22
 * <p>
 * There scrollbar option 'Automatically hide scrollbars in Windows'
 * <p>
 * We get new value on each focus/unfocus window
 */
class WindowsScrollBarOptionTracker implements AWTEventListener {
  private static final String KEY = "Control Panel\\Accessibility";
  private static final String VALUE = "DynamicScrollbars";

  private final UIElementWeakStorage<ConfigurableScrollBarUI> myElementWeakStorage;
  private Boolean myDynamicScrollBars;

  WindowsScrollBarOptionTracker(UIElementWeakStorage<ConfigurableScrollBarUI> storage) {
    myElementWeakStorage = storage;
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    Boolean oldValue = myDynamicScrollBars;
    myDynamicScrollBars = calcState();

    if (oldValue != myDynamicScrollBars) {
      SwingUtilities.invokeLater(this::updateAll);
    }
  }

  private void updateAll() {
    List<ConfigurableScrollBarUI> list = new ArrayList<>();
    myElementWeakStorage.processReferences(null, null, list);
    for (ConfigurableScrollBarUI scrollBarUI : list) {
      scrollBarUI.updateStyle(isDynamicScrollBars() ? ConfigurableScrollBarUI.Style.Overlay : ConfigurableScrollBarUI.Style.Legacy);
    }
  }

  public boolean isDynamicScrollBars() {
    if (myDynamicScrollBars == null) {
      myDynamicScrollBars = calcState();
    }

    return myDynamicScrollBars == Boolean.TRUE;
  }

  private boolean calcState() {
    if (!JnaLoader.isLoaded()) {
      return false;
    }

    if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, KEY, VALUE)) {
      return Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, KEY, VALUE) == 1;
    }
    return false;
  }
}
