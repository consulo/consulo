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
package consulo.ui.event;

import consulo.ui.Component;
import consulo.util.lang.BitUtil;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-10-31
 */
public final class KeyEvent extends UIEvent<Component> {
  public static final int K_ENTER = '\n';

  public static final int M_SHIFT = 1 << 1;
  public static final int M_CTRL = 1 << 2;
  public static final int M_ALT = 1 << 3;

  private final int myKeyCode;
  private final int myModifiers;

  public KeyEvent(@Nonnull Component component, int keyCode, int modifiers) {
    super(component);
    myKeyCode = keyCode;
    myModifiers = modifiers;
  }

  public int getModifiers() {
    return myModifiers;
  }

  public int getKeyCode() {
    return myKeyCode;
  }

  public boolean withShift() {
    return BitUtil.isSet(myModifiers, M_SHIFT);
  }

  public boolean withCtrl() {
    return BitUtil.isSet(myModifiers, M_CTRL);
  }

  public boolean withAlt() {
    return BitUtil.isSet(myModifiers, M_ALT);
  }
}
