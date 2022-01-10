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
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.EDT;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.Menu;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.desktop.internal.alert.DesktopAlertFactory;
import consulo.ui.desktop.internal.image.*;
import consulo.ui.desktop.internal.image.libraryImage.DesktopAWTImageImpl;
import consulo.ui.desktop.internal.image.libraryImage.DesktopImageKeyImpl;
import consulo.ui.desktop.internal.image.libraryImage.DesktopLibraryInnerImage;
import consulo.ui.desktop.internal.image.libraryImage.DesktopSvgImageImpl;
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
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.model.TableModel;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
  public Image _Image_fromUrl(URL url) throws IOException {

    if (url.toString().endsWith(".svg")) {
      SVGUniverse svgUniverse = new SVGUniverse();

      try {
        SVGDiagram diagram = svgUniverse.getDiagram(url.toURI());
        return new DesktopSvgImageImpl(diagram, null, (int)diagram.getWidth(), (int)diagram.getHeight(), null, null, null);
      }
      catch (URISyntaxException e) {
        throw new IOException(e);
      }
    }
    else {
      BufferedImage image;
      try (InputStream stream = url.openStream()) {
        image = ImageIO.read(stream);
      }

      return new DesktopAWTImageImpl(new DesktopAWTImageImpl.ImageBytes(null, image), null, image.getWidth(), image.getHeight(), null);
    }
  }

  @Override
  public Image _Image_fromBytes(Image.ImageType imageType, byte[] bytes, int width, int height) throws IOException {
    switch (imageType) {
      case SVG:
        SVGUniverse svgUniverse = new SVGUniverse();

        URI uri = svgUniverse.loadSVG(new UnsyncByteArrayInputStream(bytes), "dummy" + System.currentTimeMillis() + ".svg");

        SVGDiagram diagram = svgUniverse.getDiagram(uri, false);

        if (diagram == null) {
          throw new IOException("Wrong svg bytes");
        }

        // dirty hack due we can't set different scale for width + height
        float scale = (float)(height / diagram.getViewRect().getHeight());
        return new DesktopSvgImageImpl(diagram, null, (int)diagram.getViewRect().getWidth(), (int)diagram.getViewRect().getHeight(), scale, null, null, null);
      default:
        BufferedImage image = ImageIO.read(new UnsyncByteArrayInputStream(bytes));
        return new DesktopAWTImageImpl(new DesktopAWTImageImpl.ImageBytes(null, image), null, width, height, null);
    }
  }

  @Override
  public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model) {
    return new DesktopTreeImpl<>(rootValue, model);
  }

  @Override
  public Image _Image_lazy(Supplier<Image> imageSupplier) {
    return new DesktopLazyImageImpl(imageSupplier);
  }

  @Override
  public Image _ImageEffects_layered(@Nonnull Image[] images) {
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
    DesktopHeavyLayeredImageImpl image = new DesktopHeavyLayeredImageImpl(2);
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
    float scale = height / (float)original.getHeight();
    return _ImageEffects_resize(original, scale);
  }

  @Override
  public Image _ImageEffects_resize(Image original, float scale) {
    if (original instanceof DesktopImage) {
      return ((DesktopImage)original).copyWithScale(scale);
    }

    if (original instanceof DesktopLibraryInnerImage) {
      return ((DesktopLibraryInnerImage)original).copyWithScale(scale);
    }
    return original;
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
  public Window _Window_create(String title, WindowOptions options) {
    return new DesktopWindowWrapper(title, options);
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
    return DesktopAlertFactory.create();
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
  public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
    return new DesktopScrollableLayoutImpl(component, options);
  }

  @Override
  public Label _Components_label(LocalizeValue text, LabelOptions options) {
    return new DesktopLabelImpl(text, options);
  }

  @Override
  public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions options) {
    return new DesktopHtmlLabelImpl(html, options);
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
  public IntSlider _Components_intSlider(int min, int max, int value) {
    return new DesktopIntSliderImpl(min, max, value);
  }

  @Override
  public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
    return new DesktopListBoxImpl<>(model);
  }

  @Override
  public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
    return new DesktopRadioButtonImpl(text, selected);
  }

  @Override
  public Button _Components_button(LocalizeValue text) {
    return new DesktopButtonImpl(text);
  }

  @Override
  public Hyperlink _Components_hyperlink(String text) {
    return new DesktopHyperlinkImpl(text);
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
    return EDT.isCurrentThreadEdt();
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

  @Nonnull
  @Override
  public TaskBar _TaskBar_get() {
    return DesktopTaskBarImpl.ourInstance;
  }

  @Override
  public FocusManager _FocusManager_get() {
    return DesktopFocusManager.ourInstance;
  }

  @Override
  public <T> Table<T> _Table_create(@Nonnull Iterable<? extends TableColumn> columns, @Nonnull TableModel<T> model) {
    return new DesktopTableImpl<>(columns, model);
  }

  @Override
  public <T> TableModel<T> _TableModel_create(Collection<? extends T> list) {
    return new DesktopTableModelImpl<>(list);
  }

  @Override
  public ToggleSwitch _Components_toggleSwitch(boolean selected) {
    return new DesktopToggleSwitchImpl(selected);
  }

  @Nonnull
  @Override
  public PasswordBox _Components_passwordBox(@Nullable String passwordText) {
    return new DesktopPasswordBoxImpl(passwordText);
  }

  @Override
  public void _ShowNotifier_once(@Nonnull Component component, @Nonnull Runnable action) {
    java.awt.Component awtComponent = TargetAWT.to(component);

    UiNotifyConnector.doWhenFirstShown(awtComponent, action);
  }

  @Nonnull
  @Override
  public AdvancedLabel _Components_advancedLabel() {
    return new DesktopAdvancedLabelImpl();
  }

  @Nonnull
  @Override
  public HtmlView _Components_htmlView() {
    return new DesktopHtmlViewImpl();
  }
}
