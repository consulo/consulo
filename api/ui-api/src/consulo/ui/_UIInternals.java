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

import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
abstract class _UIInternals {
  static _UIInternals impl() {
    return Holder.ourInstance;
  }

  private static class Holder {
    public static _UIInternals ourInstance = impl();

    private static _UIInternals impl() {
      _UIInternals bindingInternal = null;

      try {
        Class<?> bindingClass = Class.forName(_UIInternals.class.getName() + "Impl");
        bindingInternal = (_UIInternals)bindingClass.newInstance();
      }
      catch (Exception e) {
        throw new Error("Fail to init ui binding", e);
      }
      return bindingInternal;
    }
  }

  abstract CheckBox _Components_checkBox(@NotNull String text, boolean selected);

  abstract DockLayout _Layouts_dock();

  abstract VerticalLayout _Layouts_vertical();

  abstract SplitLayout _Layouts_horizontalSplit();

  abstract SplitLayout _Layouts_verticalSplit();

  abstract TabbedLayout _Layouts_tabbed();

  abstract LabeledLayout _Layouts_labeled(String label);

  abstract TableLayout _Layouts_table(int rows, int columns);

  abstract HorizontalLayout _Layouts_horizontal();

  abstract Label _Components_label(String text);

  abstract HtmlLabel _Components_htmlLabel(String html);

  abstract <E> ComboBox<E> _Components_comboBox(ListModel<E> model);

  abstract RadioButton _Components_radioButton(String text, boolean selected);

  abstract Image _Components_image(ImageRef imageRef);

  abstract ImageRef _Images_imageRef(URL url);

  abstract MenuItem _MenuItems_item(String text);

  abstract Menu _MenuItems_menu(String text);

  abstract MenuBar _MenuItems_menuBar();

  @RequiredUIAccess
  @NotNull
  abstract UIAccess _UIAccess_get();

  abstract boolean _UIAccess_isUIThread();
}
