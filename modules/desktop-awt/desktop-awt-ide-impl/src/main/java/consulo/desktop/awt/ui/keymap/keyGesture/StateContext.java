/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.keymap.keyGesture;

import consulo.dataContext.DataContext;
import consulo.ui.ex.action.KeyboardGestureAction;
import consulo.ui.ex.action.Presentation;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.*;

class StateContext {
  KeyEvent actionKey;
  KeyStroke actionShortcut;
  KeyboardGestureAction.ModifierType modifierType;
  Presentation actionPresentation;
  public String actionPlace;
  DataContext dataContext;

  Component focusOwner;
  KeyEvent keyToProcess;
  boolean isModal;

  @Override
  public String toString() {
    return
      "actionKey=" + actionKey + "\n"
      + "actionShortcut=" + actionShortcut + "\n"
      + "modifierType=" + modifierType + "\n"
      + "actionPresentation=" + actionPresentation + "\n"
      + "actionPlace=" + actionPlace + "\n"
      + "dataContext=" + dataContext + "\n"
      + "focusOwner=" + focusOwner + "\n"
      + "keyToProcess=" + keyToProcess + "\n"
      + "isModal=" + isModal + "\n";
  }
}