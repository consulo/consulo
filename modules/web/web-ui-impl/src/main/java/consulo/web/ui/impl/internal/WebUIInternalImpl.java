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
package consulo.web.ui.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinSession;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.AdvancedLabel;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.EmptyImage;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.ImageState;
import consulo.ui.ex.impl.internal.UnifiedAlertImpl;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.DummyTaskBarImpl;
import consulo.ui.impl.model.FlatDataModelImpl;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.FlatDataModel;
import consulo.ui.impl.model.LazyFlatDataModelImpl;
import consulo.ui.model.LazyFlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import consulo.web.ui.impl.internal.WebApplicationImpl;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebShowNotifier;
import consulo.web.ui.impl.internal.htmlView.WebHtmlViewImpl;
import consulo.web.ui.impl.internal.image.*;
import org.jspecify.annotations.NullMarked;
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
 * @since 2016-06-11
 */
@NullMarked
public class WebUIInternalImpl extends UIInternal {
    private final DummyTaskBarImpl myTaskBar = new DummyTaskBarImpl();

    @Override
    public CheckBox _Components_checkBox() {
        return new WebCheckBoxImpl();
    }

    @Override
    public TriStateCheckBox _Components_triStateCheckBox() {
        return new WebTriStateCheckBoxImpl();
    }

    @Override
    public DockLayout _Layouts_dock(int gapInPixels) {
        return new WebDockLayoutImpl(gapInPixels);
    }

    @Override
    public WrappedLayout _Layouts_wrapped() {
        return new WebWrappedLayoutImpl();
    }

    @Override
    public VerticalLayout _Layouts_vertical(int vGap) {
        return new WebVerticalLayoutImpl(vGap);
    }

    @Override
    public VerticalLayout _Layouts_vertical(int vGap, HorizontalAlignment alignment) {
        return new WebVerticalLayoutImpl(vGap, alignment);
    }

    @Override
    public SwipeLayout _Layouts_swipe() {
        return new WebSwipeLayoutImpl();
    }

