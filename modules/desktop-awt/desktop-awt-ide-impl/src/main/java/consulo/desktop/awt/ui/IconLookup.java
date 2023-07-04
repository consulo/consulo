/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.ui;

import consulo.application.util.SystemInfo;
import consulo.desktop.awt.ui.plaf.darcula.LafIconLookup;
import consulo.desktop.awt.ui.plaf.windows.WinIconLookup;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 28/06/2023
 */
public interface IconLookup {
  static IconLookup get() {
    if (SystemInfo.isWindows) {
      return WinIconLookup.INSTANCE;
    }
    
    return LafIconLookup.INSTANCE;
  }

  default Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled) {
    return getIcon(name, selected, focused, enabled, false, false);
  }

  default Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable) {
    return getIcon(name, selected, focused, enabled, editable, false);
  }

  Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable, boolean pressed);
}
