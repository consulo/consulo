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
import consulo.ui.image.FoldedImage;
import consulo.ui.image.Image;
import consulo.ui.internal.*;
import consulo.ui.internal.icon.DesktopFoldedImageImpl;
import consulo.ui.internal.icon.DesktopImageImpl;
import consulo.ui.model.ListModel;
import consulo.ui.style.StyleManager;
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
  public Image _Images_image(URL url) {
    return new DesktopImageImpl(url);
  }

  @Override
  public FoldedImage _Images_foldedImage(Image[] images) {
    return new DesktopFoldedImageImpl(images);
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
  ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return new DesktopBoolValueGroup();
  }

  @Override
  MenuBar _MenuItems_menuBar() {
    return new DesktopMenuBarImpl();
  }

  @NotNull
  @Override
  public StyleManager _StyleManager_get() {
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
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
    return new DesktopComboBoxImpl<>(model);
  }

  @Override
  TextBox _Components_textBox(String text) {
    return new DesktopTextBoxImpl(text);
  }

  @Override
  <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new DesktopListBoxImpl<>(model);
  }

  @Override
  RadioButton _Components_radioButton(String text, boolean selected) {
    return new DesktopRadioButtonImpl(text, selected);
  }

  @Override
  Button _Components_button(String text) {
    return new DesktopButtonImpl(text, null);
  }

  @Override
  HorizontalLayout _Layouts_horizontal() {
    return new DesktopHorizontalLayoutImpl();
  }

  @Override
  ImageBox _Components_imageBox(Image image) {
    return new DesktopImageBoxImpl(image);
  }

  @Override
  <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @NotNull
  @Override
  UIAccess _UIAccess_get() {
    return AWTUIAccessImpl.ourInstance;
  }

  @Override
  boolean _UIAccess_isUIThread() {
    return SwingUtilities.isEventDispatchThread();
  }
}
