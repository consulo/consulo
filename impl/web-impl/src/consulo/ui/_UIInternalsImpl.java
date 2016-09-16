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
import consulo.ui.internal.*;
import consulo.ui.model.ListModel;
import consulo.web.servlet.ui.UIAccessHelper;
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
    return null;
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
  MenuBar _MenuItems_menuBar() {
    return new WGwtMenuBarImpl();
  }

  @NotNull
  @Override
  UIAccess _UIAccess_get() {
    return UIAccessHelper.ourInstance.get();
  }

  @Override
  boolean _UIAccess_isUIThread() {
    return UIAccessHelper.ourInstance.isUIThread();
  }
}
