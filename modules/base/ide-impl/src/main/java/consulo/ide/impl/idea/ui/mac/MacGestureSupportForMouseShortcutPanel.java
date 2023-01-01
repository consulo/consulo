/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.mac;

import consulo.ui.ex.action.PressureShortcut;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.MouseShortcutPanel;
import consulo.eawt.wrapper.GestureUtilitiesWrapper;
import consulo.eawt.wrapper.event.PressureEventWrapper;
import consulo.eawt.wrapper.event.PressureListenerWrapper;

/**
 * @author denis
 */
public class MacGestureSupportForMouseShortcutPanel {
  public MacGestureSupportForMouseShortcutPanel(MouseShortcutPanel panel, Runnable runnable) {
    PressureListenerWrapper pressureListener = new PressureListenerWrapper() {
      @Override
      public void pressure(PressureEventWrapper e) {
        if (e.getStage() == 2) {
          panel.setShortcut(new PressureShortcut(e.getStage()));
          runnable.run();
        }
      }
    };

    GestureUtilitiesWrapper.addGestureListenerTo(panel, pressureListener);
  }
}
