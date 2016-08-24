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

import consulo.ui.model.ImmutableListModel;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

/**
* @author VISTALL
* @since 14-Jun-16
*/
public class Components {
  @NotNull
  public static CheckBox checkBox(@NotNull String text) {
    return checkBox(text, false);
  }

  @NotNull
  public static CheckBox checkBox(@NotNull String text, boolean selected) {
    return _UIInternals.get()._Components_checkBox(text, selected);
  }

  @NotNull
  public static RadioButton radioButton(@NotNull String text) {
    return radioButton(text, false);
  }

  @NotNull
  public static RadioButton radioButton(@NotNull String text, boolean selected) {
    return _UIInternals.get()._Components_radioButton(text, selected);
  }

  @NotNull
  public static Label label(@NotNull String text) {
    return _UIInternals.get()._Components_label(text);
  }

  @NotNull
  public static HtmlLabel htmlLabel(@NotNull String html) {
    return _UIInternals.get()._Components_htmlLabel(html);
  }

  @NotNull
  public static <E> ComboBox<E> comboBox(@NotNull E... elements) {
    return _UIInternals.get()._Components_comboBox(new ImmutableListModel<E>(elements));
  }

  @NotNull
  public static <E> ComboBox<E> comboBox(@NotNull ListModel<E> model) {
    return _UIInternals.get()._Components_comboBox(model);
  }

  @NotNull
  public static Image image(@NotNull ImageRef imageRef) {
    return _UIInternals.get()._Components_image(imageRef);
  }
}