    @Override
    public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
        return new WebHorizontalTwoComponentSplitLayoutImpl(position);
    }

    @Override
    public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
        return new WebThreeComponentSplitLayoutImpl(position);
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
    public <L extends Layout> LoadingLayout<L> _Layouts_LoadingLayout(L innerLayout, Disposable parent) {
        return new WebLoadingLayoutImpl<>(innerLayout, parent);
    }

    @Override
    public ScrollableLayout _ScrollLayout_create(Component component, ScrollableLayoutOptions options) {
        return new WebScrollLayoutImpl(component, options);
    }

    @Override
    public Label _Components_label(LocalizeValue text, LabelOptions options) {
        return new WebLabelImpl(text, options);
    }

    @Override
    public AdvancedLabel _Components_advancedLabel() {
        return new WebAdvancedLabelImpl();
    }

    @Override
    public HtmlView _Components_htmlView() {
        return new WebHtmlViewImpl();
    }

    @Override
    public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions labelOptions) {
        return new WebHtmlLabelImpl(html, labelOptions);
    }

    @Override
    public <E> ComboBox<E> _Components_comboBox(FlatDataModel<E> model) {
        return new WebComboBoxImpl<>(model);
    }

    @Override
    @RequiredUIAccess
    public TextBox _Components_textBox(String text) {
        return new WebTextBoxImpl(text);
    }

    @Override
    @RequiredUIAccess
    public TextBoxWithHistory _Components_textBoxWithHistory(String text) {
        return new WebTextBoxWithHistoryImpl(text);
    }

    @Override
    public ProgressBar _Components_progressBar() {
        return new WebProgressBarImpl();
    }

    @Override
    @RequiredUIAccess
    public IntBox _Components_intBox(int value) {
        return new WebIntBoxImpl(value);
    }

    @Override
    public <E> ListBox<E> _Components_listBox(FlatDataModel<E> model) {
        // a model which says it may be read a page at a time gets the pooled list - the rows are made once and
        // rebound, so a model which changes on every typed character costs what actually differs rather than a tree
        // of components per visible line
        if (model instanceof LazyFlatDataModel) {
            return new WebPooledListBoxImpl<>(model);
        }

        return new WebListBoxImpl<>(model);
    }

    @Override
    @RequiredUIAccess
    public RadioButton _Components_radioButton(LocalizeValue text, boolean selected) {
        return new WebRadioButtonImpl(selected, text);
    }

    @Override
    public Button _Components_button(LocalizeValue text) {
        return new WebButtonImpl(text);
    }

    @Override
    public ToggleButton _Components_toggleButton(LocalizeValue text) {
        return new WebToggleButtonImpl(text);
    }

    @Override
    @RequiredUIAccess
    public Hyperlink _Components_hyperlink(LocalizeValue text) {
        WebHyperlinkImpl hyperlink = new WebHyperlinkImpl();
        hyperlink.setText(text);
        return hyperlink;
    }

    @Override
    public HorizontalLayout _Layouts_horizontal(int gapInPixesl) {
        return new WebHorizontalLayoutImpl(gapInPixesl);
    }

    @Override
    public ImageBox _Components_imageBox(Image image) {
        return new WebImageBoxImpl(image);
    }

    @Override
    public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
        return new WebColorBoxImpl(colorValue);
    }

    @Override
    public ColorPickerBuilder _ColorPicker_create() {
        return new WebColorPickerBuilderImpl();
    }

    @Override
    public <E> Tree<E> _Components_tree(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return new WebTreeImpl<>(rootValue, model, executor);
    }

    @Override
    public Image _Image_fromUrl(URL url) throws IOException {
        return new WebImageImpl(url);
    }

    /**
     * Without this a plugin icon - the only image the platform hands over as bytes rather than by an id - threw
     * out of {@link Image#fromStream}, and every plugin was drawn with the stand-in icon of the caller.
     */
    @Override
    public Image _Image_fromStream(Image.ImageType imageType, InputStream stream) throws IOException {
        return new WebBytesImageImpl(imageType, stream.readAllBytes());
    }

    @Override
    public Image _Image_lazy(Supplier<Image> imageSupplier) {
        return new WebLazyImageImpl(imageSupplier);
    }

    @Override
    public <S> Image _Image_stated(ImageState<S> state, Function<S, Image> funcCall) {
        return new WebStatedImageImpl<>(state, funcCall);
    }

    @Override
    public Image _ImageEffects_layered(Image[] images) {
        return new WebLayeredImageImpl(images);
    }

    @Override
    public Image _ImageEffects_transparent(Image original, float alpha) {
        return new WebTransparentImageImpl(original, alpha);
    }

    @Override
    public Image _ImageEffects_grayed(Image original) {
        if (original instanceof WebGrayedImageImpl) {
            return original;
        }
        return new WebGrayedImageImpl(original);
    }

    @Override
    public Image _ImageEffects_appendRight(Image i0, Image i1) {
        return new WebAppendImageImpl(i0, i1);
    }

    @Override
    public EmptyImage _ImageEffects_empty(int width, int height) {
        return new WebEmptyImageImpl(width, height);
    }

    @Override
    public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
        return new WebCanvasImageImpl(width, height, consumer);
    }

    @Override
    public Image _ImageEffects_withText(Image baseImage, String text) {
        return new WebTextImageImpl(baseImage, text);
    }

    @Override
    public Image _ImageEffects_colorize(Image baseImage, ColorValue colorValue) {
        return new WebColorizeImageImpl(baseImage, colorValue);
    }

    @Override
    public Image _ImageEffects_resize(Image original, int width, int height) {
        return new WebResizeImageImpl(original, width, height);
    }

    @Override
    public MenuItem _MenuItem_create(LocalizeValue text) {
        return new WebMenuItemImpl(text);
    }

    @Override
    public Menu _Menu_create(LocalizeValue text) {
        return new WebMenuImpl(text);
    }

    @Override
    public MenuSeparator _MenuSeparator_create() {
        return new WebMenuSeparatorImpl();
    }

    @Override
    public Separator _Separator_create(SeparatorStyle style) {
        return new WebSeparatorImpl(style);
    }

    @Override
    public ValueGroup<Boolean> _ValueGroups_boolGroup() {
        return new ValueGroup<>() {
            @Override
            @RequiredUIAccess
            public void clearValues() {
            }

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

    @Override
    public StyleManager _StyleManager_get() {
        return WebStyleManagerImpl.ourInstance;
    }

    @Override
    public FontManager _FontManager_get() {
        return WebFontManagerImpl.ourInstance;
    }

    @Override
    @RequiredUIAccess
    public Window _Window_create(String title, WindowOptions options) {
        WebWindowImpl window = new WebWindowImpl(options.isModal(), options);
        window.setTitle(title);
        return window;
    }

    @Override
    @RequiredUIAccess
    public LightPopup _LightPopup_create(PopupOptions options) {
        return new WebLightPopupImpl(options);
    }

    @Override
    public HeavyPopup _HeavyPopup_create(PopupOptions options) {
        return new WebHeavyPopupImpl(options);
    }

    @Override
    public @Nullable Window _Window_getActiveWindow() {
        return null;
    }

    @Override
    public @Nullable Window _Window_getFocusedWindow() {
        return null;
    }
    @Override
    public <T> Alert<T> _Alerts_create() {
        return new UnifiedAlertImpl<>();
    }

    @Override
    public <T> MutableFlatDataModel<T> _FlatDataModel_create(Collection<? extends T> list) {
        return new FlatDataModelImpl<>(list);
    }

    @Override
    public <T> MutableFlatDataModel<T> _FlatDataModel_createLazy(Collection<? extends T> list) {
        return new LazyFlatDataModelImpl<>(list);
    }

    @Override
    @RequiredUIAccess
    public DelayedAction _DelayedAction_start(ComponentEvent<?> anchor) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return () -> {
            };
        }

        InputDetails details = anchor.getInputDetails();

        ui.getElement().executeJs(
            "if (!document.getElementById('consulo-delayed-action-style')) {" +
                "  const style = document.createElement('style');" +
                "  style.id = 'consulo-delayed-action-style';" +
                "  style.textContent = '@keyframes consulo-delayed-action-spin { to { transform: rotate(360deg); } }';" +
                "  document.head.appendChild(style);" +
                "}"
        );

        Div spinner = new Div();
        spinner.getStyle()
            .set("position", "fixed")
            .set("width", "20px")
            .set("height", "20px")
            .set("left", (details.getXOnScreen() - 10) + "px")
            .set("top", (details.getYOnScreen() - 10) + "px")
            .set("border", "2px solid var(--lumo-contrast-20pct)")
            .set("border-top-color", "var(--lumo-primary-color)")
            .set("border-radius", "50%")
            .set("animation", "consulo-delayed-action-spin 0.8s linear infinite")
            .set("pointer-events", "none")
            .set("z-index", "1000");

        ui.add(spinner);

        return () -> ui.remove(spinner);
    }

    @Override
    @RequiredUIAccess
    public UIAccess _UIAccess_get() {
        UI ui = UI.getCurrent();
        assert ui != null;
        return VaadinComponentDelegate.getUIAccess(ui);
    }

    @Override
    public boolean _UIAccess_supportsMultipleUI() {
        return true;
    }

    @Override
    public Collection<UIAccess> _UIAccess_all() {
        Application application = ApplicationManager.getApplication();
        return application instanceof WebApplicationImpl webApplication ? webApplication.getUIAccesses() : List.of();
    }

    @Override
    public boolean _UIAccess_isUIThread() {
        if (UI.getCurrent() != null) {
            return true;
        }

        // vaadin sets the ui current instance only inside ui.access(), plain uidl request handling just
        // holds the session lock - both are the single threaded ui state, like the awt event dispatch thread
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && session.hasLock();
    }

    @Override
    public IconLibraryManager _IconLibraryManager_get() {
        return WebIconLibraryManagerImpl.ourInstance;
    }

    @Override
    public ImageKey _ImageKey_of(String groupId, String imageId, int width, int height) {
        return new WebImageKeyImpl(groupId, imageId, width, height);
    }

    @Override
    public TextBoxWithExpandAction _Components_textBoxWithExpandAction(
        @Nullable Image editButtonImage,
        String dialogTitle,
        Function<String, List<String>> parser,
        Function<List<String>, String> joiner
    ) {
        return new WebTextBoxWithExpandActionImpl(editButtonImage, dialogTitle, parser, joiner);
    }

    @Override
    public TextBoxWithExtensions _Components_textBoxWithExtensions(@Nullable String text) {
        return new WebTextBoxWithExtensionsImpl(text);
    }

    @Override
    public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
        return new WebFoldoutLayoutImpl(titleValue, component, show);
    }

    @Override
    public ToggleSwitch _Components_toggleSwitch(boolean selected) {
        return new WebToggleSwitchImpl(selected);
    }

    @Override
    public PasswordBox _Components_passwordBox(@Nullable String passwordText) {
        return new WebPasswordBoxImpl(StringUtil.notNullize(passwordText));
    }

    @Override
    public <T> Table<T> _Table_create(FlatDataModel<T> model) {
        return new WebTableImpl<>(model);
    }

    @Override
    public IntSlider _Components_intSlider(int min, int max, int value) {
        return new WebIntSliderImpl(min, max, value);
    }

    @Override
    public DatePicker _Components_datePicker(@Nullable String datePattern) {
        return new WebDatePickerImpl(datePattern);
    }

    @Override
    public FocusManager _FocusManager_get() {
        return WebFocusManagerImpl.ourInstance;
    }

    @Override
    public TaskBar _TaskBar_get() {
        return myTaskBar;
    }

    @Override
    public PopupMenu _PopupMenu_create(Component target) {
        return new WebPopupMenuImpl(target);
    }

    @Override
    public void _ShowNotifier_once(Component component, Runnable action) {
        WebShowNotifier.once(component, action);
    }

    @Override
    public ModalityState _ModalityState_any() {
        return WebModalityState.INSTANCE;
    }

    @Override
    public ModalityState _ModalityState_nonModal() {
        return WebModalityState.INSTANCE;
    }

    private RuntimeException notSupported() {
        return new UnsupportedOperationException();
    }
}
