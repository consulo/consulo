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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import consulo.annotations.Internal;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.internal.*;
import consulo.ui.internal.image.*;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.model.ImmutableListModelImpl;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.MutableListModelImpl;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.StaticPosition;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.net.URL;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
@Internal
public class DesktopUIInternalImpl extends UIInternal {
  @Override
  public Image _Images_image(URL url) {
    return new DesktopImageImpl(url);
  }

  @Override
  public Image _ImageEffects_layered(Image[] images) {
    return new DesktopLayeredImageImpl(images);
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return new DesktopTransparentImageImpl(original, alpha);
  }

  @Override
  public Image _ImageEffects_grayed(@Nonnull Image original) {
    return DesktopDisabledImageImpl.of(original);
  }

  @Override
  public Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    DesktopAppendImageImpl image = new DesktopAppendImageImpl(2);
    image.setIcon(TargetAWT.to(i0), 0);
    image.setIcon(TargetAWT.to(i1), 1);
    return image;
  }

  @Override
  public Image _ImageEffects_empty(int width, int height) {
    return DesktopEmptyImageImpl.get(width, height);
  }

  @Override
  public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
    return new DesktopCanvasImageImpl(width, height, consumer);
  }

  @Override
  MenuItem _MenuItem_create(String text) {
    return new DesktopMenuItemImpl(text);
  }

  @Override
  Menu _Menu_create(String text) {
    return new DesktopMenuImpl(text);
  }

  @Override
  MenuSeparator _MenuSeparator_create() {
    return DesktopMenuSeparatorImpl.INSTANCE;
  }

  @Override
  ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return new DesktopBoolValueGroup();
  }

  @Override
  MenuBar _MenuItems_menuBar() {
    return new DesktopMenuBarImpl();
  }

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return new DesktopStyleManagerImpl();
  }

  @Nonnull
  @Override
  public Window _Windows_modalWindow(String title) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> AlertBuilder<T> _Alerts_create() {
    return null;
  }

  @Override
  public <T> ListModel<T> _ListModel_create(Collection<? extends T> list) {
    return new ImmutableListModelImpl<>(list);
  }

  @Override
  public <T> MutableListModel<T> _MutableListModel_create(Collection<? extends T> list) {
    return new MutableListModelImpl<>(list);
  }

  @Override
  public CheckBox _Components_checkBox() {
    return new DesktopCheckBoxImpl();
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new DesktopDockLayoutImpl();
  }

  @Override
  WrappedLayout _Layouts_wrapped() {
    return new DesktopWrappedLayoutImpl();
  }

  @Override
  VerticalLayout _Layouts_vertical() {
    return new DesktopVerticalLayoutImpl();
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    return new DesktopSwipeLayoutImpl();
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
  TableLayout _Layouts_table(StaticPosition fillOption) {
    return new DesktopTableLayoutImpl(fillOption);
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
  Hyperlink _Components_hyperlink(String text) {
    return null;
  }

  @Override
  HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return new DesktopHorizontalLayoutImpl(gapInPixesl);
  }

  @Override
  ImageBox _Components_imageBox(Image image) {
    return new DesktopImageBoxImpl(image);
  }

  @Override
  ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
    return new DesktopColorBoxImpl(colorValue);
  }

  @Override
  <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  UIAccess _UIAccess_get() {
    Application application = ApplicationManager.getApplication();
    return application == null ? AWTUIAccessImpl.ourInstance : IdeAWTUIAccessImpl.ourInstance;
  }

  @Override
  boolean _UIAccess_isUIThread() {
    return SwingUtilities.isEventDispatchThread();
  }
}
