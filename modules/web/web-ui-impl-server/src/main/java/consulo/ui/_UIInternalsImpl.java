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

import com.intellij.openapi.util.IconLoader;
import com.vaadin.ui.UI;
import consulo.ui.internal.*;
import consulo.ui.model.ListModel;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
class _UIInternalsImpl extends _UIInternals {
  static {
    IconLoader.activate(); // TODO [VISTALL] hack until we not start Consulo app
  }

  @Override
  CheckBox _Components_checkBox(@NotNull String text, boolean selected) {
    return new WGwtCheckBoxImpl(selected, text);
  }

  @Override
  DockLayout _Layouts_dock() {
    return new WGwtDockLayoutImpl();
  }

  @Override
  VerticalLayout _Layouts_vertical() {
    return new WGwtVerticalLayoutImpl();
  }

  @Override
  SplitLayout _Layouts_horizontalSplit() {
    return new WGwtHorizontalSplitLayoutImpl();
  }

  @Override
  SplitLayout _Layouts_verticalSplit() {
    return new WGwtVerticalSplitLayoutImpl();
  }

  @Override
  TabbedLayout _Layouts_tabbed() {
    return new WGwtTabbedLayoutImpl();
  }

  @Override
  LabeledLayout _Layouts_labeled(String label) {
    return new WGwtLabeledLayoutImpl(label);
  }

  @Override
  TableLayout _Layouts_table(int rows, int columns) {
    throw new UnsupportedOperationException();
  }

  @Override
  Label _Components_label(String text) {
    return new WGwtLabelImpl(text);
  }

  @Override
  HtmlLabel _Components_htmlLabel(String html) {
    return new WGwtHtmlLabelImpl(html);
  }

  @Override
  <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return new WGwtComboBoxImpl<E>(model);
  }

  @Override
  TextField _Components_TextField(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  RadioButton _Components_radioButton(String text, boolean selected) {
    return new WGwtRadioButtonImpl(selected, text);
  }

  @Override
  HorizontalLayout _Layouts_horizontal() {
    return new WGwtHorizontalLayoutImpl();
  }

  @Override
  Image _Components_image(ImageRef imageRef) {
    return new WGwtImageImpl((WGwtImageRefImpl)imageRef);
  }

  @Override
  ImageRef _Images_imageRef(URL url) {
    return new WGwtImageRefImpl(url);
  }

  @Override
  MenuItem _MenuItems_item(String text) {
    return new WGwtMenuItemImpl(text);
  }

  @Override
  Menu _MenuItems_menu(String text) {
    return new WGwtMenuImpl(text);
  }

  @Override
  ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return new ValueGroup<Boolean>() {
      @RequiredUIAccess
      @Override
      public void clearValues() {

      }

      @NotNull
      @Override
      public ValueGroup<Boolean> add(ValueComponent<Boolean> component) {
        return this;
      }
    };
  }

  @Override
  MenuBar _MenuItems_menuBar() {
    return new WGwtMenuBarImpl();
  }

  @RequiredUIAccess
  @NotNull
  @Override
  UIAccess _UIAccess_get() {
    UI ui = UI.getCurrent();
    assert ui != null;
    Object data = ui.getData();
    if(data != null) {
      return (UIAccess)data;
    }
    else {
      VaadinUIAccessImpl access = new VaadinUIAccessImpl(ui);
      ui.setData(access);
      return access;
    }
  }

  @Override
  boolean _UIAccess_isUIThread() {
    return UI.getCurrent() != null;
  }
}
