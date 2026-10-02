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
package consulo.desktop.awt.ui.impl;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import consulo.ui.RadioGroup;
import consulo.ui.graph.Graph;
import consulo.ui.graph.GraphModel;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.impl.internal.ModalityStateImpl;
import consulo.desktop.awt.ui.impl.alert.DesktopAlertFactory;
import consulo.desktop.awt.ui.impl.graph.DesktopAWTGraphImpl;
import consulo.desktop.awt.ui.impl.image.viewer.DesktopAWTImageViewerImpl;
import consulo.ui.ex.awt.internal.image.ImageIODecoder;
import consulo.ui.image.viewer.ImageViewer;
import consulo.desktop.awt.ui.impl.htmlView.DesktopAWTHtmlViewImpl;
import consulo.desktop.awt.ui.impl.image.*;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTImageKey;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTPNGImageReference;
import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTSVGImageReference;
import consulo.desktop.awt.ui.impl.layout.*;
import consulo.desktop.awt.ui.impl.style.DesktopAWTStyleManagerImpl;
import consulo.desktop.awt.ui.impl.textBox.*;
import consulo.desktop.awt.ui.impl.image.DesktopDeferredIconImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.Menu;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.Window;
import consulo.desktop.awt.ui.impl.color.DesktopAWTColorPickerBuilder;
import consulo.ui.*;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ModalityStateListener;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.internal.EDT;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.*;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.model.FlatDataModelImpl;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import consulo.desktop.awt.ui.impl.messagebox.DesktopMessageBoxBuilderImpl;
import consulo.desktop.awt.ui.impl.messagebox.DesktopInputBoxBuilderImpl;
import java.util.ArrayList;

/**
 * @author VISTALL
 * @since 2016-06-09
 */
public class DesktopUIInternalImpl extends UIInternal implements UIInternalEx {
    @Override
    public void addModalityStateListener(ModalityStateListener listener, Disposable parentDisposable) {
        LaterInvocator.addModalityStateListener(listener, parentDisposable);
    }

    @Override
    public Image _Image_fromUrl(URL url) throws IOException {
        Image.ImageType imageType = Image.ImageType.fromFileName(url.getPath());
        if (imageType.vector()) {
            return fromSvg(new SVGLoader().load(url), "url", url.toString());
        }

        try (InputStream stream = url.openStream()) {
            return fromRaster(imageType, stream.readAllBytes());
        }
    }

    @Override
    public Image _Image_fromStream(Image.ImageType imageType, InputStream stream) throws IOException {
        if (imageType.vector()) {
            return fromSvg(new SVGLoader().load(stream), "bytes", "[]");
        }
        return fromRaster(imageType, stream.readAllBytes());
    }

    private static Image fromSvg(@Nullable SVGDocument document, String groupId, String imageId) throws IOException {
        if (document == null) {
            throw new IOException("Not a svg document: " + imageId);
        }

        FloatSize size = document.size();
        return new DesktopAWTSimpleImageImpl(
            new DesktopAWTSVGImageReference(groupId, imageId, document, null),
            (int) size.getWidth(),
            (int) size.getHeight()
        );
    }

    private static Image fromRaster(Image.ImageType imageType, byte[] bytes) throws IOException {
        // the type is a hint only - the readers look at the bytes, and those of plugins are asked as well
        BufferedImage image = ImageIODecoder.read(bytes);
        if (image == null) {
            throw new IOException("Unable to read " + imageType.id() + " image of " + bytes.length + " bytes");
        }

        return new DesktopAWTSimpleImageImpl(
            new DesktopAWTPNGImageReference(new DesktopAWTPNGImageReference.ImageBytes(null, image), null),
            image.getWidth(),
            image.getHeight()
        );
    }

    @Override
    public <E> Tree<E> _Components_tree(E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return new DesktopTreeImpl<>(rootValue, model, executor);
    }

    @Override
    public Image _Image_lazy(Supplier<Image> imageSupplier) {
        return new DesktopLazyImageImpl(imageSupplier);
    }

