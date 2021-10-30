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
package consulo.ui.web.internal;

import com.vaadin.shared.Registration;
import com.vaadin.ui.UI;
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
import consulo.ui.model.TableModel;
import consulo.ui.style.StyleManager;
import consulo.ui.web.internal.image.*;
import consulo.ui.web.internal.layout.*;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;

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
 * @since 11-Jun-16
 */
public class WebUIInternalImpl extends UIInternal {
  @Override
  public CheckBox _Components_checkBox() {
    return new WebCheckBoxImpl();
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new WebDockLayoutImpl();
  }

  @Override
  public WrappedLayout _Layouts_wrapped() {
    return new WebWrappedLayoutImpl();
  }

  @Override
  public VerticalLayout _Layouts_vertical(int vGap) {
    return new WebVerticalLayoutImpl();
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    return new WebSwipeLayoutImpl();
  }

  @Override
  public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
    if (position == SplitLayoutPosition.HORIZONTAL) {
      return new WebHorizontalTwoComponentSplitLayoutImpl();
    }
    else if (position == SplitLayoutPosition.VERTICAL) {
      return new WebVerticalTwoComponentSplitLayoutImpl();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
    return new WebThreeComponentSplitLayoutImpl();
  }

  @Override
  public TabbedLayout _Layouts_tabbed() {
    return new WebTabbedLayoutImpl();
  }

  @Override
  public LabeledLayout _Layouts_labeled(LocalizeValue label) {
    return new WebLabeledLayoutImpl(label);
  }

  @Override
  public TableLayout _Layouts_table(StaticPosition fillOption) {
    return new WebTableLayoutImpl(fillOption);
  }

