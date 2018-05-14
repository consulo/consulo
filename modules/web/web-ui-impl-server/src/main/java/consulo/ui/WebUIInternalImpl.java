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
import consulo.annotations.Internal;
import consulo.ui.image.Image;
import consulo.ui.internal.*;
import consulo.ui.internal.image.WGwtFoldedImageImpl;
import consulo.ui.internal.image.WGwtImageImpl;
import consulo.ui.internal.image.WGwtTransparentImageImpl;
import consulo.ui.model.ListModel;
import consulo.ui.shared.StaticPosition;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
@Internal
public class WebUIInternalImpl extends UIInternal {
  static {
    IconLoader.activate(); // TODO [VISTALL] hack until we not start Consulo app
  }

  @Override
  CheckBox _Components_checkBox() {
    return new WGwtCheckBoxImpl();
  }

  @Override
  DockLayout _Layouts_dock() {
    return new WGwtDockLayoutImpl();
  }

  @Override
  WrappedLayout _Layouts_wrapped() {
    return new WGwtWrappedLayoutImpl();
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
  TableLayout _Layouts_table(StaticPosition fillOption) {
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
    return new WGwtComboBoxImpl<>(model);
  }

  @Override
  TextBox _Components_textBox(String text) {
    return new WGwtTextBoxImpl(text);
  }

  @Override
  <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new WGwtListBoxImpl<>(model);
  }

  @Override
  RadioButton _Components_radioButton(String text, boolean selected) {
    return new WGwtRadioButtonImpl(selected, text);
  }

  @Override
  Button _Components_button(String text) {
    return new WGwtButtonImpl(text, null);
  }

  @Override
  Hyperlink _Components_hyperlink(String text) {
    return new WGwtHyperlinkImpl(text);
  }

  @Override
  HorizontalLayout _Layouts_horizontal() {
    return new WGwtHorizontalLayoutImpl();
  }

  @Override
  ImageBox _Components_imageBox(Image image) {
    return new WGwtImageBoxImpl(image);
  }

  @Override
  <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    return new WGwtTreeImpl<>(rootValue, model);
  }

  @Override
  public Image _Images_image(URL url) {
    return new WGwtImageImpl(url);
  }

  @Override
  public Image _ImageEffects_layered(Image[] images) {
    return new WGwtFoldedImageImpl(images);
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return new WGwtTransparentImageImpl(original, alpha);
  }

  @Override
  public Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    return null;
  }

  @Override
  public Image _ImageEffects_empty(int width, int height) {
    return null;
  }

  @Override
  MenuItem _MenuItem_create(String text) {
    return new WGwtMenuItemImpl(text);
  }

  @Override
  Menu _Menu_create(String text) {
    return new WGwtMenuImpl(text);
  }

  @Override
  MenuSeparator _MenuSeparator_create() {
    return new WGwtMenuSeparatorImpl();
  }

  @Override
  ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return new ValueGroup<Boolean>() {
      @RequiredUIAccess
      @Override
      public void clearValues() {

      }

      @Nonnull
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

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return WGwtStyleManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public Window _Windows_modalWindow(String title) {
    VaadinWindowImpl window = new VaadinWindowImpl(true);
    window.setTitle(title);
    return window;
  }

  @Override
  public AlertBuilder _Alerts_builder() {
    return null;
  }

  @RequiredUIAccess
  @Nonnull
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