    @Override
    public Image _Image_busy(int widthAndHeight) {
        return DesktopBusyImageImpl.of(widthAndHeight, widthAndHeight);
    }

    @Override
    public Image _ImageEffects_layered(Image[] images) {
        return new DesktopLayeredImageImpl(images);
    }

    @Override
    public Image _ImageEffects_transparent(Image original, float alpha) {
        return new DesktopTransparentImageImpl(original, alpha);
    }

    @Override
    public Image _ImageEffects_grayed(Image original) {
        return DesktopAWTImage.copyGrayed(original);
    }

    @Override
    public Image _ImageEffects_blinking(Image original) {
        return DesktopBlinkingImageImpl.of(original);
    }

    @Override
    public Image _ImageEffects_appendRight(Image i0, Image i1) {
        return new DesktopAppendImageImpl(i0, i1);
    }

    @Override
    public EmptyImage _ImageEffects_empty(int width, int height) {
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
        image.setImage(baseImage, 0);
        image.setImage(withText, 1, SwingConstants.SOUTH_EAST);
        return image;
    }

    @Override
    public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
        return new DesktopColorizeImageImpl(TargetAWT.to(baseImage), colorValue);
    }

    @Override
    public Image _ImageEffects_resize(Image original, int width, int height) {
        if (original instanceof DesktopAWTImage resizableImage) {
            return resizableImage.copyWithNewSize(width, height);
        }

        if (original instanceof DesktopDeferredIconImpl deferredIcon) {
            Image image = deferredIcon.evaluateImage();
            return _ImageEffects_resize(image, width, height);
        }
        return original;
    }

    @Override
    public Image _ImageEffects_resize(Image original, float scale) {
        if (original instanceof DesktopAWTImage resizableImage) {
            return resizableImage.copyWithNewScale(scale);
        }

        if (original instanceof DesktopDeferredIconImpl deferredIcon) {
            Image image = deferredIcon.evaluateImage();
            return _ImageEffects_resize(image, scale);
        }
        return original;
    }

    @Override
    public MenuItem _MenuItem_create(LocalizeValue text) {
        return new DesktopMenuItemImpl(text);
    }

    @Override
    public Menu _Menu_create(LocalizeValue text) {
        return new DesktopMenuImpl(text);
    }

    @Override
    public MenuSeparator _MenuSeparator_create() {
        return DesktopMenuSeparatorImpl.INSTANCE;
    }


    @Override
    public MenuBar _MenuItems_menuBar() {
        return new DesktopMenuBarImpl();
    }

    @Override
    public StyleManager _StyleManager_get() {
        return DesktopAWTStyleManagerImpl.INSTANCE;
    }

    @Override
    public FontManager _FontManager_get() {
        return DesktopFontManagerImpl.ourInstance;
    }

    @Override
    public Window _Window_create(String title, WindowOptions options) {
        return new DesktopWindowWrapper(title, options);
    }

    @Override
    public @Nullable Window _Window_getActiveWindow() {
        Container window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return (Window) TargetAWT.from((java.awt.Window) window);
    }

    @Override
    public @Nullable Window _Window_getFocusedWindow() {
        Container window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        return (Window) TargetAWT.from((java.awt.Window) window);
    }
    @Override
    public <T> Alert<T> _Alerts_create() {
        return DesktopAlertFactory.create();
    }

    @Override
    public <T> MutableFlatDataModel<T> _FlatDataModel_create(Collection<? extends T> list) {
        return new FlatDataModelImpl<>(list);
    }

    @Override
    public CheckBox _Components_checkBox() {
        return new DesktopCheckBoxImpl();
    }

    @Override
    public TriStateCheckBox _Components_triStateCheckBox() {
        return new DesktopTriStateCheckBoxImpl();
    }

    @Override
    public DockLayout _Layouts_dock(Space gapInPixels) {
        return new DesktopDockLayoutImpl(gapInPixels);
    }

    @Override
    public WrappedLayout _Layouts_wrapped() {
        return new DesktopWrappedLayoutImpl();
    }

    @Override
    public VerticalLayout _Layouts_vertical(Space vGap) {
        return new DesktopVerticalLayoutImpl(vGap);
    }

    @Override
    public VerticalLayout _Layouts_vertical(Space vGap, HorizontalAlignment alignment) {
        return new DesktopVerticalLayoutImpl(vGap, alignment);
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
    public <E> ComboBox<E> _Components_comboBox(consulo.ui.model.FlatDataModel<E> model) {
        return new DesktopComboBoxImpl<>(model);
    }

    @Override
    public <E> MultiSelectComboBox<E> _Components_multiSelectComboBox(FlatDataModel<E> model) {
        return new DesktopMultiSelectComboBoxImpl<>(model);
    }

    @Override
    public TextBox _Components_textBox(String text) {
        return new DesktopTextBoxImpl(text);
    }

    @Override
    public TextArea _Components_textArea(String text) {
        return new DesktopTextAreaImpl(text);
    }

    @Override
    public ProgressBar _Components_progressBar() {
        return new DesktopProgressBarImpl();
    }


    @Override
    public IntBox _Components_intBox(int value) {
        return new DesktopIntBoxImpl(value);
    }

    @Override
    public IntSlider _Components_intSlider(int min, int max, int value) {
        return new DesktopIntSliderImpl(min, max, value);
    }

    @Override
    public <E> ListBox<E> _Components_listBox(FlatDataModel<E> model) {
        return new DesktopListBoxImpl<>(model);
    }

    @Override
    public <E> MultiSelectListBox<E> _Components_multiSelectListBox(FlatDataModel<E> model) {
        return new DesktopMultiSelectListBoxImpl<>(model);
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
    public Hyperlink _Components_hyperlink(LocalizeValue text) {
        return new DesktopHyperlinkImpl(text);
    }

    @Override
    public Separator _Separator_create(SeparatorStyle style) {
        return new DesktopSeparatorImpl(style);
    }

    @Override
    public HorizontalLayout _Layouts_horizontal(Space gapInPixels) {
        return new DesktopHorizontalLayoutImpl(gapInPixels);
    }

    @Override
    public ImageBox _Components_imageBox(Image image) {
        return new DesktopImageBoxImpl(image);
    }

    @Override
    public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
        return new DesktopColorBoxImpl(colorValue);
    }

    @Override
    public FontBox _Components_fontBox() {
        return new DesktopFontBoxImpl();
    }

    @Override
    public ColorPickerBuilder _ColorPicker_create() {
        return new DesktopAWTColorPickerBuilder();
    }

    @Override
    @RequiredUIAccess
    public DelayedAction _DelayedAction_start(ComponentEvent<?> anchor) {
        java.awt.Component component = TargetAWT.to(anchor.getComponent());
        InputDetails details = anchor.getInputDetails();
        if (details instanceof ProgrammaticInputDetails) {
            return DesktopDelayedActionImpl.start(component, 0, 0);
        }
        return DesktopDelayedActionImpl.startOnScreen(component, details.getXOnScreen(), details.getYOnScreen());
    }

    @Override
    @RequiredUIAccess
    public DelayedAction _DelayedAction_start(Component target, int relativeX, int relativeY) {
        return DesktopDelayedActionImpl.start(TargetAWT.to(target), relativeX, relativeY);
    }

    @Override
    @RequiredUIAccess
    public UIAccess _UIAccess_get() {
        return AWTUIAccessImpl.ourInstance;
    }

    @Override
    public Collection<UIAccess> _UIAccess_all() {
        return List.of(AWTUIAccessImpl.ourInstance);
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
    public TextBoxWithExpandAction _Components_textBoxWithExpandAction(
        Image editButtonImage,
        String dialogTitle,
        Function<String, List<String>> parser,
        Function<List<String>, String> joiner
    ) {
        return DesktopTextBoxWithExpandAction.create(editButtonImage, dialogTitle, parser, joiner);
    }

    @Override
    public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
        return new DesktopTextBoxWithExtensions.Supported(text);
    }

    @Override
    public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
        return new DesktopFoldoutLayoutImpl(titleValue, component, show);
    }

    @Override
    public <S> Image _Image_stated(ImageState<S> state, Function<S, Image> funcCall) {
        return new DesktopStatedImageImpl<>(state, funcCall);
    }
    @Override
    public IconLibraryManager _IconLibraryManager_get() {
        return DesktopIconLibraryManagerImpl.ourInstance;
    }

    @Override
    public ImageKey _ImageKey_of(String groupId, String imageId, int width, int height) {
        return new DesktopAWTImageKey(null, groupId, imageId, width, height);
    }

    @Override
    public TaskBar _TaskBar_get() {
        return DesktopTaskBarImpl.ourInstance;
    }

    @Override
    public FocusManager _FocusManager_get() {
        return DesktopAWTFocusManager.ourInstance;
    }

    @Override
    public <T> Table<T> _Table_create(FlatDataModel<T> model) {
        return new DesktopTableImpl<>(model);
    }

    @Override
    public ToggleSwitch _Components_toggleSwitch(boolean selected) {
        return new DesktopToggleSwitchImpl(selected);
    }

    @Override
    public PasswordBox _Components_passwordBox(@Nullable String passwordText) {
        return new DesktopPasswordBoxImpl(passwordText);
    }

    @Override
    public void _ShowNotifier_once(Component component, Runnable action) {
        java.awt.Component awtComponent = TargetAWT.to(component);

        UiNotifyConnector.doWhenFirstShown(awtComponent, action);
    }

    @Override
    public AdvancedLabel _Components_advancedLabel() {
        return new DesktopAdvancedLabelImpl();
    }

    @Override
    public <E> Graph<E> _Components_graph(GraphModel<E> model) {
        return new DesktopAWTGraphImpl<>(model);
    }

    @Override
    public ImageViewer _Components_imageViewer() {
        return new DesktopAWTImageViewerImpl();
    }

    @Override
    public HtmlView _Components_htmlView() {
        return new DesktopAWTHtmlViewImpl();
    }

    @Override
    public <L extends Layout> LoadingLayout<L> _Layouts_LoadingLayout(L innerLayout, Disposable parent) {
        return new DesktopAWTLoadingLayout<>(innerLayout, parent);
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
    public DatePicker _Components_datePicker(@Nullable String datePattern) {
        return new DesktopDatePickerImpl(datePattern);
    }

    @Override
    public <V> RadioGroup<V> _Components_radioGroup() {
        return new DesktopRadioGroupImpl<>();
    }


    @Override
    public <T> MessageBoxBuilder<T> _MessageBox_create() {
        return new DesktopMessageBoxBuilderImpl<>();
    }

    @Override
    public InputBoxBuilder<String, TextBox> _InputBox_text() {
        return new DesktopInputBoxBuilderImpl<>(TextBox::create);
    }

    @Override
    public InputBoxBuilder<Integer, IntBox> _InputBox_integer() {
        return new DesktopInputBoxBuilderImpl<>(IntBox::create);
    }

    @Override
    public <V> InputBoxBuilder<V, ComboBox<V>> _InputBox_items(Collection<? extends V> items) {
        List<V> copy = new ArrayList<>(items);
        return new DesktopInputBoxBuilderImpl<>(() -> ComboBox.create(copy));
    }

    @Override
    public InputBoxBuilder<String, PasswordBox> _InputBox_password() {
        return new DesktopInputBoxBuilderImpl<>(PasswordBox::create);
    }

    @Override
    public void _AnAction_registerCustomShortcutSet(AnAction action, Component component) {
        if (TargetAWT.to(component) instanceof JComponent jComponent) {
            action.registerCustomShortcutSet(jComponent, null);
        }
    }

    @Override
    public void _AnAction_unregisterCustomShortcutSet(AnAction action, Component component) {
        if (TargetAWT.to(component) instanceof JComponent jComponent) {
            action.unregisterCustomShortcutSet(jComponent);
        }
    }
}
