/*
 * Copyright 2013-2017 consulo.io
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

package consulo.ide.impl.presentationAssistant;

import consulo.ui.ex.action.MouseShortcut;

import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 21-Aug-17
 */
class MouseShortcutPresentation {
  private static Map<Integer, String> ourButtonNames = new HashMap<>();

  static {
    ourButtonNames.put(MouseEvent.BUTTON1, "Left");
    ourButtonNames.put(MouseEvent.BUTTON2, "Middle");
    ourButtonNames.put(MouseEvent.BUTTON3, "Right");
  }

  @Nullable
  static String getMouseShortcutText(MouseShortcut shortcut) {
    String buttonName = ourButtonNames.get(shortcut.getButton());
    if (buttonName != null) {
      if (shortcut.getClickCount() > 1) {
        return buttonName + "&nbsp;" + "Double&nbsp;Click";
      }
      else {
        return buttonName + "&nbsp;" + "Click";
      }
    }
    return null;
  }
}
