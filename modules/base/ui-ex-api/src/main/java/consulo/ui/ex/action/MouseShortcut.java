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
package consulo.ui.ex.action;

import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.event.details.MouseInputDetails;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A click of a mouse button with modifiers held down. Buttons are numbered from one: {@link #BUTTON_LEFT},
 * {@link #BUTTON_MIDDLE}, {@link #BUTTON_RIGHT}, then the extra buttons such as back and forward, and the wheel turned
 * up or down counts as a button of its own.
 */
public class MouseShortcut extends Shortcut {
  public static final int BUTTON_LEFT = 1;
  public static final int BUTTON_MIDDLE = 2;
  public static final int BUTTON_RIGHT = 3;
  public static final int BUTTON_WHEEL_UP = 143;
  public static final int BUTTON_WHEEL_DOWN = 142;

  private final int myButton;
  private final Set<Modifier> myModifiers;
  private final int myClickCount;

  /**
   * @param clickCount the click count of the click, one or more
   */
  public MouseShortcut(int button, Set<Modifier> modifiers, int clickCount) {
    if (clickCount < 1) {
      throw new IllegalArgumentException("wrong clickCount: " + clickCount);
    }
    myButton = button;
    myModifiers = modifiers.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
    myClickCount = clickCount;
  }

  public int getButton() {
    return myButton;
  }

  public Set<Modifier> getModifiers() {
    return myModifiers;
  }

  public int getClickCount() {
    return myClickCount;
  }

  public boolean matches(MouseInputDetails details, int clickCount) {
    if (myClickCount != clickCount) return false;

    int button = switch (details.getButton()) {
      case LEFT -> BUTTON_LEFT;
      case MIDDLE -> BUTTON_MIDDLE;
      case RIGHT -> BUTTON_RIGHT;
    };
    if (getButton() != button) return false;

    Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    modifiers.addAll(getModifiers());
    modifiers.remove(Modifier.ALT_GRAPH);
    return modifiers.equals(details.getModifiers());
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    MouseShortcut other = (MouseShortcut) obj;

    return myButton == other.myButton
           && myModifiers.equals(other.myModifiers)
           && myClickCount == other.myClickCount;
  }

  public int hashCode() {
    return 31 * (31 * myButton + myModifiers.hashCode()) + myClickCount;
  }

  @Override
  public boolean isKeyboard() {
    return false;
  }

  @Override
  public boolean startsWith(Shortcut sc) {
    return equals(sc);
  }

  @Override
  public String toString() {
    return "button=" + myButton + " clickCount=" + myClickCount + " modifiers=" + myModifiers;
  }
}
