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

import com.vaadin.ui.UI;
import consulo.annotations.Internal;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.internal.*;
import consulo.ui.internal.image.WGwtFoldedImageImpl;
import consulo.ui.internal.image.WGwtImageImpl;
import consulo.ui.internal.image.WGwtResizeImageImpl;
import consulo.ui.internal.image.WGwtTransparentImageImpl;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.model.ImmutableListModelImpl;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.MutableListModelImpl;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.StaticPosition;
import consulo.ui.style.StyleManager;
import consulo.ui.web.internal.VaadinUIAccessImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
@Internal
public class WebUIInternalImpl extends UIInternal {
  @Override
  public CheckBox _Components_checkBox() {
    return new WGwtCheckBoxImpl();
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new WGwtDockLayoutImpl();
  }

  @Override
  public WrappedLayout _Layouts_wrapped() {
    return new WGwtWrappedLayoutImpl();
  }

  @Override
  public VerticalLayout _Layouts_vertical() {
    return new WGwtVerticalLayoutImpl();
  }

  @Override
  public SwipeLayout _Layouts_swipe() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SplitLayout _Layouts_horizontalSplit() {
    return new WGwtHorizontalSplitLayoutImpl();
  }

  @Override
  public SplitLayout _Layouts_verticalSplit() {
    return new WGwtVerticalSplitLayoutImpl();
  }

  @Override
  public TabbedLayout _Layouts_tabbed() {
    return new WGwtTabbedLayoutImpl();
  }

  @Override
  public LabeledLayout _Layouts_labeled(String label) {
    return new WGwtLabeledLayoutImpl(label);
  }

  @Override
  public TableLayout _Layouts_table(StaticPosition fillOption) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Label _Components_label(String text) {
    return new WGwtLabelImpl(text);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(String html) {
    return new WGwtHtmlLabelImpl(html);
  }

  @Override
  public <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
    return new WGwtComboBoxImpl<>(model);
  }

  @Override
  public TextBox _Components_textBox(String text) {
    return new WGwtTextBoxImpl(text);
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new WGwtListBoxImpl<>(model);
  }

  @Override
  public RadioButton _Components_radioButton(String text, boolean selected) {
    return new WGwtRadioButtonImpl(selected, text);
  }

  @Override
  public Button _Components_button(String text) {
    return new WGwtButtonImpl(text, null);
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    return new WGwtHyperlinkImpl(text);
  }

  @Override
  public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
    return new WGwtHorizontalLayoutImpl(gapInPixesl);
  }

  @Override
  public ImageBox _Components_imageBox(Image image) {
    return new WGwtImageBoxImpl(image);
  }

  @Override
  public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    return new WGwtTreeImpl<>(rootValue, model);
  }

  @Override
  public Image _Image_fromUrl(URL url) {
    return new WGwtImageImpl(url);
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
    return new WGwtFoldedImageImpl(images);
  }

  @Override
  public Image _ImageEffects_transparent(@Nonnull Image original, float alpha) {
    return new WGwtTransparentImageImpl(original, alpha);
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
    throw new UnsupportedOperationException();
  }

  @Override
  public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
    return null;
  }

  @Override
  public Image _ImageEffects_resize(Image original, int width, int height) {
    return new WGwtResizeImageImpl(original, width, height);
  }

  @Override
  public MenuItem _MenuItem_create(String text) {
    return new WGwtMenuItemImpl(text);
  }

  @Override
  public Menu _Menu_create(String text) {
    return new WGwtMenuImpl(text);
  }

  @Override
  public MenuSeparator _MenuSeparator_create() {
    return new WGwtMenuSeparatorImpl();
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
      VaadinUIAccessImpl access = new VaadinUIAccessImpl(ui);
      ui.setData(access);
      return access;
    }
  }

  @Override
  public boolean _UIAccess_isUIThread() {
    return UI.getCurrent() != null;
  }
}
