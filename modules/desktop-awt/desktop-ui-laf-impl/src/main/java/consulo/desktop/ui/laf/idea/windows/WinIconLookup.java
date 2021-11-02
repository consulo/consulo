/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.ui.laf.idea.windows;

import consulo.awt.TargetAWT;
import consulo.desktop.ui.laf.idea.darcula.LafIconLookup;
import consulo.desktop.ui.laf.windows.icon.WindowsIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * just adapter for icon groups
 *
 * @see LafIconLookup
 */
public class WinIconLookup {
  private static final Map<String, Icon> ourCache = new ConcurrentHashMap<>();

  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled) {
    return getIcon(name, selected, focused, enabled, false, false);
  }

  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable) {
    return getIcon(name, selected, focused, enabled, editable, false);
  }

  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable, boolean pressed) {
    return findIcon(name, selected, focused, enabled, editable, pressed, true);
  }

  @Nonnull
  public static Icon findIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable, boolean pressed, boolean isThrowErrorIfNotFound) {
    String key = name;
    if (editable) {
      key = name + "Editable";
    }

    if (selected) {
      key = key + "Selected";
    }

    if (pressed) {
      key = key + "Pressed";
    }
    else if (focused) {
      key = key + "Focused";
    }
    else if (!enabled) {
      key = key + "Disabled";
    }

    int width = Image.DEFAULT_ICON_SIZE;
    int height = Image.DEFAULT_ICON_SIZE;

    if (name.equals("checkBox") || name.equals("radio") || name.equals("checkBoxIndeterminate")) {
      width = 13;
      height = 13;
    }
    else if (name.equals("comboDropTriangle")) {
      width = 10;
      height = 6;
    }


    String imageId = "components." + key.toLowerCase(Locale.ROOT);

    final int finalWidth = width;
    final int finalHeight = height;
    return ourCache.computeIfAbsent(imageId, s -> TargetAWT.to(ImageKey.of(WindowsIconGroup.ID, imageId, finalWidth, finalHeight)));
  }
}
