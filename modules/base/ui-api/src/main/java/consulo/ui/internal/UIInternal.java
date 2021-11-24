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
package consulo.ui.internal;

import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.container.StartupError;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.ImageState;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.TableModel;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public abstract class UIInternal {
  private static UIInternal ourInstance = findImplementation(UIInternal.class);

  @Nonnull
  @ReviewAfterMigrationToJRE(value = 9, description = "Use consulo.container.plugin.util.PlatformServiceLocator#findImplementation after migration")
  private static <T, S> T findImplementation(@Nonnull Class<T> interfaceClass) {
    for (T value : ServiceLoader.load(interfaceClass, UIInternal.class.getClassLoader())) {
      return value;
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (PluginIds.isPlatformImplementationPlugin(descriptor.getPluginId())) {
        ServiceLoader<T> loader = ServiceLoader.load(interfaceClass, descriptor.getPluginClassLoader());

        Iterator<T> iterator = loader.iterator();
        if (iterator.hasNext()) {
          return iterator.next();
        }
      }
    }

    throw new StartupError("Can't find platform implementation: " + interfaceClass);
  }

  @Nonnull
  public static UIInternal get() {
    return ourInstance;
  }

  public abstract CheckBox _Components_checkBox();

  public abstract DockLayout _Layouts_dock();

  public abstract WrappedLayout _Layouts_wrapped();

  public abstract VerticalLayout _Layouts_vertical(int vGap);

  public abstract SwipeLayout _Layouts_swipe();

  public abstract TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position);

  public abstract ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position);

  public abstract TabbedLayout _Layouts_tabbed();

  public abstract LabeledLayout _Layouts_labeled(LocalizeValue label);

  public TableLayout _Layouts_table(StaticPosition fillOption) {
    throw new UnsupportedOperationException();
  }

  public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
    throw new UnsupportedOperationException();
  }

  public abstract HorizontalLayout _Layouts_horizontal(int gapInPixesl);

  public abstract Label _Components_label(LocalizeValue text, LabelOptions options);

  public abstract HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions labelOptions);

  public abstract <E> ComboBox<E> _Components_comboBox(ListModel<E> model);

  public abstract TextBox _Components_textBox(String text);

  public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
    throw new UnsupportedOperationException();
  }

  public abstract ProgressBar _Components_progressBar();

  @Nonnull
  public abstract IntBox _Components_intBox(int value);

  public IntSlider _Components_intSlider(int min, int max, int value) {
    throw new UnsupportedOperationException();
  }

  public abstract <E> ListBox<E> _Components_listBox(ListModel<E> model);

  @Nonnull
  public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
    throw new UnsupportedOperationException();
  }

  public Button _Components_button(LocalizeValue text) {
    throw new UnsupportedOperationException();
  }

  public abstract Hyperlink _Components_hyperlink(String text);

  public abstract ImageBox _Components_imageBox(Image image);

  public abstract ColorBox _Components_colorBox(@Nullable ColorValue colorValue);

  public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    throw new UnsupportedOperationException();
  }

  public abstract Image _Image_fromUrl(URL url) throws IOException;

  public Image _Image_fromBytes(Image.ImageType imageType, byte[] bytes, int width, int height) throws IOException {
    throw new UnsupportedOperationException();
  }

  public abstract Image _Image_lazy(Supplier<Image> imageSupplier);

  public abstract Image _ImageEffects_layered(@Nonnull Image[] images);

  public abstract Image _ImageEffects_transparent(@Nonnull Image original, float alpha);

  public abstract Image _ImageEffects_grayed(@Nonnull Image original);

  public abstract Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1);

  public abstract Image _ImageEffects_empty(int width, int height);

  public abstract Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer);

  public abstract Image _ImageEffects_withText(Image baseImage, String text);

  public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
    throw new UnsupportedOperationException();
  }

  public ImageKey _ImageKey_of(@Nonnull String groupId, @Nonnull String imageId, int width, int height) {
    throw new UnsupportedOperationException();
  }

  public abstract Image _ImageEffects_resize(Image original, int width, int height);

  public Image _ImageEffects_resize(Image original, float scale) {
    return original;
  }

  public abstract MenuItem _MenuItem_create(String text);

  public abstract Menu _Menu_create(String text);

  public abstract MenuSeparator _MenuSeparator_create();

  public abstract ValueGroup<Boolean> _ValueGroups_boolGroup();

  public abstract MenuBar _MenuItems_menuBar();

  @Nonnull
  public abstract StyleManager _StyleManager_get();

  @Nonnull
  public abstract FontManager _FontManager_get();

  @Nonnull
  public abstract Window _Window_create(String title, WindowOptions options);

  @Nullable
  public abstract Window _Window_getActiveWindow();

  @Nullable
  public Window _Window_getFocusedWindow() {
    return _Window_getActiveWindow();
  }

  public abstract <T> Alert<T> _Alerts_create();

  public abstract <T> ListModel<T> _ListModel_create(Collection<? extends T> list);

  public <T> TableModel<T> _TableModel_create(Collection<? extends T> list) {
    throw new UnsupportedOperationException();
  }

  public abstract <T> MutableListModel<T> _MutableListModel_create(Collection<? extends T> list);

  @RequiredUIAccess
  @Nonnull
  public abstract UIAccess _UIAccess_get();

  public abstract boolean _UIAccess_isUIThread();

  public abstract TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner);

  public abstract TextBoxWithExtensions _Components_textBoxWithExtensions(String text);

  public abstract FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show);

  @Nonnull
  public <S> Image _Image_stated(ImageState<S> state, Function<S, Image> funcCall) {
    throw new UnsupportedOperationException();
  }

  public <Value, Item> TableColumn<Value, Item> _Components_tableColumBuild(String name, Function<Item, Value> converter) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public IconLibraryManager _IconLibraryManager_get() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public TaskBar _TaskBar_get() {
    throw new UnsupportedOperationException();
  }

  public FocusManager _FocusManager_get() {
    throw new UnsupportedOperationException();
  }

  public <T> Table<T> _Table_create(@Nonnull Iterable<? extends TableColumn> columns, @Nonnull TableModel<T> model) {
    throw new UnsupportedOperationException();
  }

  public ToggleSwitch _Components_toggleSwitch(boolean selected) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public PasswordBox _Components_passwordBox(@Nullable String passwordText) {
    throw new UnsupportedOperationException();
  }

  public void _ShowNotifier_once(@Nonnull Component component, @RequiredUIAccess @Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public PopupMenu _PopupMenu_create(Component target) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public AdvancedLabel _Components_advancedLabel() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  public HtmlView _Components_htmlView() {
    throw new UnsupportedOperationException();
  }
}
