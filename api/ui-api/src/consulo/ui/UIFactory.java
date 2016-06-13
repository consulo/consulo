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

import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.ImmutableListModel;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class UIFactory {
  public static class Components {
    @NotNull
    public static CheckBox checkBox(@NotNull String text) {
      return checkBox(text, false);
    }

    @NotNull
    public static CheckBox checkBox(@NotNull String text, boolean selected) {
      return _UIInternals.impl()._Components_checkBox(text, selected);
    }

    @NotNull
    public static Label label(@NotNull String text) {
      return _UIInternals.impl()._Components_label(text);
    }

    @NotNull
    public static HtmlLabel htmlLabel(@NotNull String html) {
      return _UIInternals.impl()._Components_htmlLabel(html);
    }

    @NotNull
    public static <E> ComboBox<E> comboBox(@NotNull E... elements) {
      return _UIInternals.impl()._Components_comboBox(new ImmutableListModel<E>(elements));
    }

    @NotNull
    public static <E> ComboBox<E> comboBox(@NotNull ListModel<E> model) {
      return _UIInternals.impl()._Components_comboBox(model);
    }

    @NotNull
    public static Image image(@NotNull URL url) {
      return _UIInternals.impl()._Components_image(url);
    }
  }

  public static class Layouts {
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
  }
}