  @Override
  public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
    return new WebScrollLayoutImpl(component);
  }

  @Override
  public Label _Components_label(LocalizeValue text, LabelOptions options) {
    return new WebLabelImpl(text, options);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions labelOptions) {
    return new WebHtmlLabelImpl(html, labelOptions);
  }

  @Override
  public <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return new WebComboBoxImpl<>(model);
  }

  @Override
  public TextBox _Components_textBox(String text) {
    return new WebTextBoxImpl(text);
  }

  @Override
  public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
    return new WebTextBoxWithHistoryImpl(text);
  }

  @Override
  public ProgressBar _Components_progressBar() {
    return new WebProgressBarImpl();
  }

  @Nonnull
  @Override
  public IntBox _Components_intBox(int value) {
    return new WebIntBoxImpl(value);
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new WebListBoxImpl<>(model);
  }

  @Nonnull
  @Override
  public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
    return new WebRadioButtonImpl(selected, text);
  }

  @Override
  public Button _Components_button(LocalizeValue text) {
    return new WebButtonImpl(text.get());
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    WebHyperlinkImpl hyperlink = new WebHyperlinkImpl();
    hyperlink.setText(text);
    return hyperlink;
  }

  @Override
  public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return new WebHorizontalLayoutImpl();
  }

  @Override
  public ImageBox _Components_imageBox(Image image) {
    return new WebImageBoxImpl(image);
  }

  @Override
  public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    return new WebTreeImpl<>(rootValue, model);
  }

  @Override
  public Image _Image_fromUrl(URL url) throws IOException {
    return new WebImageImpl(url);
  }

  @Override
  public Image _Image_fromBytes(Image.ImageType imageType, byte[] bytes, int width, int height) throws IOException {
    return null;
  }

  @Override
  public Image _Image_lazy(Supplier<Image> imageSupplier) {
    return imageSupplier.get();
  }

  @Override
  public Image _ImageEffects_layered(Image[] images) {
    return new WebLayeredImageImpl(images);
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return new WebTransparentImageImpl(original, alpha);
  }

  @Override
  public Image _ImageEffects_grayed(@Nonnull Image original) {
    return original;
  }

  @Override
  public Image _ImageEffects_appendRight(@Nonnull Image i0, @Nonnull Image i1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Image _ImageEffects_empty(int width, int height) {
    return new WebEmptyImageImpl(width, height);
  }

  @Override
  public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
    return new WebCanvasImageImpl(width, height, consumer);
  }

  @Override
  public Image _ImageEffects_withText(Image baseImage, String text) {
    return baseImage;
  }

  @Override
  public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
    return baseImage;
  }

  @Override
  public Image _ImageEffects_resize(Image original, int width, int height) {
    return new WebResizeImageImpl(original, width, height);
  }

  @Override
  public MenuItem _MenuItem_create(String text) {
    return new WebMenuItemImpl(text);
  }

  @Override
  public Menu _Menu_create(String text) {
    return new WebMenuImpl(text);
  }

  @Override
  public MenuSeparator _MenuSeparator_create() {
    return new WebMenuSeparatorImpl();
  }

  @Override
  public ValueGroup<Boolean> _ValueGroups_boolGroup() {
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
  public MenuBar _MenuItems_menuBar() {
    return new WebMenuBarImpl();
  }

  @Nonnull
  @Override
  public StyleManager _StyleManager_get() {
    return WebStyleManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public FontManager _FontManager_get() {
    return WebFontManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public Window _Window_create(String title, WindowOptions options) {
    WebWindowImpl window = new WebWindowImpl(true, options);
    window.setTitle(title);
    return window;
  }

  @Nullable
  @Override
  public Window _Window_getActiveWindow() {
    return null;
  }

  @Nullable
  @Override
  public Window _Window_getFocusedWindow() {
    return null;
  }

  @Override
  public <T> Alert<T> _Alerts_create() {
    throw new UnsupportedOperationException();
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
    UI ui = UI.getCurrent();
    assert ui != null;
    Object data = ui.getData();
    if (data != null) {
      return (UIAccess)data;
    }
    else {
      WebUIAccessImpl access = new WebUIAccessImpl(ui);
      ui.setData(access);
      return access;
    }
  }

  @Override
  public boolean _UIAccess_isUIThread() {
    return UI.getCurrent() != null;
  }

  @Nonnull
  @Override
  public IconLibraryManager _IconLibraryManager_get() {
    return WebIconLibraryManagerImpl.ourInstance;
  }

  @Override
  public ImageKey _ImageKey_of(@Nonnull String groupId, @Nonnull String imageId, int width, int height) {
    return new WebImageKeyImpl(groupId, imageId, width, height);
  }

  @Override
  public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
    return new WebTextBoxWithExtensionsImpl(text);
  }

  @Override
  public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
    return new WebFoldoutLayoutImpl();
  }

  @Override
  public ToggleSwitch _Components_toggleSwitch(boolean selected) {
    return new WebToggleSwitchImpl(selected);
  }

  @Nonnull
  @Override
  public PasswordBox _Components_passwordBox(@Nullable String passwordText) {
    return new WebPasswordBoxImpl(StringUtil.notNullize(passwordText));
  }

  @Override
  public <T> Table<T> _Table_create(@Nonnull Iterable<? extends TableColumn> columns, @Nonnull TableModel<T> model) {
    return new WebTableImpl<>();
  }

  @Override
  public IntSlider _Components_intSlider(int min, int max, int value) {
    return new WebIntSliderImpl(min, max, value);
  }

  @Override
  public <Value, Item> TableColumn<Value, Item> _Components_tableColumBuild(String name, Function<Item, Value> converter) {
    return new WebTableColumn<>(name, converter);
  }

  @Override
  public <T> TableModel<T> _TableModel_create(Collection<? extends T> list) {
    return new WebTableModel<>(list);
  }

  @Override
  public FocusManager _FocusManager_get() {
    return WebFocusManagerImpl.ourInstance;
  }

  @Nonnull
  @Override
  public PopupMenu _PopupMenu_create(Component target) {
    return new WebPopupMenuImpl(target);
  }

  @Override
  public void _ShowNotifier_once(@Nonnull Component component, @Nonnull Runnable action) {
    // TODO [VISTALL] logic for this notifier is not fully correct. Run only on first attach to parent, npt vo
    com.vaadin.ui.Component vaadinComponent = TargetVaddin.to(component);

    SimpleReference<Registration> ref = SimpleReference.create();

    Registration registration = vaadinComponent.addAttachListener(attachEvent -> {
      UIAccess uiAccess = UIAccess.current();

      uiAccess.give(() -> {
        ref.get().remove();
        
        action.run();
      });
    });
    ref.set(registration);

    action.run();
  }
}
