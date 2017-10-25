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
import consulo.ui.image.Image;
import consulo.ui.model.ImmutableListModel;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
@Deprecated
@DeprecationInfo("Check children description")
public class Components {
  @NotNull
  @Deprecated
  @DeprecationInfo("Use TextBox#create")
  public static TextBox textBox() {
    return textBox(null);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use TextBox#create")
  public static TextBox textBox(@Nullable String text) {
    return UIInternal.get()._Components_textBox(text);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use CheckBox#create")
  public static CheckBox checkBox(@NotNull String text) {
    return checkBox(text, false);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use CheckBox#create")
  public static CheckBox checkBox(@NotNull String text, boolean selected) {
    return UIInternal.get()._Components_checkBox(text, selected);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use RadioButton#create")
  public static RadioButton radioButton(@NotNull String text) {
    return radioButton(text, false);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use RadioButton#create")
  public static RadioButton radioButton(@NotNull String text, boolean selected) {
    return UIInternal.get()._Components_radioButton(text, selected);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use Label#create")
  public static Label label(@NotNull String text) {
    return UIInternal.get()._Components_label(text);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo("Use HtmlLabel#create")
  public static HtmlLabel htmlLabel(@NotNull String html) {
    return UIInternal.get()._Components_htmlLabel(html);
  }

  @Deprecated
  @DeprecationInfo("Use ComboBox#create")
  @SafeVarargs
  @NotNull
  public static <E> ComboBox<E> comboBox(@NotNull E... elements) {
    return UIInternal.get()._Components_comboBox(new ImmutableListModel<E>(elements));
  }

  @Deprecated
  @DeprecationInfo("Use ComboBox#create")
  @NotNull
  public static <E> ComboBox<E> comboBox(@NotNull ListModel<E> model) {
    return UIInternal.get()._Components_comboBox(model);
  }

  @Deprecated
  @DeprecationInfo("Use ListBox#create")
  @SafeVarargs
  @NotNull
  public static <E> ListBox<E> listBox(@NotNull E... elements) {
    return UIInternal.get()._Components_listBox(new ImmutableListModel<E>(elements));
  }

  @Deprecated
  @DeprecationInfo("Use ListBox#create")
  @NotNull
  public static <E> ListBox<E> listBox(@NotNull ListModel<E> model) {
    return UIInternal.get()._Components_listBox(model);
  }

  @Deprecated
  @DeprecationInfo("Use ImageBox#create")
  @NotNull
  public static ImageBox imageBox(@NotNull Image image) {
    return UIInternal.get()._Components_imageBox(image);
  }

  @Deprecated
  @DeprecationInfo("Use Tree#create")
  @NotNull
  public static <E> Tree<E> tree(@NotNull TreeModel<E> model) {
    return tree(null, model);
  }

  @Deprecated
  @DeprecationInfo("Use Tree#create")
  @NotNull
  public static <E> Tree<E> tree(@Nullable E rootValue, @NotNull TreeModel<E> model) {
    return UIInternal.get()._Components_tree(rootValue, model);
  }

  @Deprecated
  @DeprecationInfo("Use Button#create")
  @NotNull
  public static Button button(@NotNull String text) {
    return button(text, null);
  }

  @Deprecated
  @DeprecationInfo("Use Button#create")
  @NotNull
  public static Button button(@NotNull String text, @Nullable @RequiredUIAccess Button.ClickHandler clickHandler) {
    Button button = UIInternal.get()._Components_button(text);
    if (clickHandler != null) {
      button.addListener(Button.ClickHandler.class, clickHandler);
    }
    return button;
  }
}
