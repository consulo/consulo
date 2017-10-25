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

import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
@Deprecated
@DeprecationInfo("Check children description")
public class Layouts {
  @NotNull
  @Deprecated
  @DeprecationInfo("Use DockLayout#create")
  public static DockLayout dock() {
    return UIInternal.get()._Layouts_dock();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use VerticalLayout#create")
  public static VerticalLayout vertical() {
    return UIInternal.get()._Layouts_vertical();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use HorizontalLayout#create")
  public static HorizontalLayout horizontal() {
    return UIInternal.get()._Layouts_horizontal();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use SplitLayout#createHorizontal")
  public static SplitLayout horizontalSplit() {
    return UIInternal.get()._Layouts_horizontalSplit();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use SplitLayout#createVertical")
  public static SplitLayout verticalSplit() {
    return UIInternal.get()._Layouts_verticalSplit();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use TabbedLayout#create")
  public static TabbedLayout tabbed() {
    return UIInternal.get()._Layouts_tabbed();
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use TableLayout#create")
  public static TableLayout table(int rows, int columns) {
    return UIInternal.get()._Layouts_table(rows, columns);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use LabeledLayout#create")
  public static LabeledLayout labeled(@NotNull String label) {
    return UIInternal.get()._Layouts_labeled(label);
  }
}
