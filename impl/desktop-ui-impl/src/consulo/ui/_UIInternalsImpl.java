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

import com.intellij.openapi.util.IconLoader;
import consulo.ui.internal.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URL;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
class _UIInternalsImpl extends _UIInternals {
  static {
    IconLoader.activate(); // TODO [VISTALL] hack until we not start Consulo app
  }

  @Override
  ImageRef _Images_imageRef(URL url) {
    return new DesktopImageRefImpl(url);
  }

  @Override
  MenuItem _MenuItems_item(String text) {
    return new DesktopMenuItemImpl(text);
  }

  @Override
  Menu _MenuItems_menu(String text) {
    return new DesktopMenuImpl(text);
  }

  @Override
  MenuBar _MenuItems_menuBar() {
    return new DesktopMenuBarImpl();
  }

  @Override
  public CheckBox _Components_checkBox(@NotNull String text, boolean selected) {
    return new DesktopCheckBoxImpl(text, selected);
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new DesktopDockLayoutImpl();
  }

  @Override
  VerticalLayout _Layouts_vertical() {
    return new DesktopVerticalLayoutImpl();
  }

  @Override
  SplitLayout _Layouts_horizontalSplit() {
    return new DesktopSplitLayoutImpl();
  }

  @Override
  SplitLayout _Layouts_verticalSplit() {
    DesktopSplitLayoutImpl impl = new DesktopSplitLayoutImpl();
    impl.setOrientation(true);
    return impl;
  }

  @Override
  TabbedLayout _Layouts_tabbed() {
    return new DesktopTabbedLayoutImpl();
  }

  @Override
  LabeledLayout _Layouts_labeled(String label) {
    return new DesktopLabeledLayoutImpl(label);
  }

  @Override
  TableLayout _Layouts_table(int rows, int columns) {
    return null;
  }

  @Override
  Label _Components_label(String text) {
    return new DesktopLabelImpl(text);
  }

  @Override
  HtmlLabel _Components_htmlLabel(String html) {
    return new DesktopHtmlLabelImpl(html);
  }

  @Override
  <E> ComboBox<E> _Components_comboBox(consulo.ui.model.ListModel<E> model) {
    return new DesktopComboBoxImpl<E>(model);
  }

  @Override
  RadioButton _Components_radioButton(String text, boolean selected) {
    return new DesktopRadioButtonImpl(text, selected);
  }

  @Override
  HorizontalLayout _Layouts_horizontal() {
    return new DesktopHorizontalLayoutImpl();
  }

  @Override
  Image _Components_image(ImageRef imageRef) {
    return null;
  }

  @NotNull
  @Override
  UIAccess _UIAccess_get() {
    return DesktopUIAccessImpl.ourInstance;
  }

  @Override
  boolean _UIAccess_isUIThread() {
    return SwingUtilities.isEventDispatchThread();
  }
}
