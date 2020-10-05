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
package consulo.ui.desktop.internal;

import com.intellij.ui.scale.JBUIScale;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Menu;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.image.*;
import consulo.ui.desktop.internal.image.libraryImage.DesktopImageKeyImpl;
import consulo.ui.desktop.internal.layout.*;
import consulo.ui.desktop.internal.style.DesktopStyleManagerImpl;
import consulo.ui.desktop.internal.textBox.*;
import consulo.ui.font.FontManager;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.ImageState;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.model.ImmutableListModelImpl;
import consulo.ui.impl.model.MutableListModelImpl;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.StaticPosition;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopUIInternalImpl extends UIInternal {
  @Override
  public Image _Image_fromUrl(URL url) {
    return new DesktopImageImpl(url);
  }

  @Override
  public Image _Image_fromBytes(byte[] bytes, int width, int height) {
    return new DesktopFromBytesImageImpl(bytes, width, height);
  }

  @Override
  public Image _Image_lazy(Supplier<Image> imageSupplier) {
    return new DesktopLazyImageImpl(imageSupplier);
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
    return new DesktopAppendImageImpl(i0, i1);
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
  public Image _ImageEffects_withText(Image baseImage, String text) {
    DesktopImageWithTextImpl withText = new DesktopImageWithTextImpl(text, new JLabel(), JBUIScale.scaleFontSize(6f));
    DesktopLayeredImageImpl image = new DesktopLayeredImageImpl(2);
    image.setIcon(TargetAWT.to(baseImage), 0);
    image.setIcon(TargetAWT.to(withText), 1, SwingConstants.SOUTH_EAST);
    return image;
  }

  @Override
  public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
    return new DesktopColorizeImageImpl(TargetAWT.to(baseImage), colorValue);
  }

  @Override
  public Image _ImageEffects_resize(Image original, int width, int height) {
    return new DesktopResizeImageImpl(TargetAWT.to(original), width, height);
  }

  @Override
  public MenuItem _MenuItem_create(String text) {
    return new DesktopMenuItemImpl(text);
  }

  @Override
  public Menu _Menu_create(String text) {
    return new DesktopMenuImpl(text);
  }

  @Override
  public MenuSeparator _MenuSeparator_create() {
    return DesktopMenuSeparatorImpl.INSTANCE;
  }

  @Override
  public ValueGroup<Boolean> _ValueGroups_boolGroup() {
    return new DesktopBoolValueGroup();
  }

  @Override
  public MenuBar _MenuItems_menuBar() {
    return new DesktopMenuBarImpl();
  }

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return DesktopStyleManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public FontManager _FontManager_get() {
    return DesktopFontManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public Window _Window_modalWindow(String title) {
    return new DesktopWindowWrapper(title);
  }

  @Nullable
  @Override
  public Window _Window_getActiveWindow() {
    Container window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    return (Window)TargetAWT.from((java.awt.Window)window);
  }

  @Nullable
  @Override
  public Window _Window_getFocusedWindow() {
    Container window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    return (Window)TargetAWT.from((java.awt.Window)window);
  }

  @Override
  public <T> Alert<T> _Alerts_create() {
    return new DesktopAlertImpl<>();
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
  public WrappedLayout _Layouts_wrapped() {
    return new DesktopWrappedLayoutImpl();
  }

  @Override
  public VerticalLayout _Layouts_vertical(int vGap) {
    return new DesktopVerticalLayoutImpl(vGap);
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    return new DesktopSwipeLayoutImpl();
  }

  @Override
  public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
    return new DesktopTwoComponentSplitLayoutImpl(position);
  }

  @Override
  public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
    return new DesktopThreeComponentSplitLayoutImpl(position);
  }

  @Override
  public TabbedLayout _Layouts_tabbed() {
    return new DesktopTabbedLayoutImpl();
  }

  @Override
  public LabeledLayout _Layouts_labeled(LocalizeValue label) {
    return new DesktopLabeledLayoutImpl(label);
  }

  @Override
  public TableLayout _Layouts_table(StaticPosition fillOption) {
    return new DesktopTableLayoutImpl(fillOption);
  }

  @Override
  public ScrollLayout _ScrollLayout_create(Component component) {
    return new DesktopScrollLayoutImpl(component);
  }

  @Override
  public Label _Components_label(LocalizeValue text) {
    return new DesktopLabelImpl(text);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(LocalizeValue html) {
    return new DesktopHtmlLabelImpl(html);
  }

  @Override
  public <E> ComboBox<E> _Components_comboBox(consulo.ui.model.ListModel<E> model) {
    return new DesktopComboBoxImpl<>(model);
  }

  @Override
  public TextBox _Components_textBox(String text) {
    return new DesktopTextBoxImpl(text);
  }

  @Override
  public ProgressBar _Components_progressBar() {
    return new DesktopProgressBarImpl();
  }

  @Nonnull
  @Override
  public IntBox _Components_intBox(int value) {
    return new DesktopIntBoxImpl(value);
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new DesktopListBoxImpl<>(model);
  }

  @Override
  public RadioButton _Components_radioButton(String text, boolean selected) {
    return new DesktopRadioButtonImpl(text, selected);
  }

  @Override
  public Button _Components_button(String text) {
    return new DesktopButtonImpl(text);
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    return null;
  }

  @Override
  public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return new DesktopHorizontalLayoutImpl(gapInPixesl);
  }

  @Override
  public ImageBox _Components_imageBox(Image image) {
    return new DesktopImageBoxImpl(image);
  }

  @Override
  public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
    return new DesktopColorBoxImpl(colorValue);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public UIAccess _UIAccess_get() {
    return AWTUIAccessImpl.ourInstance;
  }

  @Override
  public boolean _UIAccess_isUIThread() {
    return SwingUtilities.isEventDispatchThread();
  }

  @Override
  public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
    return new DesktopTextBoxWithHistoryImpl(text);
  }

  @Override
  public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
    return DesktopTextBoxWithExpandAction.create(editButtonImage, dialogTitle, parser, joiner);
  }

  @Override
  public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
    return DesktopTextBoxWithExtensions.create(text);
  }

  @Override
  public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
    return new DesktopFoldoutLayoutImpl(titleValue, component, show);
  }

  @Nonnull
  @Override
  public <S> Image _Image_stated(ImageState<S> state, Function<S, Image> funcCall) {
    return new DesktopStatedImageImpl<>(state, funcCall);
  }

  @Override
  public <V, E> TableColumn<V, E> _Components_tableColumBuild(String name, Function<E, V> converter) {
    return new DesktopTableColumnInfo<>(name, converter);
  }

  @Nonnull
  @Override
  public IconLibraryManager _IconLibraryManager_get() {
    return DesktopIconLibraryManagerImpl.ourInstance;
  }

  @Override
  public ImageKey _ImageKey_of(@Nonnull String groupId, @Nonnull String imageId, int width, int height) {
    return new DesktopImageKeyImpl(null, groupId, imageId, width, height);
  }
}
