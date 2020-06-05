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

import com.vaadin.ui.UI;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.model.ImmutableListModelImpl;
import consulo.ui.impl.model.MutableListModelImpl;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.StaticPosition;
import consulo.ui.style.StyleManager;
import consulo.ui.web.internal.image.*;
import consulo.ui.web.internal.layout.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  public VerticalLayout _Layouts_vertical() {
    return new WebVerticalLayoutImpl();
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
    if(position == SplitLayoutPosition.HORIZONTAL) {
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
    throw new UnsupportedOperationException();
  }

  @Override
  public ScrollLayout _ScrollLayout_create(Component component) {
    return new WebScrollLayoutImpl(component);
  }

  @Override
  public Label _Components_label(String text) {
    return new WebLabelImpl(text);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(String html) {
    return new WebHtmlLabelImpl(html);
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

  @Override
  public RadioButton _Components_radioButton(String text, boolean selected) {
    return new WebRadioButtonImpl(selected, text);
  }

  @Override
  public Button _Components_button(String text) {
    return new WebButtonImpl(text);
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
  public Image _Image_fromUrl(URL url) {
    return new WebImageImpl(url);
  }

  @Override
  public Image _Image_fromBytes(byte[] bytes, int width, int height) {
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
    throw new UnsupportedOperationException();
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
    throw new UnsupportedOperationException();
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
  public Window _Window_modalWindow(String title) {
    WebWindowImpl window = new WebWindowImpl(true);
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
    if(data != null) {
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

  @Override
  public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage, String dialogTitle, Function<String, List<String>> parser, Function<List<String>, String> joiner) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
    throw new UnsupportedOperationException();
  }
}
