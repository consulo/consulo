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
package consulo.ui.util;

import consulo.annotation.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Nov-16
 */
@Deprecated
@DeprecationInfo("See LabeledBuilder")
public class LabeledComponents {
  @RequiredUIAccess
  public static Component left(@Nonnull String text, @Nonnull Component component) {
    return left(text, () -> component);
  }

  @RequiredUIAccess
  public static Component left(@Nonnull String text, @Nonnull PseudoComponent component) {
    if (!StringUtil.endsWithChar(text, ':')) {
      text += ": ";
    }

    HorizontalLayout horizontal = HorizontalLayout.create(5);
    horizontal.add(Label.create(text));
    horizontal.add(component);
    return horizontal;
  }

  @RequiredUIAccess
  public static Component leftFilled(@Nonnull String text, @Nonnull Component component) {
    return leftFilled(text, () -> component);
  }

  @RequiredUIAccess
  public static Component leftFilled(@Nonnull String text, @Nonnull PseudoComponent component) {
    if (!StringUtil.endsWithChar(text, ':')) {
      text += ": ";
    }

    DockLayout dock = DockLayout.create();
    dock.left(Label.create(text));
    dock.center(component);
    return dock;
  }

  @RequiredUIAccess
  public static Component leftWithRight(@Nonnull String text, @Nonnull Component component) {
    return leftWithRight(text, () -> component);
  }

  @RequiredUIAccess
  public static Component leftWithRight(@Nonnull String text, @Nonnull PseudoComponent component) {
    DockLayout dock = DockLayout.create();
    dock.left(Label.create(text));
    dock.right(component);
    return dock;
  }
}
