/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.EmptyImage;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-11-08
 */
public class LightUIInternal extends UIInternal {
  @Override
  public CheckBox _Components_checkBox() {
    return null;
  }

  @Override
  public DockLayout _Layouts_dock(int gapInPixels) {
    return null;
  }

  @Override
  public WrappedLayout _Layouts_wrapped() {
    return null;
  }

  @Override
  public VerticalLayout _Layouts_vertical(int vGap) {
    return null;
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    return null;
  }

  @Override
  public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
    return null;
  }

  @Override
  public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
    return null;
  }

  @Override
  public TabbedLayout _Layouts_tabbed() {
    return null;
  }

  @Override
  public LabeledLayout _Layouts_labeled(LocalizeValue label) {
    return null;
  }

  @Override
  public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return null;
  }

  @Override
  public Label _Components_label(LocalizeValue text, LabelOptions options) {
    return null;
  }

  @Override
  public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions labelOptions) {
    return null;
  }

  @Override
  public <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return null;
  }

  @Override
  public TextBox _Components_textBox(String text) {
    return null;
  }

  @Override
  public ProgressBar _Components_progressBar() {
    return null;
  }

  @Nonnull
  @Override
  public IntBox _Components_intBox(int value) {
    return null;
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return null;
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    return null;
  }

  @Override
  public ImageBox _Components_imageBox(Image image) {
    return null;
  }

  @Override
  public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
    return null;
  }

  @Override
  public Image _Image_lazy(Supplier<Image> imageSupplier) {
    return null;
  }

  @Override
  public Image _ImageEffects_layered(@Nonnull Image[] images) {
    return null;
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return null;
  }

  @Override
  public Image _ImageEffects_grayed(@Nonnull Image original) {
    return null;
  }

  @Override
  public Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    return null;
  }

  @Override
  public EmptyImage _ImageEffects_empty(int width, int height) {
    return null;
  }

  @Override
  public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
    return null;
  }

  @Override
  public Image _ImageEffects_withText(Image baseImage, String text) {
    return null;
  }

  @Override
  public Image _ImageEffects_resize(Image original, int width, int height) {
    return null;
  }

  @Override
  public MenuItem _MenuItem_create(String text) {
    return null;
  }

  @Override
  public Menu _Menu_create(String text) {
    return null;
  }

  @Override
  public MenuSeparator _MenuSeparator_create() {
    return null;
  }

  @Override
  public ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return null;
  }

  @Override
  public MenuBar _MenuItems_menuBar() {
    return null;
  }

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return null;
  }

  @Nonnull
  @Override
  public FontManager _FontManager_get() {
    return null;
  }

  @Nonnull
  @Override
  public Window _Window_create(String title, WindowOptions options) {
    return null;
  }

  @Nullable
  @Override
  public Window _Window_getActiveWindow() {
    return null;
  }

  @Override
  public <T> Alert<T> _Alerts_create() {
    return null;
  }

  @Override
  public <T> ListModel<T> _ListModel_create(Collection<? extends T> list) {
    return null;
  }

  @Override
  public <T> MutableListModel<T> _MutableListModel_create(Collection<? extends T> list) {
    return null;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public UIAccess _UIAccess_get() {
    return null;
  }

  @Override
  public boolean _UIAccess_isUIThread() {
    return false;
  }

  @Override
  public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage,
                                                                     String dialogTitle,
                                                                     Function<String, List<String>> parser,
                                                                     Function<List<String>, String> joiner) {
    return null;
  }

  @Override
  public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
    return null;
  }

  @Override
  public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState _ModalityState_any() {
    return LightModalityState.INSTANCE;
  }

  @Nonnull
  @Override
  public ModalityState _ModalityState_nonModal() {
    return LightModalityState.INSTANCE;
  }
}
