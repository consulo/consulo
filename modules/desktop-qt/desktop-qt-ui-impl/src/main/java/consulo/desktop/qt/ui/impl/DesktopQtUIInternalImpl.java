/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.ui.impl;

import consulo.application.impl.internal.ModalityStateImpl;
import consulo.desktop.qt.ui.impl.base.DesktopQtShowNotifier;
import consulo.desktop.qt.ui.impl.font.DesktopQtFontManagerImpl;
import consulo.desktop.qt.ui.impl.htmlView.DesktopQtHtmlViewImpl;
import consulo.desktop.qt.ui.impl.image.*;
import consulo.desktop.qt.ui.impl.layout.*;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.EmptyImage;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.ImageState;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.DummyTaskBarImpl;
import consulo.ui.impl.model.FlatDataModelImpl;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StyleManager;
import consulo.ui.ex.impl.internal.UnifiedAlertImpl;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import io.qt.core.Qt;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtUIInternalImpl extends UIInternal {
    private final TaskBar myTaskBar = new DummyTaskBarImpl();

    @Override
    public TaskBar _TaskBar_get() {
        return myTaskBar;
    }

    @Override
    public IconLibraryManager _IconLibraryManager_get() {
        return DesktopQtIconLibraryManager.INSTANCE;
    }

    @Override
    public ImageKey _ImageKey_of(String groupId, String imageId, int width, int height) {
        return new DesktopQtImageKeyImpl(groupId, imageId, width, height);
    }

    @Override
    public CheckBox _Components_checkBox() {
        return new DesktopQtCheckBoxImpl();
    }

    @Override
    public TriStateCheckBox _Components_triStateCheckBox() {
        return new DesktopQtTriStateCheckBoxImpl();
    }

    @Override
    public ToggleSwitch _Components_toggleSwitch(boolean selected) {
        return new DesktopQtToggleSwitchImpl(selected);
    }

    @Override
    public IntSlider _Components_intSlider(int min, int max, int value) {
        return new DesktopQtIntSliderImpl(min, max, value);
    }

    @Override
    public AdvancedLabel _Components_advancedLabel() {
        return new DesktopQtAdvancedLabelImpl();
    }

    @Override
    public DatePicker _Components_datePicker(@Nullable String datePattern) {
        return new DesktopQtDatePickerImpl(datePattern);
    }

    @Override
    public ColorPickerBuilder _ColorPicker_create() {
        return new DesktopQtColorPickerBuilderImpl();
    }

    @Override
    public <T> Table<T> _Table_create(FlatDataModel<T> model) {
        return new DesktopQtTableImpl<>(model);
    }

    @Override
    public DockLayout _Layouts_dock(int gapInPixels) {
        return new DesktopQtDockLayoutImpl(gapInPixels);
    }

    @Override
    public WrappedLayout _Layouts_wrapped() {
        return new DesktopQtWrappedLayoutImpl();
    }

    @Override
    public VerticalLayout _Layouts_vertical(int vGap) {
        return new DesktopQtVerticalLayoutImpl(vGap);
    }

    @Override
    public VerticalLayout _Layouts_vertical(int vGap, HorizontalAlignment alignment) {
        return new DesktopQtVerticalLayoutImpl(vGap, alignment);
    }

    @Override
    public SwipeLayout _Layouts_swipe() {
        return new DesktopQtSwipeLayoutImpl();
    }

    @Override
    public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
        return new DesktopQtTwoComponentSplitLayoutImpl(position);
    }

    @Override
    public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
        return new DesktopQtThreeComponentSplitLayoutImpl(position);
    }

    @Override
    public TabbedLayout _Layouts_tabbed() {
        return new DesktopQtTabbedLayoutImpl();
    }

    @Override
    public LabeledLayout _Layouts_labeled(LocalizeValue label) {
        return new DesktopQtLabeledLayoutImpl(label);
    }

    @Override
    public TableLayout _Layouts_table(StaticPosition fillOption) {
        return new DesktopQtTableLayoutImpl(fillOption);
    }

    @Override
    public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
        return new DesktopQtScrollableLayoutImpl(component);
    }

    @Override
    public <L extends Layout> LoadingLayout<L> _Layouts_LoadingLayout(L innerLayout, Disposable parent) {
        return new DesktopQtLoadingLayoutImpl<>(innerLayout, parent);
    }

    @Override
    public HorizontalLayout _Layouts_horizontal(int gapInPixels) {
        return new DesktopQtHorizontalLayoutImpl(gapInPixels);
    }

    @Override
    public Label _Components_label(LocalizeValue text, LabelOptions options) {
        return new DesktopQtLabelImpl(text);
    }

    @Override
    public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions options) {
        return new DesktopQtHtmlLabelImpl(html);
    }

    @Override
    public HtmlView _Components_htmlView() {
        return new DesktopQtHtmlViewImpl();
    }

    @Override
    public <E> ComboBox<E> _Components_comboBox(FlatDataModel<E> model) {
        return new DesktopQtComboBoxImpl<>(model);
    }

    @Override
    public TextBox _Components_textBox(String text) {
        return new DesktopQtTextBoxImpl(text);
    }

    @Override
    public PasswordBox _Components_passwordBox(String password) {
        return new DesktopQtPasswordBoxImpl(password);
    }

    @Override
    public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
        return new DesktopQtTextBoxWithHistoryImpl(text);
    }

    @Override
    public ProgressBar _Components_progressBar() {
        return new DesktopQtProgressBarImpl();
    }

    @Override
    @RequiredUIAccess
    public DelayedAction _DelayedAction_start(ComponentEvent<?> anchor) {
        InputDetails details = anchor.getInputDetails();

        QWidget host = new QWidget(null, Qt.WindowType.ToolTip, Qt.WindowType.FramelessWindowHint, Qt.WindowType.WindowStaysOnTopHint);
        host.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating, true);
        host.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, true);

        DesktopQtProgressBarImpl progressBar = new DesktopQtProgressBarImpl();
        progressBar.setIndeterminate(true);
        progressBar.addStyle(ProgressBarStyle.SPINNER);
        progressBar.bind(host, null);

        host.adjustSize();
        host.move(details.getXOnScreen() - host.width() / 2, details.getYOnScreen() - host.height() / 2);
        host.show();

        return () -> {
            host.close();
            host.disposeLater();
        };
    }

    @Override
    public IntBox _Components_intBox(int value) {
        return new DesktopQtIntBoxImpl(value);
    }

    @Override
    public <E> ListBox<E> _Components_listBox(FlatDataModel<E> model) {
        return new DesktopQtListBoxImpl<>(model);
    }

    @Override
    public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
        return new DesktopQtRadioButtonImpl(text, selected);
    }

    @Override
    public Button _Components_button(LocalizeValue text) {
        return new DesktopQtButtonImpl(text);
    }

    @Override
    public ToggleButton _Components_toggleButton(LocalizeValue text) {
        return new DesktopQtToggleButtonImpl(text);
    }

    @Override
    public Hyperlink _Components_hyperlink(LocalizeValue text) {
        return new DesktopQtHyperlinkImpl(text);
    }

    @Override
    public ImageBox _Components_imageBox(Image image) {
        return new DesktopQtImageBoxImpl(image);
    }

    @Override
    public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
        return new DesktopQtColorBoxImpl(colorValue);
    }

    @Override
    public Image _Image_fromUrl(URL url) throws IOException {
        return DesktopQtBytesImageImpl.fromUrl(url);
    }

    /**
     * Without this a plugin icon - the only image the platform hands over as bytes rather than by an id - threw
     * out of {@link Image#fromStream}, and every plugin was drawn with the stand-in icon of the caller.
     */
    @Override
    public Image _Image_fromStream(Image.ImageType imageType, InputStream stream) throws IOException {
        return new DesktopQtBytesImageImpl(imageType, stream.readAllBytes());
    }

    @Override
    public Image _Image_lazy(Supplier<Image> imageSupplier) {
        return new DesktopQtLazyImageImpl(imageSupplier);
    }

    @Override
    public <S> Image _Image_stated(ImageState<S> state, Function<S, Image> funcCall) {
        return new DesktopQtStatedImageImpl<>(state, funcCall);
    }

    @Override
    public Image _ImageEffects_layered(Image[] images) {
        return new DesktopQtLayeredImageImpl(images);
    }

    @Override
    public Image _ImageEffects_transparent(Image original, float alpha) {
        return new DesktopQtTransparentImageImpl(original, alpha);
    }

    @Override
    public Image _ImageEffects_grayed(Image original) {
        if (original instanceof DesktopQtGrayedImageImpl) {
            return original;
        }
        return new DesktopQtGrayedImageImpl(original);
    }

    @Override
    public Image _ImageEffects_appendRight(Image i0, Image i1) {
        return new DesktopQtAppendImageImpl(i0, i1);
    }

    @Override
    public EmptyImage _ImageEffects_empty(int width, int height) {
        return new DesktopQtEmptyImageImpl(width, height);
    }

    @Override
    public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
        return new DesktopQtCanvasImageImpl(width, height, consumer);
    }

    @Override
    public Image _ImageEffects_withText(Image baseImage, String text) {
        return new DesktopQtTextImageImpl(baseImage, text);
    }

    @Override
    public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
        return new DesktopQtColorizeImageImpl(baseImage, colorValue);
    }

    @Override
    public Image _ImageEffects_resize(Image original, int width, int height) {
        return new DesktopQtResizeImageImpl(original, width, height);
    }

    @Override
    public Menu _Menu_create(LocalizeValue text) {
        return new DesktopQtMenuImpl(text);
    }

    @Override
    public PopupMenu _PopupMenu_create(Component target) {
        return new DesktopQtPopupMenuImpl(target);
    }

    @Override
    public MenuItem _MenuItem_create(LocalizeValue text) {
        return new DesktopQtMenuItemImpl(text);
    }

    @Override
    public MenuSeparator _MenuSeparator_create() {
        return new DesktopQtMenuSeparatorImpl();
    }

    @Override
    public Separator _Separator_create(SeparatorStyle style) {
        return new DesktopQtSeparatorImpl(style);
    }

    @Override
    public ValueGroup<Boolean> _ValueGroups_boolGroup() {
        return new DesktopQtBoolValueGroup();
    }

    @Override
    public MenuBar _MenuItems_menuBar() {
        return new DesktopQtMenuBar();
    }

    @Override
    public StyleManager _StyleManager_get() {
        return DesktopQtStyleManagerImpl.INSTANCE;
    }

    @Override
    public FontManager _FontManager_get() {
        return DesktopQtFontManagerImpl.INSTANCE;
    }

    @Override
    public FocusManager _FocusManager_get() {
        return DesktopQtFocusManagerImpl.INSTANCE;
    }

    @Override
    public Window _Window_create(String title, WindowOptions options) {
        return new DesktopQtWindowImpl(title, options);
    }

    @Override
    @RequiredUIAccess
    public LightPopup _LightPopup_create(PopupOptions options) {
        return new DesktopQtLightPopupImpl(options);
    }

    @Override
    @RequiredUIAccess
    public HeavyPopup _HeavyPopup_create(PopupOptions options) {
        return new DesktopQtHeavyPopupImpl(options);
    }

    @Override
    public @Nullable Window _Window_getActiveWindow() {
        QWidget activeWindow = QApplication.activeWindow();
        if (activeWindow != null && TargetQt.from(activeWindow) instanceof Window window) {
            return window;
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public UIAccess _UIAccess_get() {
        return DesktopQtUIAccess.INSTANCE;
    }

    @Override
    public Collection<UIAccess> _UIAccess_all() {
        return List.of(DesktopQtUIAccess.INSTANCE);
    }

    @Override
    public boolean _UIAccess_isUIThread() {
        return DesktopQtUIAccess.INSTANCE.isUIThread();
    }

    @Override
    public TextBoxWithExpandAction _Components_textBoxWithExpandAction(
        Image editButtonImage,
        String dialogTitle,
        Function<String, List<String>> parser,
        Function<List<String>, String> joiner
    ) {
        return new DesktopQtTextBoxWithExpandActionImpl(editButtonImage, dialogTitle, parser, joiner);
    }

    @Override
    public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
        return new DesktopQtTextBoxWithExtensionsImpl(text);
    }

    @Override
    public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
        return new DesktopQtFoldoutLayoutImpl(titleValue, component, show);
    }

    @Override
    public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return new DesktopQtTreeImpl<>(rootValue, model, executor);
    }

    @Override
    public void _ShowNotifier_once(Component component, Runnable action) {
        DesktopQtShowNotifier.once(component, action);
    }

    @Override
    public ModalityState _ModalityState_any() {
        return ModalityStateImpl.ANY;
    }

    @Override
    public ModalityState _ModalityState_nonModal() {
        return ModalityStateImpl.NON_MODAL;
    }

    @Override
    public <T> Alert<T> _Alerts_create() {
        return new UnifiedAlertImpl<>();
    }

    @Override
    public <T> MutableFlatDataModel<T> _FlatDataModel_create(Collection<? extends T> list) {
        return new FlatDataModelImpl<>(list);
    }
}
