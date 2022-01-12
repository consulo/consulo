/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.desktop.swt.ui.impl.font.DesktopSwtFontManagerImpl;
import consulo.desktop.swt.ui.impl.image.*;
import consulo.desktop.swt.ui.impl.layout.*;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.model.ImmutableListModelImpl;
import consulo.ui.impl.model.MutableListModelImpl;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.style.StyleManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtUIInternalImpl extends UIInternal {
  @Nonnull
  @Override
  public IconLibraryManager _IconLibraryManager_get() {
    return DesktopSwtIconLibraryManager.INSTANCE;
  }

  @Override
  public ImageKey _ImageKey_of(@Nonnull String groupId, @Nonnull String imageId, int width, int height) {
    return new DesktopSwtImageKeyImpl(groupId, imageId, width, height);
  }

  @Override
  public CheckBox _Components_checkBox() {
    return new DesktopSwtCheckBoxImpl();
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new DesktopSwtDockLayoutImpl();
  }

  @Override
  public WrappedLayout _Layouts_wrapped() {
    return new DesktopSwtWrappedLayoutImpl();
  }

  @Override
  public VerticalLayout _Layouts_vertical(int vGap) {
    return new DesktopSwtVerticalLayoutImpl(vGap);
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    return null;
  }

  @Override
  public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
    return new DesktopSwtTwoComponentSplitLayoutImpl(position);
  }

  @Override
  public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
    return new DesktopSwtThreeComponentSplitLayoutImpl(position);
  }

  @Override
  public TabbedLayout _Layouts_tabbed() {
    return new DesktopSwtTabbedLayoutImpl();
  }

  @Override
  public LabeledLayout _Layouts_labeled(LocalizeValue label) {
    return new DesktopSwtLabeledLayoutImpl(label);
  }

  @Override
  public TableLayout _Layouts_table(StaticPosition fillOption) {
    return new DesktopSwtTableLayoutImpl(fillOption);
  }

  @Override
  public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
    return new DesktopSwtScrollableLayoutImpl(component);
  }

  @Override
  public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return new DesktopSwtHorizontalLayoutImpl(gapInPixesl);
  }

  @Override
  public Label _Components_label(LocalizeValue text, LabelOptions options) {
    return new DesktopSwtLabelImpl(text);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions options) {
    return null;
  }

  @Override
  public <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return new DesktopSwtComboBoxImpl<>(model);
  }

  @Override
  public TextBox _Components_textBox(String text) {
    return new DesktopSwtTextBoxImpl(text);
  }

  @Override
  public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
    return new DesktopSwtTextBoxWithHistoryImpl(text);
  }

  @Override
  public ProgressBar _Components_progressBar() {
    return new DesktopSwtProgressBarImpl();
  }

  @Nonnull
  @Override
  public IntBox _Components_intBox(int value) {
    return new DesktopSwtIntBoxImpl(value);
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new DesktopSwtListBoxImpl<E>(model);
  }

  @Override
  public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
    return new DesktopSwtRadioButtonImpl(text, selected);
  }

  @Override
  public Button _Components_button(LocalizeValue text) {
    return new DesktopSwtButtonImpl(text);
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    return new DesktopSwtHyperlinkImpl(text);
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
  public Image _Image_fromUrl(URL url) throws IOException {
    return null;
  }

  @Override
  public Image _Image_lazy(Supplier<Image> imageSupplier) {
    return imageSupplier.get();
  }

  @Override
  public Image _ImageEffects_layered(@Nonnull Image[] images) {
    return new DesktopSwtLayeredImageImpl(images);
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return new DesktopSwtTransparentImageImpl(original, alpha);
  }

  @Override
  public Image _ImageEffects_grayed(@Nonnull Image original) {
    return original;
  }

  @Override
  public Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    return i1;
  }

  @Override
  public Image _ImageEffects_empty(int width, int height) {
    return new DesktopSwtEmptyImageImpl(width, height);
  }

  @Override
  public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
    return Image.empty(width, height);
  }

  @Override
  public Image _ImageEffects_withText(Image baseImage, String text) {
    return baseImage;
  }

  @Override
  public Image _ImageEffects_resize(Image original, int width, int height) {
    return new DesktopSwtResizeImageImpl(original, width, height);
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
    return new DesktopSwtBoolValueGroup();
  }

  @Override
  public MenuBar _MenuItems_menuBar() {
    return new DesktopSwtMenuBar();
  }

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return DesktopSwtStyleManagerImpl.INSTANCE;
  }

  @Nonnull
  @Override
  public FontManager _FontManager_get() {
    return DesktopSwtFontManagerImpl.INSTANCE;
  }

  @Override
  public FocusManager _FocusManager_get() {
    return DesktopSwtFocusManagerImpl.INSTANCE;
  }

  @Nonnull
  @Override
  public Window _Window_create(String title, WindowOptions options) {
    return new DesktopSwtWindowImpl(title, options);
  }

  @Nullable
  @Override
  public Window _Window_getActiveWindow() {
    Display display = DesktopSwtUIAccess.INSTANCE.getDisplay();

    Shell activeShell = display.getActiveShell();

    if(activeShell != null) {
      return (Window)TargetSWT.from(activeShell);
    }
    return null;
  }

  @Override
  public <T> Alert<T> _Alerts_create() {
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

  @RequiredUIAccess
  @Nonnull
  @Override
  public UIAccess _UIAccess_get() {
    return DesktopSwtUIAccess.INSTANCE;
  }

  @Override
  public boolean _UIAccess_isUIThread() {
    return Thread.currentThread().equals(Display.getDefault().getThread());
  }

  @Override
  public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
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

  @Override
  public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    return new DesktopSwtTreeImpl<E>(rootValue, model);
  }
}
