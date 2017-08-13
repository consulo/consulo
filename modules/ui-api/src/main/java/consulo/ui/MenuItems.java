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

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class MenuItems {
  @NotNull
  public static MenuItem item(@NotNull String text) {
    return _UIInternals.get()._MenuItems_item(text);
  }

  @NotNull
  public static MenuBar menuBar() {
    return _UIInternals.get()._MenuItems_menuBar();
  }

  @NotNull
  public static Menu menu(@NotNull String text) {
    return _UIInternals.get()._MenuItems_menu(text);
  }
}
