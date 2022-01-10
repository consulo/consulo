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
package consulo.desktop.ui.laf.idea.darcula;

import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * from kotlin
 */
public class LafIconLookup {
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

    String imageId = "components." + key.toLowerCase(Locale.ROOT);

    return ourCache.computeIfAbsent(imageId, s -> TargetAWT.to(ImageKey.of("consulo.platform.desktop.laf.LookAndFeelIconGroup", imageId, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE)));
  }
}
