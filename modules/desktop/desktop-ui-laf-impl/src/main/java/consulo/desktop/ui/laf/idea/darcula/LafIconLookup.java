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

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * from kotlin
 */
public class LafIconLookup {
  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled) {
    return getIcon(name, selected, focused, enabled, false, false);
  }

  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable) {
    return getIcon(name, selected, focused, enabled, editable, false);
  }

  public static Icon getIcon(@Nonnull String name, boolean selected, boolean focused, boolean enabled, boolean editable, boolean pressed) {
    Icon icon = findIcon(name, selected, focused, enabled, editable, pressed, true);
    if (icon == null) {
      icon = EmptyIcon.ICON_16;
    }

    return icon;
  }

  @Nullable
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

    String dir;
    /*if (UIUtil.isUnderDefaultMacTheme()) {
      dir = (UIUtil.isGraphite() ? "graphite/" : "");
    }
    else*/
    {
     /* if (UIUtil.isUnderWin10LookAndFeel()) {
        dir = "win10/";
      }
      else */
      {
        if (UIUtil.isUnderDarcula()) {
          dir = "darcula/";
        }
        else {
          dir = (UIUtil.isUnderIntelliJLaF() ? "intellij/" : "");
        }
      }
    }
    return findLafIcon(dir + key, LafIconLookup.class, isThrowErrorIfNotFound);
  }

  @Nullable
  public static Icon findLafIcon(@Nonnull String key, @Nonnull Class aClass, boolean strict) {
    return IconLoader.findIcon("/icons/" + key + ".png", aClass, true, strict);
  }

}
