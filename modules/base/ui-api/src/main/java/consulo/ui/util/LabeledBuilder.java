/*
 * Copyright 2013-2020 consulo.io
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

import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-08-23
 */
public class LabeledBuilder {
  private static final BiFunction<LocalizeManager, String, String> SEMICOLON_APPENDER = (localizeManager, text) -> !StringUtil.endsWithChar(text, ':') ? text + ":" : text;

  @RequiredUIAccess
  public static Component simple(@Nonnull LocalizeValue localizeValue, @Nonnull Component component) {
    return simple(localizeValue, () -> component);
  }

  @RequiredUIAccess
  public static Component simple(@Nonnull LocalizeValue localizeValue, @Nonnull PseudoComponent component) {
    HorizontalLayout horizontal = HorizontalLayout.create(5);
    horizontal.add(Label.create(localizeValue.map(SEMICOLON_APPENDER)));
    horizontal.add(component);
    return horizontal;
  }

  @RequiredUIAccess
  public static Component filled(@Nonnull LocalizeValue localizeValue, @Nonnull Component component) {
    return filled(localizeValue, () -> component);
  }

  @RequiredUIAccess
  public static Component filled(@Nonnull LocalizeValue localizeValue, @Nonnull PseudoComponent component) {
    DockLayout dock = DockLayout.create();
    dock.left(Label.create(localizeValue.map(SEMICOLON_APPENDER)));
    dock.center(component);
    return dock;
  }

  @RequiredUIAccess
  public static Component sided(@Nonnull LocalizeValue localizeValue, @Nonnull Component component) {
    return sided(localizeValue, () -> component);
  }

  @RequiredUIAccess
  public static Component sided(@Nonnull LocalizeValue localizeValue, @Nonnull PseudoComponent component) {
    DockLayout dock = DockLayout.create();
    dock.left(Label.create(localizeValue));
    dock.right(component);
    return dock;
  }
}
