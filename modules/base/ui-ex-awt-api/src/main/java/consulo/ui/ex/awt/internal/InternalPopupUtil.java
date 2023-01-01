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
package consulo.ui.ex.awt.internal;

import consulo.application.ApplicationManager;
import consulo.ui.ex.popup.StackingPopupDispatcher;

import javax.swing.*;

public class InternalPopupUtil {
  public static boolean handleEscKeyEvent() {
    MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
    MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
    if (selectedPath.length > 0) { // hide popup menu if any
      menuSelectionManager.clearSelectedPath();
      return true;
    }
    else {
      if (ApplicationManager.getApplication() == null) {
        return false;
      }
      final StackingPopupDispatcher popupDispatcher = StackingPopupDispatcher.getInstance();
      return popupDispatcher.isPopupFocused();
    }
  }
}
