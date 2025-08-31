/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;

/**
 * A keyboard shortcut, which can consist of one or two individual key strokes.
 */
public final class KeyboardShortcut extends Shortcut {
  private final KeyStroke myFirstKeyStroke;
  private final KeyStroke mySecondKeyStroke;

  /**
   * @throws IllegalArgumentException if <code>firstKeyStroke</code> is <code>null</code>
   */
  public KeyboardShortcut(@Nonnull KeyStroke firstKeyStroke, @Nullable KeyStroke secondKeyStroke) {
    myFirstKeyStroke = firstKeyStroke;
    mySecondKeyStroke = secondKeyStroke;
  }

  @Nonnull
  public KeyStroke getFirstKeyStroke() {
    return myFirstKeyStroke;
  }

  @Nullable
  public KeyStroke getSecondKeyStroke() {
    return mySecondKeyStroke;
  }

  public int hashCode() {
    int hashCode = myFirstKeyStroke.hashCode();
    if (mySecondKeyStroke != null) {
      hashCode += mySecondKeyStroke.hashCode();
    }
    return hashCode;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof KeyboardShortcut)) {
      return false;
    }
    KeyboardShortcut second = (KeyboardShortcut)obj;
    return Objects.equals(myFirstKeyStroke, second.myFirstKeyStroke) && Objects.equals(mySecondKeyStroke, second.mySecondKeyStroke);
  }

  @Override
  public boolean isKeyboard() {
    return true;
  }

  @Override
  public boolean startsWith(Shortcut sc) {
    if (sc instanceof KeyboardShortcut) {
      KeyboardShortcut other = (KeyboardShortcut)sc;
      return myFirstKeyStroke.equals(other.myFirstKeyStroke) && (other.mySecondKeyStroke == null || other.mySecondKeyStroke.equals(mySecondKeyStroke)); 
    }
    else {
      return false;
    }
  }

  public static KeyboardShortcut fromString(@NonNls String s) {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(s);
    assert keyStroke != null : "Can't create key stroke for " + s;
    return new KeyboardShortcut(keyStroke, null);
  }

  @Override
  public String toString() {
    return mySecondKeyStroke == null ? "[" + myFirstKeyStroke + "]" : "[" + myFirstKeyStroke + "]+[" + mySecondKeyStroke + "]";
  }
}
