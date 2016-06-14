/*
 * Copyright 2013-2016 must-be.org
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

import org.jetbrains.annotations.NotNull;

/**
* @author VISTALL
* @since 14-Jun-16
*/
public class Layouts {
  @NotNull
  public static DockLayout dock() {
    return _UIInternals.impl()._Layouts_dock();
  }

  @NotNull
  public static VerticalLayout vertical() {
    return _UIInternals.impl()._Layouts_vertical();
  }

  @NotNull
  public static HorizontalLayout horizontal() {
    return _UIInternals.impl()._Layouts_horizontal();
  }

  @NotNull
  public static SplitLayout horizontalSplit() {
    return _UIInternals.impl()._Layouts_horizontalSplit();
  }

  @NotNull
  public static SplitLayout verticalSplit() {
    return _UIInternals.impl()._Layouts_verticalSplit();
  }

  @NotNull
  public static TabbedLayout tabbed() {
    return _UIInternals.impl()._Layouts_tabbed();
  }
}
