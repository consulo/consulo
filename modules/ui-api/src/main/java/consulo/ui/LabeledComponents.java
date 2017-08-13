/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import com.intellij.openapi.util.text.StringUtilRt;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 05-Nov-16
 */
public class LabeledComponents {
  @RequiredUIAccess
  public static Component left(@NotNull String text, @NotNull Component component) {
    if (!StringUtilRt.endsWithChar(text, ':')) {
      text += ": ";
    }

    HorizontalLayout horizontal = Layouts.horizontal();
    horizontal.add(Components.label(text));
    horizontal.add(component);
    return horizontal;
  }

  @RequiredUIAccess
  public static Component leftFilled(@NotNull String text, @NotNull Component component) {
    if (!StringUtilRt.endsWithChar(text, ':')) {
      text += ": ";
    }

    DockLayout dock = Layouts.dock();
    dock.left(Components.label(text));
    dock.center(component);
    return dock;
  }
}
